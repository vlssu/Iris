package net.coderbot.iris.shaderpack.rendergraph.lowering;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class FlipTracker {
	private final IntSet flippedBuffers;

	private FlipTracker(IntSet set) {
		flippedBuffers = new IntOpenHashSet(set);
	}

	public FlipTracker() {
		this.flippedBuffers = new IntOpenHashSet();
	}

	public void flip(int target) {
		if (!flippedBuffers.remove(target)) {
			// If the target wasn't in the set, add it to the set.
			flippedBuffers.add(target);
		}
	}

	/**
	 * Returns true if this buffer is flipped.
	 *
	 * If this buffer is not flipped, then users should write to the alternate variant and read from the main variant.
	 *
	 * If this buffer is flipped, then users should write to the main variant and read from the alternate variant.
	 */
	public boolean isFlipped(int target) {
		return flippedBuffers.contains(target);
	}

	public IntIterator getFlippedBuffers() {
		return flippedBuffers.iterator();
	}

	public ImmutableSet<Integer> snapshot() {
		return ImmutableSet.copyOf(flippedBuffers);
	}

	public FlipTracker fork() {
		return new FlipTracker(flippedBuffers);
	}
}
