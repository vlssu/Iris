package net.coderbot.iris.shaderpack.rendergraph.lowering;

import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;
import net.coderbot.iris.shaderpack.rendergraph.TextureSize;

public class ColorTargets {
	private TextureSize[] sizes;
	private TextureHandle[] altTargets;
	private TextureHandle[] mainTargets;

	public ColorTargets(String name, int size) {
		sizes = new TextureSize[size];
		altTargets = new TextureHandle[size];
		mainTargets = new TextureHandle[size];

		for (int i = 0; i < size; i++) {
			sizes[i] = new TextureSize();
			altTargets[i] = new TextureHandle(name + i + ":alt");
			mainTargets[i] = new TextureHandle(name + i + ":main");
		}
	}

	public int numColorTargets() {
		return sizes.length;
	}

	public TextureHandle get(int index, boolean alt) {
		if (alt) {
			return altTargets[index];
		} else {
			return mainTargets[index];
		}
	}

	public TextureSize getSize(int index) {
		return sizes[index];
	}
}
