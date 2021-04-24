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
package at.ac.tuwien.kr.alpha.core.solver.heuristics;

import at.ac.tuwien.kr.alpha.api.solver.BinaryNoGoodPropagationEstimationStrategy;
import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.common.NoGoodInterface.Type;
import at.ac.tuwien.kr.alpha.core.solver.BinaryNoGoodPropagationEstimation;
import at.ac.tuwien.kr.alpha.core.solver.ChoiceInfluenceManager;
import at.ac.tuwien.kr.alpha.core.solver.ChoiceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomOf;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Manages a heap of atoms that are assigned an activity, such that the most active atom
 * resides at the top of the heap.
 * In contrast to standard heuristics like VSIDS, activities are not periodically decayed but
 * the increment added when increasing activities is constantly increased itself, which has the
 * same effect.
 *
 */
public class HeapOfActiveAtoms {
	protected static final Logger LOGGER = LoggerFactory.getLogger(HeapOfActiveAtoms.class);

	private static final double NORMALIZATION_THRESHOLD = 1E100;
	private static final double INCREMENT_TO_AVOID_DENORMALS = Double.MIN_VALUE * NORMALIZATION_THRESHOLD;
	private static final double SCORE_EPSILON = 1E-100;

	private boolean[] incrementedActivityScores = new boolean[0];
	protected double[] activityScores = new double[0];
	protected final PriorityQueue<Integer> heap = new PriorityQueue<>(new AtomActivityComparator().reversed());

	protected ChoiceManager choiceManager;
	private int decayPeriod;
	private double decayFactor;
	private int stepsSinceLastDecay;
	private double currentActivityIncrement = 1.0;
	private int numberOfNormalizations;
	
	private final MOMs moms;

	public HeapOfActiveAtoms(int decayPeriod, double decayFactor, ChoiceManager choiceManager) {
		this.decayPeriod = decayPeriod;
		this.decayFactor = decayFactor;
		this.choiceManager = choiceManager;
		this.choiceManager.setChoicePointActivityListener(new ChoicePointActivityListener());
		BinaryNoGoodPropagationEstimation bnpEstimation = choiceManager.getBinaryNoGoodPropagationEstimation();
		this.moms = bnpEstimation == null ? null : new MOMs(bnpEstimation);
	}

	public double getActivity(int literal) {
		return activityScores[atomOf(literal)];
	}

	/**
	 * Gets the number of steps after which activity scores are decayed.
	 */
	public int getDecayPeriod() {
		return decayPeriod;
	}

	/**
	 * @see #getDecayPeriod()
	 */
	public void setDecayPeriod(int decayPeriod) {
		this.decayPeriod = decayPeriod;
	}

	/**
	 * Gets the factor by which the activity increment is multiplied every {@link #getDecayPeriod()} steps.
	 */
	public double getDecayFactor() {
		return decayFactor;
	}

	/**
	 * @see #getDecayFactor()
	 */
	public void setDecayFactor(double decayFactor) {
		this.decayFactor = decayFactor;
	}

	/**
	 * Updates the current activity increment by multiplying it with the decay factor, if time for decay has come.
	 * 
	 * Time for decay has come if the number of conflicts since the last decay has reached the decay period.
	 */
	void decayIfTimeHasCome() {
		stepsSinceLastDecay++;
		if (stepsSinceLastDecay >= decayPeriod) {
			currentActivityIncrement *= decayFactor;
			stepsSinceLastDecay = 0;
		}
	}
	
	/**
	 * Stores newly grounded {@link NoGood}s and updates associated activity counters.
	 */
	public void newNoGoods(Collection<NoGood> newNoGoods) {
		for (NoGood newNoGood : newNoGoods) {
			Type type = newNoGood.getType();
			if (type != Type.LEARNT && type != Type.INTERNAL) {
				analyzeNewNoGood(newNoGood);
				initActivity(newNoGood);
			}
		}
	}
	
	/**
	 * May be implemented in subclasses to add specific analysis of nogoods.
	 */
	protected void analyzeNewNoGood(NoGood newNoGood) {
	}

	/**
	 * Computes and stores initial activity values for the atoms occurring in the given nogood.
	 */
	protected void initActivity(NoGood newNoGood) {
		if (moms != null) {
			initActivityMOMs(newNoGood);
		} else {
			initActivityNaive(newNoGood);
		}
	}

	/**
	 * Uses {@link MOMs} to initialize activity scores, which are then scaled to the interval [0,1].
	 * This is done by computing 1 - 1/log(s+1.01) for original score s.
	 * This guarantees a normalized score between 0 and 1 and retains relative order.
	 * 1.01 is added to avoid computing the logarithm of a number between 0 and 1 (input scores have to be greater or equal to 0!)
	 * @param newNoGood a new nogood, the atoms occurring in which will be initialized
	 */
	private void initActivityMOMs(NoGood newNoGood) {
		LOGGER.debug("Initializing activity scores with MOMs");
		for (int literal : newNoGood) {
			int atom = atomOf(literal);
			if (!incrementedActivityScores[atom]) { // update initial value as long as not incremented yet by VSIDS
				double score = moms.getScore(atom);
				if (score > 0.0) {
					double newActivity = 1 - 1 / (Math.log(score + 1.01));
					if (newActivity - activityScores[atom] > SCORE_EPSILON) {	// avoid computation overhead if score does not increase
						if (numberOfNormalizations > 0) {
							newActivity = normalizeNewActivityScore(newActivity);
						}
						setActivity(atom, newActivity);
					}
				}
			}
		}
	}

	void growToCapacity(int newCapacity) {
		activityScores = Arrays.copyOf(activityScores, newCapacity);
		incrementedActivityScores = Arrays.copyOf(incrementedActivityScores, newCapacity);
	}

	private void initActivityNaive(NoGood newNoGood) {
		LOGGER.debug("Initializing activity scores naively");
		for (Integer literal : newNoGood) {
			int atom = atomOf(literal);
			incrementActivity(atom);
		}
	}

	/**
	 * Returns the atom with the highest activity score and removes it from the heap.
	 */
	public Integer getMostActiveAtom() {
		return heap.poll();
	}

	/**
	 * Increments the activity of the given atom
	 * 
	 * by adding to it the current activity increment times the increment factor.
	 * If the new value exceeds a certain threshold, all activity scores are normalized.
	 */
	public void incrementActivity(int atom) {
		incrementActivity(atom, currentActivityIncrement);
	}
	
	protected void incrementActivity(int atom, double increment) {
		// newActivity := oldActivity + increment
		double newActivity = activityScores[atom] + increment;
		setActivity(atom, newActivity);
		incrementedActivityScores[atom] = true;
	}

	private void setActivity(int atom, double newActivity) {
		activityScores[atom] = newActivity;
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Activity of atom {} set to {}", atom, newActivity);
		}

		if (newActivity > NORMALIZATION_THRESHOLD) {
			normalizeActivityScores();
		}

		heap.add(atom); // ignores the fact that atom may already be in the heap for performance reasons (may be revised in future)
	}

	/**
	 * Makes all activity scores smaller if they get too high.
	 * 
	 * Avoids <a href="https://en.wikipedia.org/wiki/Denormal_number">denormal numbers</a> similarly as done in clasp.
	 */
	private void normalizeActivityScores() {
		LOGGER.debug("Normalizing activity scores");
		numberOfNormalizations++;
		currentActivityIncrement /= NORMALIZATION_THRESHOLD;
		for (int atom = 1; atom < activityScores.length; atom++) {
			activityScores[atom] = (activityScores[atom] + INCREMENT_TO_AVOID_DENORMALS) / NORMALIZATION_THRESHOLD;
		}
	}

	private double normalizeNewActivityScore(double newActivity) {
		for (int i = 0; i < numberOfNormalizations; i++) {
			newActivity = (newActivity + INCREMENT_TO_AVOID_DENORMALS) / NORMALIZATION_THRESHOLD;
		}
		return newActivity;
	}

	private class AtomActivityComparator implements Comparator<Integer> {

		@Override
		public int compare(Integer a1, Integer a2) {
			return Double.compare(activityScores[a1], activityScores[a2]);
		}

	}

	private class ChoicePointActivityListener implements ChoiceInfluenceManager.ActivityListener {

		@Override
		public void callbackOnChanged(int atom, boolean active) {
			if (active && choiceManager.isActiveChoiceAtom(atom)) {
				if (atom < activityScores.length) {
					/* if atom has no activity score, probably the atom is still being buffered
					   by DependencyDrivenVSIDSHeuristic and will get an initial activity
					   when the buffer is ingested */
					heap.add(atom);
				}
			}
		}
	}

	public void setMOMsStrategy(BinaryNoGoodPropagationEstimationStrategy momsStrategy) {
		if (moms != null) {
			moms.setStrategy(momsStrategy);
		}
	}

}
