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
package at.ac.tuwien.kr.alpha.solver;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static at.ac.tuwien.kr.alpha.Util.arrayGrowthSize;
import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.MBT;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.TRUE;

/**
 * Manages influence of atoms on the activity of certain other atoms. Can be used for either choice points or heuristic atoms.
 *
 * Copyright (c) 2020, the Alpha Team.
 */
public class ChoiceInfluenceManager implements Checkable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceInfluenceManager.class);
	
	// Active choice points and all atoms that influence a choice point (enabler, disabler, choice atom itself).
	private final Set<ChoicePoint> activeChoicePoints = new LinkedHashSet<>();
	private final Set<Integer> activeChoicePointsAtoms = new LinkedHashSet<>();
	private ChoicePoint[] influencers = new ChoicePoint[0];
	private ActivityListener activityListener;

	private final WritableAssignment assignment;

	private boolean checksEnabled;

	/**
	 * @param assignment
	 * 
	 */
	public ChoiceInfluenceManager(WritableAssignment assignment) {
		this.assignment = assignment;
	}

	void addInformation(Pair<Map<Integer, Integer>, Map<Integer, Integer>> choiceAtoms) {
		// Assumption: we get all enabler/disabler pairs in one call.
		Map<Integer, Integer> enablers = choiceAtoms.getLeft();
		Map<Integer, Integer> disablers = choiceAtoms.getRight();
		for (Map.Entry<Integer, Integer> atomToEnabler : enablers.entrySet()) {
			addInformation(atomToEnabler, disablers);
		}
	}

	private void addInformation(Map.Entry<Integer, Integer> atomToEnabler, Map<Integer, Integer> disablers) {
		// Construct and record ChoicePoint.
		Integer atom = atomToEnabler.getKey();
		if (atom == null) {
			throw oops("Incomplete choice point description found (no atom)");
		}
		if (influencers[atom] != null) {
			throw oops("Received choice information repeatedly");
		}
		Integer enabler = atomToEnabler.getValue();
		Integer disabler = disablers.get(atom);
		if (enabler == null || disabler == null) {
			throw oops("Incomplete choice point description found (no enabler or disabler)");
		}
		assignment.registerCallbackOnChange(atom);
		assignment.registerCallbackOnChange(enabler);
		assignment.registerCallbackOnChange(disabler);
		ChoicePoint choicePoint = new ChoicePoint(atom, enabler, disabler);
		influencers[atom] = choicePoint;
		influencers[enabler] = choicePoint;
		influencers[disabler] = choicePoint;
		choicePoint.recomputeActive();
	}

	void checkActiveChoicePoints() {
		HashSet<ChoicePoint> actualActiveChoicePoints = new HashSet<>();
		for (int i = 0; i < influencers.length; i++) {
			ChoicePoint choicePoint = influencers[i];
			if (choicePoint == null) {
				continue;
			}
			if (checkActiveChoicePoint(choicePoint)) {
				actualActiveChoicePoints.add(choicePoint);
			}
		}
		if (!actualActiveChoicePoints.equals(activeChoicePoints)) {
			throw oops("ChoiceInfluenceManager internal checker detected wrong activeChoicePoints");
		}
		LOGGER.trace("Checking internal choice manger: all ok.");
	}
	
	private boolean checkActiveChoicePoint(ChoicePoint choicePoint) {
		ThriceTruth enablerTruth = assignment.getTruth(choicePoint.enabler);
		ThriceTruth disablerTruth = assignment.getTruth(choicePoint.disabler);
		boolean isActive = enablerTruth == TRUE
			&& (disablerTruth == null || !disablerTruth.toBoolean());
		ThriceTruth atomTruth = assignment.getTruth(choicePoint.atom);
		boolean isNotChosen = atomTruth == null || atomTruth == MBT;
		return isActive && isNotChosen;
	}

	boolean isActive(int atom) {
		if (checksEnabled) {
			checkActiveChoicePoints();
		}
		ChoicePoint choicePoint = influencers[atom];
		return choicePoint != null && choicePoint.isActive && choicePoint.atom == atom;
	}

	int getNextActiveAtomOrDefault(int defaultAtom) {
		if (checksEnabled) {
			checkActiveChoicePoints();
		}
		return activeChoicePointsAtoms.size() > 0 ? activeChoicePointsAtoms.iterator().next() : defaultAtom;
	}

	boolean isAtomInfluenced(int atom) {
		ChoicePoint choicePoint = influencers[atom];
		return choicePoint != null && choicePoint.atom == atom;
	}

	@Override
	public void setChecksEnabled(boolean checksEnabled) {
		this.checksEnabled = checksEnabled;
	}

	void callbackOnChanged(int atom) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Callback received on influencer atom: {}", atom);
		}
		ChoicePoint choicePoint = influencers[atom];
		if (choicePoint != null) {
			choicePoint.recomputeActive();
		}
	}

	public void growForMaxAtomId(int maxAtomId) {
		// Grow arrays only if needed.
		if (influencers.length > maxAtomId) {
			return;
		}
		// Grow to default size, except if bigger array is required due to maxAtomId.
		int newCapacity = arrayGrowthSize(influencers.length);
		if (newCapacity < maxAtomId + 1) {
			newCapacity = maxAtomId + 1;
		}
		influencers = Arrays.copyOf(influencers, newCapacity);
	}
	
	void setActivityListener(ActivityListener listener) {
		this.activityListener = listener;
	}

	private class ChoicePoint {
		final Integer atom;
		final int enabler;
		final int disabler;
		boolean isActive;

		private ChoicePoint(Integer atom, Integer enabler, int disabler) {
			this.atom = atom;
			this.enabler = enabler;
			this.disabler = disabler;
		}

		private boolean isActiveChoicePoint() {
			ThriceTruth enablerTruth = assignment.getTruth(enabler);
			ThriceTruth disablerTruth = assignment.getTruth(disabler);
			return  enablerTruth == TRUE
				&& (disablerTruth == null || !disablerTruth.toBoolean());
		}

		private boolean isNotChosen() {
			ThriceTruth atomTruth = assignment.getTruth(atom);
			return atomTruth == null || atomTruth == MBT;
		}

		void recomputeActive() {
			LOGGER.trace("Recomputing activity of atom {}.", atom);
			final boolean wasActive = isActive;
			isActive = isNotChosen() && isActiveChoicePoint();
			boolean changed = false;
			if (isActive && !wasActive) {
				activeChoicePoints.add(this);
				activeChoicePointsAtoms.add(atom);
				changed = true;
				LOGGER.debug("Activating choice point for atom {}", this.atom);
			} else if (wasActive && !isActive) {
				activeChoicePoints.remove(this);
				activeChoicePointsAtoms.remove(atom);
				changed = true;
				LOGGER.debug("Deactivating choice point for atom {}", this.atom);
			}
			if (changed
				//&& isActive // FIXME: not sure if this should be in, apparently bugs VSIDS.
				&& activityListener != null) {
				activityListener.callbackOnChange(atom);
			}
		}

		@Override
		public String toString() {
			return String.valueOf(atom);
		}
	}
	
	public interface ActivityListener {
		void callbackOnChange(int atom);
	}

}
