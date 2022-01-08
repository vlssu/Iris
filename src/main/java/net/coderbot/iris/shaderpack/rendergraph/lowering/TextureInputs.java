package net.coderbot.iris.shaderpack.rendergraph.lowering;

import net.coderbot.iris.shaderpack.rendergraph.ImageBinding;
import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

public class TextureInputs {
	private final Map<String, TextureHandle[]> samplers;
	private final Map<String, ImageBinding[]> images;

	public TextureInputs() {
		samplers = new HashMap<>();
		images = new HashMap<>();
	}

	public Map<String, TextureHandle[]> getSamplers() {
		return samplers;
	}

	public Map<String, ImageBinding[]> getImages() {
		return images;
	}

	private static TextureHandle[] applyCustomTextureOverride(TextureHandle[] base, String[] names, Map<String, TextureHandle> customTextures) {
		for (String name : names) {
			TextureHandle customTexture = customTextures.get(name);

			if (customTexture != null) {
				return new TextureHandle[] { customTexture, customTexture };
			}
		}

		return base;
	}

	private static TextureHandle[] applyCustomTextureOverride(TextureHandle base, String[] names, Map<String, TextureHandle> customTextures) {
		for (String name : names) {
			TextureHandle customTexture = customTextures.get(name);

			if (customTexture != null) {
				return new TextureHandle[] { customTexture, customTexture };
			}
		}

		return new TextureHandle[] { base, base };
	}

	public void resolveMainColorTargetInputs(ColorTargets colorTargets,
											 Map<String, TextureHandle> customTextures,
											 FlipState flipState) {
		resolveColorTargetInputs(colorTargets, customTextures, flipState,
				TextureInputNames::getMainColorSamplerNames, TextureInputNames::getMainColorImageName);
	}

	public void resolveShadowColorTargetInputs(ColorTargets colorTargets,
											   Map<String, TextureHandle> customTextures,
											   FlipState flipState) {
		resolveColorTargetInputs(colorTargets, customTextures, flipState,
				TextureInputNames::getShadowColorSamplerNames, TextureInputNames::getShadowColorImageName);
	}

	public void resolveMainDepthTargetInputs(DepthTargets depthTargets,
											 Map<String, TextureHandle> customTextures) {
		resolveDepthTargetInputs(depthTargets, customTextures, TextureInputNames::getMainDepthSamplerNames);
	}

	public void resolveShadowDepthTargetInputs(DepthTargets depthTargets,
											   Map<String, TextureHandle> customTextures,
											   boolean waterShadowEnabled) {
		if (waterShadowEnabled) {
			resolveDepthTargetInputs(depthTargets, customTextures, TextureInputNames::getShadowDepthSamplerNamesWS);
		} else {
			resolveDepthTargetInputs(depthTargets, customTextures, TextureInputNames::getShadowDepthSamplerNamesNonWS);
		}
	}

	/**
	 * Resolves all non-shadow render target inputs, taking into account buffer flipping, custom textures, the legacy
	 * sampler name aliases, and parity swaps for non-clearing color buffers.
	 *
	 * @param colorTargets Color target texture handles
	 * @param customTextures All registered custom textures
	 * @param flipState The buffer flipping state for this pass
	 */
	public void resolveColorTargetInputs(ColorTargets colorTargets,
										 Map<String, TextureHandle> customTextures,
										 FlipState flipState,
										 IntFunction<String[]> samplerNames,
										 IntFunction<String> imageName) {
		for (int buffer = 0; buffer < colorTargets.numColorTargets(); buffer++) {
			// Use the buffer flipping / parity state to figure out the texture handles for this buffer.
			// By default, input textures come from the "main" textures - they only come from the "alt" textures
			// if a flip is active.
			boolean alt0 = flipState.isFlippedBeforePass(buffer);
			boolean alt1 = alt0 ^ flipState.isParitySwapped(buffer);

			TextureHandle[] handles = new TextureHandle[] {
					colorTargets.get(buffer, alt0),
					colorTargets.get(buffer, alt1)
			};

			// Custom texture overrides do not apply to images, and the image bindings only have one name.
			// Process them first.
			{
				ImageBinding[] imageBindings = new ImageBinding[] {
						new ImageBinding(handles[0], ImageBinding.Access.READ_WRITE, 0),
						new ImageBinding(handles[1], ImageBinding.Access.READ_WRITE, 0)
				};

				images.put(imageName.apply(buffer), imageBindings);
			}

			// Now process the sampler side of things.
			String[] names = samplerNames.apply(buffer);

			// Process custom texture overrides, handling the special behavior related to buffer flipping.
			if (!flipState.isFlippedAtLeastOnceThisGroup(buffer)) {
				// If it's not flipped, we might have a custom texture. Scan the potential names.
				handles = applyCustomTextureOverride(handles, names, customTextures);
			}

			// Register texture handles as samplers.
			for (String name : names) {
				samplers.put(name, handles);
			}
		}
	}

	public void resolveDepthTargetInputs(DepthTargets depthTargets,
										 Map<String, TextureHandle> customTextures,
										 IntFunction<String[]> samplerNames) {
		for (int buffer = 0; buffer < depthTargets.numDepthTargets(); buffer++) {
			TextureHandle texture = depthTargets.get(buffer);

			String[] names = samplerNames.apply(buffer);
			TextureHandle[] handles = applyCustomTextureOverride(texture, names, customTextures);

			for (String name : names) {
				samplers.put(name, handles);
			}
		}
	}

	public void resolveNoiseTex(TextureHandle noisetex, Map<String, TextureHandle> customTextures) {
		final String name = TextureInputNames.NOISE_TEX;

		TextureHandle[] noisetexHandles = new TextureHandle[] { noisetex, noisetex };
		samplers.put(name,
				applyCustomTextureOverride(noisetexHandles, new String[] { name }, customTextures));
	}
}
