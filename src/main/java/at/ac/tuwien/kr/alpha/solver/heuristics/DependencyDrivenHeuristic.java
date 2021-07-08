/**
 * Copyright (c) 2017-2019 Siemens AG
 * All rights reserved.
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
package at.ac.tuwien.kr.alpha.solver.heuristics;

import at.ac.tuwien.kr.alpha.common.Assignment;
import at.ac.tuwien.kr.alpha.common.Literals;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import at.ac.tuwien.kr.alpha.solver.heuristics.activity.BodyActivityProvider;
import at.ac.tuwien.kr.alpha.solver.heuristics.activity.BodyActivityProviderFactory;
import at.ac.tuwien.kr.alpha.solver.heuristics.activity.BodyActivityProviderFactory.BodyActivityType;
import at.ac.tuwien.kr.alpha.solver.learning.GroundConflictNoGoodLearner.ConflictAnalysisResult;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static at.ac.tuwien.kr.alpha.common.Literals.*;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.FALSE;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.TRUE;

/**
 * The BerkMin variants {@link BerkMin} and {@link BerkMinLiteral} suffer from the fact that choice points
 * comprise only a small portion of all the literals occuring in nogoods and therefore do not influence
 * activity and sign counters as much as other atoms.
 * The basic idea of {@link DependencyDrivenHeuristic} is therefore to find <i>dependent</i> atoms that can
 * enrich the information available for a choice point. Intuitively, all atoms occurring in the head or the
 * body of a rule depend on a choice point representing the body of this rule.
 * 
 * Copyright (c) 2017-2018 Siemens AG
 */
public class DependencyDrivenHeuristic implements ActivityBasedBranchingHeuristic {
	protected static final Logger LOGGER = LoggerFactory.getLogger(DependencyDrivenHeuristic.class);
	
	public static final double DEFAULT_ACTIVITY = 0.0;
	public static final int DEFAULT_SIGN_COUNTER = 0;

	public static final int DEFAULT_DECAY_AGE = 10;
	public static final double DEFAULT_DECAY_FACTOR = 0.25;

	protected final Assignment assignment;
	protected final ChoiceManager choiceManager;
	protected final Random rand;
	protected final BodyActivityProvider bodyActivity;

	protected final Map<Integer, Double> activityCounters = new HashMap<>();
	protected final Map<Integer, Integer> signCounters = new HashMap<>();
	protected final Deque<NoGood> stackOfNoGoods = new ArrayDeque<>();
	private int decayPeriod;
	private double decayFactor;
	private int stepsSinceLastDecay;

	/**
	 * Maps body-representing atoms to rule heads.
	 */
	protected final Map<Integer, Integer> bodyAtomToHeadAtom = new HashMap<>();

	/**
	 * Maps rule heads to atoms representing corresponding bodies.
	 * TODO: somehow use at.ac.tuwien.kr.alpha.solver.ChoiceManager.headsToBodies instead (cf. issue #181)
	 */
	protected final MultiValuedMap<Integer, Integer> headToBodies = new HashSetValuedHashMap<>();

	/**
	 * Maps body-representing atoms to literals occuring in the rule body.
	 */
	protected final MultiValuedMap<Integer, Integer> bodyAtomToLiterals = new HashSetValuedHashMap<>();

	/**
	 * Maps atoms to atoms representing bodies of rules in which the former atoms occur (in the head or the body).
	 */
	protected final MultiValuedMap<Integer, Integer> atomsToBodiesAtoms = new HashSetValuedHashMap<>();

	/**
	 *
	 * @param assignment
	 * @param choiceManager
	 * @param decayPeriod the number of steps after which all counters are decayed (i.e. multiplied by {@code decayFactor}).
	 * @param decayFactor
	 * @param random
	 * @param bodyActivityType
	 */
	public DependencyDrivenHeuristic(Assignment assignment, ChoiceManager choiceManager, int decayPeriod, double decayFactor, Random random, BodyActivityType bodyActivityType) {
		this.assignment = assignment;
		this.choiceManager = choiceManager;
		this.decayPeriod = decayPeriod;
		this.decayFactor = decayFactor;
		this.rand = random;
		this.bodyActivity = BodyActivityProviderFactory.getInstance(bodyActivityType, bodyAtomToLiterals, activityCounters, DEFAULT_ACTIVITY);
	}

	public DependencyDrivenHeuristic(Assignment assignment, ChoiceManager choiceManager, Random random, BodyActivityType bodyActivityType) {
		this(assignment, choiceManager, DEFAULT_DECAY_AGE, DEFAULT_DECAY_FACTOR, random, bodyActivityType);
	}

	public DependencyDrivenHeuristic(Assignment assignment, ChoiceManager choiceManager, Random random) {
		this(assignment, choiceManager, random, BodyActivityType.DEFAULT);
	}

	@Override
	public void violatedNoGood(NoGood violatedNoGood) {
		pushToStack(violatedNoGood);
	}

	@Override
	public void analyzedConflict(ConflictAnalysisResult analysisResult) {
		pushToStack(analysisResult.learnedNoGood);
		for (int resolutionAtom : analysisResult.resolutionAtoms) {
			incrementActivityCounter(atomToLiteral(resolutionAtom, true));
			incrementActivityCounter(atomToLiteral(resolutionAtom, false));
		}
		if (analysisResult.learnedNoGood != null) {
			for (Integer literal : analysisResult.learnedNoGood) {
				incrementSignCounter(literal);
			}
		}
		decayAllIfTimeHasCome();
	}

	@Override
	public void newNoGood(NoGood newNoGood) {
		recordAtomRelationships(newNoGood);
		pushToStack(newNoGood);
		for (Integer literal : newNoGood) {
			incrementSignCounter(literal);
		}
	}

	@Override
	public void newNoGoods(Collection<NoGood> newNoGoods) {
		newNoGoods.forEach(this::newNoGood);
	}

	@Override
	public double getActivity(int literal) {
		return activityCounters.getOrDefault(atomOf(literal), DEFAULT_ACTIVITY);
	}
	
	/**
	 * {@link DependencyDrivenHeuristic} manages a stack of nogoods in the fashion of {@link BerkMin}
	 * and starts by looking at the most active atom <code>a</code> in the nogood currently at the top of the stack.
	 * If <code>a</code> is an active choice point (i.e. representing the body of an applicable rule), it is immediately chosen;
	 * else the most active choice point dependent on <code>a</code> is.
	 * If there is no such atom, we continue further down the stack.
	 * When choosing between dependent atoms, a {@link BodyActivityProvider} is employed to define the activity of a choice point.
	 */
	@Override
	public int chooseLiteral() {
		return chooseLiteral(null);
	}
	
	@Override
	public int chooseLiteral(Set<Integer> admissibleChoices) {
		int atom = chooseAtom(admissibleChoices);
		if (atom == DEFAULT_CHOICE_ATOM) {
			return DEFAULT_CHOICE_LITERAL;
		}
		boolean sign = chooseSign(atom);
		return atomToLiteral(atom, sign);
	}
	
	@Override
	public int chooseAtom(Set<Integer> admissibleChoices) {
		for (NoGood noGood : stackOfNoGoods) {
			int mostActiveAtom = getMostActiveAtom(noGood);
			if (choiceManager.isActiveChoiceAtom(mostActiveAtom) && (admissibleChoices == null || admissibleChoices.contains(mostActiveAtom))) {
				return mostActiveAtom;
			}

			Collection<Integer> bodies = atomsToBodiesAtoms.get(mostActiveAtom);
			Optional<Integer> mostActiveBody = getMostActiveBody(bodies.stream(), admissibleChoices);
			if (mostActiveBody.isPresent()) {
				return mostActiveBody.get();
			}
		}
		return DEFAULT_CHOICE_ATOM;
	}

	protected Optional<Integer> getMostActiveBody(Stream<Integer> streamOfBodies, Set<Integer> admissibleChoices) {
		if (admissibleChoices != null) {
			streamOfBodies = streamOfBodies.filter(admissibleChoices::contains);
		}
		Optional<Integer> mostActiveBody = streamOfBodies.filter(this::isUnassigned).filter(choiceManager::isActiveChoiceAtom)
				.max(Comparator.comparingDouble(bodyActivity::get));
		return mostActiveBody;
	}

	protected boolean chooseSign(int atom) {
		atom = getAtomForChooseSign(atom);

		if (assignment.getTruth(atom) == ThriceTruth.MBT) {
			return true;
		}

		int positiveCounter = signCounters.getOrDefault(atomToLiteral(atom, true),  DEFAULT_SIGN_COUNTER);
		int negativeCounter = signCounters.getOrDefault(atomToLiteral(atom, false), DEFAULT_SIGN_COUNTER);

		if (positiveCounter > negativeCounter) {
			return false;
		} else if (negativeCounter > positiveCounter) {
			return true;
		} else {
			return rand.nextBoolean();
		}
	}

	protected int getAtomForChooseSign(int atom) {
		Integer head = bodyAtomToHeadAtom.get(atom);
		if (head != null) {
			atom = head; // head atom can give more relevant information than atom representing rule body
		}
		return atom;
	}

	protected void recordAtomRelationships(NoGood noGood) {
		if (isBodyNotHead(noGood, choiceManager::isAtomChoice)) {
			int body = atomOf(noGood.getLiteral(1));
			int head = atomOf(noGood.getHead());
			bodyAtomToHeadAtom.put(body, head);
			headToBodies.put(head, body);
			atomsToBodiesAtoms.put(head, body);
		} else if (isBodyElementsNotBody(noGood, choiceManager::isAtomChoice)) {
			Set<Integer> literals = new HashSet<>();
			int bodyAtom = atomOf(noGood.getHead());
			for (int i = 0; i < noGood.size(); i++) {
				int literal = noGood.getLiteral(i);
				literals.add(literal);
				if (bodyAtom != 0) {
					atomsToBodiesAtoms.put(atomOf(literal), bodyAtom);
				} // else {
					// TODO
				// }
			}
			assert bodyAtom != 0;
			bodyAtomToLiterals.putAll(bodyAtom, literals);
		}
	}

	private void pushToStack(NoGood noGood) {
		if (noGood != null) {
			stackOfNoGoods.push(noGood);
		}
	}

	protected void incrementActivityCounter(int literal) {
		int atom = atomOf(literal);
		if (choiceManager.isAtomChoice(atom)) {
			activityCounters.compute(atom, (k, v) -> (v == null ? DEFAULT_ACTIVITY : v) + 1);
		}
		// TODO: check performance
		// note that here (and in incrementSignCounter) we only count atoms that are
		// choice points, which might affect performance.
		// alternative approaches:
		// 1. count everything and from time to time do a garbage collection (i.e.
		// remove atoms that are no choice points)
		// 2. make check cheaper, e.g. by using dedicated atom IDs (e.g. even
		// integers for rule bodies, uneven for other atoms)
	}
	
	protected void incrementSignCounter(Integer literal) {
		signCounters.compute(literal, (k, v) -> (v == null ? DEFAULT_SIGN_COUNTER : v) + 1);
	}

	private void decayAllIfTimeHasCome() {
		stepsSinceLastDecay++;
		if (stepsSinceLastDecay >= decayPeriod) {
			// Decay all:
			activityCounters.replaceAll((k, v) -> v * decayFactor);
			stepsSinceLastDecay = 0;
		}
	}

	protected boolean isUnassigned(int atom) {
		ThriceTruth truth = assignment.getTruth(atom);
		return truth != FALSE && truth != TRUE; // do not use assignment.isAssigned(atom) because we may also choose MBTs
	}

	private int getMostActiveAtom(NoGood noGood) {
		if (noGood != null) {
			return getMostActiveAtom(noGood.stream().boxed());
		} else {
			return getMostActiveAtom(activityCounters.keySet().stream());
		}
	}

	private int getMostActiveAtom(Stream<Integer> streamOfLiterals) {
		return streamOfLiterals.map(Literals::atomOf).max(Comparator.comparingDouble(this::getActivity)).orElse(DEFAULT_CHOICE_ATOM);
		// TODO: exploit synergy with getMostActiveChoosableAtom
	}

	/**
	 * Analyzes the type of this NoGood and checks if it is the so-called "body, not head" type. Uses the given {@code isRuleBody} predicate to check whether an
	 * atom represents a rule body.
	 *
	 * @return {@code true} iff: the NoGood is binary, and it has a head, and its tail is an atom representing a rule body.
	 */
	public static boolean isBodyNotHead(NoGood noGood, Predicate<? super Integer> isRuleBody) {
		return noGood.isBinary() && noGood.hasHead() && isRuleBody.test(atomOf(noGood.getLiteral(1)));
	}

	/**
	 * Analyzes the type of this NoGood and checks if it is the so-called "body elements, not body" type. Uses the given {@code isRuleBody} predicate to check
	 * whether an atom represents a rule body.
	 *
	 * @return {@code true} iff: the NoGood contains at least two literals, and the head is a negative literal whose atom represents a rule body.
	 */
	public static boolean isBodyElementsNotBody(NoGood noGood, Predicate<? super Integer> isRuleBody) {
		return noGood.size() > 1 && noGood.hasHead() && isNegated(noGood.getHead()) && isRuleBody.test(atomOf(noGood.getHead()));
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
