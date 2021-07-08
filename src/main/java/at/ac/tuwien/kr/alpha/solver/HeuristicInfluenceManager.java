/*
 * Copyright (c) 2017-2021, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.IDX_F;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.IDX_M;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.IDX_T;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.IDX_TM;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.MBT;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.TRUE;

/**
 * Manages influence of atoms on the activity of heuristics.
 *
 */
public class HeuristicInfluenceManager extends InfluenceManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(HeuristicInfluenceManager.class);

	// Maps heuristic choice points to all atoms that influence them (enablers, disablers, heuristic choice atom itself).
	private final Map<Integer, HeuristicChoicePoint> influencers = new HashMap<>();

	// Maps head atoms of heuristics to heuristic choice points of heuristics with those atoms in the head (there might be several)
	private final MultiValuedMap<Integer, HeuristicChoicePoint> headAtomsToHeuristics = new HashSetValuedHashMap<>();

	public HeuristicInfluenceManager(WritableAssignment assignment) {
		super(assignment);
	}

	void addInformation(Pair<Map<Integer, Integer[]>, Map<Integer, Integer[]>> heuristicAtoms, Map<Integer, HeuristicDirectiveValues> heuristicValues) {
		// Assumption: we get all enabler/disabler pairs in one call.
		Map<Integer, Integer[]> enablers = heuristicAtoms.getLeft();
		Map<Integer, Integer[]> disablers = heuristicAtoms.getRight();
		for (Map.Entry<Integer, Integer[]> atomToEnablers : enablers.entrySet()) {
			addInformation(atomToEnablers, disablers, heuristicValues);
		}
	}

	private void addInformation(Map.Entry<Integer, Integer[]> atomToEnablers, Map<Integer, Integer[]> atomsToDisablers,
								Map<Integer, HeuristicDirectiveValues> heuristicValues) {
		// Construct and record ChoicePoint.
		Integer choicePointAtom = atomToEnablers.getKey();
		if (choicePointAtom == null) {
			throw oops("Incomplete choice point description found (no atom)");
		}
		if (influencers.get(choicePointAtom) != null) {
			throw oops("Received choice information repeatedly");
		}
		Integer[] enablers = atomToEnablers.getValue();
		Integer[] disablers = atomsToDisablers.get(choicePointAtom);
		if (enablers == null || disablers == null) {
			throw oops("Incomplete choice point description found (no enabler or disabler)");
		}
		final int headAtom = heuristicValues.get(choicePointAtom).getHeadAtomId();
		registerCallbackOnChange(headAtom);
		registerCallbackOnChange(enablers);
		registerCallbackOnChange(disablers);
		HeuristicChoicePoint choicePoint = new HeuristicChoicePoint(choicePointAtom, headAtom, enablers, disablers);
		addHeuristicChoicePointToHeadAtomId(choicePoint, headAtom);
		addHeuristicChoicePointToInfluencers(choicePoint, enablers);
		addHeuristicChoicePointToInfluencers(choicePoint, disablers);
		choicePoint.recomputeActive();
	}

	private void registerCallbackOnChange(Integer... atoms) {
		for (Integer atom : atoms) {
			if (atom != null) {
				assignment.registerCallbackOnChange(atom);
			}
		}
	}

	private void addHeuristicChoicePointToInfluencers(HeuristicChoicePoint heuristicChoicePoint, Integer... atoms) {
		for (Integer atom : atoms) {
			if (atom != null) {
				if (influencers.put(atom, heuristicChoicePoint) != null) {
					throw oops("Multiple heuristic choice points mapped to same atom " + atom);
				}
			}
		}
	}

	private void addHeuristicChoicePointToHeadAtomId(HeuristicChoicePoint heuristicChoicePoint, int headAtomId) {
		headAtomsToHeuristics.put(headAtomId, heuristicChoicePoint);
	}

	boolean isActive(int atom) {
		HeuristicChoicePoint choicePoint = influencers.get(atom);
		return choicePoint != null && choicePoint.isActive && choicePoint.atom == atom;
	}

	void callbackOnChanged(int atom) {
		LOGGER.trace("Callback received on influencer atom: {}", atom);
		HeuristicChoicePoint influencedHeuristic = influencers.get(atom);
		if (influencedHeuristic != null) {
			influencedHeuristic.recomputeActive();
		}
		for (HeuristicChoicePoint heuristicForHead : headAtomsToHeuristics.get(atom)) {
			heuristicForHead.recomputeActive();
		}

	}

	private class HeuristicChoicePoint {
		final int atom;
		final int headAtom;
		final Integer[] enablers;
		final Integer[] disablers;
		boolean isActive;

		private HeuristicChoicePoint(int atom, int headAtom, Integer[] enablers, Integer[] disablers) {
			this.atom = atom;
			this.headAtom = headAtom;
			this.enablers = enablers;
			this.disablers = disablers;
		}

		/**
		 * A heuristic choice point is active if all of the following conditions hold:
		 * <ul>
		 * 	<li>enabler[T] is assigned TRUE</li>
		 * 	<li>enabler[TM] is assigned TRUE or MBT</li>
		 * 	<li>enabler[M] is assigned MBT</li>
		 * 	<li>enabler[F] is assigned TRUE</li>
		 * 	<li>disabler[T] is not assigned TRUE</li>
		 * 	<li>disabler[TM] is assigned neither TRUE nor MBT</li>
		 * 	<li>disabler[M] is not assigned MBT</li>
		 * 	<li>disabler[F] is not assigned TRUE.</li>
		 * </ul>
		 * @return {@code true} iff all the above conditions hold and the heuristic's head atom is unassigned or assigned MBT.
		 */
		private boolean isActive() {
			final ThriceTruth truthEnablerTM = getNullsafeTruth(enablers[IDX_TM]);
			boolean isActive = enablers[IDX_TM] == null || truthEnablerTM == TRUE || truthEnablerTM == MBT;
			isActive &= enablers[IDX_T] == null || getNullsafeTruth(enablers[IDX_T]) == TRUE;
			isActive &= enablers[IDX_M] == null || getNullsafeTruth(enablers[IDX_M]) == MBT;
			isActive &= enablers[IDX_F] == null || getNullsafeTruth(enablers[IDX_F]) == TRUE;

			final ThriceTruth truthDisablerTM = getNullsafeTruth(disablers[IDX_TM]);
			isActive &= truthDisablerTM != TRUE && truthDisablerTM != MBT;
			isActive &= getNullsafeTruth(disablers[IDX_T]) != TRUE;
			isActive &= getNullsafeTruth(disablers[IDX_M]) != MBT;
			isActive &= getNullsafeTruth(disablers[IDX_F]) != TRUE;

			isActive &= assignment.isUnassignedOrMBT(headAtom);
			return isActive;
		}

		private ThriceTruth getNullsafeTruth(Integer atom) {
			return atom == null ? null : assignment.getTruth(atom);
		}

		private boolean isNotChosen() {
			ThriceTruth atomTruth = assignment.getTruth(atom);
			return atomTruth == null || atomTruth == MBT;
		}

		void recomputeActive() {
			LOGGER.trace("Recomputing activity of atom {}.", atom);
			final boolean wasActive = isActive;
			isActive = isNotChosen() && isActive();
			boolean changed = false;
			if (isActive && !wasActive) {
				activeChoicePointsAtoms.add(atom);
				changed = true;
				LOGGER.debug("Activating choice point for atom {}", this.atom);
			} else if (wasActive && !isActive) {
				activeChoicePointsAtoms.remove(atom);
				changed = true;
				LOGGER.debug("Deactivating choice point for atom {}", this.atom);
			}
			if (changed && activityListener != null) {
				activityListener.callbackOnChanged(atom, isActive);
			}
		}

		@Override
		public String toString() {
			return String.valueOf(atom);
		}
	}

}
