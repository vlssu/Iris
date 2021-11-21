package net.coderbot.iris.gl.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.opengl.GL45C;

public interface Texture2dCommands {
	int createHandle();
	void createHandles(int[] handles);
	void allocate(int handle, int level, InternalTextureFormat format, int width, int height, );
	void textureParameter(int handle, int pname, int param);

	class B3dImpl implements Texture2dCommands {
		private static final int GL_TEXTURE_2D = GL21C.GL_TEXTURE_2D;

		private void bind(int handle) {
			RenderSystem.bindTexture(handle);
		}

		@Override
		public int createHandle() {
			return GlStateManager._genTexture();
		}

		@Override
		public void createHandles(int[] handles) {
			GlStateManager._genTextures(handles);
		}

		@Override
		public void allocate(int handle, int level, InternalTextureFormat format, int width, int height) {
			bind(handle);

			// TODO: Check if the InternalTextureFormat is supported on this OpenGL version.

			int pixelFormat;
			int pixelType;

			GlStateManager._texImage2D(GL_TEXTURE_2D, level, format.getGlFormat(), width, height, 0, pixelFormat,
					pixelType, null);
		}

		@Override
		public void textureParameter(int handle, int pname, int param) {
			bind(handle);
			RenderSystem.texParameter(GL_TEXTURE_2D, param, param);
		}
	}
	
	class DsaImpl implements Texture2dCommands {
		@Override
		public int createHandle() {
			return GL45C.glCreateTextures(GL45C.GL_TEXTURE_2D);
		}

		@Override
		public void createHandles(int[] handles) {
			GL45C.glCreateTextures(GL45C.GL_TEXTURE_2D, handles);
		}

		@Override
		public void textureParameter(int handle, int pname, int param) {
			GL45C.glTextureParameteri(handle, pname, param);
		}
	}
}
