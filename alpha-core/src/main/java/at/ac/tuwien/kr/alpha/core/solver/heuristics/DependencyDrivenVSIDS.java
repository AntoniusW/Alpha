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
import at.ac.tuwien.kr.alpha.core.common.Assignment;
import at.ac.tuwien.kr.alpha.core.solver.ChoiceManager;

import java.util.Random;

/**
 * Non-dependency-driven heuristics like {@link BerkMin}, {@link BerkMinLiteral}, and {@link VSIDS}
 * suffer from the fact that choice points comprise only a small portion of all the literals
 * occurring in nogoods and therefore do not influence activity and sign counters as much as
 * other atoms.
 * <p/>
 * The basic idea of {@link DependencyDrivenVSIDS} is therefore to find <i>dependent</i> atoms that can
 * enrich the information available for a choice point. Intuitively, all atoms occurring in the head or the
 * body of a rule depend on a choice point representing the body of this rule.
 * <p/>
 * In contrast to {@link DependencyDrivenHeuristic} and {@link GeneralizedDependencyDrivenHeuristic},
 * this heuristic is based on ideas from VSIDS instead of BerkMin.
 */
public class DependencyDrivenVSIDS extends VSIDS {

	public DependencyDrivenVSIDS(Assignment assignment, ChoiceManager choiceManager, int decayPeriod, double decayFactor, Random random, BinaryNoGoodPropagationEstimationStrategy momsStrategy) {
		super(assignment, choiceManager, new HeapOfActiveChoicePoints(decayPeriod, decayFactor, choiceManager), momsStrategy);
	}

	public DependencyDrivenVSIDS(Assignment assignment, ChoiceManager choiceManager, Random random, BinaryNoGoodPropagationEstimationStrategy momsStrategy) {
		this(assignment, choiceManager, DEFAULT_DECAY_PERIOD, DEFAULT_DECAY_FACTOR, random, momsStrategy);
	}
	
	/**
	 * Returns the head derived by the rule corresponding to the given choice point,
	 * since the head atom may give more relevant information than the atom representing rule body.
	 */
	@Override
	protected int getAtomForChooseSign(int atom) {
		Integer head = choiceManager.getHeadDerivedByChoiceAtom(atom);
		if (head != null) {
			atom = head;
		}
		return atom;
	}

}
