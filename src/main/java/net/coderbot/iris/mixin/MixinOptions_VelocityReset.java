package net.coderbot.iris.mixin;

import net.coderbot.iris.Iris;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinOptions_VelocityReset {
	@Inject(method = "setCameraType", at = @At("HEAD"))
	private void iris$onSetCameraType(CallbackInfo ci) {
		CapturedRenderingState.INSTANCE.setCameraTypeDirty(true);
	}
}
