package at.ac.tuwien.kr.alpha.core.solver;

/**
 * An interface to reasons of implications as used internally by the solver. This is a lightweight {@link at.ac.tuwien.kr.alpha.core.common.NoGood} that only
 * provides an array of literals (in some order) and has an activity that may change.
 *
 * Copyright (c) 2019, the Alpha Team.
 */
public interface Antecedent {

	int[] getReasonLiterals();

	void bumpActivity();

	void decreaseActivity();

}
