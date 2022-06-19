package net.coderbot.iris.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.coderbot.iris.vertices.BlockSensitiveBufferBuilder;
import net.coderbot.iris.vertices.EntityVelocity;
import net.coderbot.iris.vertices.EntityVelocityInterface;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ModelPart.Cube.class)
public class MixinModelPartCube implements EntityVelocityInterface {
	private EntityVelocity[][] velocity;

	@Inject(method = {
		"<init>"
	}, at = @At("TAIL"))
	private void allocateVelocity(CallbackInfo ci) {
		velocity = EntityVelocity.createMatrix();
	}

	@Override
	public EntityVelocity[][] getEntityVelocity() {
		return velocity;
	}

	@Override
	public void setEntityVelocity(EntityVelocity[][] velocity) {
		this.velocity = velocity;
	}
}
