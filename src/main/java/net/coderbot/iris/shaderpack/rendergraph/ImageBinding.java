package net.coderbot.iris.shaderpack.rendergraph;

public class ImageBinding {
	TextureHandle texture;
	Access access;
	int mipmapLevel;

	public ImageBinding(TextureHandle texture, Access access, int mipmapLevel) {
		this.texture = texture;
		this.access = access;
		this.mipmapLevel = mipmapLevel;
	}

	public TextureHandle getTexture() {
		return texture;
	}

	public Access getAccess() {
		return access;
	}

	public int getMipmapLevel() {
		return mipmapLevel;
	}

	public enum Access {
		READ_ONLY,
		WRITE_ONLY,
		READ_WRITE
	}
}
