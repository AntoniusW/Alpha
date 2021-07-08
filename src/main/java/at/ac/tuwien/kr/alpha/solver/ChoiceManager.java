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

import at.ac.tuwien.kr.alpha.common.Assignment;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import at.ac.tuwien.kr.alpha.solver.heuristics.domspec.DefaultDomainSpecificHeuristicsStore;
import at.ac.tuwien.kr.alpha.solver.heuristics.domspec.DomainSpecificHeuristicsStore;
import at.ac.tuwien.kr.alpha.solver.heuristics.domspec.EmptyDomainSpecificHeuristicsStore;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.Literals.atomToLiteral;

/**
 * This class provides functionality for choice point management, detection of active choice points, etc.
 * Copyright (c) 2017-2020, the Alpha Team.
 */
public class ChoiceManager implements Checkable {

	private static final int DEFAULT_CHOICE_ATOM = 0;

	private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceManager.class);
	private final WritableAssignment assignment;
	private final Stack<Choice> choiceStack;
	private final DomainSpecificHeuristicsStore domainSpecificHeuristics;
	private final Map<Integer, Set<Integer>> headsToBodies = new HashMap<>();
	private final Map<Integer, Integer> bodiesToHeads = new HashMap<>();

	// Two "influence managers" managing active choice points and heuristics.
	private final ChoicePointInfluenceManager choicePointInfluenceManager;
	private final HeuristicInfluenceManager heuristicInfluenceManager;

	private final NoGoodStore store;
	private final BinaryNoGoodPropagationEstimation bnpEstimation;
	private final ChoiceManagerStatistics choiceManagerStatistics = new ChoiceManagerStatistics();

	private boolean checksEnabled;
	private DebugWatcher debugWatcher;

	public ChoiceManager(WritableAssignment assignment, NoGoodStore store) {
		this(assignment, store, null);
	}

	protected ChoiceManager(WritableAssignment assignment, NoGoodStore store, DomainSpecificHeuristicsStore domainSpecificHeuristicsStore) {
		this.store = store;
		this.assignment = assignment;
		this.choicePointInfluenceManager = new ChoicePointInfluenceManager(assignment);
		this.heuristicInfluenceManager = new HeuristicInfluenceManager(assignment);
		this.choiceStack = new Stack<>();
		if (domainSpecificHeuristicsStore != null) {
			this.domainSpecificHeuristics = domainSpecificHeuristicsStore;
		} else {
			this.domainSpecificHeuristics = new DefaultDomainSpecificHeuristicsStore();
		}
		this.domainSpecificHeuristics.setChoiceManager(this);
		assignment.setCallback(this);
		this.bnpEstimation = store instanceof BinaryNoGoodPropagationEstimation
				? (BinaryNoGoodPropagationEstimation)store
				: null;
	}

	public WritableAssignment getAssignment() {
		return assignment;
	}

	NoGood computeEnumeration() {
		int[] enumerationLiterals = new int[choiceStack.size()];
		int enumerationPos = 0;
		for (Choice e : choiceStack) {
			enumerationLiterals[enumerationPos++] = atomToLiteral(e.getAtom(), e.getValue());
		}
		return new NoGood(enumerationLiterals);
	}

	@Override
	public void setChecksEnabled(boolean checksEnabled) {
		this.checksEnabled = checksEnabled;
		this.choicePointInfluenceManager.setChecksEnabled(checksEnabled);
		this.heuristicInfluenceManager.setChecksEnabled(checksEnabled);

		if (checksEnabled) {
			debugWatcher = new DebugWatcher();
		} else {
			debugWatcher = null;
		}
	}

	public boolean isChecksEnabled() {
		return checksEnabled;
	}

	void callbackOnChanged(int atom) {
		choicePointInfluenceManager.callbackOnChanged(atom);
		heuristicInfluenceManager.callbackOnChanged(atom);
	}

	public ChoiceManagerStatistics getStatistics() {
		return choiceManagerStatistics;
	}

	void updateAssignments() {
		LOGGER.trace("Updating assignments of ChoiceManager.");
		if (checksEnabled) {
			choicePointInfluenceManager.checkActiveChoicePoints();
		}
	}

	public void choose(Choice choice) {
		if (!choice.isBacktracked()) {
			choiceManagerStatistics.incrementChoices();
		}

		if (assignment.choose(choice.getAtom(), choice.getValue()) != null) {
			throw oops("Picked choice is incompatible with current assignment");
		}
		LOGGER.debug("Choice {} is {}@{}", choiceManagerStatistics.getChoices(), choice, assignment.getDecisionLevel());

		if (debugWatcher != null) {
			debugWatcher.runWatcher();
		}

		choiceStack.push(choice);
	}

	void backjump(int target) {
		if (target < 0) {
			throw oops("Backjumping to decision level less than 0");
		}

		choiceManagerStatistics.incrementBackjumps();
		LOGGER.debug("Backjumping to decision level {}.", target);

		// Remove everything above the target level, but keep the target level unchanged.
		int currentDecisionLevel = assignment.getDecisionLevel();
		assignment.backjump(target);
		while (currentDecisionLevel-- > target) {
			final Choice choice = choiceStack.pop();
			choiceManagerStatistics.incrementBacktracksWithinBackjumps();
			choiceManagerStatistics.incrementBacktracks();
			LOGGER.debug("Backjumping removed choice {}", choice);
		}
	}

	/**
	 * Fast backtracking will backtrack but not give any information about which choice was backtracked. This is
	 * handy in cases where higher level backtracking mechanisms already know what caused the backtracking and what
	 * to do next.
	 *
	 * In order to analyze the choice that was backtracked, use the more expensive {@link #backtrackSlow()}.
	 */
	void backtrackFast() {
		backtrack();

		final Choice choice = choiceStack.pop();

		LOGGER.debug("Backtracked (fast) to level {} from choice {}", assignment.getDecisionLevel(), choice);
	}

	/**
	 * Slow backtracking will take more time, but allow to analyze the {@link Assignment.Entry} corresponding to the
	 * choice that is being backtracked. Higher level backtracking mechanisms can use this information to change
	 * their plans.
	 *
	 * @return the assignment entry of the choice being backtracked, or {@code null} if the choice cannot be
	 *         backtracked any further (it already is a backtracking choice)
	 */
	Assignment.Entry backtrackSlow() {
		final Choice choice = choiceStack.pop();
		final Assignment.Entry lastChoiceEntry = assignment.get(choice.getAtom());

		backtrack();
		LOGGER.debug("Backtracked (slow) to level {} from choice {}", assignment.getDecisionLevel(), choice);

		if (choice.isBacktracked()) {
			return null;
		}

		return lastChoiceEntry;
	}

	/**
	 * This method implements the backtracking "core" that will be executed for both slow and fast backtracking.
	 * It backtracks the NoGoodStore and recomputes choice points.
	 */
	private void backtrack() {
		store.backtrack();
		choiceManagerStatistics.incrementBacktracks();
	}

	void addChoiceInformation(Pair<Map<Integer, Integer>, Map<Integer, Integer>> choiceAtoms, Map<Integer, Set<Integer>> headsToBodies) {
		choicePointInfluenceManager.addInformation(choiceAtoms);
		addHeadsToBodies(headsToBodies);
	}

	void addHeuristicInformation(Pair<Map<Integer, Integer[]>, Map<Integer, Integer[]>> heuristicAtoms, Map<Integer, HeuristicDirectiveValues> heuristicValues) {
		addInformation(heuristicValues);
		heuristicInfluenceManager.addInformation(heuristicAtoms, heuristicValues);
	}

	private void addInformation(Map<Integer, HeuristicDirectiveValues> heuristicValues) {
		for (Entry<Integer, HeuristicDirectiveValues> entry : heuristicValues.entrySet()) {
			domainSpecificHeuristics.addInfo(entry.getKey(), entry.getValue());
		}
	}

	public void growForMaxAtomId(int maxAtomId) {
		choicePointInfluenceManager.growForMaxAtomId(maxAtomId);
	}

	private void addHeadsToBodies(Map<Integer, Set<Integer>> headsToBodies) {
		for (Entry<Integer, Set<Integer>> entry : headsToBodies.entrySet()) {
			Integer head = entry.getKey();
			Set<Integer> newBodies = entry.getValue();
			addHeadsToBodies(head, newBodies);
			addBodiesToHeads(head, newBodies);
		}
	}

	private void addHeadsToBodies(Integer head, Set<Integer> bodies) {
		Set<Integer> existingBodies = this.headsToBodies.computeIfAbsent(head, k -> new HashSet<>());
		existingBodies.addAll(bodies);
	}

	private void addBodiesToHeads(Integer head, Set<Integer> bodies) {
		for (Integer body : bodies) {
			bodiesToHeads.put(body, head);
		}
	}

	public DomainSpecificHeuristicsStore getDomainSpecificHeuristics() {
		return domainSpecificHeuristics;
	}

	public boolean isActiveChoiceAtom(int atom) {
		return choicePointInfluenceManager.isActive(atom);
	}

	public int getNextActiveChoiceAtom() {
		return choicePointInfluenceManager.getNextActiveAtomOrDefault(DEFAULT_CHOICE_ATOM);
	}

	/**
	 * @return the number of active choice points
	 */
	public int getNumberOfActiveChoicePoints() {
		return choicePointInfluenceManager.activeChoicePointsAtoms.size();
	}

	public boolean isAtomChoice(int atom) {
		return choicePointInfluenceManager.isAtomInfluenced(atom);
	}

	public void setChoicePointActivityListener(InfluenceManager.ActivityListener activityListener) {
		choicePointInfluenceManager.setActivityListener(activityListener);
	}

	public void setHeuristicActivityListener(InfluenceManager.ActivityListener activityListener) {
		heuristicInfluenceManager.setActivityListener(activityListener);
	}

	public Integer getHeadDerivedByChoiceAtom(int choiceAtomId) {
		return bodiesToHeads.get(choiceAtomId);
	}

	/**
	 * Gets the active choice atoms representing bodies of rules that can derive the given head atom.
	 * @param headAtomId internal ID of head atom
	 * @return a subset of all active choice atoms that can derive {@code headAtomId}.
	 */
	public Set<Integer> getActiveChoiceAtomsDerivingHead(int headAtomId) {
		Set<Integer> bodies = headsToBodies.get(headAtomId);
		if (bodies == null) {
			return Collections.emptySet();
		}
		Set<Integer> activeBodies = new HashSet<>();
		for (Integer body : bodies) {
			if (isActiveChoiceAtom(body)) {
				activeBodies.add(body);
			}
		}
		return activeBodies;
	}

	/**
	 * Returns an unmodifiable view on the current choice stack which does not contain choices that have been removed during backtracking.
	 *
	 * @return an unmodifiable view on the current choice stack
	 */
	List<Choice> getChoiceStack() {
		return Collections.unmodifiableList(choiceStack);
	}

	public BinaryNoGoodPropagationEstimation getBinaryNoGoodPropagationEstimation() {
		return bnpEstimation;
	}

	static ChoiceManager withoutDomainSpecificHeuristics(WritableAssignment assignment, NoGoodStore store) {
		return new ChoiceManager(assignment, store, new EmptyDomainSpecificHeuristicsStore());
	}

	static ChoiceManager withDomainSpecificHeuristics(WritableAssignment assignment, NoGoodStore store) {
		return new ChoiceManager(assignment, store);
	}

	/**
	 * A helper class for halting the debugger when certain assignments occur on the choice stack.
	 *
	 * Example usage (called from DefaultSolver):
	 * choiceStack = new ChoiceStack(grounder, true);
	 * choiceStack.getDebugWatcher().watchAssignments("_R_(0,_C:red_V:7)=TRUE", "_R_(0,_C:green_V:8)=TRUE", "_R_(0,_C:red_V:9)=TRUE", "_R_(0,_C:red_V:4)=TRUE");
	 */
	class DebugWatcher {
		ArrayList<String> toWatchFor = new ArrayList<>();

		private void runWatcher() {
			if (toWatchFor.size() == 0) {
				return;
			}
			String current = choiceStack.stream().map(Choice::toString).collect(Collectors.joining(", "));
			boolean contained = true;
			for (String s : toWatchFor) {
				if (!current.contains(s)) {
					contained = false;
					break;
				}
			}
			if (contained && toWatchFor.size() != 0) {
				LOGGER.debug("Marker hit.");	// Set debug breakpoint here to halt when desired assignment occurs.
			}
		}

		/**
		 * Registers atom assignments to watch for.
		 * @param toWatch one or more strings as they occur in ChoiceStack.toString()
		 */
		public void watchAssignments(String... toWatch) {
			toWatchFor = new ArrayList<>();
			Collections.addAll(toWatchFor, toWatch);
		}
	}
}
