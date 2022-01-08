package net.coderbot.iris.shaderpack.rendergraph.lowering;

import net.coderbot.iris.shaderpack.PackRenderTargetDirectives;

public class TextureInputNames {
	public static final String NOISE_TEX = "noisetex";

	public static String[] getMainColorSamplerNames(int buffer) {
		String primaryName = "colortex" + buffer;

		if (buffer < PackRenderTargetDirectives.LEGACY_RENDER_TARGETS.size()) {
			String secondaryName = PackRenderTargetDirectives.LEGACY_RENDER_TARGETS.get(buffer);
			return new String[] { primaryName, secondaryName };
		} else {
			return new String[] { primaryName };
		}
	}

	public static String[] getShadowColorSamplerNames(int buffer) {
		if (buffer == 0) {
			return new String[] { "shadowcolor", "shadowcolor0" };
		} else {
			return new String[] { "shadowcolor" + buffer };
		}
	}

	public static String getMainColorImageName(int buffer) {
		return "colorimg" + buffer;
	}

	public static String getShadowColorImageName(int buffer) {
		return "shadowcolorimg" + buffer;
	}

	public static String[] getMainDepthSamplerNames(int buffer) {
		// TODO: Constrain these appropriately outside of fullscreen passes:
		//  - depthtex0: gbuffers, but not shadow
		//  - depthtex1: gbuffers, but not shadow
		//  - depthtex2: none

		if (buffer == 0) {
			// Note: In ShadersMod/OptiFine, the gdepthtex alias is only available in composite passes.
			// Here, we make it available everywhere that depthtex0 is available.
			// I don't think this should cause issues.
			return new String[] { "gdepthtex", "depthtex0" };
		} else {
			return new String[] { "depthtex" + buffer };
		}
	}

	public static String[] getShadowDepthSamplerNamesNonWS(int buffer) {
		if (buffer == 0) {
			return new String[] { "shadow", "watershadow", "shadowtex0" };
		} else {
			return new String[] { "shadowtex" + buffer };
		}
	}

	public static String[] getShadowDepthSamplerNamesWS(int buffer) {
		if (buffer == 0) {
			return new String[] { "watershadow", "shadowtex0" };
		} else if (buffer == 1) {
			return new String[] { "shadow", "shadowtex1" };
		} else {
			return new String[] { "shadowtex" + buffer };
		}
	}
}
