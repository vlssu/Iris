package net.coderbot.iris.platform;

import java.util.Locale;

public enum GlRenderer {
	RADEON("amd", "ati", "radeon"),
	GALLIUM("gallium"),
	INTEL("intel"),
	GEFORCE("geforce", "nvidia"),
	QUADRO("quadro", "nvs"),
	MESA("mesa"),
	OTHER;

	private final String[] prefixes;

	GlRenderer(String... prefixes) {
		this.prefixes = prefixes;
	}

	public GlRenderer fromGlRendererString(String rendererString) {
		rendererString = rendererString.toLowerCase(Locale.ROOT);

		for (GlRenderer renderer : values()) {
			for (String prefix : renderer.prefixes) {
				if (rendererString.startsWith(prefix)) {
					return renderer;
				}
			}
		}

		return OTHER;
	}

	/**
	 * Returns the graphics driver being used
	 *
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L725-L733">Optifine Doc</a>
	 *
	 * @return graphics driver prefixed with "MC_GL_RENDERER_"
	 */
	public String toDefineString() {
		return "MC_GL_RENDERER_" + name();
	}
}
