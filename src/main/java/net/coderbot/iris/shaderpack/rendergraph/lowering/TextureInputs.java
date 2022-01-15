package net.coderbot.iris.shaderpack.rendergraph.lowering;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.coderbot.iris.shaderpack.PackShadowDirectives;
import net.coderbot.iris.shaderpack.rendergraph.ImageBinding;
import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;
import net.coderbot.iris.shaderpack.rendergraph.sampler.EdgeBehavior;
import net.coderbot.iris.shaderpack.rendergraph.sampler.SamplerBinding;
import net.coderbot.iris.shaderpack.rendergraph.sampler.SamplerFiltering;
import net.coderbot.iris.shaderpack.rendergraph.sampler.TextureCompareMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

public class TextureInputs {
	private String defaultSamplerName;
	private final Map<String, SamplerBinding[]> samplers;
	private final Map<String, ImageBinding[]> images;

	public TextureInputs() {
		samplers = new HashMap<>();
		images = new HashMap<>();
	}

	public String getDefaultSamplerName() {
		return defaultSamplerName;
	}

	public Map<String, SamplerBinding[]> getSamplers() {
		return samplers;
	}

	public Map<String, ImageBinding[]> getImages() {
		return images;
	}

	private static SamplerBinding[] applyCustomTextureOverride(SamplerBinding[] base, String[] names, Map<String, SamplerBinding> customTextures) {
		for (String name : names) {
			SamplerBinding customTexture = customTextures.get(name);

			if (customTexture != null) {
				return new SamplerBinding[] { customTexture, customTexture };
			}
		}

		return base;
	}

	private static SamplerBinding[] applyCustomTextureOverride(SamplerBinding base, String[] names, Map<String, SamplerBinding> customTextures) {
		for (String name : names) {
			SamplerBinding customTexture = customTextures.get(name);

			if (customTexture != null) {
				return new SamplerBinding[] { customTexture, customTexture };
			}
		}

		return new SamplerBinding[] { base, base };
	}

	private static SamplerFiltering getFilteringNM(boolean nearest, boolean mipmap) {
		if (mipmap) {
			if (nearest) {
				return SamplerFiltering.NEAREST_MIPMAP_NEAREST;
			} else {
				return SamplerFiltering.LINEAR_MIPMAP_LINEAR;
			}
		} else {
			if (nearest) {
				return SamplerFiltering.NEAREST;
			} else {
				return SamplerFiltering.LINEAR;
			}
		}
	}

	/*
	TODO for gbuffer/shadow:
	 - texture / tex / gtexture
	 - lightmap
	 - normals
	 - specular
	 - don't expose certain depth targets?
	 */

	public void resolveMainColorTargetInputs(ColorTargets colorTargets,
											 Map<String, SamplerBinding> customTextures,
											 FlipState flipState,
											 ColorTargetMipmapping mainColorMipmaps) {
		// TODO: If we don't do anything special here, this will make colortex0-3 (and their legacy names) available to
		//       gbuffer/shadow programs... Should we allow that?
		String defaultSamplerName = resolveColorTargetInputs(colorTargets, customTextures, flipState,
				TextureInputNames::getMainColorSamplerNames, TextureInputNames::getMainColorImageName,
				mainColorMipmaps, null);

		// TODO: Not for GBuffer programs!
		this.defaultSamplerName = defaultSamplerName;
	}

	public void resolveShadowColorTargetInputs(ColorTargets colorTargets,
											   Map<String, SamplerBinding> customTextures,
											   FlipState flipState,
											   ColorTargetMipmapping shadowColorMipmaps,
											   List<PackShadowDirectives.SamplingSettings> samplingSettings) {
		// TODO: Shadow rendering settings / mipmaps
		resolveColorTargetInputs(colorTargets, customTextures, flipState,
				TextureInputNames::getShadowColorSamplerNames, TextureInputNames::getShadowColorImageName,
				shadowColorMipmaps, samplingSettings);
	}

	public void resolveMainDepthTargetInputs(DepthTargets depthTargets,
											 Map<String, SamplerBinding> customTextures) {
		// The ShadersMod pipeline doesn't allow mipmapping on main depth targets, and doesn't allow their sampling
		// to be configured.
		resolveDepthTargetInputs(depthTargets, customTextures, TextureInputNames::getMainDepthSamplerNames,
				IntSets.EMPTY_SET, null);
	}

	public void resolveShadowDepthTargetInputs(DepthTargets depthTargets,
											   Map<String, SamplerBinding> customTextures,
											   boolean waterShadowEnabled,
											   IntSet shadowDepthMipmaps,
											   List<PackShadowDirectives.DepthSamplingSettings> settingList) {
		if (waterShadowEnabled) {
			resolveDepthTargetInputs(depthTargets, customTextures, TextureInputNames::getShadowDepthSamplerNamesWS,
					shadowDepthMipmaps, settingList);
		} else {
			resolveDepthTargetInputs(depthTargets, customTextures, TextureInputNames::getShadowDepthSamplerNamesNonWS,
					shadowDepthMipmaps, settingList);
		}
	}

	public static TextureHandle[] getInputHandles(ColorTargets colorTargets, FlipState flipState, int buffer) {
		// Use the buffer flipping / parity state to figure out the texture handles for this buffer.
		// By default, input textures come from the "main" textures - they only come from the "alt" textures
		// if a flip is active.
		boolean alt0 = flipState.isFlippedBeforePass(buffer);
		boolean alt1 = alt0 ^ flipState.isParitySwapped(buffer);

		return new TextureHandle[] {
				colorTargets.get(buffer, alt0),
				colorTargets.get(buffer, alt1)
		};
	}

	// TODO: Address code duplication between this function and getInputHandles.
	public static SamplerBinding[] getInputSamplerBindings(ColorTargets colorTargets, FlipState flipState,
														   ColorTargetMipmapping mipmapping, int buffer,
														   boolean nearest) {
		// Use the buffer flipping / parity state to figure out the texture handles for this buffer.
		// By default, input textures come from the "main" textures - they only come from the "alt" textures
		// if a flip is active.
		boolean alt0 = flipState.isFlippedBeforePass(buffer);
		boolean alt1 = alt0 ^ flipState.isParitySwapped(buffer);

		return new SamplerBinding[] {
				new SamplerBinding(colorTargets.get(buffer, alt0), EdgeBehavior.CLAMP,
						getFilteringNM(nearest, mipmapping.isMipmapped(buffer, alt0))),
				new SamplerBinding(colorTargets.get(buffer, alt1), EdgeBehavior.CLAMP,
						getFilteringNM(nearest, mipmapping.isMipmapped(buffer, alt1)))
		};
	}

	/**
	 * Resolves all non-shadow render target inputs, taking into account buffer flipping, custom textures, the legacy
	 * sampler name aliases, and parity swaps for non-clearing color buffers.
	 *
	 * @param colorTargets Color target texture handles
	 * @param customTextures All registered custom textures
	 * @param flipState The buffer flipping state for this pass
	 */
	public String resolveColorTargetInputs(ColorTargets colorTargets,
										   Map<String, SamplerBinding> customTextures,
										   FlipState flipState,
										   IntFunction<String[]> samplerNames,
										   IntFunction<String> imageName,
										   ColorTargetMipmapping mipmapped,
										   List<PackShadowDirectives.SamplingSettings> samplingSettings) {
		String defaultSamplerName = null;

		for (int buffer = 0; buffer < colorTargets.numColorTargets(); buffer++) {
			// Check if we need to use nearest-neighbor sampling.
			boolean nearest = false;

			if (samplingSettings != null) {
				nearest = samplingSettings.get(buffer).getNearest();
			}

			// Get the input samplers.
			SamplerBinding[] samplerBindings = getInputSamplerBindings(colorTargets, flipState, mipmapped, buffer, nearest);

			// Custom texture overrides do not apply to images, and the image bindings only have one name.
			// Process them first.
			{
				ImageBinding[] imageBindings = new ImageBinding[] {
						new ImageBinding(samplerBindings[0].getTexture(), ImageBinding.Access.READ_WRITE, 0),
						new ImageBinding(samplerBindings[1].getTexture(), ImageBinding.Access.READ_WRITE, 0)
				};

				images.put(imageName.apply(buffer), imageBindings);
			}

			// Now process the sampler side of things.
			String[] names = samplerNames.apply(buffer);

			if (buffer == 0) {
				defaultSamplerName = names[0];
			}

			// Process custom texture overrides, handling the special behavior related to buffer flipping.
			if (!flipState.isFlippedAtLeastOnceThisGroup(buffer)) {
				// If it's not flipped, we might have a custom texture. Scan the potential names.
				samplerBindings = applyCustomTextureOverride(samplerBindings, names, customTextures);
			}

			// Register texture handles as samplers.
			for (String name : names) {
				samplers.put(name, samplerBindings);
			}
		}

		return defaultSamplerName;
	}

	public void resolveDepthTargetInputs(DepthTargets depthTargets,
										 Map<String, SamplerBinding> customTextures,
										 IntFunction<String[]> samplerNames,
										 IntSet mipmapEnabled,
										 List<PackShadowDirectives.DepthSamplingSettings> settingList) {
		for (int buffer = 0; buffer < depthTargets.numDepthTargets(); buffer++) {
			TextureHandle texture = depthTargets.get(buffer);
			TextureCompareMode compareMode = TextureCompareMode.NONE;

			SamplerBinding sampler;

			boolean mipmap = mipmapEnabled.contains(buffer);

			// Nearest-neighbor sampling by default for depth samplers - blending depth values might not
			// make sense.
			boolean nearest = true;

			if (settingList != null) {
				PackShadowDirectives.DepthSamplingSettings settings = settingList.get(buffer);

				if (settings.getHardwareFiltering()) {
					// Default compare mode, ShadersMod/OptiFine do not set the mode away from default aside from
					// enabling comparisons, but we make this explicit.
					compareMode = TextureCompareMode.LEQUAL;
				}

				nearest = settings.getNearest();
			}

			SamplerFiltering filtering = getFilteringNM(nearest, mipmap);
			sampler = new SamplerBinding(texture, EdgeBehavior.CLAMP, filtering, compareMode);

			String[] names = samplerNames.apply(buffer);
			SamplerBinding[] handles = applyCustomTextureOverride(sampler, names, customTextures);

			for (String name : names) {
				samplers.put(name, handles);
			}
		}
	}

	public void resolveNoiseTex(SamplerBinding noisetex, Map<String, SamplerBinding> customTextures) {
		final String name = TextureInputNames.NOISE_TEX;

		SamplerBinding[] noisetexHandles = new SamplerBinding[] { noisetex, noisetex };
		samplers.put(name,
				applyCustomTextureOverride(noisetexHandles, new String[] { name }, customTextures));
	}
}
