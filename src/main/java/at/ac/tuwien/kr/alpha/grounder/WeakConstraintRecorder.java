package at.ac.tuwien.kr.alpha.grounder;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporarily stores associations of weak constraint atoms, and their weights and levels.
 *
 * Copyright (c) 2020, the Alpha Team.
 */
class WeakConstraintRecorder {

	private ArrayList<Triple<Integer, Integer, Integer>> weakConstraintAtomWeightLevels = new ArrayList<>();

	void addWeakConstraint(int headId, Integer weight, Integer level) {
		weakConstraintAtomWeightLevels.add(new ImmutableTriple<>(headId, weight, level));
	}

	List<Triple<Integer, Integer, Integer>> getAndResetWeakConstraintAtomWeightLevels() {
		List<Triple<Integer, Integer, Integer>> currentWeakConstraintAtomWeightLevels = weakConstraintAtomWeightLevels;
		this.weakConstraintAtomWeightLevels = new ArrayList<>();
		return currentWeakConstraintAtomWeightLevels;
	}
}
