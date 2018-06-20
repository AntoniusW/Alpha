/**
 * Copyright (c) 2016-2017, the Alpha Team.
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
import at.ac.tuwien.kr.alpha.common.AtomTranslator;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.grounder.Grounder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;
import static at.ac.tuwien.kr.alpha.solver.Atoms.isAtom;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.*;

/**
 * An implementation of the Assignment using ArrayList as underlying structure for storing assignment entries.
 */
public class ArrayAssignment implements WritableAssignment, Checkable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArrayAssignment.class);

	private final AtomTranslator translator;
	private final ArrayList<Entry> assignment = new ArrayList<>();

	private final List<List<Integer>> atomsAssignedInDecisionLevel;
	private final ArrayList<Integer> propagationCounterPerDecisionLevel;
	private final Queue<ArrayAssignment.Entry> assignmentsToProcess = new LinkedList<>();
	private Queue<Assignment.Entry> newAssignments = new LinkedList<>();
	private Queue<Assignment.Entry> newAssignmentsForChoice = new LinkedList<>();
	private int[] values;
	private int[] strongDecisionLevels;
	private NoGood[] impliedBy;

	private int mbtCount;
	private boolean checksEnabled;

	public ArrayAssignment(AtomTranslator translator, boolean checksEnabled) {
		this.checksEnabled = checksEnabled;
		this.translator = translator;
		this.atomsAssignedInDecisionLevel = new ArrayList<>();
		this.atomsAssignedInDecisionLevel.add(new ArrayList<>());
		this.propagationCounterPerDecisionLevel = new ArrayList<>();
		this.propagationCounterPerDecisionLevel.add(0);
		this.values = new int[0];
		this.strongDecisionLevels = new int[0];
		this.impliedBy = new NoGood[0];
	}

	public ArrayAssignment(Grounder translator) {
		this(translator, false);
	}

	public ArrayAssignment() {
		this(null, false);
	}

	@Override
	public void clear() {
		atomsAssignedInDecisionLevel.clear();
		atomsAssignedInDecisionLevel.add(new ArrayList<>());
		assignment.clear();
		mbtCount = 0;
		Arrays.fill(values, 0);
		Arrays.fill(strongDecisionLevels, -1);
	}

	private static class ArrayPollable implements Pollable<ArrayAssignment.Entry> {
		private final Queue<ArrayAssignment.Entry> delegate;

		private ArrayPollable(Queue<Entry> delegate) {
			this.delegate = delegate;
		}

		@Override
		public Entry peek() {
			return delegate.peek();
		}

		@Override
		public Entry remove() {
			return delegate.remove();
		}

		@Override
		public boolean isEmpty() {
			return delegate.isEmpty();
		}
	}

	@Override
	public Pollable<? extends Assignment.Entry> getAssignmentsToProcess() {
		return new ArrayPollable(assignmentsToProcess);
	}

	@Override
	public void backtrack() {
		backtrackWithLowerAssignments();
	}

	private void backtrackWithLowerAssignments() {
		// Remove all assignments on the current decision level from the queue of assignments to process.
		HashSet<Integer> removedEntries = new HashSet<>();
		for (Iterator<ArrayAssignment.Entry> iterator = assignmentsToProcess.iterator(); iterator.hasNext();) {
			Assignment.Entry entry = iterator.next();
			if (entry.getDecisionLevel() == getDecisionLevel()) {
				iterator.remove();
				removedEntries.add(entry.getAtom());
			}
		}
		// If backtracking removed the first assigning entry, any reassignment becomes an ordinary (first) assignment.
		for (ArrayAssignment.Entry entry : assignmentsToProcess) {
			// NOTE: this check is most often not needed, perhaps there is a better way to realize this check?
			if (entry.isReassignAtLowerDecisionLevel() && removedEntries.contains(entry.getAtom())) {
				entry.setReassignFalse();
			}
		}
		for (Iterator<Assignment.Entry> iterator = newAssignments.iterator(); iterator.hasNext();) {
			Assignment.Entry entry = iterator.next();
			if (entry.getDecisionLevel() == getDecisionLevel()) {
				iterator.remove();
			}
		}
		// Hint: it might be faster to just return all assignments for choice and let the ChoiceManager avoid duplicate checks.
		for (Iterator<Assignment.Entry> iterator = newAssignmentsForChoice.iterator(); iterator.hasNext();) {
			Assignment.Entry entry = iterator.next();
			if (entry.getDecisionLevel() == getDecisionLevel() && !entry.isReassignAtLowerDecisionLevel()) {
				iterator.remove();
			}
		}

		int decisionLevelToRemove = atomsAssignedInDecisionLevel.size() - 1;
		for (Integer atom : atomsAssignedInDecisionLevel.remove(decisionLevelToRemove)) {
			Entry current = assignment.get(atom);
			if (current == null) {
				throw oops("Entry not in current assignment");
			}
			// If assignment was moved to lower decision level, do not remove it while backtracking from previously higher decision level.
			if (current.getDecisionLevel() < decisionLevelToRemove) {
				continue;
			}

			if (current.hasPreviousMBT() && current.getMBTDecisionLevel() < decisionLevelToRemove) {
				// Restore previous MBT.
				mbtCount++;
				Entry restoredPrevious = new Entry(MBT, current.getAtom(), current.getMBTDecisionLevel(), current.getMBTPropagationLevel(), current.getMBTImpliedBy(), false);
				assignment.set(atom, restoredPrevious);
				if (current.getMBTDecisionLevel() == -1) {
					throw oops("MBT assigned at decision level -1.");
				}
				values[atom] = (current.getMBTDecisionLevel() << 2) | translateTruth(MBT);
				LOGGER.trace("Backtracking assignment: {}={} restored to {}={}.", atom, current, atom, restoredPrevious);
			} else {
				if (MBT.equals(current.getTruth())) {
					mbtCount--;
				}
				assignment.set(atom, null);
				values[atom] = 0;
				LOGGER.trace("Backtracking assignment: {}={} removed.", atom, current);
			}
			strongDecisionLevels[atom] = -1;
		}

		if (atomsAssignedInDecisionLevel.isEmpty()) {
			atomsAssignedInDecisionLevel.add(new ArrayList<>());
		}
		propagationCounterPerDecisionLevel.remove(propagationCounterPerDecisionLevel.size() - 1);
	}

	@Override
	public int getMBTCount() {
		return mbtCount;
	}

	@Override
	public ConflictCause choose(int atom, ThriceTruth value) {
		atomsAssignedInDecisionLevel.add(new ArrayList<>());
		propagationCounterPerDecisionLevel.add(0);
		return assign(atom, value, null);
	}

	@Override
	public void registerCallbackOnChange(int atom) {
	}

	@Override
	public void setCallback(ChoiceManager choiceManager) {
	}

	@Override
	public ConflictCause assign(int atom, ThriceTruth value, NoGood impliedBy, int decisionLevel) {
		return assignWithLowverDecisionLevel(atom, value, impliedBy, decisionLevel);
	}

	private ConflictCause assignWithLowverDecisionLevel(int atom, ThriceTruth value, NoGood impliedBy, int decisionLevel) {
		if (decisionLevel > getDecisionLevel() || decisionLevel < 0) {
			throw new IllegalArgumentException("Given decisionLevel is outside range of possible decision levels. Given decisionLevel is: " + decisionLevel);
		}
		if (decisionLevel < getDecisionLevel() && LOGGER.isDebugEnabled()) {
			String atomString = translator != null ? translator.atomToString(atom) : Integer.toString(atom);
			LOGGER.trace("Assign called with lower decision level. Atom: {}_{}@{}.", value, atomString, decisionLevel);
		}
		ConflictCause isConflictFree = assignWithDecisionLevel(atom, value, impliedBy, decisionLevel);
		if (isConflictFree != null) {
			LOGGER.debug("Assign is conflicting: atom: {}, value: {}, impliedBy: {}.", atom, value, impliedBy);
		}
		if (checksEnabled) {
			runInternalChecks();
		}
		return isConflictFree;
	}

	private boolean assignmentsConsistent(ThriceTruth oldTruth, ThriceTruth value) {
		return oldTruth == null || oldTruth.toBoolean() == value.toBoolean();
	}

	private ConflictCause assignWithDecisionLevel(int atom, ThriceTruth value, NoGood impliedBy, int decisionLevel) {
		if (checksEnabled) {
			if (getMBTCount() != getMBTAssignedAtoms().size()) {
				throw oops("MBT counter and amount of actually MBT-assigned atoms disagree");
			} else {
				LOGGER.trace("MBT count agrees with amount of MBT-assigned atoms.");
			}
		}
		if (!isAtom(atom)) {
			throw new IllegalArgumentException("not an atom");
		}

		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}
		if (decisionLevel < getDecisionLevel() && LOGGER.isDebugEnabled()) {
			LOGGER.debug("Assign on lower-than-current decision level: atom: {}, decisionLevel: {}, value: {}.", atom, decisionLevel, value);
		}

		// Check if the new assignments contradicts the current one.
		final Entry current = get(atom);
		final ThriceTruth currentTruth = getTruth(atom);
		final int currentAtomWeakDecisionLevel = getWeakDecisionLevel(atom);

		// Nothing to do if the new value is the same as the current one (or current is TRUE and new is MBT),
		// and the current one has lower decision level.
		if (currentTruth != null && current.getDecisionLevel() <= decisionLevel &&
			(value == currentTruth || value.isMBT() && TRUE == currentTruth)) {
			return null;
		}

		// If the atom currently is not assigned, simply record the assignment.
		if (currentTruth == null) {
			// If assigned value is MBT, increase counter.
			if (MBT.equals(value)) {
				mbtCount++;
			}
			recordAssignment(atom, value, impliedBy, decisionLevel);
			return null;
		}

		// Check consistency.
		if (!assignmentsConsistent(currentTruth, value)) {
			ConflictCause conflictCause = new ConflictCause(impliedBy);
			// Assignments are inconsistent, prepare the reason.
			NoGood violated;
			if (decisionLevel < currentAtomWeakDecisionLevel) {
				// New assignment is lower than the current one, hence cause is the reason for the (higher) current one.
				violated = current.hasPreviousMBT() ? current.getMBTImpliedBy() : current.getImpliedBy();	// take MBT reason if it exists.
				if (violated == null) {
					conflictCause = new ConflictCause(current);
				} else {
					conflictCause = new ConflictCause(violated);
				}
				// The lower assignment takes precedence over the current value, overwrite it and adjust mbtCounter.
				if (currentTruth == MBT) {
					mbtCount--;
				}
				recordAssignment(atom, value, impliedBy, decisionLevel);
				if (value == MBT) {
					mbtCount++;
				}

			}
			return conflictCause;
		}

		// Previous assignment exists, and the new one is consistent with it.
		switch (value) {
			case FALSE:
				// Previous must be false, simply re-assign it.
				recordAssignment(atom, value, impliedBy, decisionLevel);
				return null;
			case TRUE:
				// Check if TRUE is below current TRUE but above-or-equal a current MBT.
				if (currentTruth == MBT && current.getDecisionLevel() <= decisionLevel) {
					recordAssignment(atom, value, impliedBy, decisionLevel, current.getDecisionLevel(), current.getPropagationLevel(), current.getImpliedBy());
				} else if (current.hasPreviousMBT() && current.getMBTDecisionLevel() <= decisionLevel) {
					recordAssignment(atom, value, impliedBy, decisionLevel, current.getMBTDecisionLevel(), current.getMBTPropagationLevel(), current.getMBTImpliedBy());
				} else {
					// TRUE is below current TRUE and below an eventual MBT.
					recordAssignment(atom, value, impliedBy, decisionLevel);
				}
				if (currentTruth.isMBT()) {
					mbtCount--;
				}
				return null;
			case MBT:
				if (current.hasPreviousMBT() && current.getMBTDecisionLevel() <= decisionLevel
					|| TRUE.equals(current.getTruth()) && current.getDecisionLevel() <= decisionLevel) {
					// New assignment is above-or-equal to an already existing MBT,
					// or current is TRUE and at same decision level as the new MBT. Ignore it.
					return null;
				}
				if (!currentTruth.isMBT()) {
					// Current assignment is TRUE and new one is MBT below (and lower than a previous MBT).
					LOGGER.debug("Updating current assignment {}: {} new MBT below at {}, impliedBy: {}.", atom, current, decisionLevel, impliedBy);
					recordMBTBelowTrue(atom, value, impliedBy, decisionLevel);
				} else {
					// Current assignment is MBT and the new one is below it (no TRUE above exists).
					recordAssignment(atom, value, impliedBy, decisionLevel);
				}
				return null;
		}
		throw oops("Statement should be unreachable, algorithm misses some case");
	}

	private void recordMBTBelowTrue(int atom, ThriceTruth value, NoGood impliedBy, int decisionLevel) {
		Entry oldEntry = get(atom);
		if (!TRUE.equals(oldEntry.getTruth()) || !MBT.equals(value)) {
			throw oops("Recording MBT below TRUE but truth values do not match");
		}
		//final int previousPropagationLevel = atomsAssignedInDecisionLevel.get(decisionLevel).size();
		final int previousPropagationLevel = propagationCounterPerDecisionLevel.get(decisionLevel);
		propagationCounterPerDecisionLevel.set(decisionLevel, previousPropagationLevel + 1);
		final Entry previous = new Entry(value, atom, decisionLevel, previousPropagationLevel, impliedBy, true);
		atomsAssignedInDecisionLevel.get(decisionLevel).add(atom);
		assignmentsToProcess.add(previous); // Process MBT on lower decision level.
		// Replace the current TRUE entry with one where previous is set correctly.
		Entry trueEntry = new Entry(oldEntry.getTruth(), atom, oldEntry.getDecisionLevel(), oldEntry.getPropagationLevel(), oldEntry.getImpliedBy(), oldEntry.isReassignAtLowerDecisionLevel(), decisionLevel, previousPropagationLevel, impliedBy);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recording assignment {}: MBT below TRUE {} impliedBy: {}", atom, trueEntry, trueEntry.getMBTImpliedBy());
			if (trueEntry.getMBTImpliedBy() != null) {
				for (Integer literal : trueEntry.getMBTImpliedBy()) {
					LOGGER.trace("NoGood impliedBy literal assignment: {}={}.", atomOf(literal), assignment.get(atomOf(literal)));
				}
			}
		}
		atomsAssignedInDecisionLevel.get(oldEntry.getDecisionLevel()).add(trueEntry.getAtom());
		assignment.set(atom, trueEntry);
		values[atom] = (trueEntry.getMBTDecisionLevel() << 2) | translateTruth(TRUE);
	}

	private void recordAssignment(int atom, ThriceTruth value, NoGood impliedBy, int decisionLevel) {
		recordAssignment(atom, value, impliedBy, decisionLevel, -1, -1, null);
	}

	private void recordAssignment(int atom, ThriceTruth value, NoGood impliedBy, int decisionLevel, int previousDecisionLevel, int previousPropagationLevel, NoGood previousImpliedBy) {
		Entry oldEntry = get(atom);
		if (oldEntry != null && decisionLevel >= oldEntry.getDecisionLevel() && !(TRUE.equals(value) && MBT.equals(oldEntry.getTruth()))) {
			throw oops("Assigning value into higher decision level");
		}
		if (previousDecisionLevel != -1 && TRUE != value) {
			throw oops("Assignment has previous decision level, but truth value is not TRUE");
		}
		// Create and record new assignment entry.
		final int propagationLevel = propagationCounterPerDecisionLevel.get(decisionLevel);
		propagationCounterPerDecisionLevel.set(decisionLevel, propagationLevel + 1);
		final boolean isReassignAtLowerDecisionLevel = oldEntry != null && oldEntry.getDecisionLevel() > decisionLevel && !isConflicting(oldEntry.getTruth(), value);
		final Entry next = new Entry(value, atom, decisionLevel, propagationLevel, impliedBy, isReassignAtLowerDecisionLevel, previousDecisionLevel, previousPropagationLevel, previousImpliedBy);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recording assignment {}: {} impliedBy: {}", atom, next, next.getImpliedBy());
			if (next.getImpliedBy() != null) {
				for (Integer literal : next.getImpliedBy()) {
					LOGGER.trace("NoGood impliedBy literal assignment: {}={}.", atomOf(literal), assignment.get(atomOf(literal)));
				}
			}
		}
		int decisionLevelRespectingLowerMBT = previousDecisionLevel == -1 ? decisionLevel : previousDecisionLevel;
		values[atom] = (decisionLevelRespectingLowerMBT << 2) | translateTruth(value);
		if (value != MBT) {
			strongDecisionLevels[atom] = decisionLevel;
		} else {
			strongDecisionLevels[atom] = -1;
		}
		// Record atom for backtracking (avoid duplicate records if MBT and TRUE are assigned on the same decision level).
		if (!next.hasPreviousMBT() || next.getMBTDecisionLevel() < decisionLevel) {
			atomsAssignedInDecisionLevel.get(decisionLevel).add(next.getAtom());
		}
		assignmentsToProcess.add(next);
		newAssignments.add(next);
		newAssignmentsForChoice.add(next);
		assignment.set(atom, next);
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
		return strongDecisionLevels[atom];
	}

	@Override
	public Set<Integer> getTrueAssignments() {
		Set<Integer> result = new HashSet<>();
		for (Entry entry : assignment) {
			if (entry != null && TRUE.equals(entry.getTruth())) {
				result.add(entry.getAtom());
			}
		}
		return result;
	}

	@Override
	public Entry get(int atom) {
		return assignment.get(atom);
	}

	private void runInternalChecks() {
		// Ensure that truth values in assignment entries agree with those in values array.
		LOGGER.trace("Checking assignment.");
		if (assignment.size() > values.length) {
			throw oops("Assignment is bigger than values.");
		}
		for (int i = 0; i < assignment.size(); i++) {
			if (assignment.get(i) == null) {
				if (values[i] == 0) {
					continue;
				}
				throw oops("Assigned truth values disagree for atom " + i + ", entry is " + assignment.get(i) + " and value is " + values[i] + ".");
			}
			if (!(assignment.get(i).getTruth() == translateTruth(values[i]) && assignment.get(i).getWeakDecisionLevel() == getWeakDecisionLevel(i))) {
				throw oops("Assigned truth values disagree for atom " + i + ", entry is " + assignment.get(i) + " and value is " + values[i] + ".");
			}
			if (assignment.get(i).getStrongDecisionLevel() != strongDecisionLevels[i]) {
				throw oops("Assigned strong decision levels disagree for atom " + i + ", entry is " + assignment.get(i) + " and strong decision level is " + strongDecisionLevels[i] + ".");
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
		for (int i = 0; i < assignment.size(); i++) {
			Entry entry = assignment.get(i);
			if (entry != null && entry.getTruth() == MBT) {
				ret.add(i);
			}
		}
		return ret;
	}

	@Override
	public void growForMaxAtomId(int maxAtomId) {
		if (assignment.size() > maxAtomId) {
			return;
		}
		assignment.ensureCapacity(maxAtomId + 1);
		// Grow backing array with nulls.
		for (int i = assignment.size(); i <= maxAtomId; i++) {
			assignment.add(i, null);
		}
		// Grow arrays only if needed.
		if (values.length > maxAtomId) {
			return;
		}
		// Grow by 1.5 current size, except if bigger array is required due to maxAtomId.
		int newCapacity = values.length + (values.length >> 1);
		if (newCapacity < maxAtomId + 1) {
			newCapacity = maxAtomId + 1;
		}
		int[] newValues = new int[newCapacity];
		System.arraycopy(values, 0, newValues, 0, values.length);
		values = newValues;
		int[] newStrongDecisionLevels = new int[newCapacity];
		System.arraycopy(strongDecisionLevels, 0, newStrongDecisionLevels, 0, strongDecisionLevels.length);
		Arrays.fill(newStrongDecisionLevels, strongDecisionLevels.length, newStrongDecisionLevels.length, -1);
		strongDecisionLevels = newStrongDecisionLevels;
		NoGood[] newimpliedBy = new NoGood[newCapacity];
		System.arraycopy(impliedBy, 0, newimpliedBy, 0, impliedBy.length);
		impliedBy = newimpliedBy;
	}

	@Override
	public int getDecisionLevel() {
		return atomsAssignedInDecisionLevel.size() - 1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		boolean isFirst = true;
		for (Entry assignmentEntry : assignment) {
			if (assignmentEntry == null) {
				continue;
			}
			if (!isFirst) {
				sb.append(", ");
			}
			isFirst = false;
			sb.append(assignmentEntry.getTruth());
			sb.append("_");
			if (translator != null) {
				sb.append(translator.atomToString(assignmentEntry.getAtom()));
			} else {
				sb.append(assignmentEntry.getAtom());
			}
			sb.append("@");
			sb.append(assignmentEntry.getDecisionLevel());
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public Iterator<Assignment.Entry> getNewAssignmentsIterator() {
		Iterator<Assignment.Entry> it = newAssignments.iterator();
		newAssignments = new LinkedList<>();
		return it;
	}

	@Override
	public Iterator<Assignment.Entry> getNewAssignmentsForChoice() {
		Iterator<Assignment.Entry> it = newAssignmentsForChoice.iterator();
		newAssignmentsForChoice = new LinkedList<>();
		return it;
	}

	@Override
	public void setChecksEnabled(boolean checksEnabled) {
		this.checksEnabled = checksEnabled;
	}

	private static final class Entry implements Assignment.Entry {
		private final ThriceTruth value;
		private final int decisionLevel;
		private final int propagationLevel;
		private final int previousDecisionLevel;
		private final int previousPropagationLevel;
		private final NoGood previousImpliedBy;
		private final NoGood impliedBy;
		private final int atom;
		private boolean isReassignAtLowerDecisionLevel;

		Entry(ThriceTruth value, int atom, int decisionLevel, int propagationLevel, NoGood noGood, boolean isReassignAtLowerDecisionLevel) {
			this(value, atom, decisionLevel, propagationLevel, noGood, isReassignAtLowerDecisionLevel, -1, -1, null);
		}

		Entry(ThriceTruth value, int atom, int decisionLevel, int propagationLevel, NoGood impliedBy, boolean isReassignAtLowerDecisionLevel, int previousDecisionLevel, int previousPropagationLevel, NoGood previousImpliedBy) {
			this.value = value;
			this.decisionLevel = decisionLevel;
			this.propagationLevel = propagationLevel;
			this.impliedBy = impliedBy;
			this.previousDecisionLevel = previousDecisionLevel;
			this.previousPropagationLevel = previousPropagationLevel;
			this.previousImpliedBy = previousImpliedBy;
			this.atom = atom;
			this.isReassignAtLowerDecisionLevel = isReassignAtLowerDecisionLevel;
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
		public int getMBTPropagationLevel() {
			return previousPropagationLevel;
		}

		@Override
		public NoGood getMBTImpliedBy() {
			return previousImpliedBy;
		}

		@Override
		public NoGood getImpliedBy() {
			return impliedBy;
		}

		@Override
		public int getAtom() {
			return atom;
		}

		@Override
		public int getPropagationLevel() {
			return propagationLevel;
		}

		@Override
		public boolean isReassignAtLowerDecisionLevel() {
			return isReassignAtLowerDecisionLevel;
		}

		public void setReassignFalse() {
			this.isReassignAtLowerDecisionLevel = false;
		}

		@Override
		public String toString() {
			return value.toString() + "(DL" + decisionLevel + ", PL" + propagationLevel + ")"
				+ (hasPreviousMBT() ? "MBT(DL" + getMBTDecisionLevel() + ", PL" + getMBTPropagationLevel() + ")" : "");
		}
	}
}
