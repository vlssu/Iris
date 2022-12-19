package net.coderbot.iris.compat.sodium.mixin.shadow_map;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.SodiumWorldRenderer;
import net.caffeinemc.sodium.render.chunk.BlockEntityRenderManager;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.SectionTree;
import net.caffeinemc.sodium.render.chunk.SortedSectionLists;
import net.caffeinemc.sodium.render.chunk.TerrainRenderManager;
import net.caffeinemc.sodium.render.chunk.compile.ChunkBuilder;
import net.caffeinemc.sodium.render.chunk.cull.SectionCuller;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.render.chunk.draw.SortedTerrainLists;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.coderbot.iris.pipeline.ShadowRenderer;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.coderbot.iris.compat.sodium.impl.shadow_map.SwappableRenderSectionManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Modifies {@link TerrainRenderManager
 * } to support maintaining a separate visibility list for the shadow camera, as well
 * as disabling chunk rebuilds when computing visibility for the shadow camera.
 */
@Mixin(TerrainRenderManager.class)
public abstract class MixinTerrainRenderManager implements SwappableRenderSectionManager {

	@Shadow(remap = false)
	private boolean needsUpdate;

	@Mutable
	@Shadow
	@Final
	private RenderRegionManager regionManager;

	@Shadow(remap = false)
	protected static TerrainVertexType createVertexType() {
		return null;
	}

	@Shadow
	@Final
	private ChunkBuilder builder;

	@Shadow
	private int frameIndex;
	@Mutable
	@Shadow
	@Final
	private SortedTerrainLists sortedTerrainLists;
	@Mutable
	@Shadow
	@Final
	private SortedSectionLists sortedSectionLists;
	@Mutable
	@Shadow
	@Final
	private BlockEntityRenderManager blockEntityRenderManager;

	@Mutable
	@Shadow
	@Final
	private SectionCuller sectionCuller;
	@Shadow
	@Final
	private SectionTree sectionTree;
	@Unique
    private SortedSectionLists sortedSectionListsSwap;

    @Unique
    private SortedTerrainLists sortedTerrainListsSwap;

    @Unique
    private BlockEntityRenderManager blockEntityRenderManagerSwap;
	@Unique
    private SectionCuller sectionCullerSwap;


    @Unique
	private boolean needsUpdateSwap;

    @Unique
    private static final ObjectArrayFIFOQueue<?> EMPTY_QUEUE = new ObjectArrayFIFOQueue<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void iris$onInit(RenderDevice device, SodiumWorldRenderer worldRenderer, ChunkRenderPassManager renderPassManager, ClientLevel world, ChunkCameraContext camera, int chunkViewDistance, CallbackInfo ci) {
        this.sortedSectionListsSwap = new SortedSectionLists(this.sectionTree);
		this.sortedTerrainListsSwap = new SortedTerrainLists(this.regionManager, renderPassManager, this.sortedSectionListsSwap, camera);
		this.blockEntityRenderManagerSwap = new BlockEntityRenderManager(this.sectionTree, this.sortedSectionListsSwap);
		this.needsUpdateSwap = true;
		this.sectionCullerSwap = new SectionCuller(this.sectionTree, this.sortedSectionListsSwap, chunkViewDistance);

	}

    @Override
    public void iris$swapVisibilityState() {
		SortedSectionLists sortedSectionListsTmp = sortedSectionLists;
		sortedSectionLists = this.sortedSectionListsSwap;
        this.sortedSectionListsSwap = sortedSectionListsTmp;

		SortedTerrainLists sortedTerrainListsTmp = sortedTerrainLists;
		sortedTerrainLists = this.sortedTerrainListsSwap;
		this.sortedTerrainListsSwap = sortedTerrainListsTmp;

		BlockEntityRenderManager blockEntityRenderManagerTmp = blockEntityRenderManager;
		blockEntityRenderManager = this.blockEntityRenderManagerSwap;
		this.blockEntityRenderManagerSwap = blockEntityRenderManagerTmp;

		SectionCuller sectionCullerTmp = sectionCuller;
		sectionCuller = this.sectionCullerSwap;
		this.sectionCullerSwap = sectionCullerTmp;

        boolean needsUpdateTmp = needsUpdate;
        needsUpdate = needsUpdateSwap;
        needsUpdateSwap = needsUpdateTmp;
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
	private void iris$captureVisibleBlockEntities(Frustum frustum, boolean spectator, CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			ShadowRenderer.visibleBlockEntities = StreamSupport
				.stream(this.blockEntityRenderManager.getSectionedBlockEntities().spliterator(), false)
				.collect(Collectors.toList());;
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite(remap = false)
	public boolean isGraphDirty() {
		return true;
	}

//	@Redirect(method = "resetLists", remap = false,
//			at = @At(value = "INVOKE", target = "java/util/Collection.iterator ()Ljava/util/Iterator;"))
	private Iterator<?> iris$noQueueClearingInShadowPass(Collection<?> collection) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			return Collections.emptyIterator();
		} else {
			return collection.iterator();
		}
	}

	// TODO: check needsUpdate and needsUpdateSwap patches?
}
