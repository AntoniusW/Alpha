package at.ac.tuwien.kr.alpha.solver;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.solver.NoGoodStore.LBD_NO_VALUE;

/**
 * A restart strategy that mixes dynamic restarts and Luby sequence-based restarts.
 *
 * Copyright (c) 2019, the Alpha Team.
 */
public class MixedRestartStrategy {
	private final LearnedNoGoodDeletion learnedNoGoodDeletion;

	// Variables for dynamic restarts.
	private long fast;
	private long slow;
	private long currentConflictsLimit;
	private int numTotalRestarts;

	// Variables for Luby-based restarts.
	private int un = 1;
	private int vn = 1;
	private static final int LUBY_FACTOR = 32;
	private int lubyLimit = LUBY_FACTOR;


	MixedRestartStrategy(LearnedNoGoodDeletion learnedNoGoodDeletion) {
		this.learnedNoGoodDeletion = learnedNoGoodDeletion;
		reset();
	}

	private void reset() {
		fast = 0;
		slow = 0;
		currentConflictsLimit = 50;
		numTotalRestarts = 0;
		un = 1;
		vn = 1;
	}

	boolean shouldRestart() {
		long numTotalConflicts = learnedNoGoodDeletion.getNumTotalConflicts();
		return (numTotalConflicts > currentConflictsLimit && fast / 125 > slow / 100)
			|| numTotalConflicts > lubyLimit;
	}

	void onRestart() {
		currentConflictsLimit = learnedNoGoodDeletion.getNumTotalConflicts() + 50;
		nextLuby();
		numTotalRestarts++;
	}

	void newConflictWithLbd(int lbd) {
		/// Below is a fast 64-bit fixed point implementation of the following:
		// slow += (lbd - slow)/(double)(1<<14);
		// fast += (lbd - fast)/(double)(1<<5);
		// See Armin Biere's POS'15 slides.
		if (lbd == LBD_NO_VALUE) {
			throw oops("Conflict has no LBD value.");
		}
		fast -= fast >> 5;
		fast += lbd << (32 - 5);
		slow -= slow >> 14;
		slow += lbd << (32 - 14);
	}

	int getTotalRestarts() {
		return numTotalRestarts;
	}

	private void nextLuby() {
		// Compute Luby-sequence [Knuth'12]-style.
		if ((un & -un) == vn) {
			un++;
			vn = 1;
		} else {
			vn <<= 1;
		}
		lubyLimit = vn * LUBY_FACTOR + learnedNoGoodDeletion.getNumTotalConflicts();
	}
}
