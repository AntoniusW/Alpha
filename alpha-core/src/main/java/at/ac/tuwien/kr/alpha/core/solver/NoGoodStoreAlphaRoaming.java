/*
 * Copyright (c) 2016-2020, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.api.solver.BinaryNoGoodPropagationEstimationStrategy;
import at.ac.tuwien.kr.alpha.core.common.Assignment;
import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.common.NoGoodInterface;
import at.ac.tuwien.kr.alpha.core.common.NoGoodInterface.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomOf;
import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomToLiteral;
import static at.ac.tuwien.kr.alpha.core.atoms.Literals.isNegated;
import static at.ac.tuwien.kr.alpha.core.atoms.Literals.isPositive;
import static at.ac.tuwien.kr.alpha.core.atoms.Literals.literalToString;
import static at.ac.tuwien.kr.alpha.core.common.NoGood.HEAD;
import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.FALSE;
import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.MBT;
import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.TRUE;
import static at.ac.tuwien.kr.alpha.api.Util.arrayGrowthSize;
import static at.ac.tuwien.kr.alpha.api.Util.oops;

/**
 * NoGoodStore using for each NoGood three watches, two ordinary ones and an alpha watch.
 * The alpha watch may point to positive or negative literals but not the head of the NoGood.
 *
 * Note the following invariant for watches:
 * (Inv): For every NoGood it either holds that both watched literals are unassigned, or for one watch holds: it
 *        points to a literal that is assigned a satisfying truth value at decision level d and the other
 *        watched literal is assigned at decision level d' such that d <= d'.
 *  The second condition ensures that after backtracking the NoGood is still satisfied or both watches
 *  point to unassigned literals. Observe that for an assignment to TRUE the (potentially lower) decision level of MBT
 *  is taken.
 *
 *  Copyright (c) 2017-2020, the Alpha Team.
 */
public class NoGoodStoreAlphaRoaming implements NoGoodStore, BinaryNoGoodPropagationEstimation, Checkable {
	private static final Logger LOGGER = LoggerFactory.getLogger(NoGoodStoreAlphaRoaming.class);
	private static final int UNASSIGNED = Integer.MAX_VALUE;

	private final WritableAssignment assignment;
	private final LearnedNoGoodDeletion learnedNoGoodDeletion;
	@SuppressWarnings("unchecked")
	private ArrayList<WatchedNoGood>[] watches = new ArrayList[0];
	@SuppressWarnings("unchecked")
	private ArrayList<WatchedNoGood>[] watchesAlpha = new ArrayList[0];
	private BinaryWatchList[] binaryWatches = new BinaryWatchList[0];
	private int maxAtomId;

	private boolean checksEnabled;
	private boolean didPropagate;
	private boolean hasBinaryNoGoods;

	private final NoGoodCounter counter = new NoGoodCounter();

	public NoGoodStoreAlphaRoaming(WritableAssignment assignment, boolean checksEnabled) {
		this.assignment = assignment;
		this.checksEnabled = checksEnabled;
		this.learnedNoGoodDeletion = new LearnedNoGoodDeletion(this, assignment);
	}

	public NoGoodStoreAlphaRoaming(WritableAssignment assignment) {
		this(assignment, false);
	}

	@SuppressWarnings("unchecked")
	void clear() {
		assignment.clear();
		learnedNoGoodDeletion.reset();
		binaryWatches = new BinaryWatchList[0];
		watches = new ArrayList[0];
		watchesAlpha = new ArrayList[0];
		maxAtomId = 0;
	}

	public LearnedNoGoodDeletion getLearnedNoGoodDeletion() {
		return learnedNoGoodDeletion;
	}

	@Override
	public void backtrack() {
		didPropagate = false;
		assignment.backtrack();
		if (checksEnabled) {
			if (assignment.getAssignmentsToProcess().isEmpty()) {
				new WatchedNoGoodsChecker().doWatchesCheck();
			} else {
				LOGGER.trace("Skipping watches check since there are assignments to process first.");
			}
		}
	}

	@Override
	public void growForMaxAtomId(int maxAtomId) {
		int requiredMaxSize = 2 * (maxAtomId + 2);
		if (requiredMaxSize < binaryWatches.length) {
			return;
		}
		int newCapacity = arrayGrowthSize(binaryWatches.length);
		if (newCapacity < requiredMaxSize) {
			newCapacity = requiredMaxSize;
		}
		int oldlength = binaryWatches.length;
		binaryWatches = Arrays.copyOf(binaryWatches, newCapacity);
		for (int i = oldlength; i < binaryWatches.length; i++) {
			binaryWatches[i] = new BinaryWatchList(i);
		}
		watches = Arrays.copyOf(watches, newCapacity);
		for (int i = oldlength; i < watches.length; i++) {
			watches[i] = new ArrayList<>();
		}
		watchesAlpha = Arrays.copyOf(watchesAlpha, newCapacity);
		for (int i = oldlength; i < watchesAlpha.length; i++) {
			watchesAlpha[i] = new ArrayList<>();
		}
		this.maxAtomId = maxAtomId;
	}

	@Override
	public NoGoodCounter getNoGoodCounter() {
		return counter;
	}

	@Override
	public void cleanupLearnedNoGoods() {
		if (learnedNoGoodDeletion.needToRunNoGoodDeletion()) {
			learnedNoGoodDeletion.runNoGoodDeletion();
		}
	}

	void removeFromWatches(WatchedNoGood toRemove) {
		counter.remove(toRemove);
		int watchedLiteral1 = toRemove.getLiteral(0);
		int watchedLiteral2 = toRemove.getLiteral(1);
		if (!watches(watchedLiteral2).remove(toRemove)
			|| !watches(watchedLiteral1).remove(toRemove)) {
			throw oops("Could not remove learned NoGood from watch lists.");
		}
		if (toRemove.hasHead()) {
			throw oops("NoGood has a head.");	// If this occurs, we need to remove the alpha watch too.
		}
	}

	private ArrayList<WatchedNoGood> watches(int literal) {
		return watches[literal];
	}

	private ArrayList<WatchedNoGood> watchesAlpha(int literal) {
		return watchesAlpha[literal];
	}

	private void addOrdinaryWatch(WatchedNoGood wng, int pointer) {
		final int literal = wng.getLiteral(pointer);
		watches(literal).add(wng);
	}

	private void addAlphaWatch(WatchedNoGood wng) {
		final int literal = wng.getLiteralAtAlpha();
		watchesAlpha(literal).add(wng);
	}

	@Override
	public ConflictCause add(int id, NoGood noGood, int lbd) {
		LOGGER.trace("Adding {}", noGood);

		final ConflictCause conflictCause;
		if (noGood.isUnary()) {
			conflictCause = addUnary(noGood);
		} else if (noGood.isBinary()) {
			conflictCause = addAndWatchBinary(noGood);
		} else {
			conflictCause = addAndWatch(noGood, lbd);
		}

		if (conflictCause == null) {
			counter.add(noGood);
		}
		return conflictCause;
	}

	@Override
	public ConflictCause add(int id, NoGood noGood) {
		return add(id, noGood, -1);
	}

	/**
	 * Takes a noGood containing only a single literal and translates it into an assignment (because it
	 * is trivially unit). Still, a check for conflict is performed.
	 */
	private ConflictCause addUnary(final NoGood noGood) {
		if (noGood.hasHead()) {
			return assignStrongComplement(noGood, 0);
		} else {
			return assignWeakComplement(0, noGood, 0);
		}
	}

	private static boolean isComplementaryAssigned(int literal, ThriceTruth literalTruth) {
		return literalTruth != null && literalTruth.toBoolean() != isPositive(literal);
	}

	private int strongDecisionLevel(int atom) {
		int strongDecisionLevel = assignment.getStrongDecisionLevel(atom);
		return strongDecisionLevel == -1 ? UNASSIGNED : strongDecisionLevel;
	}

	private ConflictCause addAndWatch(final NoGood noGood, int lbd) {
		// Collect potential watch candidates.
		int posWeakUnassigned1 = -1;
		int posWeakUnassigned2 = -1;
		int posSatisfiedLiteral1 = -1;
		int posSatisfiedLiteral2 = -1;
		int posWeakHighestAssigned = -1;
		int weakDecisionLevelHighestAssigned = -1;
		int posStrongHighestAssigned = -1;
		int strongDecisionLevelHighestAssigned = -1;
		int posPotentialAlphaWatch = -1;
		int satisfiedLiteralWeakDecisionLevel = -1;

		// Used to detect always-satisfied NoGoods of form { L, -L, ... }.
		Map<Integer, Boolean> occurringAtomPolarity = new HashMap<>();

		// Iterate noGood and record satisfying/unassigned/etc positions.
		int headAtom = atomOf(noGood.getHead());
		final ThriceTruth headTruth = noGood.hasHead() ? assignment.getTruth(headAtom) : null;
		final boolean isHeadTrue = headTruth == TRUE;
		for (int i = 0; i < noGood.size(); i++) {
			final int literal = noGood.getLiteral(i);
			final int atom = atomOf(literal);
			final ThriceTruth atomTruthValue = assignment.getTruth(atom);
			final int atomWeakDecisionLevel = assignment.getWeakDecisionLevel(atom);
			final int atomStrongDecisionLevel = assignment.getStrongDecisionLevel(atom);

			// Check if NoGood can never be violated (atom occurring positive and negative)
			if (occurringAtomPolarity.containsKey(atomOf(literal))) {
				if (occurringAtomPolarity.get(atomOf(literal)) != isNegated(literal)) {
					// NoGood cannot be violated or propagate, ignore it.
					LOGGER.debug("Added NoGood can never propagate or be violated, ignoring it. NoGood is: {}", noGood);
					return null;
				}
			} else {
				occurringAtomPolarity.put(atomOf(literal), isNegated(literal));
			}

			// Check weak unassigned.
			if (atomTruthValue == null) {
				if (posWeakUnassigned1 == -1) {
					posWeakUnassigned1 = i;
				} else {
					posWeakUnassigned2 = i;
				}
			}
			// Alpha watch:
			if (posPotentialAlphaWatch == -1 && noGood.hasHead() && i != HEAD) {
				// Current literal is potential alpha watch if:
				// 1) the head of the nogood is true and the literal is assigned at a higher-or-equal decision level.
				// 2) the literal is complementary assigned and thus satisfies the nogood, or
				// 3) the literal is unassigned or assigned must-be-true.
				if (isHeadTrue && strongDecisionLevel(atomOf(literal)) >= strongDecisionLevel(headAtom)
					|| isComplementaryAssigned(noGood.getLiteral(i), atomTruthValue)
					|| strongDecisionLevel(atomOf(literal)) == Integer.MAX_VALUE) {
					posPotentialAlphaWatch = i;
				}
			}
			// Check satisfaction
			if (atomTruthValue != null && atomTruthValue.toBoolean() != isPositive(literal)) {
				if (posSatisfiedLiteral1 == -1) {
					posSatisfiedLiteral1 = i;
					satisfiedLiteralWeakDecisionLevel = atomWeakDecisionLevel;
				} else {
					posSatisfiedLiteral2 = i;
				}
			}
			// Check violation.
			if (atomTruthValue != null && atomTruthValue.toBoolean() == isPositive(literal)) {
				if (atomWeakDecisionLevel > weakDecisionLevelHighestAssigned) {
					weakDecisionLevelHighestAssigned = atomWeakDecisionLevel;
					posWeakHighestAssigned = i;
				}
				if (!atomTruthValue.isMBT() && noGood.hasHead()	// Ensure strong violation.
					&& atomStrongDecisionLevel > strongDecisionLevelHighestAssigned) {
					strongDecisionLevelHighestAssigned = atomStrongDecisionLevel;
					posStrongHighestAssigned = i;
				}
			}
		}

		// Set ordinary and alpha watches now.
		// Compute ordinary watches:
		final int watch1;
		final int watch2;


		if (posWeakUnassigned1 != -1 && posWeakUnassigned2 != -1) {
			// NoGood has two unassigned literals.
			watch1 = posWeakUnassigned1;
			watch2 = posWeakUnassigned2;
		} else if (posSatisfiedLiteral1 != -1) {
			// NoGood is satisfied.
			int bestSecondPointer = posSatisfiedLiteral2 != -1 ? posSatisfiedLiteral2
						: posWeakUnassigned1 != -1 ? posWeakUnassigned1
						: posWeakHighestAssigned;
			if (posSatisfiedLiteral2 == -1 && posWeakUnassigned1 == -1) {
				// The NoGood has only one satisfied literal and is unit without it.
				// If it is unit on lower decision level than it is satisfied, it propagates the satisfying literal on lower decision level.
				if (satisfiedLiteralWeakDecisionLevel > weakDecisionLevelHighestAssigned) {
					ConflictCause conflictCause = assignWeakComplement(posSatisfiedLiteral1, noGood, weakDecisionLevelHighestAssigned);
					if (conflictCause != null) {
						return conflictCause;
					}
				}
			}
			watch1 = posSatisfiedLiteral1;
			watch2 = bestSecondPointer;
		} else if (posWeakUnassigned1 != -1) {
			// NoGood is weakly unit; propagate.
			ConflictCause conflictCause = assignWeakComplement(posWeakUnassigned1, noGood, weakDecisionLevelHighestAssigned);
			if (conflictCause != null) {
				return conflictCause;
			}
			watch1 = posWeakUnassigned1;
			watch2 = posWeakHighestAssigned;
		} else {
			// NoGood is violated.
			return new ConflictCause(noGood.asAntecedent());
		}

		// Compute alpha watch:
		int watchAlpha = -1;
		if (noGood.hasHead()) {
			if (posPotentialAlphaWatch != -1) {
				// Found potential alpha watch.
				watchAlpha = posPotentialAlphaWatch;
			} else {
				// No potential alpha watch found: noGood must be strongly unit.
				ConflictCause conflictCause = assignStrongComplement(noGood, strongDecisionLevelHighestAssigned);
				if (conflictCause != null) {
					return conflictCause;
				}
				watchAlpha = posStrongHighestAssigned;
			}
		}
		WatchedNoGood wng = new WatchedNoGood(noGood, watch1, watch2, watchAlpha);
		LOGGER.trace("WatchedNoGood is {}.", wng);

		// Record for eventual removal if this NoGood is learned.
		if (noGood.getType() == Type.LEARNT) {
			wng.setLBD(lbd);
			learnedNoGoodDeletion.recordLearnedNoGood(wng);
		}

		if (wng.getAlphaPointer() == -1 && noGood.hasHead()) {
			throw oops("Did not set alpha watch for nogood with head.");
		}

		// Register alpha watch if present.
		if (wng.getAlphaPointer() != -1) {
			addAlphaWatch(wng);
		}
		// Set ordinary watches.
		addOrdinaryWatch(wng, 0);
		addOrdinaryWatch(wng, 1);
		return null;
	}

	private ConflictCause addAndWatchBinary(final NoGood noGood) {
		// Shorthands for viewing the nogood as { a, b }.
		final int a = noGood.getLiteral(0);
		final int b = noGood.getLiteral(1);
		final int atomA = atomOf(a);
		final int atomB = atomOf(b);

		// Ignore NoGoods of the form { -a, a }.
		if (a != b && atomA == atomB) {
			return null;
		}

		// Note: it might be faster to not check explicitly for violation but wait for conflict from assign.
		final ThriceTruth atomATruthValue = assignment.getTruth(atomA);
		final ThriceTruth atomBTruthValue = assignment.getTruth(atomB);

		final boolean isViolatedA = atomATruthValue != null && isPositive(a) == atomATruthValue.toBoolean();
		final boolean isViolatedB = atomBTruthValue != null && isPositive(b) == atomBTruthValue.toBoolean();

		// Check for violation.
		if (isViolatedA && isViolatedB) {
			return new ConflictCause(noGood.asAntecedent());
		}

		// The above violation check guarantees that adding (and propagation on other literal) results in no conflict.
		binaryWatches[a].add(noGood);
		binaryWatches[b].add(noGood);
		hasBinaryNoGoods = true;
		return null;
	}

	private ConflictCause assignWeakComplement(final int literalIndex, final NoGoodInterface impliedBy, int decisionLevel) {
		final int literal = impliedBy.getLiteral(literalIndex);
		ThriceTruth truth = isNegated(literal) ? MBT : FALSE;
		return assignTruth(atomOf(literal), truth, impliedBy.asAntecedent(), decisionLevel);
	}

	private ConflictCause assignStrongComplement(final NoGoodInterface impliedBy, int decisionLevel) {
		return assignTruth(atomOf(impliedBy.getHead()), TRUE, impliedBy.asAntecedent(), decisionLevel);
	}

	private ConflictCause assignTruth(int atom, ThriceTruth truth, Antecedent impliedBy, int decisionLevel) {
		ConflictCause cause = assignment.assign(atom, truth, impliedBy, decisionLevel);
		if (cause == null) {
			didPropagate = true;
		}
		return cause;
	}

	/**
	 * Propagates from Unassigned to MBT/FALSE.
	 * @param literal the literal that triggers the propagation.
	 */
	private ConflictCause propagateWeakly(int literal, int currentDecisionLevel, boolean restrictToBinaryNoGoods) {
		final ArrayList<WatchedNoGood> watchesOfAssignedAtom = watches(literal);

		// Propagate binary watches.
		ConflictCause conflictCause = binaryWatches[literal].propagateWeakly();
		if (conflictCause != null || restrictToBinaryNoGoods) {
			return conflictCause;
		}

		// Check all watched multi-ary NoGoods.
		Iterator<WatchedNoGood> watchIterator = watchesOfAssignedAtom.iterator();
		clearOrdinaryWatchList(literal);
		while (watchIterator.hasNext()) {
			WatchedNoGood nextNoGood = watchIterator.next();
			conflictCause = processWeaklyWatchedNoGood(literal, nextNoGood, currentDecisionLevel);
			if (conflictCause != null) {
				// Copy over all non-treated NoGoods, so that they can be treated after backtracking.
				ArrayList<WatchedNoGood> watchlist = watches(literal);
				watchlist.add(nextNoGood);
				while (watchIterator.hasNext()) {
					watchlist.add(watchIterator.next());
				}
				return conflictCause;
			}
		}
		return null;
	}

	private ConflictCause processWeaklyWatchedNoGood(int assignedLiteral, WatchedNoGood watchedNoGood, int currentDecisionLevel) {
		final int assignedWatch = watchedNoGood.getLiteral(0) == assignedLiteral ? 0 : 1;
		final int otherWatch = 1 - assignedWatch;

		final int otherLiteral = watchedNoGood.getLiteral(otherWatch);
		final ThriceTruth otherAtomTruth = assignment.getTruth(atomOf(otherLiteral));

		// Find new literal to watch.

		// Check if the other watch already satisfies the noGood.
		if (otherAtomTruth != null && otherAtomTruth.toBoolean() != isPositive(otherLiteral)) {
			// Keep this watch and return early.
			addOrdinaryWatch(watchedNoGood, assignedWatch);
			return null;
		} else {
			for (int i = 2; i < watchedNoGood.size(); i++) {
				final int currentLiteral = watchedNoGood.getLiteral(i);
				final ThriceTruth currentTruth = assignment.getTruth(atomOf(currentLiteral));

				// Break if: 1) current literal is unassigned, or 2) satisfies the nogood.
				if (currentTruth == null || currentTruth.toBoolean() != isPositive(currentLiteral)) {
					// Move pointer to new literal.
					watchedNoGood.setWatch(assignedWatch, i);
					addOrdinaryWatch(watchedNoGood, assignedWatch);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Moved watch pointers of nogood:");
						logNoGoodAndAssignment(watchedNoGood, assignment);
					}
					return null;
				}
			}
		}

		// NoGood is unit, propagate the other watched literal.
		// Note: Violation is detected by Assignment.
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Nogood is unit:");
			logNoGoodAndAssignment(watchedNoGood, assignment);
		}
		ConflictCause conflictCause = assignWeakComplement(otherWatch, watchedNoGood, currentDecisionLevel);
		// Return conflict if noGood is violated.
		if (conflictCause != null) {
			return conflictCause;
		}
		// Watch same literal again.
		addOrdinaryWatch(watchedNoGood, assignedWatch);
		return null;
	}

	private void logNoGoodAndAssignment(WatchedNoGood noGood, Assignment assignment) {
		if (!LOGGER.isTraceEnabled()) {
			return;
		}
		StringBuilder sb = new StringBuilder("Watched NoGood is: " + noGood + "\t\t Assigned: ");
		for (Integer literal : noGood) {
			Assignment.Entry assignmentEntry = assignment.get(atomOf(literal));
			sb.append(atomOf(literal));
			sb.append("=");
			sb.append(assignmentEntry);
			sb.append(", ");
		}
		LOGGER.trace(sb.toString());
	}

	private ConflictCause propagateStrongly(int literal, int currentDecisionLevel, boolean restrictToBinaryNoGoods) {

		// Propagate binary watches.
		ConflictCause conflictCause = binaryWatches[literal].propagateStrongly();
		if (conflictCause != null || restrictToBinaryNoGoods) {
			return conflictCause;
		}

		// Check all watched multi-ary NoGoods.
		Iterator<WatchedNoGood> watchIterator = watchesAlpha(literal).iterator();
		clearAlphaWatchList(literal);
		while (watchIterator.hasNext()) {
			WatchedNoGood nextNoGood = watchIterator.next();
			if (!nextNoGood.hasHead()) {
				throw oops("Strong propagation encountered NoGood without head");
			}
			conflictCause = processStronglyWatchedNoGood(nextNoGood, currentDecisionLevel);
			if (conflictCause != null) {
				// Copy over all non-treated NoGoods, so that they can be treated after backtracking.
				ArrayList<WatchedNoGood> watchlist = watchesAlpha(literal);
				watchlist.add(nextNoGood);
				while (watchIterator.hasNext()) {
					watchlist.add(watchIterator.next());
				}
				return conflictCause;
			}
		}
		return null;
	}

	private ConflictCause processStronglyWatchedNoGood(WatchedNoGood watchedNoGood, int currentDecisionLevel) {

		final int headAtom = atomOf(watchedNoGood.getHead());
		final ThriceTruth headAtomTruth = assignment.getTruth(headAtom);
		// Check if the other watch, i.e., the head, already satisfies the noGood.
		if (headAtomTruth != null && TRUE == headAtomTruth) {
			// Keep this watch and return early.
			addAlphaWatch(watchedNoGood);
			return null;
		}

		final int assignedIndex = watchedNoGood.getAlphaPointer();

		// Find new literal to watch.
		for (int i = 0; i < watchedNoGood.size(); i++) {
			if (i == assignedIndex || i == watchedNoGood.getHeadIndex()) {
				continue;
			}
			int currentLiteral = watchedNoGood.getLiteral(i);
			int currentAtom = atomOf(currentLiteral);
			ThriceTruth currentAtomTruth = assignment.getTruth(currentAtom);

			// Break if: 1) current literal is unassigned (or MBT), or 2) satisfies the nogood.
			if (currentAtomTruth == null || currentAtomTruth.isMBT() || currentAtomTruth.toBoolean() != isPositive(currentLiteral)) {
				// Move pointer to new literal.
				watchedNoGood.setAlphaPointer(i);
				addAlphaWatch(watchedNoGood);
				return null;
			}
		}

		// NoGood is unit, propagate.
		ConflictCause conflictCause = assignStrongComplement(watchedNoGood, currentDecisionLevel);
		if (conflictCause != null) {
			return conflictCause;
		}
		// Watch same literal again.
		addAlphaWatch(watchedNoGood);
		return null;
	}

	@Override
	public ConflictCause propagate() {
		ConflictCause conflictCause = propagate(false);
		if (conflictCause != null) {
			learnedNoGoodDeletion.increaseConflictCounter();
		}
		return conflictCause;
	}

	private ConflictCause propagateOnlyBinaryNoGoods() {
		return propagate(true);
	}
	
	private ConflictCause propagate(boolean restrictToBinaryNoGoods) {
		didPropagate = false;

		Assignment.Pollable assignmentsToProcess = assignment.getAssignmentsToProcess();
		int currentDecisionLevel = assignment.getDecisionLevel();
		while (!assignmentsToProcess.isEmpty()) {
			final int atom = assignmentsToProcess.peek();
			final ThriceTruth currentTruth = assignment.getTruth(atom);
			final int literal = atomToLiteral(atom, currentTruth.toBoolean());
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Propagation processing atom: {}={}", atom, currentTruth);
			}

			// Propagate weakly, except if there is an earlier MBT, where propagation already took place.
			if (currentTruth != TRUE || assignment.getWeakDecisionLevel(atom) == currentDecisionLevel) {
				ConflictCause conflictCause = propagateWeakly(literal, currentDecisionLevel, restrictToBinaryNoGoods);
				if (conflictCause != null) {
					LOGGER.trace("Halting propagation due to conflict. Current assignment: {}.", assignment);
					return conflictCause;
				}
			}

			// Propagate strongly only for TRUE/FALSE assignments.
			if (currentTruth != MBT) {
				ConflictCause conflictCause = propagateStrongly(literal, currentDecisionLevel, restrictToBinaryNoGoods);
				if (conflictCause != null) {
					LOGGER.trace("Halting propagation due to conflict. Current assignment: {}.", assignment);
					return conflictCause;
				}
			}
			assignmentsToProcess.remove();
		}
		if (checksEnabled && !restrictToBinaryNoGoods) {
			runInternalChecks();
		}
		return null;
	}

	@Override
	public boolean didPropagate() {
		return didPropagate;
	}

	@Override
	public void setChecksEnabled(boolean checksEnabled) {
		this.checksEnabled = checksEnabled;
	}

	class BinaryWatchList implements ShallowAntecedent {
		private int[] noGoodsWithoutHead = new int[10];
		private int noGoodsWithoutHeadSize;
		private int[] noGoodsWithHead = new int[10];
		private int noGoodsWithHeadSize;
		private final int forLiteral;

		private BinaryWatchList(int forLiteral) {
			this.forLiteral = forLiteral;
		}

		ConflictCause add(NoGood noGood) {
			if (!noGood.isBinary()) {
				throw oops("Received noGood is not binary.");
			}
			if (noGood.hasHead() && noGood.getHead() != forLiteral) {
				return addHeadedNoGood(noGood);
			} else {
				return addOrdinaryNoGood(noGood);
			}
		}

		private ConflictCause addHeadedNoGood(NoGood noGood) {
			if (noGoodsWithHeadSize + 1 > noGoodsWithHead.length) {
				noGoodsWithHead = Arrays.copyOf(noGoodsWithHead, arrayGrowthSize(noGoodsWithHeadSize));
			}
			int otherLiteral = noGood.getLiteral(0) == forLiteral ? noGood.getLiteral(1) : noGood.getLiteral(0);
			if (isPositive(otherLiteral)) {
				throw oops("NoGood has wrong head.");
			}
			noGoodsWithHead[noGoodsWithHeadSize++] = otherLiteral;
			// Assign (weakly) otherLiteral if the newly added NoGood is unit.
			ThriceTruth literalTruth = assignment.getTruth(atomOf(forLiteral));
			if (literalTruth != null && literalTruth.toBoolean() == isPositive(forLiteral)) {
				int weakDecisionLevel = assignment.getWeakDecisionLevel(atomOf(forLiteral));
				ConflictCause conflictCause = assignment.assign(atomOf(otherLiteral), isPositive(otherLiteral) ? FALSE : MBT, this, weakDecisionLevel);
				if (conflictCause != null) {
					return conflictCause;
				}
			}
			// Assign head (strongly) if the newly added NoGood is unit.
			int strongDecisionLevel = assignment.getStrongDecisionLevel(atomOf(forLiteral));
			if (strongDecisionLevel != -1 && assignment.getTruth(atomOf(forLiteral)).toBoolean() == isPositive(forLiteral)) {
				return assignment.assign(atomOf(otherLiteral), TRUE, this, strongDecisionLevel);
			}
			return null;
		}

		private ConflictCause addOrdinaryNoGood(NoGood noGood) {
			if (noGoodsWithoutHeadSize + 1 > noGoodsWithoutHead.length) {
				noGoodsWithoutHead = Arrays.copyOf(noGoodsWithoutHead, arrayGrowthSize(noGoodsWithoutHeadSize));
			}
			int otherLiteral = noGood.getLiteral(0) == forLiteral ? noGood.getLiteral(1) : noGood.getLiteral(0);
			noGoodsWithoutHead[noGoodsWithoutHeadSize++] = otherLiteral;
			// Assign otherLiteral if the newly added NoGood is unit.
			ThriceTruth literalTruth = assignment.getTruth(atomOf(forLiteral));
			if (literalTruth != null && literalTruth.toBoolean() == isPositive(forLiteral)) {
				int weakDecisionLevel = assignment.getWeakDecisionLevel(atomOf(forLiteral));
				return assignment.assign(atomOf(otherLiteral), isPositive(otherLiteral) ? FALSE : MBT, this, weakDecisionLevel);
			}
			return null;
		}

		ConflictCause propagateWeakly() {
			didPropagate |= noGoodsWithHeadSize > 0 || noGoodsWithoutHeadSize > 0;
			for (int i = 0; i < noGoodsWithoutHeadSize; i++) {
				final int otherLiteral = noGoodsWithoutHead[i];
				ConflictCause conflictCause = assignment.assign(atomOf(otherLiteral), isPositive(otherLiteral) ? FALSE : MBT, this);
				if (conflictCause != null) {
					return conflictCause;
				}
			}
			for (int i = 0; i < noGoodsWithHeadSize; i++) {
				final int otherLiteral = noGoodsWithHead[i];
				ConflictCause conflictCause = assignment.assign(atomOf(otherLiteral), isPositive(otherLiteral) ? FALSE : MBT, this);
				if (conflictCause != null) {
					return conflictCause;
				}
			}
			return null;
		}

		ConflictCause propagateStrongly() {
			didPropagate |= noGoodsWithHeadSize > 0;
			for (int i = 0; i < noGoodsWithHeadSize; i++) {
				final int headLiteral = noGoodsWithHead[i];
				ConflictCause conflictCause = assignment.assign(atomOf(headLiteral), TRUE, this);
				if (conflictCause != null) {
					return conflictCause;
				}
			}
			return null;
		}
		
		public int size() {
			return noGoodsWithHeadSize + noGoodsWithoutHeadSize;
		}

		@Override
		public String toString() {
			return "BinaryWatchList(" + forLiteral + ")";
		}

		@Override
		public Antecedent instantiateAntecedent(int impliedLiteral) {
			return new BinaryAntecedent(impliedLiteral, forLiteral);
		}

		private class BinaryAntecedent implements Antecedent {
			private final int[] literals = new int[2];

			BinaryAntecedent(int lit1, int lit2) {
				literals[0] = lit1;
				literals[1] = lit2;
			}

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

			@Override
			public String toString() {
				return "{" + literalToString(literals[0]) + ", " + literalToString(literals[1]) + "}";
			}
		}
	}

	private void clearOrdinaryWatchList(int literal) {
		watches[literal] = new ArrayList<>();
	}

	private void clearAlphaWatchList(int literal) {
		watchesAlpha[literal] = new ArrayList<>();
	}

	@Override
	public int estimate(int atom, boolean truth, BinaryNoGoodPropagationEstimationStrategy strategy) {
		switch (strategy) {
			case BinaryNoGoodPropagation:
				if (hasBinaryNoGoods) {
					return estimateEffectsOfBinaryNoGoodPropagation(atom, truth) - 1;
				}
			case CountBinaryWatches:
			default:
				return getNumberOfBinaryWatches(atom, truth);
		}
	}

	private int getNumberOfBinaryWatches(int atom, boolean truth) {
		return binaryWatches[atomToLiteral(atom, truth)].size();
	}

	private int estimateEffectsOfBinaryNoGoodPropagation(int atom, boolean truth) {
		assignment.choose(atom, truth);
		propagateOnlyBinaryNoGoods();
		int assignedNewly = assignment.getNumberOfAtomsAssignedSinceLastDecision();
		assignment.backtrack();
		return assignedNewly;
	}

	private void runInternalChecks() {
		new WatchedNoGoodsChecker().doWatchesCheck();
	}

	/**
	 * This class provides helper methods to detect NoGoods that are not properly watched by this NoGoodStore.
	 * This should be used only during debugging since checking is costly.
	 */
	private class WatchedNoGoodsChecker {

		void doWatchesCheck() {
			LOGGER.debug("Checking watch invariant.");
			// Check all watched NoGoods, if their pointers adhere to the watch-pointer invariant.
			for (int literal = 0; literal < watches.length; literal++) {
				if (isNegated(literal)) {
					// We treat positive and negative ones at the iteration of the positive literal.
					continue;
				}
				final int atom = atomOf(literal);
				if (atom > maxAtomId) {
					break;
				}
				checkOrdinaryWatchesInvariant(atom, true);
				checkOrdinaryWatchesInvariant(atom, false);
				checkAlphaWatchesInvariant(atom, true);
				checkAlphaWatchesInvariant(atom, false);
			}
			LOGGER.debug("Checking watch invariant: all good.");
		}

		int weakDecisionLevel(Assignment.Entry entry) {
			return entry == null ? UNASSIGNED : entry.hasPreviousMBT() ? entry.getMBTDecisionLevel() : entry.getDecisionLevel();
		}

		int weakReplayLevel(int atom) {
			if (assignment instanceof TrailAssignment) {
				return ((TrailAssignment) assignment).getOutOfOrderDecisionLevel(atom);
			}
			return UNASSIGNED;
		}

		int trailAwareStrongDecisionLevel(int atom) {
			int trailStrongDecisionLevel = UNASSIGNED;
			if (assignment instanceof TrailAssignment) {
				trailStrongDecisionLevel = ((TrailAssignment) assignment).getOutOfOrderStrongDecisionLevel(atom);
			}
			int atomStrongDecisionLevel = strongDecisionLevel(atom);
			return Math.min(trailStrongDecisionLevel, atomStrongDecisionLevel);
		}

		private void checkAlphaWatchesInvariant(int atom, boolean truth) {
			Assignment.Entry atomEntry = assignment.get(atom);
			int atomLiteral = atomToLiteral(atom, truth);
			boolean atomSatisfies = atomEntry != null && isPositive(atomLiteral) != atomEntry.getTruth().toBoolean();
			int atomDecisionLevel = strongDecisionLevel(atom);
			BinaryWatchList binaryWatchList = binaryWatches[atomLiteral];
			for (int i = 0; i < binaryWatchList.noGoodsWithHeadSize; i++) {
				int headLiteral = binaryWatchList.noGoodsWithHead[i];
				if (headLiteral == atomLiteral) {
					throw oops("Watch invariant violated: alpha watch points at head.");
				}
				Assignment.Entry headEntry = assignment.get(atomOf(headLiteral));
				boolean headViolates = headEntry != null && isPositive(headLiteral) == headEntry.getTruth().toBoolean();
				int headDecisionLevel = trailAwareStrongDecisionLevel(atomOf(headLiteral));
				if (watchInvariant(atomSatisfies, atomDecisionLevel, headLiteral, headDecisionLevel, headEntry)
					|| headViolates) {	// Head "pointer" is never moved and violation is checked by weak propagation, hence a violated head is okay.
					continue;
				}
				throw oops("Watch invariant (alpha) violated");
			}
			for (WatchedNoGood watchedNoGood : watchesAlpha(atomLiteral)) {
				int headLiteral = watchedNoGood.getHead();
				if (headLiteral == atomLiteral) {
					throw oops("Watch invariant violated: alpha watch points at head.");
				}
				Assignment.Entry headEntry = assignment.get(atomOf(headLiteral));
				boolean headViolates = headEntry != null && isPositive(headLiteral) == headEntry.getTruth().toBoolean();
				int headDecisionLevel = trailAwareStrongDecisionLevel(atomOf(headLiteral));
				if (watchInvariant(atomSatisfies, atomDecisionLevel, headLiteral, headDecisionLevel, headEntry)
					|| headViolates) {	// Head "pointer" is never moved and violation is checked by weak propagation, hence a violated head is okay.
					continue;
				}
				throw oops("Watch invariant (alpha) violated");
			}
		}

		private void checkOrdinaryWatchesInvariant(int atom, boolean truth) {
			Assignment.Entry atomEntry = assignment.get(atom);
			int atomLiteral = atomToLiteral(atom, truth);
			boolean atomSatisfies = atomEntry != null && isPositive(atomLiteral) != atomEntry.getTruth().toBoolean();
			int atomDecisionLevel = weakDecisionLevel(atomEntry);
			int atomReplayLevel = weakReplayLevel(atom);
			BinaryWatchList binaryWatchList = binaryWatches[atomLiteral];
			for (int i = 0; i < binaryWatchList.noGoodsWithoutHeadSize; i++) {
				int otherLiteral = binaryWatchList.noGoodsWithoutHead[i];
				checkBinaryWatch(atomSatisfies, atomDecisionLevel, atomReplayLevel, otherLiteral);
			}
			for (int i = 0; i < binaryWatchList.noGoodsWithHeadSize; i++) {
				int otherLiteral = binaryWatchList.noGoodsWithHead[i];
				checkBinaryWatch(atomSatisfies, atomDecisionLevel, atomReplayLevel, otherLiteral);
			}
			for (WatchedNoGood watchedNoGood : watches(atomLiteral)) {
				// Ensure both watches are either unassigned, or one satisfies NoGood, or both are on highest decision level.
				int otherPointer = atom ==  atomOf(watchedNoGood.getLiteral(1)) ? 0 : 1;
				int otherLiteral = watchedNoGood.getLiteral(otherPointer);
				int otherAtom = atomOf(otherLiteral);
				int thisAtom = atomOf(watchedNoGood.getLiteral(1 - otherPointer));
				if (thisAtom != atom) {
					throw oops("Watched atom is not at first/second position in literals array.");
				}
				Assignment.Entry otherEntry = assignment.get(otherAtom);
				int otherDecisionLevel = weakDecisionLevel(otherEntry);
				int otherReplayLevel = weakReplayLevel(otherAtom);
				boolean otherSatisfies = otherEntry != null && isPositive(otherLiteral) != otherEntry.getTruth().toBoolean();
				if (watchInvariant(atomSatisfies, otherSatisfies, atomDecisionLevel, atomReplayLevel, otherDecisionLevel, otherReplayLevel)) {
					continue;
				}
				throw oops("Watch invariant violated");
			}
		}

		private void checkBinaryWatch(boolean atomSatisfies, int atomDecisionLevel, int atomReplayLevel, int otherLiteral) {
			int otherAtom = atomOf(otherLiteral);
			Assignment.Entry otherEntry = assignment.get(otherAtom);
			boolean otherSatisfies = otherEntry != null && isPositive(otherLiteral) != otherEntry.getTruth().toBoolean();
			int otherDecisionLevel = weakDecisionLevel(otherEntry);
			int otherReplayLevel = weakReplayLevel(otherAtom);
			if (watchInvariant(atomSatisfies, otherSatisfies, atomDecisionLevel, atomReplayLevel, otherDecisionLevel, otherReplayLevel)) {
				return;
			}
			throw oops("Watch invariant violated");
		}

		private boolean watchInvariant(boolean atomSatisfies, boolean otherAtomSatisfies, int atomDecisionLevel, int atomReplayLevel, int otherAtomDecisionLevel, int otherAtomReplayLevel) {
			if (atomDecisionLevel == UNASSIGNED && otherAtomDecisionLevel == UNASSIGNED) {
				// Both watches are unassigned.
				return true;
			}
			if ((atomSatisfies && (otherAtomDecisionLevel >= atomDecisionLevel || otherAtomDecisionLevel >= atomReplayLevel))
				|| (otherAtomSatisfies && (atomDecisionLevel >= otherAtomDecisionLevel || atomDecisionLevel >= otherAtomReplayLevel))) {
				// One watch satisfies the nogood and the other is assigned at higher decision level (or higher than the replay level of the satisfying one).
				return true;
			}
			return false;
		}

		private boolean watchInvariant(boolean atomSatisfies, int atomDecisionLevel, int otherLiteral, int otherDecisionLevel, Assignment.Entry otherEntry) {
			boolean otherSatisfies = otherEntry != null && isPositive(otherLiteral) != otherEntry.getTruth().toBoolean();
			if (atomDecisionLevel == Integer.MAX_VALUE && otherDecisionLevel == Integer.MAX_VALUE) {
				// Both watches are unassigned.
				return true;
			}
			if ((atomSatisfies && otherDecisionLevel >= atomDecisionLevel)
				|| (otherSatisfies && atomDecisionLevel >= otherDecisionLevel)) {
				// One watch satisfies the nogood and the other is assigned at higher decision level.
				return true;
			}
			return false;
		}
	}
}
