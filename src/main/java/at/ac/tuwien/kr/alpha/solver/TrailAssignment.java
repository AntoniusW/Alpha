/*
 * Copyright (c) 2016-2021, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.common.Assignment;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.IntIterator;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static at.ac.tuwien.kr.alpha.Util.arrayGrowthSize;
import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.Literals.*;
import static at.ac.tuwien.kr.alpha.solver.Atoms.isAtom;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.*;

/**
 * An implementation of Assignment using a trail (of literals) and arrays as underlying structures for storing
 * assignments.
 *
 * Copyright (c) 2018-2020, the Alpha Team.
 */
public class TrailAssignment implements WritableAssignment, Checkable {
	private static final Logger LOGGER = LoggerFactory.getLogger(TrailAssignment.class);
	static final Antecedent CLOSING_INDICATOR_ANTECEDENT = new Antecedent() {
		int[] literals = new int[0];

		@Override
		public int[] getReasonLiterals() {
			return literals;
		}

		@Override
		public void bumpActivity() {
		}

		@Override
		public void decreaseActivity() {

		}
	};

	private final AtomStore atomStore;
	private ChoiceManager choiceManager;

	/**
	 * Contains for each known atom a value whose two least
	 * significant bits encode the atom's truth value
	 * (cf. {@link TrailAssignment#translateTruth(int)}
	 * and whose remaining bits encode the atom's weak decision level. 
	 */
	private int[] values;

	private int[] strongDecisionLevels;
	private Antecedent[] impliedBy;
	private boolean[] callbackUponChange;
	private ArrayList<OutOfOrderLiteral> outOfOrderLiterals = new ArrayList<>();
	private int highestDecisionLevelContainingOutOfOrderLiterals;
	private int[] trail = new int[0];
	private int trailSize;
	private ArrayList<Integer> trailIndicesOfDecisionLevels = new ArrayList<>();

	private int nextPositionInTrail;
	private int newAssignmentsPositionInTrail;
	private int newAssignmentsIterator;
	private int assignmentsForChoicePosition;
	private int mbtCount;
	private boolean checksEnabled;
	long replayCounter;

	public TrailAssignment(AtomStore atomStore, boolean checksEnabled) {
		this.checksEnabled = checksEnabled;
		this.atomStore = atomStore;
		this.values = new int[0];
		this.strongDecisionLevels = new int[0];
		this.impliedBy = new Antecedent[0];
		this.callbackUponChange = new boolean[0];
		this.trailIndicesOfDecisionLevels.add(0);
		nextPositionInTrail = 0;
		newAssignmentsIterator = 0;
		newAssignmentsPositionInTrail = 0;
		assignmentsForChoicePosition = 0;
	}

	public TrailAssignment(AtomStore atomStore) {
		this(atomStore, false);
	}

	@Override
	public void clear() {
		mbtCount = 0;
		Arrays.fill(values, 0);
		Arrays.fill(strongDecisionLevels, -1);
		Arrays.fill(impliedBy, null);
		Arrays.fill(callbackUponChange, false);
		outOfOrderLiterals = new ArrayList<>();
		highestDecisionLevelContainingOutOfOrderLiterals = 0;
		Arrays.fill(trail, 0);
		trailSize = 0;
		trailIndicesOfDecisionLevels = new ArrayList<>();
		trailIndicesOfDecisionLevels.add(0);
		nextPositionInTrail = 0;
		newAssignmentsIterator = 0;
		newAssignmentsPositionInTrail = 0;
		assignmentsForChoicePosition = 0;
	}

	@Override
	public void registerCallbackOnChange(int atom) {
		callbackUponChange[atom] = true;
	}

	@Override
	public void setCallback(ChoiceManager choiceManager) {
		this.choiceManager = choiceManager;
	}

	@Override
	public boolean isAssigned(int atom) {
		return values[atom] != 0;
	}

	@Override
	public int getBasicAtomAssignedMBT() {
		for (int atom = 1; atom <= atomStore.getMaxAtomId(); atom++) {
			if (getTruth(atom) == MBT && atomStore.get(atom) instanceof BasicAtom) {
				return atom;
			}
		}
		throw oops("No BasicAtom is assigned MBT.");
	}

	@Override
	public Pollable getAssignmentsToProcess() {
		return new TrailPollable();
	}

	@Override
	public int getRealWeakDecisionLevel(int atom) {
		if (getTruth(atom) == null) {
			return -1;
		}
		int lowestDecisionLevelForAtom = getWeakDecisionLevel(atom);
		for (OutOfOrderLiteral outOfOrderLiteral : outOfOrderLiterals) {
			if (outOfOrderLiteral.atom == atom && outOfOrderLiteral.decisionLevel < lowestDecisionLevelForAtom) {
				lowestDecisionLevelForAtom = outOfOrderLiteral.decisionLevel;
			}
		}
		return lowestDecisionLevelForAtom;
	}

	/**
	 * Searches out-of-order literals for the lowest decision level the atom is assigned in.
	 * @param atom the atom to check.
	 * @return Integer.MAX_VALUE if the atom is not assigned out-of-order, otherwise the lowest decision level it is assigned.
	 */
	public int getOutOfOrderDecisionLevel(int atom) {
		int lowestDecisionLevel = Integer.MAX_VALUE;
		for (OutOfOrderLiteral outOfOrderLiteral : outOfOrderLiterals) {
			if (outOfOrderLiteral.atom == atom && outOfOrderLiteral.decisionLevel < lowestDecisionLevel) {
				lowestDecisionLevel = outOfOrderLiteral.decisionLevel;
			}
		}
		return lowestDecisionLevel;
	}

	int getOutOfOrderStrongDecisionLevel(int atom) {
		int lowestDecisionLevel = Integer.MAX_VALUE;
		for (OutOfOrderLiteral outOfOrderLiteral : outOfOrderLiterals) {
			if (outOfOrderLiteral.atom == atom && outOfOrderLiteral.value == TRUE && outOfOrderLiteral.decisionLevel < lowestDecisionLevel) {
				lowestDecisionLevel = outOfOrderLiteral.decisionLevel;
			}
		}
		return lowestDecisionLevel;
	}

	private void informCallback(int atom) {
		if (callbackUponChange[atom]) {
			choiceManager.callbackOnChanged(atom);
		}
	}

	private void removeLastDecisionLevel() {
		// Remove all atoms recorded in the highest decision level.
		int start = trailIndicesOfDecisionLevels.get(getDecisionLevel());
		for (int i = start; i < trailSize; i++) {
			int backtrackAtom = atomOf(trail[i]); //atomOf(backtrackIterator.next());
			// Skip already backtracked atoms.
			if (getTruth(backtrackAtom) == null) {
				continue;
			}
			if (getWeakDecisionLevel(backtrackAtom) < getDecisionLevel()) {
				// Restore TRUE to MBT if this was assigned at a lower level.
				if (getTruth(backtrackAtom) != TRUE) {
					throw oops("Backtracking assignment with lower decision level whose value is not TRUE.");
				}
				values[backtrackAtom] = (getWeakDecisionLevel(backtrackAtom) << 2) | translateTruth(MBT);
				mbtCount++;
			} else {
				if (getTruth(backtrackAtom) == MBT) {
					mbtCount--;
				}
				values[backtrackAtom] = 0;
			}
			strongDecisionLevels[backtrackAtom] = -1;
			informCallback(backtrackAtom);
		}
		// Remove atoms from trail.
		trailSize = start;
		trailIndicesOfDecisionLevels.remove(trailIndicesOfDecisionLevels.size() - 1);
	}

	private void replayOutOfOrderLiterals() {
		// Replay out-of-order assigned literals.
		if (highestDecisionLevelContainingOutOfOrderLiterals >= getDecisionLevel()) {
			// Replay all still-valid literals and remove invalid ones.
			LOGGER.trace("Replaying out-of-order literals.");
			int k = 0; // counter for out-of-order literals to keep further.
			for (OutOfOrderLiteral outOfOrderLiteral : outOfOrderLiterals) {
				if (outOfOrderLiteral.decisionLevel <= getDecisionLevel()) {
					// Replay assignment at current decision level.
					assign(outOfOrderLiteral.atom, outOfOrderLiteral.value, outOfOrderLiteral.impliedBy);
					// If literal is actually below current decision level, keep it.
					if (outOfOrderLiteral.decisionLevel < getDecisionLevel()) {
						outOfOrderLiterals.set(k++, outOfOrderLiteral);
					}
				}
			}
			replayCounter += outOfOrderLiterals.size();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Replay list contained {} literals, {} were again assigned. Overall replays: {}.", outOfOrderLiterals.size(), k, replayCounter);
			}
			// Remove remaining entries from k onwards.
			while (outOfOrderLiterals.size() > k) {
				outOfOrderLiterals.remove(outOfOrderLiterals.size() - 1);
			}
			highestDecisionLevelContainingOutOfOrderLiterals = outOfOrderLiterals.isEmpty() ? 0 : getDecisionLevel();
		}
	}

	private void resetTrailPointersAndReplayOutOfOrderLiterals() {
		nextPositionInTrail = Math.min(nextPositionInTrail, trailSize);
		newAssignmentsPositionInTrail = Math.min(newAssignmentsPositionInTrail, trailSize);
		newAssignmentsIterator = Math.min(newAssignmentsIterator, trailSize);
		assignmentsForChoicePosition = Math.min(assignmentsForChoicePosition, trailSize);
		replayOutOfOrderLiterals();
		if (checksEnabled) {
			runInternalChecks();
		}
	}

	@Override
	public void backjump(int decisionLevel) {
		// Remove everything above the target level, but keep the target level unchanged.
		while (getDecisionLevel() > decisionLevel) {
			removeLastDecisionLevel();
		}
		resetTrailPointersAndReplayOutOfOrderLiterals();
	}

	@Override
	public void backtrack() {
		removeLastDecisionLevel();
		resetTrailPointersAndReplayOutOfOrderLiterals();
	}

	@Override
	public int getMBTCount() {
		return mbtCount;
	}

	@Override
	public ConflictCause choose(int atom, ThriceTruth value) {
		if (checksEnabled) {
			runInternalChecks();
		}
		trailIndicesOfDecisionLevels.add(trailSize);
		return assign(atom, value, null);
	}

	@Override
	public ConflictCause assign(int atom, ThriceTruth value, Antecedent impliedBy) {
		ConflictCause conflictCause = assignWithTrail(atom, value, impliedBy);
		if (conflictCause != null && LOGGER.isDebugEnabled()) {
			LOGGER.debug("Assign is conflicting: atom: {}, value: {}, impliedBy: {}.", atom, value, impliedBy);
		}
		return conflictCause;
	}

	@Override
	public ConflictCause assign(int atom, ThriceTruth value, Antecedent impliedBy, int decisionLevel) {
		ConflictCause conflictCause = assign(atom, value, impliedBy);
		if (conflictCause == null && decisionLevel < getDecisionLevel()) {
			outOfOrderLiterals.add(new OutOfOrderLiteral(atom, value, decisionLevel, impliedBy));
			if (highestDecisionLevelContainingOutOfOrderLiterals < getDecisionLevel()) {
				highestDecisionLevelContainingOutOfOrderLiterals = getDecisionLevel();
			}
		}
		return conflictCause;
	}

	private boolean assignmentsConsistent(ThriceTruth oldTruth, ThriceTruth value) {
		return oldTruth == null || oldTruth.toBoolean() == value.toBoolean();
	}

	private ConflictCause assignWithTrail(int atom, ThriceTruth value, Antecedent impliedBy) {
		if (!isAtom(atom)) {
			throw new IllegalArgumentException("not an atom");
		}
		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recording assignment {}={}@{} impliedBy: {}", atom, value, getDecisionLevel(), impliedBy);
			if (impliedBy != null) {
				for (Integer literal : impliedBy.getReasonLiterals()) {
					LOGGER.trace("impliedBy literal assignment: {}={}.", atomOf(literal), get(atomOf(literal)));
				}
			}
		}

		// If the atom currently is not assigned, simply record the assignment.
		final ThriceTruth currentTruth = getTruth(atom);
		if (currentTruth == null) {
			trail[trailSize++] = atomToLiteral(atom, value.toBoolean());
			values[atom] = (getDecisionLevel() << 2) | translateTruth(value);
			this.impliedBy[atom] = impliedBy;
			// Adjust MBT counter.
			if (value == MBT) {
				mbtCount++;
			}
			if (value == TRUE) {
				strongDecisionLevels[atom] = getDecisionLevel();
			}
			informCallback(atom);
			return null;
		}

		// Nothing to do if the new value is the same as the current one (or current is TRUE and new is MBT).
		if (value == currentTruth || value == MBT && currentTruth == TRUE) {
			return null;
		}

		// Check if the new assignments contradicts the current one.
		if (!assignmentsConsistent(currentTruth, value)) {
			// Assignments are inconsistent, prepare the reason.
			// Due to the conflict, the impliedBy NoGood is not the recorded one but the one this method is invoked with.stems from this assignment.
			if (impliedBy instanceof ShallowAntecedent) {
				// Instantiate Antecedent in case it is a shallow one for binary nogoods.
				return new ConflictCause(((ShallowAntecedent) impliedBy).instantiateAntecedent(atomToLiteral(atom, !value.toBoolean())));
			}
			return new ConflictCause(impliedBy);
		}
		if (value == TRUE && currentTruth != MBT) {
			throw oops("Assigning TRUE without assigning MBT before.");
		}

		// Previous assignment exists, and the new one is consistent with it.
		// There is nothing to do except if MBT becomes TRUE.
		if (currentTruth == MBT && value == TRUE) {
			// Record new truth value but keep weak decision level unchanged.
			trail[trailSize++] = atomToLiteral(atom, true);
			values[atom] = (getWeakDecisionLevel(atom) << 2) | translateTruth(TRUE);
			// Record strong decision level.
			strongDecisionLevels[atom] = getDecisionLevel();
			// Adjust MBT counter.
			mbtCount--;
			informCallback(atom);
			return null;
		}
		throw oops("Assignment conditions are not covered.");
	}

	private ThriceTruth translateTruth(int value) {
		switch (value & 0x3) {
			case 0:
				return null;
			case 1:
				return FALSE;
			case 2:
				return MBT;
			case 3:
				return TRUE;
			default:
				throw oops("Unknown truth value.");
		}
	}

	private int translateTruth(ThriceTruth value) {
		if (value == null) {
			return 0;
		}
		switch (value) {
			case FALSE:
				return 1;
			case MBT:
				return 2;
			case TRUE:
				return 3;
		}
		throw oops("Unknown truth value.");
	}

	@Override
	public ThriceTruth getTruth(int atom) {
		return translateTruth(values[atom]);
	}

	@Override
	public int getWeakDecisionLevel(int atom) {
		return values[atom] >> 2;
	}

	@Override
	public int getStrongDecisionLevel(int atom) {
		return getTruth(atom) == FALSE ? getWeakDecisionLevel(atom) : strongDecisionLevels[atom];
	}

	@Override
	public Antecedent getImpliedBy(int atom) {
		Antecedent antecedent = impliedBy[atom];
		// Check if the Antecedent is a shallow one from binary nogoods, in this case instantiate it to a full Antecedent.
		if (antecedent instanceof ShallowAntecedent) {
			return ((ShallowAntecedent) antecedent).instantiateAntecedent(atomToLiteral(atom, !getTruth(atom).toBoolean()));
		}
		return antecedent;
	}

	@Override
	public Set<Integer> getTrueAssignments() {
		Set<Integer> result = new HashSet<>();
		for (int i = 0; i < values.length; i++) {
			if (getTruth(i) == TRUE) {
				result.add(i);
			}
		}
		return result;
	}

	@Override
	public Entry get(int atom) {
		if (values[atom] == 0) {
			return null;
		}
		if (strongDecisionLevels[atom] == -1) {
			return new Entry(getTruth(atom), atom, getWeakDecisionLevel(atom), getImpliedBy(atom));
		} else {
			return new Entry(getTruth(atom), atom, strongDecisionLevels[atom], getImpliedBy(atom), getWeakDecisionLevel(atom));
		}
	}

	private void runInternalChecks() {
		// Ensure that incrementally updated values agree with actual values.
		LOGGER.trace("Checking assignment.");
		// Check that MBT counter is correct.
		if (getMBTCount() != getMBTAssignedAtoms().size()) {
			throw oops("MBT counter and amount of actually MBT-assigned atoms disagree");
		} else {
			LOGGER.trace("MBT count agrees with amount of MBT-assigned atoms.");
		}
		// Check that out of order literals are actually assigned.
		for (OutOfOrderLiteral outOfOrderLiteral : outOfOrderLiterals) {
			if (outOfOrderLiteral.decisionLevel <= getDecisionLevel()) {
				ThriceTruth atomTruth = getTruth(outOfOrderLiteral.atom);
				if (outOfOrderLiteral.value == atomTruth || outOfOrderLiteral.value == MBT && atomTruth == TRUE) {
					continue;
				}
				throw oops("Out-of-order assigned literal is not in current assignment.");
			}
		}
		LOGGER.trace("Checking assignment: all good.");
	}

	/**
	 * Debug helper collecting all atoms that are assigned MBT.
	 * @return a list of all atomIds that are assigned MBT (and not TRUE).
	 */
	private List<Integer> getMBTAssignedAtoms() {
		List<Integer> ret = new ArrayList<>();
		for (int i = 0; i < values.length; i++) {
			if (getTruth(i) == MBT) {
				ret.add(i);
			}
		}
		return ret;
	}

	@Override
	public boolean closeUnassignedAtoms() {
		boolean didAssign = false;
		for (int i = 1; i <= atomStore.getMaxAtomId(); i++) {
			if (!isAssigned(i)) {
				assign(i, FALSE, CLOSING_INDICATOR_ANTECEDENT);
				didAssign = true;
			}
		}
		return didAssign;
	}

	@Override
	public void growForMaxAtomId() {
		int maxAtomId = atomStore.getMaxAtomId();
		// Grow arrays only if needed.
		if (values.length > maxAtomId) {
			return;
		}
		// Grow by 1.5 current size, except if bigger array is required due to maxAtomId.
		int newCapacity = arrayGrowthSize(values.length);
		if (newCapacity < maxAtomId + 1) {
			newCapacity = maxAtomId + 1;
		}
		values = Arrays.copyOf(values, newCapacity);
		int oldLength = strongDecisionLevels.length;
		strongDecisionLevels = Arrays.copyOf(strongDecisionLevels, newCapacity);
		Arrays.fill(strongDecisionLevels, oldLength, strongDecisionLevels.length, -1);
		impliedBy = Arrays.copyOf(impliedBy, newCapacity);
		callbackUponChange = Arrays.copyOf(callbackUponChange, newCapacity);
		trail = Arrays.copyOf(trail, newCapacity * 2);	// Trail has at most 2 assignments (MBT+TRUE) for each atom.
	}

	public int getNumberOfAssignedAtoms() {
		int n = 0;
		for (int value : values) {
			if (translateTruth(value) != null) {
				n++;
			}
		}
		return n;
	}

	@Override
	public int getNumberOfAtomsAssignedSinceLastDecision() {
		Set<Integer> newlyAssignedAtoms = new HashSet<>();
		int trailIndex = trailIndicesOfDecisionLevels.get(getDecisionLevel());
		for (; trailIndex < trailSize; trailIndex++) {
			newlyAssignedAtoms.add(atomOf(trail[trailIndex]));
		}
		return newlyAssignedAtoms.size();
	}

	@Override
	public int getNumberOfActiveChoicePoints() {
		if (choiceManager == null) {
			throw new UnsupportedOperationException("No ChoiceManager known by TrailAssignment");
		}
		return choiceManager.getNumberOfActiveChoicePoints();
	}

	@Override
	public int getDecisionLevel() {
		return trailIndicesOfDecisionLevels.size() - 1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		boolean isFirst = true;
		for (int i = 0; i < values.length; i++) {
			ThriceTruth atomTruth = getTruth(i);
			if (atomTruth == null) {
				continue;
			}
			if (!isFirst) {
				sb.append(", ");
			}
			isFirst = false;
			sb.append(atomTruth);
			sb.append("_");
			if (atomStore != null) {
				sb.append(atomStore.atomToString(i));
			} else {
				sb.append(i);
			}
			sb.append("@");
			sb.append(getWeakDecisionLevel(i));
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public AssignmentIterator getNewPositiveAssignmentsIterator() {
		return new AssignmentIterator();
	}

	@Override
	public void setChecksEnabled(boolean checksEnabled) {
		this.checksEnabled = checksEnabled;
	}

	private class AssignmentIterator implements IntIterator {

		private void advanceCursorToNextPositiveAssignment() {
			while (newAssignmentsIterator < trailSize) {
				ThriceTruth truth = getTruth(atomOf(trail[newAssignmentsIterator]));
				if (truth != null && truth.toBoolean()) {
					return;
				}
				newAssignmentsIterator++;
			}
		}

		@Override
		public boolean hasNext() {
			advanceCursorToNextPositiveAssignment();
			return newAssignmentsIterator < trailSize;
		}

		@Override
		public int next() {
			return atomOf(trail[newAssignmentsIterator++]);

		}
	}

	private static class OutOfOrderLiteral {
		final int atom;
		final ThriceTruth value;
		final int decisionLevel;
		final Antecedent impliedBy;

		private OutOfOrderLiteral(int atom, ThriceTruth value, int decisionLevel, Antecedent impliedBy) {
			this.atom = atom;
			this.decisionLevel = decisionLevel;
			this.impliedBy = impliedBy;
			this.value = value;
		}

		@Override
		public String toString() {
			return atom + "=" + value + "@" + decisionLevel + " implied by " + impliedBy;
		}
	}

	private class TrailPollable implements Pollable {

		@Override
		public int peek() {
			return atomOf(trail[nextPositionInTrail]);
		}

		@Override
		public int remove() {
			int current = peek();
			nextPositionInTrail++;
			return current;
		}

		@Override
		public boolean isEmpty() {
			return nextPositionInTrail >= trailSize;
		}
	}

	public TrailBackwardsWalker getTrailBackwardsWalker() {
		return new TrailBackwardsWalker();
	}

	public class TrailBackwardsWalker {

		int trailPos;

		TrailBackwardsWalker() {
			trailPos = trailSize;
		}

		public int getNextLowerLiteral() {
			return trail[--trailPos];
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("[");
			for (int i = 0; i < trailSize; i++) {
				int literalOnTrail = trail[i];
				sb.append(isPositive(literalOnTrail) ? "+" + atomOf(literalOnTrail) : "-" + atomOf(literalOnTrail));
				sb.append("@");
				sb.append(TrailAssignment.this.getWeakDecisionLevel(atomOf(literalOnTrail)));
				sb.append(", ");
			}
			sb.append("]");
			return sb.toString();
		}
	}

	private static final class Entry implements Assignment.Entry {
		private final ThriceTruth value;
		private final int decisionLevel;
		private final int previousDecisionLevel;
		private final Antecedent impliedBy;
		private final int atom;

		Entry(ThriceTruth value, int atom, int decisionLevel, Antecedent noGood) {
			this(value, atom, decisionLevel, noGood, -1);
		}

		Entry(ThriceTruth value, int atom, int decisionLevel, Antecedent impliedBy, int previousDecisionLevel) {
			this.value = value;
			this.decisionLevel = decisionLevel;
			this.impliedBy = impliedBy;
			this.previousDecisionLevel = previousDecisionLevel;
			this.atom = atom;
			if (previousDecisionLevel != -1 && value != TRUE) {
				throw oops("Assignment.Entry instantiated with previous entry set and truth values other than TRUE now and MBT previous");
			}
		}

		@Override
		public ThriceTruth getTruth() {
			return value;
		}

		@Override
		public int getDecisionLevel() {
			return decisionLevel;
		}

		@Override
		public boolean hasPreviousMBT() {
			return previousDecisionLevel != -1;
		}

		@Override
		public int getMBTDecisionLevel() {
			return previousDecisionLevel;
		}

		@Override
		public Antecedent getImpliedBy() {
			return impliedBy;
		}

		@Override
		public int getAtom() {
			return atom;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Entry entry = (Entry) o;

			return atom == entry.atom;
		}

		@Override
		public int hashCode() {
			return atom;
		}

		@Override
		public String toString() {
			return atom + "=" + value.toString() + "(DL" + decisionLevel + ")"
				+ (hasPreviousMBT() ? "MBT(DL" + getMBTDecisionLevel() + ")" : "");
		}
	}
}
