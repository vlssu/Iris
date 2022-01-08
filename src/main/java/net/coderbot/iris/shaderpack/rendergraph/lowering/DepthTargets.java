package net.coderbot.iris.shaderpack.rendergraph.lowering;

import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;
import net.coderbot.iris.shaderpack.rendergraph.TextureSize;

public class DepthTargets {
	private TextureSize size;
	private TextureHandle[] depthTargets;

	public DepthTargets(String name, int size) {
		this.size = new TextureSize();

		depthTargets = new TextureHandle[size];

		for (int i = 0; i < size; i++) {
			depthTargets[i] = new TextureHandle(name + i);
		}
	}

	public int numDepthTargets() {
		return depthTargets.length;
	}

	public TextureHandle get(int index) {
		return depthTargets[index];
	}

	public TextureSize getSize() {
		return size;
	}
}
