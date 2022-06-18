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
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ModelPart.class)
public class MixinModelPart {
	private EntityVelocity[][] velocity;

	@Inject(method = {
		"<init>(Lnet/minecraft/client/model/Model;)V",
		"<init>(Lnet/minecraft/client/model/Model;II)V",
		"<init>(IIII)V",
		"<init>()V"
	}, at = @At("TAIL"))
	private void allocateVelocity(CallbackInfo ci) {
		velocity = EntityVelocity.createMatrix();
	}

	@Inject(method = "compile", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;vertex(FFFFFFFFFIIFFF)V"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void setModelPart(PoseStack.Pose arg, VertexConsumer arg2, int i, int j, float f, float g, float h, float k, CallbackInfo ci, Matrix4f lv, Matrix3f lv2, ObjectListIterator var11, ModelPart.Cube lv3, ModelPart.Polygon[] var13, int var14, int var15, ModelPart.Polygon lv4, Vector3f lv5, float l, float m, float n, int o, ModelPart.Vertex lv6, float p, float q, float r, Vector4f lv7) {
		velocity[var15][o].setPos(lv7.x(), lv7.y(), lv7.z());
		((BlockSensitiveBufferBuilder) arg2).beginEntity(velocity[var15][o]);
	}

	@Inject(method = "compile", at = @At(value = "TAIL"))
	private void resetModelPart(PoseStack.Pose arg, VertexConsumer arg2, int i, int j, float f, float g, float h, float k, CallbackInfo ci) {
		((BlockSensitiveBufferBuilder) arg2).endEntity();
	}
}
