package net.coderbot.iris.shaderpack.rendergraph.lowering;

import java.util.Arrays;

/**
 * Stores the mipmap enable states for a given {@link ColorTargets}.
 */
public class ColorTargetMipmapping {
	private final boolean[] altMipmapped;
	private final boolean[] mainMipmapped;

	public ColorTargetMipmapping(int numTargets) {
		this.altMipmapped = new boolean[numTargets];
		this.mainMipmapped = new boolean[numTargets];
	}

	private ColorTargetMipmapping(ColorTargetMipmapping from) {
		this.altMipmapped = Arrays.copyOf(from.altMipmapped, from.altMipmapped.length);
		this.mainMipmapped = Arrays.copyOf(from.mainMipmapped, from.mainMipmapped.length);
	}

	public ColorTargetMipmapping snapshot() {
		return new ColorTargetMipmapping(this);
	}

	public void enableMipmapping(int target, boolean alt) {
		if (alt) {
			enableAltMipmapping(target);
		} else {
			enableMainMipmapping(target);
		}
	}

	public void enableAltMipmapping(int target) {
		altMipmapped[target] = true;
	}

	public void enableMainMipmapping(int target) {
		mainMipmapped[target] = true;
	}

	public boolean isMipmapped(int target, boolean alt) {
		if (alt) {
			return isAltMipmapped(target);
		} else {
			return isMainMipmapped(target);
		}
	}

	public boolean isAltMipmapped(int target) {
		return altMipmapped[target];
	}

	public boolean isMainMipmapped(int target) {
		return mainMipmapped[target];
	}
}
