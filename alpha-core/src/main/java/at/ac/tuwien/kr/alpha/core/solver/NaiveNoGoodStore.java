/**
 * Copyright (c) 2017-2019, the Alpha Team.
 * All rights reserved.
 *
 * Additional changes made by Siemens.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.core.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.core.common.NoGood;

import java.util.HashMap;

import static at.ac.tuwien.kr.alpha.core.atoms.Literals.*;
import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.*;

public class NaiveNoGoodStore implements NoGoodStore {
	private static final Logger LOGGER = LoggerFactory.getLogger(NaiveNoGoodStore.class);

	private HashMap<Integer, NoGood> delegate = new HashMap<>();
	private final WritableAssignment assignment;
	private final NoGoodCounter counter = new NoGoodCounter();

	private boolean hasInferredAssignments;

	public NaiveNoGoodStore(WritableAssignment assignment) {
		this.assignment = assignment;
	}

	void clear() {
		assignment.clear();
		delegate.clear();
	}

	@Override
	public ConflictCause add(int id, NoGood noGood, int lbd) {
		counter.add(noGood);
		if (assignment.violates(noGood)) {
			return new ConflictCause(noGood.asAntecedent());
		}

		delegate.put(id, noGood);
		return null;
	}

	@Override
	public ConflictCause add(int id, NoGood noGood) {
		return add(id, noGood, Integer.MAX_VALUE);
	}

	@Override
	public ConflictCause propagate() {
		hasInferredAssignments = false;

		boolean any = false;
		boolean retry;

		do {
			retry = false;
			ConflictCause conflictCause;
			for (NoGood noGood : delegate.values()) {
				hasInferredAssignments = false;
				conflictCause = propagateWeakly(noGood);
				if (conflictCause != null) {
					return conflictCause;
				}
				if (hasInferredAssignments) {
					any = true;
					hasInferredAssignments = false;
					retry = true;
				}
			}
			for (NoGood noGood : delegate.values()) {
				hasInferredAssignments = false;
				conflictCause = propagateStrongly(noGood);
				if (conflictCause != null) {
					return conflictCause;
				}
				if (hasInferredAssignments) {
					any = true;
					hasInferredAssignments = false;
					retry = true;
				}
			}
		} while (retry);

		for (NoGood noGood : delegate.values()) {
			if (assignment.violates(noGood)) {
				return new ConflictCause(noGood.asAntecedent());
			}
		}

		if (any) {
			hasInferredAssignments = true;
		}

		return null;
	}

	@Override
	public boolean didPropagate() {
		return hasInferredAssignments;
	}

	@Override
	public void backtrack() {
		assignment.backtrack();
	}

	@Override
	public void growForMaxAtomId(int maxAtomId) {
	}

	@Override
	public NoGoodCounter getNoGoodCounter() {
		return counter;
	}

	@Override
	public void cleanupLearnedNoGoods() {
	}

	/**
	 * Infer an assignment from a nogood if it is weakly unit.
	 *
	 * This method iterates over all literals in the given nogood
	 * in order to check whether it is weakly unit. If the nogood
	 * turns out to be unit, then an assignment is generated and
	 * {@code true} is returned. Otherwise, {@code false} is
	 * returned.
	 *
	 * @param noGood the nogood to analyze.
	 * @return {@code true} iff an assignment was inferred,
	 *         {@code false} otherwise.
	 */
	private ConflictCause propagateWeakly(NoGood noGood) {
		int index = -1;
		for (int i = 0; i < noGood.size(); i++) {
			final int literal = noGood.getLiteral(i);

			if (assignment.isAssigned(atomOf(literal))) {
				if (!assignment.isViolated(literal)) {
					// Literal is satisfied!
					return null;
				}
			} else if (index != -1) {
				// There is more than one unassigned literal!
				return null;
			} else {
				index = i;
			}
		}

		if (index == -1) {
			return null;
		}

		hasInferredAssignments = true;

		final int literal = noGood.getLiteral(index);
		return assignment.assign(atomOf(literal), isNegated(literal) ? MBT : FALSE, noGood.asAntecedent());
	}

	/**
	 * Infer an assignment from a nogood if it is strongly unit.
	 *
	 * This method iterates over all literals in the given nogood
	 * in order to check whether it is strongly unit. If the nogood
	 * turns out to be unit, then an assignment for the head is
	 * generated and {@code true} is returned. Otherwise,
	 * {@code false} is returned.
	 *
	 * @param noGood the nogood to analyze.
	 * @return {@code true} iff an assignment was inferred,
	 *         {@code false} otherwise.
	 */
	private ConflictCause propagateStrongly(NoGood noGood) {
		if (!noGood.hasHead()) {
			return null;
		}

		final int headAtom = atomOf(noGood.getHead());

		if (assignment.getTruth(headAtom) != MBT) {
			return null;
		}

		// Check that NoGood is violated except for the head
		// (i.e., without the head set it would be unit) and
		// that none of the true values are assigned MBT, but
		// instead are all TRUE.
		for (int i = 1; i < noGood.size(); i++) {
			final int literal = noGood.getLiteral(i);

			if (!assignment.isViolated(literal)) {
				return null;
			}

			// Skip if positive literal is assigned MBT.
			if (isPositive(literal) && assignment.getTruth(atomOf(literal)) != TRUE) {
				return null;
			}
		}

		hasInferredAssignments = true;

		return assignment.assign(headAtom, TRUE, noGood.asAntecedent());
	}
}
