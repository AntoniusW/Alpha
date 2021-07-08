/**
 * Copyright (c) 2018-2019 Siemens AG
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
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.solver.BinaryNoGoodPropagationEstimation;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import at.ac.tuwien.kr.alpha.solver.heuristics.activity.BodyActivityProvider;
import at.ac.tuwien.kr.alpha.solver.learning.GroundConflictNoGoodLearner.ConflictAnalysisResult;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.Util.arrayGrowthSize;
import static at.ac.tuwien.kr.alpha.common.Literals.*;

/**
 * This implementation is inspired by the VSIDS implementation in <a href="https://github.com/potassco/clasp">clasp</a>.
 * Therefore, for example, decay is not realized by decreasing activity scores with age, but by
 * steadily increasing the increment added to the score of active atoms
 * (such that the activity score of older atoms does not have to be changed later).
 * <p/>
 * The implementation is simplified in some ways, e.g. it is not possible to specify a frequency in which the activity
 * increment is updated (which corresponds to decay), but it is updated after every conflict.
 * <p/>
 * Reference for VSIDS:
 * Moskewicz, Matthew W.; Madigan, Conor F.; Zhao, Ying; Zhang, Lintao; Malik, Sharad (2001):
 * Chaff: engineering an efficient SAT solver.
 * In: Proceedings of the 38th Design Automation Conference. IEEE, pp. 530–535.
 */
public class VSIDS implements ActivityBasedBranchingHeuristic {
	protected static final Logger LOGGER = LoggerFactory.getLogger(VSIDS.class);

	public static final int DEFAULT_DECAY_PERIOD = 1;

	/**
	 * The default factor by which VSID's activity increment will be multiplied when the decay period has expired.
	 * The value is taken from clasp's tweety configuration which clasp uses by default.
	 */
	public static final double DEFAULT_DECAY_FACTOR = 1 / 0.92;

	protected final Assignment assignment;
	protected final ChoiceManager choiceManager;

	protected final HeapOfActiveAtoms heapOfActiveAtoms;
	protected int[] signBalances = new int[0];

	private final Collection<NoGood> bufferedNoGoods = new ArrayList<>();

	private int nChoicesTrue;
	private int nChoicesFalse;
	private int nChoicesRand;

	/**
	 * Maps rule heads to atoms representing corresponding bodies.
	 */
	protected final MultiValuedMap<Integer, Integer> headToBodies = new HashSetValuedHashMap<>();

	protected VSIDS(Assignment assignment, ChoiceManager choiceManager, HeapOfActiveAtoms heapOfActiveAtoms, BinaryNoGoodPropagationEstimation.Strategy momsStrategy) {
		this.assignment = assignment;
		this.choiceManager = choiceManager;
		this.heapOfActiveAtoms = heapOfActiveAtoms;
		this.heapOfActiveAtoms.setMOMsStrategy(momsStrategy);
	}

	public VSIDS(Assignment assignment, ChoiceManager choiceManager, int decayPeriod, double decayFactor, BinaryNoGoodPropagationEstimation.Strategy momsStrategy) {
		this(assignment, choiceManager, new HeapOfActiveAtoms(decayPeriod, decayFactor, choiceManager),  momsStrategy);
	}

	public VSIDS(Assignment assignment, ChoiceManager choiceManager, BinaryNoGoodPropagationEstimation.Strategy momsStrategy) {
		this(assignment, choiceManager, DEFAULT_DECAY_PERIOD, DEFAULT_DECAY_FACTOR,  momsStrategy);
	}

	@Override
	public void violatedNoGood(NoGood violatedNoGood) {
	}

	@Override
	public void analyzedConflict(ConflictAnalysisResult analysisResult) {
		ingestBufferedNoGoods();	//analysisResult may contain new atoms whose activity must be initialized
		for (int resolutionAtom : analysisResult.resolutionAtoms) {
			heapOfActiveAtoms.incrementActivity(resolutionAtom);
		}
		if (analysisResult.learnedNoGood != null) {
			for (int literal : analysisResult.learnedNoGood) {
				incrementSignCounter(literal);
				heapOfActiveAtoms.incrementActivity(atomOf(literal));
			}
		}
		heapOfActiveAtoms.decayIfTimeHasCome();
	}

	@Override
	public void newNoGood(NoGood newNoGood) {
		this.bufferedNoGoods.add(newNoGood);
	}

	@Override
	public void newNoGoods(Collection<NoGood> newNoGoods) {
		this.bufferedNoGoods.addAll(newNoGoods);
	}

	private void ingestBufferedNoGoods() {
		heapOfActiveAtoms.newNoGoods(bufferedNoGoods);
		bufferedNoGoods.clear();
	}

	/**
	 * {@link VSIDS} manages a stack of nogoods in the fashion of {@link BerkMin}
	 * and starts by looking at the most active atom <code>a</code> in the nogood currently at the top of the stack.
	 * If <code>a</code> is an active choice point (i.e. representing the body of an applicable rule), it is immediately chosen;
	 * else the most active choice point dependent on <code>a</code> is.
	 * If there is no such atom, we continue further down the stack.
	 * When choosing between dependent atoms, a {@link BodyActivityProvider} is employed to define the activity of a choice point.
	 */
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
		if (admissibleChoices != null) {
			throw new UnsupportedOperationException("Choosing from a set of admissible choices is not yet implemented by VSIDS.");
			// TODO: idea: build new PriorityQueue, taking comparator from existing HeapOfActiveAtoms, then pick top element (is this the most performant way)? (cf. issue #181)
		}
		ingestBufferedNoGoods();
		Integer mostActiveAtom;
		while ((mostActiveAtom = heapOfActiveAtoms.getMostActiveAtom()) != null) {
			if (choiceManager.isActiveChoiceAtom(mostActiveAtom)) {
				return mostActiveAtom;
			}
		}
		return DEFAULT_CHOICE_ATOM;
	}

	/**
	 * Chooses a sign (truth value) to assign to the given atom.
	 * 
	 * To make this decision, sign counters are maintained that reflect how often an atom
	 * occurs positively or negatively in learnt nogoods.
	 * If the sign balance for the given atom is positive, {@code true} will be chosen.
	 * If it is negative, {@code false} will be chosen.
	 * If the sign balance is zero, the default sign is selected, which is {@code true}
	 * iff the atom represents a rule body (which is currently always the case for atoms chosen in Alpha).
	 * 
	 * @param atom
	 *          the chosen atom
	 * @return the truth value to assign to the given atom
	 */
	protected boolean chooseSign(int atom) {
		atom = getAtomForChooseSign(atom);

		if (assignment.getTruth(atom) == ThriceTruth.MBT) {
			return true;
		}

		int signBalance = getSignBalance(atom);
		if (LOGGER.isDebugEnabled() && (nChoicesFalse + nChoicesTrue + nChoicesRand) % 100 == 0) {
			LOGGER.debug("chooseSign stats: f={}, t={}, r={}", nChoicesFalse, nChoicesTrue, nChoicesRand);
			LOGGER.debug("chooseSign stats: signBalance={}", signBalance);
		}

		if (signBalance >= 0) {
			nChoicesTrue++;
			return true;
		} else {
			nChoicesFalse++;
			return false;
		}
	}

	/**
	 * This method just returns {@code atom} by default but can be overridden in subclasses.
	 * @param atom the atom chosen by VSIDS
	 * @return the atom to base the choice of sign upon
	 */
	protected int getAtomForChooseSign(int atom) {
		return atom;
	}

	protected void incrementSignCounter(int literal) {
		int atom = atomOf(literal);
		boolean sign = isPositive(literal);
		growForMaxAtomId(atom);
		signBalances[atom] += sign ? 1 : -1;
	}

	public void growForMaxAtomId(int maxAtomId) {
		// Grow arrays only if needed.
		if (signBalances.length > maxAtomId) {
			return;
		}
		// Grow to default size, except if bigger array is required due to maxAtomId.
		int newCapacity = arrayGrowthSize(signBalances.length);
		if (newCapacity < maxAtomId + 1) {
			newCapacity = maxAtomId + 1;
		}
		signBalances = Arrays.copyOf(signBalances, newCapacity);
		heapOfActiveAtoms.growToCapacity(newCapacity);
	}

	@Override
	public double getActivity(int literal) {
		return heapOfActiveAtoms.getActivity(literal);
	}

	/**
	 * Returns the sign balance for the given atom in learnt nogoods.
	 * @param atom
	 * @return the number of times the given atom occurrs positively more often than negatively (which may be negative)
	 */
	int getSignBalance(int atom) {
		return signBalances.length > atom ? signBalances[atom] : 0;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
