package net.coderbot.iris.uniforms.values;

import net.coderbot.iris.vendored.joml.Matrix4f;

public class InvertMat4fDerivation {
	int srcHandle;
	int dstHandle;
	private final Matrix4f tmp = new Matrix4f();

	public void apply(UniformValues values) {
		if (!values.isDirty(srcHandle)) {
			return;
		}

		values.loadMat4f(tmp, srcHandle);

		tmp.invert();

		values.storeMat4f(tmp, dstHandle);
	}

	@Override
	public String toString() {
		return "InvertMat4fDerivation{" +
			"srcHandle=" + srcHandle +
			", dstHandle=" + dstHandle +
			'}';
	}
}
