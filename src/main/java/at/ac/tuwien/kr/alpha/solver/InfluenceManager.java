/*
 * Copyright (c) 2017-2020, the Alpha Team.
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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manages influence of atoms on the activity of certain other atoms. Can be used for either choice points or heuristic atoms.
 *
 * Regarding terminology: "influenced atom" refers to any atom that is influenced by influencers.
 * Special types of "influenced atoms" are "choice points" (for rules) and "heuristic choice points" (for heuristics).
 */
public abstract class InfluenceManager implements Checkable {

	protected final WritableAssignment assignment;
	protected final Set<Integer> activeChoicePointsAtoms = new LinkedHashSet<>();

	protected ActivityListener activityListener;
	protected boolean checksEnabled;

	public InfluenceManager(WritableAssignment assignment) {
		this.assignment = assignment;
	}

	abstract boolean isActive(int atom);

	int getNextActiveAtomOrDefault(int defaultAtom) {
		return activeChoicePointsAtoms.size() > 0 ? activeChoicePointsAtoms.iterator().next() : defaultAtom;
	}

	@Override
	public void setChecksEnabled(boolean checksEnabled) {
		this.checksEnabled = checksEnabled;
	}

	abstract void callbackOnChanged(int atom);
	
	void setActivityListener(ActivityListener listener) {
		this.activityListener = listener;
	}
	
	public interface ActivityListener {
		void callbackOnChanged(int atom, boolean active);
	}

}
