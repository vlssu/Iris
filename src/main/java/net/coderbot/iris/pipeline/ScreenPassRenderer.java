package net.coderbot.iris.pipeline;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.coderbot.iris.gl.IrisRenderSystem;
import net.coderbot.iris.gl.framebuffer.GlFramebuffer;
import net.coderbot.iris.gl.program.Program;
import net.coderbot.iris.gl.program.ProgramBuilder;
import net.coderbot.iris.gl.sampler.SamplerLimits;
import net.coderbot.iris.gl.texture.InternalTextureFormat;
import net.coderbot.iris.gl.uniform.UniformUpdateFrequency;
import net.coderbot.iris.postprocess.CenterDepthSampler;
import net.coderbot.iris.postprocess.FullScreenQuadRenderer;
import net.coderbot.iris.samplers.IrisSamplers;
import net.coderbot.iris.shaderpack.ProgramSource;
import net.coderbot.iris.shaderpack.rendergraph.ColorAttachments;
import net.coderbot.iris.shaderpack.rendergraph.ImageBinding;
import net.coderbot.iris.shaderpack.rendergraph.TextureFilteringMode;
import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;
import net.coderbot.iris.shaderpack.rendergraph.pass.GenerateMipmapPassInfo;
import net.coderbot.iris.shaderpack.rendergraph.pass.PassInfo;
import net.coderbot.iris.shaderpack.rendergraph.pass.ScreenRenderPassInfo;
import net.coderbot.iris.shaderpack.rendergraph.pass.SetTextureMinFilteringPassInfo;
import net.coderbot.iris.uniforms.CommonUniforms;
import net.coderbot.iris.uniforms.FrameUpdateNotifier;
import net.coderbot.iris.vendored.joml.Vector2f;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;

public class ScreenPassRenderer {
	private final ImmutableList<Pass> passes;
	private final FrameUpdateNotifier updateNotifier;
	private final CenterDepthSampler centerDepthSampler;

	public ScreenPassRenderer(Map<TextureHandle, IntSupplier> textureIDs,
							  Map<TextureHandle, InternalTextureFormat> textureFormats,
							  FrameUpdateNotifier updateNotifier,
							  CenterDepthSampler centerDepthSampler,
							  List<PassInfo> passInfoList) {
		this.updateNotifier = updateNotifier;
		this.centerDepthSampler = centerDepthSampler;

		final ImmutableList.Builder<Pass> passes = ImmutableList.builder();

		for (PassInfo passInfoAny : passInfoList) {
			if (passInfoAny instanceof ScreenRenderPassInfo) {
				ScreenRenderPassInfo passInfo = (ScreenRenderPassInfo) passInfoAny;
				ScreenRenderPass pass = new ScreenRenderPass();

				ProgramSource source = passInfo.getSource();

				pass.program = createProgram(source, textureIDs, textureFormats, passInfo.getDefaultSamplerName(),
						passInfo.getSamplers(), passInfo.getImages());

				// Initialize framebuffer.
				ColorAttachments attachments = passInfo.getAttachmentsByParity()[0];
				TextureHandle[] colorAttachments = attachments.getTextures();
				int[] drawBuffers = new int[colorAttachments.length];

				GlFramebuffer framebuffer = new GlFramebuffer();

				for (int i = 0; i < colorAttachments.length; i++) {
					// TODO: Shouldn't use IntSupplier here, it doesn't give us enough info to tell if it will change or not.
					framebuffer.addColorAttachment(i, textureIDs.get(colorAttachments[i]).getAsInt());
					drawBuffers[i] = i;
				}

				framebuffer.drawBuffers(drawBuffers);

				pass.framebuffer = framebuffer;
				pass.viewportScale = passInfo.getViewportScale();

				passes.add(pass);
			} else if (passInfoAny instanceof GenerateMipmapPassInfo) {
				GenerateMipmapPassInfo passInfo = ((GenerateMipmapPassInfo) passInfoAny);

				passes.add(new GenerateMipmapPass(textureIDs.get(passInfo.getTarget()[0])));
			} else if (passInfoAny instanceof SetTextureMinFilteringPassInfo) {
				SetTextureMinFilteringPassInfo passInfo = ((SetTextureMinFilteringPassInfo) passInfoAny);
				TextureFilteringMode filteringMode = passInfo.getFilteringMode();
				int mode;

				if (filteringMode == TextureFilteringMode.LINEAR) {
					mode = GL20C.GL_LINEAR;
				} else if (filteringMode == TextureFilteringMode.NEAREST) {
					mode = GL20C.GL_NEAREST;
				} else if (filteringMode == TextureFilteringMode.LINEAR_MIPMAP_LINEAR) {
					mode = GL20C.GL_LINEAR_MIPMAP_LINEAR;
				} else {
					throw new IllegalArgumentException("Unknown texture filtering mode " + filteringMode);
				}

				passes.add(new SetTextureMinFilteringPass(textureIDs.get(passInfo.getTarget()[0]), mode));
			}
		}

		this.passes = passes.build();

		GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
	}

	private interface Pass {
		void run(int baseWidth, int baseHeight);
		default void destroy() {}
	}

	private static final class GenerateMipmapPass implements Pass {
		private final IntSupplier target;

		public GenerateMipmapPass(IntSupplier target) {
			this.target = target;
		}

		@Override
		public void run(int baseWidth, int baseHeight) {
			RenderSystem.activeTexture(GL20C.GL_TEXTURE0);
			RenderSystem.bindTexture(target.getAsInt());
			IrisRenderSystem.generateMipmaps(GL20C.GL_TEXTURE_2D);
		}
	}

	private static final class SetTextureMinFilteringPass implements Pass {
		private final IntSupplier target;
		private final int mode;

		public SetTextureMinFilteringPass(IntSupplier target, int mode) {
			this.target = target;
			this.mode = mode;
		}

		@Override
		public void run(int baseWidth, int baseHeight) {
			RenderSystem.activeTexture(GL20C.GL_TEXTURE0);
			RenderSystem.bindTexture(target.getAsInt());
			RenderSystem.texParameter(GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, mode);
		}
	}

	private static final class ScreenRenderPass implements Pass {
		Program program;
		GlFramebuffer framebuffer;
		Vector2f viewportScale;

		@Override
		public void destroy() {
			this.program.destroy();
		}

		@Override
		public void run(int baseWidth, int baseHeight) {
			float scaledWidth = baseWidth * viewportScale.x;
			float scaledHeight = baseHeight * viewportScale.y;
			RenderSystem.viewport(0, 0, (int) scaledWidth, (int) scaledHeight);

			framebuffer.bind();
			program.use();

			FullScreenQuadRenderer.INSTANCE.renderQuad();
		}
	}

	// we have to call the alpha test here even though it's deprecated.
	@SuppressWarnings("deprecation")
	public void renderAll() {
		RenderSystem.disableBlend();
		RenderSystem.disableAlphaTest();

		final RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		final int baseWidth = main.width;
		final int baseHeight = main.height;

		FullScreenQuadRenderer.INSTANCE.begin();

		for (Pass renderPass : passes) {
			renderPass.run(baseWidth, baseHeight);
		}

		FullScreenQuadRenderer.end();

		// Make sure to reset the viewport to how it was before... Otherwise, weird issues could occur.
		// Also bind the "main" framebuffer if it isn't already bound.
		main.bindWrite(true);
		GlStateManager._glUseProgram(0);

		// NB: Unbinding all of these textures is necessary for proper shader pack reloading.
		for (int i = 0; i < SamplerLimits.get().getMaxTextureUnits(); i++) {
			// Unbind all textures that we may have used.
			// NB: This is necessary for shader pack reloading to work properly
			RenderSystem.activeTexture(GL15C.GL_TEXTURE0 + i);
			RenderSystem.bindTexture(0);
		}

		RenderSystem.activeTexture(GL15C.GL_TEXTURE0);
	}

	private Program createProgram(ProgramSource source, Map<TextureHandle, IntSupplier> textureIDs,
								  Map<TextureHandle, InternalTextureFormat> textureFormats,
								  String defaultSamplerName,
								  Map<String, TextureHandle[]> samplers, Map<String, ImageBinding[]> images) {
		// TODO: Properly handle empty shaders
		Objects.requireNonNull(source.getVertexSource());
		Objects.requireNonNull(source.getFragmentSource());
		ProgramBuilder builder;

		try {
			builder = ProgramBuilder.begin(source.getName(), source.getVertexSource().orElse(null), source.getGeometrySource().orElse(null),
				source.getFragmentSource().orElse(null), IrisSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
		} catch (RuntimeException e) {
			// TODO: Better error handling
			throw new RuntimeException("Shader compilation failed!", e);
		}

		builder.addDefaultSampler(textureIDs.get(samplers.get(defaultSamplerName)[0]), defaultSamplerName);

		samplers.forEach((name, handles) -> {
			IntSupplier textureID = textureIDs.get(handles[0]);

			if (textureID == null) {
				throw new IllegalStateException("Missing textureID for " + handles[0]);
			}

			if (!name.equals(defaultSamplerName)) {
				builder.addDynamicSampler(textureID, name);
			}
		});

		images.forEach((name, bindings) -> {
			ImageBinding binding = bindings[0];
			IntSupplier textureID = textureIDs.get(binding.getTexture());

			if (textureID == null) {
				throw new IllegalStateException("Missing textureID for " + binding.getTexture());
			}

			InternalTextureFormat format = textureFormats.get(binding.getTexture());

			if (format == null) {
				throw new IllegalStateException("Missing textureFormat for" + binding.getTexture());
			}

			// TODO: Access and mipmap level.
			builder.addTextureImage(textureID, format, name);
		});

		CommonUniforms.addCommonUniforms(builder, source.getParent().getPack().getIdMap(), source.getParent().getPackDirectives(), updateNotifier);

		// TODO: Don't duplicate this with FinalPassRenderer
		// TODO: Parse the value of const float centerDepthSmoothHalflife from the shader pack's fragment shader configuration
		builder.uniform1f(UniformUpdateFrequency.PER_FRAME, "centerDepthSmooth", this.centerDepthSampler::getCenterDepthSmoothSample);

		return builder.build();
	}

	public void destroy() {
		for (Pass renderPass : passes) {
			renderPass.destroy();
		}
	}
}
