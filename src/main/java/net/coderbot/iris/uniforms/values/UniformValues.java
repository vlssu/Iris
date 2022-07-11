package net.coderbot.iris.uniforms.values;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.coderbot.iris.vendored.joml.Matrix4f;
import net.coderbot.iris.vendored.joml.Vector3f;
import net.coderbot.iris.vendored.joml.Vector4f;

public class UniformValues {

	// Example state:
	//
	// | index | name              | locationStart | isInt  | dirty | value (loaded from proper array)
	// | 0     | gbufferProjection | 0             | false  | false | [1.0, 0.0, 0.0, 0.0, 0.0, 1.0, ...]
	// | 1     | frameTimeCounter  | 16            | false  | true  | 123.38294
	// | 2     | worldTime         | 0             | true   | false | 4283

	// Active Uniform Table
	int[] locationStarts;
	Type[] types;
	boolean[] isInt;
	String[] names;

	IntOpenHashSet dirty;
	IntArrayFIFOQueue queue;

	// Uniform Value Table
	float[] floats;
	int[] ints;

	public float loadFloat(int handle) {
		checkType("load", handle, Type.Float);

		return floats[handle];
	}

	public void storeFloat(int handle, float value) {
		checkType("store", handle, Type.Float);
		modified(handle);

		floats[handle] = value;
	}

	public void loadMat4f(int handle, Matrix4f target) {
		checkType("load", handle, Type.Matrix4f);

		target.get(floats, locationStarts[handle]);
	}

	public void storeMat4f(int handle, Matrix4f target) {
		checkType("store", handle, Type.Matrix4f);
		modified(handle);

		target.set(floats, locationStarts[handle]);
	}

	public void loadVec4f(int handle, Vector4f target) {
		checkType("load", handle, Type.Vector4f);

		int start = locationStarts[handle];

		target.x = floats[start];
		target.y = floats[start + 1];
		target.z = floats[start + 2];
		target.w = floats[start + 3];
	}

	public void storeVec4f(int handle, Vector4f target) {
		checkType("store", handle, Type.Vector4f);
		modified(handle);

		int start = locationStarts[handle];

		floats[start] = target.x;
		floats[start + 1] = target.y;
		floats[start + 2] = target.z;
		floats[start + 3] = target.w;
	}

	public void loadVec3f(int handle, Vector3f target) {
		checkType("load", handle, Type.Vector3f);

		int start = locationStarts[handle];

		target.x = floats[start];
		target.y = floats[start + 1];
		target.z = floats[start + 2];
	}


	public void storeVec3fTruncated(int handle, Vector4f target) {
		storeVec3f(handle, target.x, target.y, target.z);
	}

	public void storeVec3f(int handle, Vector3f target) {
		storeVec3f(handle, target.x, target.y, target.z);
	}

	public void storeVec3f(int handle, float x, float y, float z) {
		checkType("store", handle, Type.Vector3f);
		modified(handle);

		int start = locationStarts[handle];

		floats[start] = x;
		floats[start + 1] = y;
		floats[start + 2] = z;
	}

	private void modified(int handle) {
		if (dirty.add(handle)) {
			queue.enqueue(handle);
		}
	}

	private void checkType(String action, int handle, Type expected) {
		if (types[handle] != expected) {
			throw new IllegalArgumentException("Tried to " + action + " a Matrix4f at handle "
				+ handle + " but the expected type is " + types[handle]);
		}
	}

	enum Type {
		Float,
		Matrix4f,
		Vector3f,
		Vector4f
	}
}
