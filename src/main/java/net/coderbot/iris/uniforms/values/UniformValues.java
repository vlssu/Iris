package net.coderbot.iris.uniforms.values;

import net.coderbot.iris.vendored.joml.Matrix4f;

public class UniformValues {

	// Example state:
	//
	// | index | name              | locationStart | isInt  | dirty | value (loaded from proper array)
	// | 0     | gbufferProjection | 0             | false  | false | [1.0, 0.0, 0.0, 0.0, 0.0, 1.0, ...]
	// | 1     | frameTimeCounter  | 16            | false  | true  | 123.38294
	// | 2     | worldTime         | 0             | true   | false | 4283

	// Active Uniform Table
	int[] locationStarts;
	boolean[] isInt;
	boolean[] dirty;
	String[] names;

	// Uniform Value Table
	float[] floats;
	int[] ints;

	public boolean isDirty(int handle) {
		return dirty[handle];
	}

	public void loadMat4f(Matrix4f target, int handle) {
		target.get(floats, locationStarts[handle]);
	}

	public void storeMat4f(Matrix4f target, int handle) {
		dirty[handle] = true;
		target.set(floats, locationStarts[handle]);
	}
}
