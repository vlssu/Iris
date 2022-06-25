package net.coderbot.iris.vertices;

import com.mojang.math.Vector4f;
import net.coderbot.iris.mixin.LevelRendererAccessor;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class EntityVelocity {
	private float posX, posY, posZ;
	private float velX, velY, velZ;
	private int frameId;

	public static EntityVelocity[][] createMatrix() {
		EntityVelocity[][] matrix = new EntityVelocity[6][4];
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 4; j++) {
				matrix[i][j] = new EntityVelocity();
			}
		}
		return matrix;
	}

	public EntityVelocity() {

	}

	public void setPos(float x, float y, float z) {
		if (((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getFrameId() == this.frameId) {
			return;
		}

		if (frameId - ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getFrameId() < 2 && !CapturedRenderingState.INSTANCE.isCameraTypeDirty()) {
			if (posX != 0) {
				this.velX = x - posX;
			}

			if (posY != 0) {
				this.velY = y - posY;
			}

			if (posZ != 0) {
				this.velZ = z - posZ;
			}
		} else {
			this.velX = 0;
			this.velY = 0;
			this.velZ = 0;
		}

		this.frameId = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getFrameId();
		this.posX = x;
		this.posY = y;
		this.posZ = z;
	}

	public void setPos(Vector4f vec4f) {
		setPos(vec4f.x(), vec4f.y(), vec4f.z());
	}

	public float getVelX() {
		return velX;
	}

	public float getVelY() {
		return velY;
	}

	public float getVelZ() {
		return velZ;
	}
}
