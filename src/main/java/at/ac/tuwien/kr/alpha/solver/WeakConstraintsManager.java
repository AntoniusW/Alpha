package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.common.NoGood;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.Literals.atomToLiteral;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.MBT;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.TRUE;
import static java.lang.Math.max;

/**
 * Manages weak constraints: stores the value of the current partial assignment, the best known value of any answer-set,
 * and handles callbacks/computations when certain atoms (that represent weak constraints) change their truth value.
 *
 * Copyright (c) 2020-2021, the Alpha Team.
 */
public class WeakConstraintsManager implements Checkable {

	private static final Logger LOGGER = LoggerFactory.getLogger(WeakConstraintsManager.class);

	private ArrayList<Integer> currentWeightAtLevels;
	private ArrayList<Integer> bestKnownWeightAtLevels;	// Stores weights in inverted order: zero is highest violated level, may also grow to represent negative levels.
	private final WritableAssignment assignment;
	private boolean foundFirstAnswerSet;
	private int maxOffsetCurrentIsAllEqualBest;	// The highest level for which holds: on all levels below (or equal), the current weights are the same as the weights for the best known.
	private boolean isCurrentBetterThanBest;
	private final ArrayList<Integer> knownCallbackAtoms;
	private final ArrayList<WeakConstraintAtomCallback> knownAtomCallbacksForFirstAnswerSet = new ArrayList<>();
	private int maxLevel;				// Stores which actual level corresponds to array position 0 in bestKnownWeightAtLevels, i.e, the highest level that is violated. Value may decrease if better answer-sets are found.

	private boolean checksEnabled;

	public WeakConstraintsManager(WritableAssignment assignment) {
		this.assignment = assignment;
		this.currentWeightAtLevels = new ArrayList<>();
		this.bestKnownWeightAtLevels = new ArrayList<>();
		this.foundFirstAnswerSet = false;
		this.maxOffsetCurrentIsAllEqualBest = -1;
		this.isCurrentBetterThanBest = true;
		this.knownCallbackAtoms = new ArrayList<>();
	}

	public void addWeakConstraintsInformation(List<Triple<Integer, Integer, Integer>> weakConstraintAtomWeightLevels) {
		// Register all newly obtained weak constraint atoms for callback at the assignment.
		for (Triple<Integer, Integer, Integer> weakConstraintAtomWeightLevel : weakConstraintAtomWeightLevels) {
			if (weakConstraintAtomWeightLevel.getMiddle() == 0) {
				// Skip weak constraints with weight 0 entirely.
				continue;
			}
			WeakConstraintAtomCallback wcA = new WeakConstraintAtomCallback(weakConstraintAtomWeightLevel.getLeft(), weakConstraintAtomWeightLevel.getMiddle(), weakConstraintAtomWeightLevel.getRight());
			assignment.registerCallbackOnChange(wcA.atom);
			assignment.getAtomCallbackManager().recordCallback(wcA.atom, wcA);
			knownCallbackAtoms.add(wcA.atom);
			if (!foundFirstAnswerSet) {
				knownAtomCallbacksForFirstAnswerSet.add(wcA);
			}
		}
	}

	/**
	 * Mark the current weight as being the best of all currently known answer-sets.
	 */
	public void markCurrentWeightAsBestKnown() {
		LOGGER.trace("Marking current answer-set as best known.");
		if (!foundFirstAnswerSet) {
			initializeFirstWeightsAtLevel();
		} else {
			// Remove unnecessary high levels (with weights 0).
			int trim = 0;
			for (int i = 0; i < currentWeightAtLevels.size(); i++) {
				if (currentWeightAtLevels.get(i) != 0) {
					trim = i;
					break;
				}
			}
			if (trim > 0) {
				bestKnownWeightAtLevels = new ArrayList<>(currentWeightAtLevels.subList(trim, currentWeightAtLevels.size()));
				currentWeightAtLevels = new ArrayList<>(bestKnownWeightAtLevels);
			} else {
				bestKnownWeightAtLevels = new ArrayList<>(currentWeightAtLevels);
			}
			maxLevel -= trim;
		}
		maxOffsetCurrentIsAllEqualBest = bestKnownWeightAtLevels.size() - 1;
		LOGGER.trace("Max offset current is all equal to best known is: {}", maxOffsetCurrentIsAllEqualBest);
		isCurrentBetterThanBest = false;
	}

	private void initializeFirstWeightsAtLevel() {
		// First answer set has been found, take its maximum level as zero-level.
		LOGGER.trace("Initializing for first answer-set.");
		foundFirstAnswerSet = true;
		HashMap<Integer, Integer> sumWeightsAtLevel = new HashMap<>();
		int highestLevel = 0;
		for (WeakConstraintAtomCallback atomCallback : knownAtomCallbacksForFirstAnswerSet) {
			ThriceTruth truth = assignment.getTruth(atomCallback.atom);
			if (truth == null || !truth.toBoolean()) {
				// Skip weak-constraint atoms being unassigned or false.
				continue;
			}
			int level = atomCallback.level;
			if (highestLevel < level) {
				// Record level if it is highest so far.
				highestLevel = level;
			}
			// Update weight information.
			if (sumWeightsAtLevel.get(level) == null) {
				sumWeightsAtLevel.put(level, atomCallback.weight);
			} else {
				int newWeight = sumWeightsAtLevel.get(level) + atomCallback.weight;
				sumWeightsAtLevel.put(level, newWeight);
			}
		}
		maxLevel = highestLevel;
		LOGGER.trace("Maximum recorded level (0-offset) is: {} ({})", maxLevel, listOffsetToLevel(0));
		bestKnownWeightAtLevels = new ArrayList<>(highestLevel);
		growForLevel(bestKnownWeightAtLevels, highestLevel);
		for (Map.Entry<Integer, Integer> levelWeight : sumWeightsAtLevel.entrySet()) {
			bestKnownWeightAtLevels.set(maxLevel - levelWeight.getKey(), levelWeight.getValue());
		}
		currentWeightAtLevels = new ArrayList<>(bestKnownWeightAtLevels);
		LOGGER.trace("Initially, current/best known weights are: {}", bestKnownWeightAtLevels);
	}

	private void growForLevel(ArrayList<Integer> listToGrow, int level) {
		int listOffset = levelToListOffset(level);
		while (listOffset >= listToGrow.size()) {
			listToGrow.add(0);
		}
	}

	private int levelToListOffset(int level) {
		int levelInList = maxLevel - level;
		if (levelInList < 0) {
			throw oops("Level optimisation in WeakConstraintsManager is negative.");
		}
		return levelInList;
	}

	private int listOffsetToLevel(int offset) {
		return maxLevel - offset;
	}

	/**
	 * Returns whether the current partial interpretation is already worse (or equal) than the best-known answer-set.
	 * @return true if the current partial interpretation has worse (or equal) weight than the best-known answer-set.
	 */
	public boolean isCurrentBetterThanBest() {
		LOGGER.trace("Is current better than best? {}", isCurrentBetterThanBest);
		return isCurrentBetterThanBest;
	}

	/**
	 * Generates a NoGood that prevents the current weight to be derived again.
	 * @return a NoGood which excludes the exact weight of the current assignment.
	 */
	public NoGood generateExcludingNoGood() {
		// Collect all currently true/mbt weights and put their respective literals in one nogood.
		// Note: alternative to searching all known callback atoms would be maintaining a list of true ones, but this is probably less efficient.
		ArrayList<Integer> trueCallbackAtoms = new ArrayList<>();
		for (Integer callbackAtom : knownCallbackAtoms) {
			ThriceTruth truth = assignment.getTruth(callbackAtom);
			if (truth != null && truth.toBoolean()) {
				trueCallbackAtoms.add(atomToLiteral(callbackAtom));
			}
		}
		return NoGood.fromConstraint(trueCallbackAtoms, Collections.emptyList());
	}

	/**
	 * Returns the current weights and their levels.
	 * @return a TreeMap mapping levels to weights. Note that values may be non-continuous and negative levels are possible.
	 */
	public TreeMap<Integer, Integer> getCurrentWeightAtLevels() {
		TreeMap<Integer, Integer> weightPerLevels = new TreeMap<>();
		for (int i = 0; i < currentWeightAtLevels.size(); i++) {
			Integer weightAti = currentWeightAtLevels.get(i);
			if (weightAti == 0) {
				continue;
			}
			weightPerLevels.put(listOffsetToLevel(i), weightAti);
		}
		return weightPerLevels;
	}

	private void increaseCurrentWeight(WeakConstraintAtomCallback atom) {
		// Record the new weight.
		growForLevel(currentWeightAtLevels, atom.level);
		int listOffset = levelToListOffset(atom.level);
		int newWeight = currentWeightAtLevels.get(listOffset) + atom.weight;
		currentWeightAtLevels.set(listOffset, newWeight);

		// Now check whether current is worse than best known.
		if (listOffset > maxOffsetCurrentIsAllEqualBest + 1) {
			// Weight in some level where differences do not (yet) matter increased, but on lower level current partial assignment is still better than best found answer-set.
			// Nothing to do here.
			return;
		} else if (listOffset <= maxOffsetCurrentIsAllEqualBest) {
			// Weight in level with previously equal weights increased, current is now worse than best.
			isCurrentBetterThanBest = false;
			maxOffsetCurrentIsAllEqualBest = listOffset - 1;
		} else if (listOffset == maxOffsetCurrentIsAllEqualBest + 1) {
			// Weight increased for the first level where current and best-known are not equal.
			moveUpwardsMaxLevelCurrentIsAllEqual();
			recomputeIsCurrentBetterThanBest();
		} else {
			throw oops("Increasing weight of current answer reached unforeseen state.");
		}
	}

	private void decreaseCurrentWeight(WeakConstraintAtomCallback atom) {
		// Record the new weight.
		int listOffset = levelToListOffset(atom.level);
		int newWeight = currentWeightAtLevels.get(listOffset) - atom.weight;
		currentWeightAtLevels.set(listOffset, newWeight);

		// Now check whether current is better than best known and adapt maxOffsetCurrentIsAllEqualBest.
		if (listOffset <= maxOffsetCurrentIsAllEqualBest) {
			// Improved below the point where current is equal to best, so current is better than best now.
			isCurrentBetterThanBest = true;
			maxOffsetCurrentIsAllEqualBest = listOffset - 1;        // All weights below the change are equal.
		} else if (listOffset > maxOffsetCurrentIsAllEqualBest + 1) {
			// Decrease in such a high level, that there is at least one level (maxLevel+1) where weights are
			// different and whose difference still dominates whether current is better than best known.
			return;
		} else if (listOffset == maxOffsetCurrentIsAllEqualBest + 1) {
			// Decrease might make current better than best known and change level of equals.
			moveUpwardsMaxLevelCurrentIsAllEqual();
			recomputeIsCurrentBetterThanBest();
		} else {
			throw oops("Decreasing weight of current answer reached unforeseen state.");
		}
	}

	private void moveUpwardsMaxLevelCurrentIsAllEqual() {
		for (int i = max(maxOffsetCurrentIsAllEqualBest, 0); i < currentWeightAtLevels.size(); i++) {
			int currentLevelWeight = currentWeightAtLevels.get(i);
			int bestLevelWeight = bestKnownWeightAtLevels.size() > i ? bestKnownWeightAtLevels.get(i) : 0;
			if (currentLevelWeight != bestLevelWeight) {
				// Level is the first where values differ, previous level was maximum one where values are equal.
				maxOffsetCurrentIsAllEqualBest = i - 1;
				return;
			}
		}
		// Iterated all levels, so weights are the same on all levels.
		maxOffsetCurrentIsAllEqualBest = currentWeightAtLevels.size() - 1;
	}

	private void recomputeIsCurrentBetterThanBest() {
		// The weights at the first level above maxOffsetCurrentIsAllEqualBest dominate whether current is worse than best known.
		if (maxOffsetCurrentIsAllEqualBest == currentWeightAtLevels.size() - 1) {
			// Current and best known are equal even up to the last level.
			isCurrentBetterThanBest = false;
			return;
		}
		int comparisonOffset = maxOffsetCurrentIsAllEqualBest + 1;
		int currentLevelWeight = currentWeightAtLevels.get(comparisonOffset);
		int bestLevelWeight = bestKnownWeightAtLevels.size() > comparisonOffset ? bestKnownWeightAtLevels.get(comparisonOffset) : 0;
		isCurrentBetterThanBest = currentLevelWeight < bestLevelWeight;
	}

	@Override
	public void setChecksEnabled(boolean checksEnabled) {
		this.checksEnabled = checksEnabled;
	}

	private void runChecks() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Running checks.");
			LOGGER.trace("Maximum recorded level (0-offset) is: {} ({})", maxLevel, listOffsetToLevel(0));
			LOGGER.trace("Currently best known weights are: {}", bestKnownWeightAtLevels);
			LOGGER.trace("Current weights are: {}", currentWeightAtLevels);
			LOGGER.trace("Max offset current is all equal to best known is: {}", maxOffsetCurrentIsAllEqualBest);
			LOGGER.trace("Is current better than best-known: {}", isCurrentBetterThanBest);

		}
		if (!foundFirstAnswerSet && !bestKnownWeightAtLevels.isEmpty()) {
			throw oops("WeakConstraintManager has best known answer set information but no answer set was found yet.");
		}
		if (foundFirstAnswerSet && bestKnownWeightAtLevels.get(0) == 0) {
			throw oops("WeakConstraintManager has best-known answer set with zero-weights at highest level.");
		}
		// Check whether isCurrentBetterThanBest flag is consistent.
		if (isCurrentBetterThanBest != checkIsCurrentBetterThanBest()) {
			throw oops("WeakConstraintManager detected valuation of current assignment is inconsistent with state flag.");
		}
		checkLevelBar();
		LOGGER.trace("Checks done.");
	}

	private void checkLevelBar() {
		int highestLevelWhereLowerLevelsAreEqual = -1;
		for (int i = 0; i < currentWeightAtLevels.size(); i++) {
			int levelValue = i < bestKnownWeightAtLevels.size() ? bestKnownWeightAtLevels.get(i) : 0;
			if (currentWeightAtLevels.get(i) != levelValue) {
				break;
			}
			highestLevelWhereLowerLevelsAreEqual = i;
		}
		// The level bar should be at highestLevelWhereLowerLevelsAreEqual+1.
		if (maxOffsetCurrentIsAllEqualBest != highestLevelWhereLowerLevelsAreEqual) {
			throw oops("WeakConstraintManager detected level bar at wrong level, it is " + maxOffsetCurrentIsAllEqualBest
				+ "and should be " + highestLevelWhereLowerLevelsAreEqual);
		}
	}

	private boolean checkIsCurrentBetterThanBest() {
		if (!foundFirstAnswerSet) {
			// If no answer set has been found, the current is always better than best known.
			return true;
		}
		for (int i = 0; i < currentWeightAtLevels.size(); i++) {
			if (i >= bestKnownWeightAtLevels.size()) {
				// Current has violations at level not violated by best.
				return false;
			}
			if (currentWeightAtLevels.get(i) < bestKnownWeightAtLevels.get(i)) {
				return true;
			}
			if (currentWeightAtLevels.get(i) > bestKnownWeightAtLevels.get(i)) {
				return false;
			}
		}
		// All levels have same weight for current as for best known, so at this point usually current is not better than best.
		return false;
	}

	class WeakConstraintAtomCallback implements AtomCallbackManager.AtomCallback {
		private final int atom;
		private final int weight;
		private final int level;
		private ThriceTruth lastTruthValue;

		WeakConstraintAtomCallback(int atom, int weight, int level) {
			this.atom = atom;
			this.weight = weight;
			this.level = level;
		}

		@Override
		public void processCallback() {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Processing callback for atom {} with weight at level {}@{}, last truth value was {}.", atom, weight, level, lastTruthValue);
				LOGGER.trace("Current atom truth value is: {}", assignment.getTruth(atom));
			}
			if (checksEnabled) {
				runChecks();
			}
			ThriceTruth currentAtomTruth = assignment.getTruth(atom);
			if (!foundFirstAnswerSet) {
				// Record old truth value and return if no answer set has been found yet.
				lastTruthValue = currentAtomTruth;
				return;
			}
			LOGGER.trace("Current truth value is: {}", currentAtomTruth);
			if (lastTruthValue == null) {
				// Change from unassigned to some truth value.
				if (currentAtomTruth == MBT) {
					increaseCurrentWeight(this);
				}
				// Note: for assignment to FALSE or MBT->TRUE, no change needed.
				lastTruthValue = currentAtomTruth;
			} else {
				if ((lastTruthValue == MBT || lastTruthValue == TRUE) && currentAtomTruth == null) {
					// Change from TRUE/MBT to unassigned.
					decreaseCurrentWeight(this);
				}
				// Note: for backtracking from TRUE to MBT no change is needed.
				lastTruthValue = currentAtomTruth;
				if (currentAtomTruth != null && lastTruthValue != TRUE && currentAtomTruth != MBT) {
					throw oops("Unexpected case of weak constraint atom changing truth value encountered. Atom/LastTruth/CurrentTruth: " + atom + "/" + lastTruthValue + "/" + currentAtomTruth);
				}
			}
			if (checksEnabled) {
				runChecks();
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			WeakConstraintAtomCallback that = (WeakConstraintAtomCallback) o;
			return atom == that.atom &&
				weight == that.weight &&
				level == that.level;
		}

		@Override
		public int hashCode() {
			return Objects.hash(atom, weight, level);
		}
	}
}
