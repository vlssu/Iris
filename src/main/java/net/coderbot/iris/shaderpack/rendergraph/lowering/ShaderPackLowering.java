package net.coderbot.iris.shaderpack.rendergraph.lowering;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.coderbot.iris.shaderpack.PackDirectives;
import net.coderbot.iris.shaderpack.PackRenderTargetDirectives;
import net.coderbot.iris.shaderpack.ProgramDirectives;
import net.coderbot.iris.shaderpack.ProgramSet;
import net.coderbot.iris.shaderpack.ProgramSource;
import net.coderbot.iris.shaderpack.rendergraph.ColorAttachments;
import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;
import net.coderbot.iris.shaderpack.rendergraph.TextureSize;
import net.coderbot.iris.shaderpack.rendergraph.pass.ScreenRenderPassInfo;
import net.coderbot.iris.shaderpack.rendergraph.pass.ScreenRenderPassInfoBuilder;
import net.coderbot.iris.vendored.joml.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Entrypoint for the ShaderPack lowering system. As in compilers, the lowering system converts high-level ShaderPack
 * concepts to our low-level RenderGraph representation. This lowering system encapsulates and handles a large portion
 * of the complexity and legacy weirdness involved in the existing shader pack format, significantly simplifying the
 * version-dependent portions of the Iris codebase that work with OpenGL & Minecraft.
 */
public class ShaderPackLowering {
	public static ScreenRenderPassInfo[] lowerCompositePasses(ProgramSet programSet, PackDirectives packDirectives, FlipTracker flipTracker) {
		boolean waterShadowEnabled = false;

		// TODO: Initialize these things!!!!!!!!!!
		//   (PROPERLY)
		ColorTargets mainColorTargets = new ColorTargets("main_color_", 16);
		DepthTargets mainDepthTargets = new DepthTargets("main_depth_", 3);
		ColorTargets shadowColorTargets = new ColorTargets("shadow_color_", 2);
		DepthTargets shadowDepthTargets = new DepthTargets("shadow_depth_", 2);
		TextureHandle noisetex = new TextureHandle("noise_tex");

		Map<String, TextureHandle> customTextures = new HashMap<>();

		// -----

		ImmutableSet<Integer> flippedBeforeComposite = flipTracker.snapshot();

		packDirectives.getExplicitFlips("composite_pre").forEach((colorTarget, shouldFlip) -> {
			if (shouldFlip) {
				flipTracker.flip(colorTarget);
			}
		});

		ProgramSource[] composite = programSet.getComposite();
		final ImmutableSet.Builder<Integer> flippedAtLeastOnce = new ImmutableSet.Builder<>();
		List<ProtoPass> protoPasses = new ArrayList<>();

		for (ProgramSource source : composite) {
			if (source == null || !source.isValid()) {
				continue;
			}

			ProgramDirectives directives = source.getDirectives();

			int[] drawBuffers = directives.getDrawBuffers();

			ImmutableMap<Integer, Boolean> explicitFlips = source.getDirectives().getExplicitFlips();

			ImmutableSet<Integer> flipped = flipTracker.snapshot();
			ImmutableSet<Integer> flippedAtLeastOnceSnapshot = flippedAtLeastOnce.build();

			ProtoPass protoPass = new ProtoPass(flipped, flippedAtLeastOnceSnapshot, source);
			protoPasses.add(protoPass);

			IntSet flippedAfterThisPass = new IntOpenHashSet();

			for (int buffer : drawBuffers) {
				flippedAfterThisPass.add(buffer);
			}

			explicitFlips.forEach((buffer, isFlipped) -> {
				if (isFlipped) {
					flippedAfterThisPass.add(buffer.intValue());
				} else {
					flippedAfterThisPass.remove(buffer.intValue());
				}
			});

			for (int buffer : flippedAfterThisPass) {
				flipTracker.flip(buffer);
			}
		}

		IntSet needsParitySwap = new IntOpenHashSet();
		PackRenderTargetDirectives renderTargetDirectives = packDirectives.getRenderTargetDirectives();
		IntList buffersToBeCleared = renderTargetDirectives.getBuffersToBeCleared();

		flipTracker.snapshot().forEach((target) -> {
			if (buffersToBeCleared.contains(target.intValue())) {
				return;
			}

			needsParitySwap.add(target.intValue());
		});

		ScreenRenderPassInfo[] screenRenderPasses = new ScreenRenderPassInfo[protoPasses.size()];
		int i = 0;

		for (ProtoPass protoPass : protoPasses) {
			FlipState mainFlipState = new FlipState(protoPass.flippedBeforePass, protoPass.flippedAtLeastOnce, needsParitySwap);
			// TODO: This will need changing to support shadowcomp
			FlipState shadowFlipState = FlipState.unflipped();

			ScreenRenderPassInfoBuilder builder = ScreenRenderPassInfo.builder();

			builder.setSource(protoPass.source);

			ProgramDirectives directives = protoPass.source.getDirectives();
			builder.setViewportScale(new Vector2f(directives.getViewportScale(), directives.getViewportScale()));

			int[] drawBuffers = directives.getDrawBuffers();
			builder.setAttachmentsByParity(resolveColorAttachments(mainColorTargets, drawBuffers, mainFlipState));

			TextureInputs textureInputs = new TextureInputs();

			// Main color / depth
			textureInputs.resolveMainColorTargetInputs(mainColorTargets, customTextures, mainFlipState);
			textureInputs.resolveMainDepthTargetInputs(mainDepthTargets, customTextures);

			// Shadow color / depth
			textureInputs.resolveShadowColorTargetInputs(shadowColorTargets, customTextures, shadowFlipState);
			textureInputs.resolveShadowDepthTargetInputs(shadowDepthTargets, customTextures, waterShadowEnabled);

			// noisetex
			textureInputs.resolveNoiseTex(noisetex, customTextures);

			builder.setSamplers(textureInputs.getSamplers());
			builder.setImages(textureInputs.getImages());

			// TODO: Uniforms???
			builder.setUniforms(new HashSet<>());

			// TODO: Mipmap passes? Samplers?

			screenRenderPasses[i++] = builder.build();
		}

		return screenRenderPasses;
	}

	private static ColorAttachments[] resolveColorAttachments(ColorTargets renderTargets, int[] drawBuffers,
															  FlipState flipState) {
		TextureSize pickedSize = null;
		TextureHandle[] textureHandles0 = new TextureHandle[drawBuffers.length];
		TextureHandle[] textureHandles1 = new TextureHandle[drawBuffers.length];

		for (int i = 0; i < drawBuffers.length; i++) {
			int drawBuffer = drawBuffers[i];

			if (drawBuffer >= renderTargets.numColorTargets()) {
				throw new IllegalStateException("Shader pass tried to write to an unavailable color target with index "
						+ drawBuffer + "; only " + renderTargets.numColorTargets() + " color targets are available");
			}

			// Resolve sizes if needed
			TextureSize size = renderTargets.getSize(drawBuffer);

			if (pickedSize == null) {
				pickedSize = size;
			} else if (!pickedSize.equals(size)) {
				throw new IllegalArgumentException("Color target size mismatch when resolving draw buffer array " +
						Arrays.toString(drawBuffers) + ": color target index " + drawBuffer + " uses size " + size
						+ " mismatching with the current picked size " + pickedSize);
			}

			// color attachments start as alt unless flipped.
			boolean alt0 = !flipState.isFlippedBeforePass(drawBuffer);

			// B ^ false = B
			// B ^ true = !B
			boolean alt1 = alt0 ^ flipState.isParitySwapped(drawBuffer);

			textureHandles0[i] = renderTargets.get(drawBuffer, alt0);
			textureHandles1[i] = renderTargets.get(drawBuffer, alt1);
		}

		return new ColorAttachments[] {
				new ColorAttachments(textureHandles0, pickedSize),
				new ColorAttachments(textureHandles1, pickedSize)
		};
	}

	private static class ProtoPass {
		private final ImmutableSet<Integer> flippedBeforePass;
		private final ImmutableSet<Integer> flippedAtLeastOnce;
		private final ProgramSource source;

		public ProtoPass(ImmutableSet<Integer> flippedBeforePass, ImmutableSet<Integer> flippedAtLeastOnce, ProgramSource source) {
			this.flippedBeforePass = flippedBeforePass;
			this.flippedAtLeastOnce = flippedAtLeastOnce;
			this.source = source;
		}
	}
}
