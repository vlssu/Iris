package net.coderbot.iris.platform;

import java.util.Locale;

public enum GlVendor {
	ATI("ati"),
	INTEL("intel"),
	NVIDIA("nvidia"),
	AMD("amd"),
	XORG("x.org"),
	OTHER;

	private final String[] prefixes;

	GlVendor(String... prefixes) {
		this.prefixes = prefixes;
	}

	public GlVendor fromGlVendorString(String vendorString) {
		vendorString = vendorString.toLowerCase(Locale.ROOT);

		for (GlVendor vendor : values()) {
			for (String prefix : vendor.prefixes) {
				if (vendorString.startsWith(prefix)) {
					return vendor;
				}
			}
		}

		return OTHER;
	}

	/**
	 * Returns a string indicating the graphics card being used
	 *
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L716-L723"></a>
	 *
	 * @return the graphics card prefixed with "MC_GL_VENDOR_"
	 */
	public String toDefineString() {
		return "MC_GL_VENDOR_" + name();
	}
}
