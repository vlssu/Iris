package net.coderbot.iris.uniforms.values;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.coderbot.iris.vendored.joml.Matrix4f;
import net.coderbot.iris.vendored.joml.Vector3f;
import net.coderbot.iris.vendored.joml.Vector4f;

public class UniformValuesBuilder {
	int maxUid = 0;
	Int2ObjectMap<int[]> dependencies;

	// source -> inverted
	Int2IntMap mat4Inverts;
	Object2IntMap<String> mat4Uploads;
	Object2IntMap<String> vec3fUploads;
	Object2IntMap<String> vec4fUploads;
	Object2IntMap<String> floatUploads;

	public UniformValuesBuilder() {
		mat4Inverts = new Int2IntOpenHashMap();
		mat4Uploads = new Object2IntOpenHashMap<>();
	}

	InMat4 inputMat4() {
		return new InMat4(maxUid++);
	}

	InFloat inputFloat() {
		return new InFloat(maxUid++);
	}

	Handle<Matrix4f> invert(Handle<Matrix4f> mat) {
		int invertedUid = mat4Inverts.computeIfAbsent(mat.uid, x -> maxUid++);

		dependencies.put(invertedUid, new int[] {mat.uid});

		return new Handle<>(invertedUid, Type.Mat4);
	}

	interface MapDerivation {
		void compute(UniformValues values, int srcHandle, int dstHandle);
	}

	interface Map2Derivation {
		void compute(UniformValues values, int src1Handle, int src2Handle, int dstHandle);
	}

	<I, R> Handle<R> map(Handle<I> input, Class<R> ret, MapDerivation fn) {
		return null;
	}

	<I1, I2, R> Handle<R> map2(Handle<I1> input1, Handle<I2> input2, Class<R> ret, Map2Derivation fn) {
		return null;
	}

	Handle<Float> mapF2F(Handle<Float> input, Float2FloatFunction fn) {
		return map(input, Float.class, (values, src, dst) ->
			values.storeFloat(dst, fn.apply(values.loadFloat(src))));
	}

	Handle<Matrix4f> uploadMat4(String name, Handle<Matrix4f> mat4f) {
		mat4Uploads.put(name, mat4f.uid());

		return mat4f;
	}

	Handle<Vector4f> uploadVec4f(String name, Handle<Vector4f> vec4f) {
		vec4fUploads.put(name, vec4f.uid());

		return vec4f;
	}

	Handle<Vector3f> uploadVec3f(String name, Handle<Vector3f> vec3f) {
		vec3fUploads.put(name, vec3f.uid());

		return vec3f;
	}

	Handle<Float> uploadFloat(String name, Handle<Float> fl) {
		floatUploads.put(name, fl.uid());

		return fl;
	}

	public void build() {
		// TODO
	}

	enum Type {
		Mat4,
		Vec4f,
		Float
	}

	class Handle<T> {
		private final int uid;
		private final Type type;

		Handle(int uid, Type type) {
			this.uid = uid;
			this.type = type;
		}

		public int uid() {
			return uid;
		}

		public Type type() {
			return type;
		}
	}

	abstract class Input<T> extends Handle<T> {
		int handle;
		UniformValues values;

		Input(int uid, Type type) {
			super(uid, type);
		}

		abstract void push(T value);
	}

	class InMat4 extends Input<Matrix4f> {
		Matrix4f check = new Matrix4f();

		InMat4(int uid) {
			super(uid, Type.Mat4);
		}

		public void push(Matrix4f value) {
			if (values == null) {
				return;
			}

			values.loadMat4f(handle, check);

			if (check.equals(value)) {
				return;
			}

			values.storeMat4f(handle, value);
		}
	}

	class InFloat extends Input<Float> {
		Matrix4f check = new Matrix4f();

		InFloat(int uid) {
			super(uid, Type.Float);
		}

		public void push(Float value) {
			push(value.floatValue());
		}

		public void push(float value) {
			if (values == null) {
				return;
			}

			float previous = values.loadFloat(handle);

			if (previous == value) {
				return;
			}

			values.storeFloat(handle, value);
		}
	}

	static {
		UniformValuesBuilder builder = new UniformValuesBuilder();

		Input<Matrix4f> gbufferProjectionMatrix = builder.inputMat4();
		Input<Matrix4f> gbufferViewMatrix = builder.inputMat4();

		builder.uploadMat4("gbufferProjection", gbufferProjectionMatrix);
		builder.uploadMat4("gbufferProjectionInverse", builder.invert(gbufferProjectionMatrix));

		InFloat skyAngleH = celestialUniforms(builder, -40.0F, gbufferViewMatrix);

		builder.build();

		/// later

		gbufferProjectionMatrix.push(new Matrix4f());
		gbufferViewMatrix.push(new Matrix4f());
	}

	static InFloat celestialUniforms(UniformValuesBuilder builder, float sunPathRotation, Handle<Matrix4f> gbufferViewMatrix) {
		InFloat skyAngleH = builder.inputFloat();

		builder.uploadVec3f("upPosition", CelestialUniformsV2.upPosition(builder, gbufferViewMatrix));

		Handle<Float> sunAngleH = builder.uploadFloat("sunAngle",
			builder.mapF2F(skyAngleH, CelestialUniformsV2::sunAngleFromSkyAngle));

		Handle<Float> shadowAngleH = builder.uploadFloat("shadowAngle",
			builder.mapF2F(sunAngleH, CelestialUniformsV2::shadowAngleFromSunAngle));

		builder.uploadVec3f("sunPosition",
			CelestialUniformsV2.celestialPosition(builder, sunPathRotation, gbufferViewMatrix, sunAngleH));

		builder.uploadVec3f("moonPosition",
			CelestialUniformsV2.celestialPosition(builder, sunPathRotation, gbufferViewMatrix,
				builder.mapF2F(sunAngleH, CelestialUniformsV2::moonAngleFromSunAngle)));

		builder.uploadVec3f("shadowLightPosition",
			CelestialUniformsV2.celestialPosition(builder, sunPathRotation, gbufferViewMatrix, shadowAngleH));

		return skyAngleH;
	}

	static class CelestialUniformsV2 {
		static Handle<Vector3f> upPosition(UniformValuesBuilder builder, Handle<Matrix4f> gbufferViewMatrix) {
			return builder.map(gbufferViewMatrix, Vector3f.class, (values, src, dst) -> {
				Matrix4f celestialViewMatrix = new Matrix4f();
				Vector4f upVec = new Vector4f(0.0F, 100.0F, 0.0F, 0.0F);

				values.loadMat4f(src, celestialViewMatrix);

				celestialViewMatrix.rotate((float) Math.toRadians(-90), new Vector3f(0.0F, 1.0F, 0.0F));
				celestialViewMatrix.transform(upVec);

				values.storeVec3fTruncated(dst, upVec);
			});
		}

		static Handle<Vector3f> celestialPosition(UniformValuesBuilder builder, float sunPathRotation,
												  Handle<Matrix4f> gbufferViewMatrix,
												  Handle<Float> bodyAngleH) {
			return builder.map2(gbufferViewMatrix, bodyAngleH, Vector3f.class, (values, srcMat, bodyAngleL, dst) -> {
				Matrix4f celestial = new Matrix4f();
				Vector4f position = new Vector4f(0.0F, 100.0F, 0.0F, 0.0F);

				values.loadMat4f(srcMat, celestial);
				float bodyAngle = values.loadFloat(bodyAngleL);

				// This is the same transformation applied by renderSky, however, it's been moved to here.
				// This is because we need the result of it before it's actually performed in vanilla.
				celestial.rotate((float) Math.toRadians(-90), new Vector3f(0.0F, 1.0F, 0.0F));
				celestial.rotate((float) Math.toRadians(sunPathRotation), new Vector3f(0.0F, 0.0F, 1.0F));
				celestial.rotate((float) Math.toRadians((bodyAngle - 0.25F) * 360.0F), new Vector3f(0.0F, 0.0F, 1.0F));

				celestial.transform(position);

				values.storeVec3fTruncated(dst, position);
			});
		}

		static float sunAngleFromSkyAngle(float skyAngle) {
			if (skyAngle < 0.75F) {
				return skyAngle + 0.25F;
			} else {
				return skyAngle - 0.75F;
			}
		}

		static float moonAngleFromSunAngle(float sunAngle) {
			return (sunAngle + 0.5F) % 1.0F;
		}

		static float shadowAngleFromSunAngle(float sunAngle) {
			if (sunAngle >= 0.5F) {
				return sunAngle - 0.5F;
			} else {
				return sunAngle;
			}
		}
	}
}
