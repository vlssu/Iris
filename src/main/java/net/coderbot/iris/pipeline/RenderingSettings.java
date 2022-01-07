package net.coderbot.iris.pipeline;

import net.coderbot.iris.shaderpack.PackDirectives;
import net.coderbot.iris.shaderpack.PackShadowDirectives;

import java.util.OptionalInt;

public class RenderingSettings {
	public static final RenderingSettings DEFAULT = new RenderingSettings();

	private final float sunPathRotation;
	private final boolean shouldRenderClouds;
	private final boolean shouldRenderUnderwaterOverlay;
	private final boolean shouldRenderVignette;
	private final boolean shouldWriteRainAndSnowToDepthBuffer;
	private final boolean shouldRenderParticlesBeforeDeferred;
	private final OptionalInt forcedShadowRenderDistanceChunks;

	private RenderingSettings() {
		sunPathRotation = 0.0F;
		shouldRenderClouds = true;
		shouldRenderUnderwaterOverlay = true;
		shouldRenderVignette = true;
		shouldWriteRainAndSnowToDepthBuffer = false;
		shouldRenderParticlesBeforeDeferred = false;
		forcedShadowRenderDistanceChunks = OptionalInt.empty();
	}

	public RenderingSettings(PackDirectives packDirectives) {
		this.sunPathRotation = packDirectives.getSunPathRotation();
		this.shouldRenderClouds = packDirectives.areCloudsEnabled();
		this.shouldRenderUnderwaterOverlay = packDirectives.underwaterOverlay();
		this.shouldRenderVignette = packDirectives.vignette();
		this.shouldWriteRainAndSnowToDepthBuffer = packDirectives.rainDepth();
		this.shouldRenderParticlesBeforeDeferred = packDirectives.areParticlesBeforeDeferred();

		PackShadowDirectives shadowDirectives = packDirectives.getShadowDirectives();

		if (shadowDirectives.isDistanceRenderMulExplicit()) {
			if (shadowDirectives.getDistanceRenderMul() >= 0.0) {
				// add 15 and then divide by 16 to ensure we're rounding up
				forcedShadowRenderDistanceChunks =
						OptionalInt.of(((int) (shadowDirectives.getDistance() * shadowDirectives.getDistanceRenderMul()) + 15) / 16);
			} else {
				forcedShadowRenderDistanceChunks = OptionalInt.of(-1);
			}
		} else {
			forcedShadowRenderDistanceChunks = OptionalInt.empty();
		}
	}

	public float getSunPathRotation() {
		return sunPathRotation;
	}

	public boolean shouldRenderClouds() {
		return shouldRenderClouds;
	}

	public boolean shouldRenderUnderwaterOverlay() {
		return shouldRenderUnderwaterOverlay;
	}

	public boolean shouldRenderVignette() {
		return shouldRenderVignette;
	}

	public boolean shouldWriteRainAndSnowToDepthBuffer() {
		return shouldWriteRainAndSnowToDepthBuffer;
	}

	public boolean shouldRenderParticlesBeforeDeferred() {
		return shouldRenderParticlesBeforeDeferred;
	}

	public OptionalInt getForcedShadowRenderDistanceChunksForDisplay() {
		return forcedShadowRenderDistanceChunks;
	}
}
