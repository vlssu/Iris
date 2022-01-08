package net.coderbot.iris.shaderpack.rendergraph.lowering;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

public class FlipState {
	private final ImmutableSet<Integer> flippedBeforePass;
	private final ImmutableSet<Integer> flippedAtLeastOnceThisGroup;
	private final IntSet paritySwap;

	public FlipState(ImmutableSet<Integer> flippedBeforePass, ImmutableSet<Integer> flippedAtLeastOnceThisGroup,
					 IntSet paritySwap) {
		this.flippedBeforePass = flippedBeforePass;
		this.flippedAtLeastOnceThisGroup = flippedAtLeastOnceThisGroup;
		this.paritySwap = paritySwap;
	}

	public static FlipState unflipped() {
		return new FlipState(ImmutableSet.of(), ImmutableSet.of(), IntSets.EMPTY_SET);
	}

	public boolean isFlippedBeforePass(int index) {
		return flippedBeforePass.contains(index);
	}

	public boolean isFlippedAtLeastOnceThisGroup(int index) {
		return flippedAtLeastOnceThisGroup.contains(index);
	}

	public boolean isParitySwapped(int index) {
		return paritySwap.contains(index);
	}
}
