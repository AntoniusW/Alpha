package at.ac.tuwien.kr.alpha.core.solver;

import static at.ac.tuwien.kr.alpha.api.Util.oops;

/**
 * Represents a shallow {@link Antecedent} that must be instantiated before use.
 *
 * Copyright (c) 2020, the Alpha Team.
 */
public interface ShallowAntecedent extends Antecedent {

	/**
	 * Instantiates the shallow antecedent.
	 * @param impliedLiteral the literal that is implied by this {@link ShallowAntecedent}.
	 * @return a (full) {@link Antecedent}.
	 */
	Antecedent instantiateAntecedent(int impliedLiteral);

	@Override
	default int[] getReasonLiterals() {
		throw oops("ShallowAntecedent must be instantiated first.");
	}

	@Override
	default void bumpActivity() {
		throw oops("ShallowAntecedent must be instantiated first.");
	}

	@Override
	default void decreaseActivity() {
		throw oops("ShallowAntecedent must be instantiated first.");
	}
}
