package net.coderbot.iris.mixin.vertices;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.coderbot.iris.vertices.BlockSensitiveBufferBuilder;
import net.coderbot.iris.vertices.EntityVelocity;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteCoordinateExpander.class)
public class MixinSpriteCoordinateExpander implements BlockSensitiveBufferBuilder {
	@Shadow
	@Final
	private VertexConsumer delegate;

	@Override
	public void beginBlock(short block, short renderType) {
		if (this.delegate instanceof BlockSensitiveBufferBuilder) {
			((BlockSensitiveBufferBuilder) this.delegate).beginBlock(block, renderType);
		}
	}

	@Override
	public void endBlock() {
		if (this.delegate instanceof BlockSensitiveBufferBuilder) {
			((BlockSensitiveBufferBuilder) this.delegate).endBlock();
		}
	}

	@Override
	public void beginEntity(EntityVelocity velocity) {
		if (this.delegate instanceof BlockSensitiveBufferBuilder) {
			((BlockSensitiveBufferBuilder) this.delegate).beginEntity(velocity);
		}
	}

	@Override
	public void endEntity() {
		if (this.delegate instanceof BlockSensitiveBufferBuilder) {
			((BlockSensitiveBufferBuilder) this.delegate).endEntity();
		}
	}
}
