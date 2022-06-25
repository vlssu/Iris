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
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = ModelPart.class, priority = 1001)
public class MixinModelPart {
	@Inject(method = "compile", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Vector3f;copy()Lcom/mojang/math/Vector3f;"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void setModelPart(PoseStack.Pose arg, VertexConsumer arg2, int i, int j, float f, float g, float h, float k, CallbackInfo ci, Matrix4f f4, Matrix3f f3, ObjectListIterator var11, ModelPart.Cube cube, ModelPart.Polygon[] var13, int var14, int var15) {
		if (arg2 instanceof BlockSensitiveBufferBuilder) {
			((BlockSensitiveBufferBuilder) arg2).beginEntity(((EntityVelocityInterface) cube).getEntityVelocity()[var15]);
		}
	}
}
