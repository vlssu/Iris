package net.coderbot.iris.pipeline;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import net.coderbot.iris.shaderpack.ProgramDirectives;
import net.coderbot.iris.shaderpack.ProgramSource;
import net.coderbot.iris.shaderpack.rendergraph.ColorAttachments;
import net.coderbot.iris.shaderpack.rendergraph.ImageBinding;
import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;
import net.coderbot.iris.shaderpack.rendergraph.pass.ScreenRenderPassInfo;
import net.coderbot.iris.uniforms.CommonUniforms;
import net.coderbot.iris.uniforms.FrameUpdateNotifier;
import net.coderbot.iris.vendored.joml.Vector2f;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

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
							  ScreenRenderPassInfo[] renderPasses) {
		this.updateNotifier = updateNotifier;
		this.centerDepthSampler = centerDepthSampler;

		final ImmutableList.Builder<Pass> passes = ImmutableList.builder();

		for (ScreenRenderPassInfo passInfo : renderPasses) {
			Pass pass = new Pass();
			ProgramSource source = passInfo.getSource();
			ProgramDirectives directives = source.getDirectives();

			pass.program = createProgram(source, textureIDs, textureFormats, passInfo.getSamplers(), passInfo.getImages());

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
			pass.mipmappedBuffers = directives.getMipmappedBuffers();

			passes.add(pass);
		}

		this.passes = passes.build();

		GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
	}

	private static final class Pass {
		Program program;
		GlFramebuffer framebuffer;
		ImmutableSet<Integer> mipmappedBuffers;
		Vector2f viewportScale;

		private void destroy() {
			this.program.destroy();
		}
	}

	public void renderAll() {
		RenderSystem.disableBlend();
		RenderSystem.disableAlphaTest();

		final RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		final int baseWidth = main.width;
		final int baseHeight = main.height;

		FullScreenQuadRenderer.INSTANCE.begin();

		for (Pass renderPass : passes) {
			// TODO: Mipmapping!
			/*if (!renderPass.mipmappedBuffers.isEmpty()) {
				RenderSystem.activeTexture(GL15C.GL_TEXTURE0);

				for (int index : renderPass.mipmappedBuffers) {
					setupMipmapping(renderTargets.get(index), renderPass.stageReadsFromAlt.contains(index));
				}
			}*/

			float scaledWidth = baseWidth * renderPass.viewportScale.x;
			float scaledHeight = baseHeight * renderPass.viewportScale.y;
			RenderSystem.viewport(0, 0, (int) scaledWidth, (int) scaledHeight);

			renderPass.framebuffer.bind();
			renderPass.program.use();

			FullScreenQuadRenderer.INSTANCE.renderQuad();
		}

		FullScreenQuadRenderer.end();

		// Make sure to reset the viewport to how it was before... Otherwise weird issues could occur.
		// Also bind the "main" framebuffer if it isn't already bound.
		main.bindWrite(true);
		GlStateManager._glUseProgram(0);

		// NB: Unbinding all of these textures is necessary for proper shaderpack reloading.
		for (int i = 0; i < SamplerLimits.get().getMaxTextureUnits(); i++) {
			// Unbind all textures that we may have used.
			// NB: This is necessary for shader pack reloading to work propely
			RenderSystem.activeTexture(GL15C.GL_TEXTURE0 + i);
			RenderSystem.bindTexture(0);
		}

		RenderSystem.activeTexture(GL15C.GL_TEXTURE0);
	}

	private static void setupMipmapping(net.coderbot.iris.rendertarget.RenderTarget target, boolean readFromAlt) {
		RenderSystem.bindTexture(readFromAlt ? target.getAltTexture() : target.getMainTexture());

		// TODO: Only generate the mipmap if a valid mipmap hasn't been generated or if we've written to the buffer
		// (since the last mipmap was generated)
		//
		// NB: We leave mipmapping enabled even if the buffer is written to again, this appears to match the
		// behavior of ShadersMod/OptiFine, however I'm not sure if it's desired behavior. It's possible that a
		// program could use mipmapped sampling with a stale mipmap, which probably isn't great. However, the
		// sampling mode is always reset between frames, so this only persists after the first program to use
		// mipmapping on this buffer.
		//
		// Also note that this only applies to one of the two buffers in a render target buffer pair - making it
		// unlikely that this issue occurs in practice with most shader packs.
		IrisRenderSystem.generateMipmaps(GL20C.GL_TEXTURE_2D);
		RenderSystem.texParameter(GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, GL20C.GL_LINEAR_MIPMAP_LINEAR);
	}

	// TODO: Don't just copy this from DeferredWorldRenderingPipeline
	private Program createProgram(ProgramSource source, Map<TextureHandle, IntSupplier> textureIDs,
								  Map<TextureHandle, InternalTextureFormat> textureFormats,
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

		samplers.forEach((name, handles) -> {
			IntSupplier textureID = textureIDs.get(handles[0]);

			if (textureID == null) {
				throw new IllegalStateException("Missing textureID for " + handles[0]);
			}

			builder.addDynamicSampler(textureID, name);
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
		// TODO: Parse the value of const float centerDepthSmoothHalflife from the shaderpack's fragment shader configuration
		builder.uniform1f(UniformUpdateFrequency.PER_FRAME, "centerDepthSmooth", this.centerDepthSampler::getCenterDepthSmoothSample);

		return builder.build();
	}

	public void destroy() {
		for (Pass renderPass : passes) {
			renderPass.destroy();
		}
	}
}
