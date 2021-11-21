package net.coderbot.iris.shadows;

import com.mojang.blaze3d.systems.RenderSystem;
import net.coderbot.iris.gl.framebuffer.GlFramebuffer;
import net.coderbot.iris.gl.texture.InternalTextureFormat;
import net.coderbot.iris.gl.texture.Texture2dCommands;
import net.coderbot.iris.rendertarget.DepthTexture;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;

import java.nio.ByteBuffer;

public class ShadowRenderTargets {
	// TODO: Make this match the value of GL_MAX_DRAW_BUFFERS (or whatever property name it is)
	public static int MAX_SHADOW_RENDER_TARGETS = 8;

	private final int[] targets;

	private final DepthTexture depthTexture;
	private final DepthTexture noTranslucents;

	private final GlFramebuffer framebuffer;

	private static final ByteBuffer NULL_BUFFER = null;

	public ShadowRenderTargets(int resolution, InternalTextureFormat[] formats) {
		if (formats.length > MAX_SHADOW_RENDER_TARGETS) {
			throw new IllegalStateException("Too many shadow render targets, requested " + formats.length +
					" but only " + MAX_SHADOW_RENDER_TARGETS + " are allowed.");
		}

		Texture2dCommands texture2dCommands = null;

		int[] drawBuffers = new int[formats.length];

		targets = new int[formats.length];
		texture2dCommands.createHandles(targets);

		depthTexture = new DepthTexture(resolution, resolution);
		noTranslucents = new DepthTexture(resolution, resolution);

		this.framebuffer = new GlFramebuffer();

		framebuffer.addDepthAttachment(depthTexture.getTextureId());

		for (int i = 0; i < formats.length; i++) {
			InternalTextureFormat format = formats[i];

			// RenderSystem.bindTexture(targets[i]);

			final int handle = targets[i];

			texture2dCommands.allocate(handle, 0, format, resolution, resolution, TODO, TODO);
			texture2dCommands.textureParameter(handle, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
			texture2dCommands.textureParameter(handle, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
			texture2dCommands.textureParameter(handle, GL11C.GL_TEXTURE_WRAP_S, GL13C.GL_CLAMP_TO_BORDER);
			texture2dCommands.textureParameter(handle, GL11C.GL_TEXTURE_WRAP_T, GL13C.GL_CLAMP_TO_BORDER);

			// TODO: GL calls
			framebuffer.addColorAttachment(i, targets[i]);
			drawBuffers[i] = i;
		}

		framebuffer.drawBuffers(drawBuffers);

		RenderSystem.bindTexture(0);
	}

	public GlFramebuffer getFramebuffer() {
		return framebuffer;
	}

	public DepthTexture getDepthTexture() {
		return depthTexture;
	}

	public DepthTexture getDepthTextureNoTranslucents() {
		return noTranslucents;
	}

	public int getColorTextureId(int index) {
		return targets[index];
	}

	public void destroy() {
		framebuffer.destroy();

		GL20C.glDeleteTextures(targets);
		depthTexture.destroy();
		noTranslucents.destroy();
	}
}
