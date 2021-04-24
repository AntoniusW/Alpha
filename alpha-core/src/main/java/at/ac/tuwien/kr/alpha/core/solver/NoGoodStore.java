package at.ac.tuwien.kr.alpha.core.solver;

import at.ac.tuwien.kr.alpha.core.common.NoGood;

/**
 * An interface defining the use of a NoGood store.
 *
 * Copyright (c) 2016-2019, the Alpha Team.
 */
public interface NoGoodStore {
	int LBD_NO_VALUE = -1;

	/**
	 * Adds a nogood with the given id.
	 * @param id the unique identifier of the nogood.
	 * @param noGood the nogood to add.
	 * @param lbd the literals block distance.
	 * @return {@code null} if the noGood was added without conflict, a {@link ConflictCause} describing
	 *         the conflict otherwise.
	 */
	ConflictCause add(int id, NoGood noGood, int lbd);

	/**
	 * Adds a NoGood with no LBD value set.
	 * @param id the unique identifier of the NoGood.
	 * @param noGood the NoGood to add.
	 * @return {@code null} if the noGood was added without conflict, a {@link ConflictCause} describing
* 	 *         the conflict otherwise.
	 */
	ConflictCause add(int id, NoGood noGood);

	/**
	 * Apply weak propagation and strong propagation. Propagation should stop as soon as some nogood is violated.
	 * @return some cause iff a conflict was reached or {@code null} otherwise
	 */
	ConflictCause propagate();

	/**
	 * After a call to {@link #propagate()} this method provides
	 * whether propagation was successful, i.e. at least one new
	 * assignment was inferred.
	 * @return {@code true} iff the last call to {@link #propagate()}
	 *         inferred at least one assignment, {@code false} otherwise.
	 */
	boolean didPropagate();

	void backtrack();

	void growForMaxAtomId(int maxAtomId);

	/**
	 * Tests whether a cleanup of the learned NoGoods database is appropriate and exectutes the cleaning if
	 * necessary.
	 */
	void cleanupLearnedNoGoods();

	NoGoodCounter getNoGoodCounter();
}
