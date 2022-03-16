package net.coderbot.iris.compat.sodium.mixin.coplanar_fluids;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.pipeline.FluidRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Ensures that fluid quads are rendered in coplanar fashion.
 *
 * Inspired by, though distinct in implementation, from:
 * https://github.com/vram-guild/frex/blob/6248a1a5c094bc1a066d8cacfacbf4fc46063ebf/common/src/main/java/io/vram/frex/api/model/fluid/SimpleFluidModel.java#L185-L199
 */
@Mixin(FluidRenderer.class)
public class MixinFluidRenderer {
	private static final String FLUSH_QUAD = "me/jellysquid/mods/sodium/client/render/pipeline/FluidRenderer.flushQuad (Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuffers;Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lme/jellysquid/mods/sodium/client/model/quad/properties/ModelQuadFacing;Z)V";

	@Shadow
	private void flushQuad(ChunkModelBuffers buffers, ModelQuadView quad, ModelQuadFacing facing, boolean flip) {
		throw new AssertionError();
	}

	@Shadow
	@Final
	private ModelQuadViewMutable quad;

	@Unique
	private final ModelQuad quad2 = new ModelQuad();

	private static final int[] Q1_SWNE = new int[] {0, 2, 3, 3, /* missing vertex: */ 1};
	private static final int[] Q1_NWSE = new int[] {2, 3, 1, 1, /* missing vertex: */ 0};
	private static final int[] Q2_SWNE = new int[] {0, 1, 2, 2, /* missing vertex: */ 3};
	private static final int[] Q2_NWSE = new int[] {1, 3, 0, 0, /* missing vertex: */ 2};

	// TODO: also handle the "backwards up" quad
	// TODO: avoid duplication & optimize

	@Redirect(method = "render", at = @At(value = "INVOKE", target = FLUSH_QUAD),
		slice = @Slice(to = @At(value = "INVOKE",
			target = "net/minecraft/world/level/material/FluidState.shouldRenderBackwardUpFace (Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z")))
	private void iris$flushTopQuadRedirect(FluidRenderer renderer, ChunkModelBuffers buffers, ModelQuadView q,
										   ModelQuadFacing facing, boolean flip) {
		ModelQuadViewMutable quad = this.quad;
		ModelQuadViewMutable quad2 = this.quad2;

		float centerNwHeight = quad.getY(0);
		float southNwHeight = quad.getY(1);
		float southEastNwHeight = quad.getY(2);
		float eastNwHeight = quad.getY(3);

		boolean coplanar = Mth.equal(0f, southNwHeight - centerNwHeight - southEastNwHeight + eastNwHeight);

		if (coplanar) {
			// no need to split the quad

			flushQuad(buffers, quad, facing, flip);
		} else {
			boolean orientSwNe = (centerNwHeight < southNwHeight && centerNwHeight < eastNwHeight)
				|| (southEastNwHeight < southNwHeight && southEastNwHeight < eastNwHeight);

			int[] copyFrom;

			quad2.setFlags(quad.getFlags());
			quad2.setSprite(quad.getSprite());
			quad2.setColorIndex(quad.getColorIndex());

			// quad 1
			if (orientSwNe) {
				copyFrom = Q1_SWNE;
			} else {
				copyFrom = Q1_NWSE;
			}

			for (int t = 0; t < 4; t++) {
				int f = copyFrom[t];

				quad2.setX(t, quad.getX(f));
				quad2.setY(t, quad.getY(f));
				quad2.setZ(t, quad.getZ(f));
				quad2.setColor(t, quad.getColor(f));
				quad2.setTexU(t, quad.getTexU(f));
				quad2.setTexV(t, quad.getTexV(f));
			}

			quad2.setTexU(3, quad.getTexU(copyFrom[4]));
			quad2.setTexV(3, quad.getTexV(copyFrom[4]));

			flushQuad(buffers, quad2, facing, flip);

			// quad 2

			if (orientSwNe) {
				copyFrom = Q2_SWNE;
			} else {
				copyFrom = Q2_NWSE;
			}

			for (int t = 0; t < 4; t++) {
				int f = copyFrom[t];

				quad2.setX(t, quad.getX(f));
				quad2.setY(t, quad.getY(f));
				quad2.setZ(t, quad.getZ(f));
				quad2.setColor(t, quad.getColor(f));
				quad2.setTexU(t, quad.getTexU(f));
				quad2.setTexV(t, quad.getTexV(f));
			}

			quad2.setTexU(3, quad.getTexU(copyFrom[4]));
			quad2.setTexV(3, quad.getTexV(copyFrom[4]));

			flushQuad(buffers, quad2, facing, flip);
		}
	}
}
