package net.coderbot.iris.gl;

import org.lwjgl.opengl.GL;

public class ExtensionList {
	private void get() {
		if (GL.getCapabilities().OpenGL30) {
			// GL 3.0+ path
		} else {
			// Deprecated GL 2.0 / GL 2.1 pathway
			// Won't work on core profile
		}
	}
}
