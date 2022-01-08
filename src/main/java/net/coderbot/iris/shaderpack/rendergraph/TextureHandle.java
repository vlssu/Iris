package net.coderbot.iris.shaderpack.rendergraph;

import java.util.Objects;

public class TextureHandle {
	private String debugName;

	public TextureHandle(String debugName) {
		this.debugName = debugName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TextureHandle that = (TextureHandle) o;
		return Objects.equals(debugName, that.debugName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(debugName);
	}

	public String toString() {
		return "TextureHandle(" + debugName + ")";
	}
}
