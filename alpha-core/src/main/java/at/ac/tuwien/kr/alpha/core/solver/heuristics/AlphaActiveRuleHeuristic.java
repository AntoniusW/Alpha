/**
 * Copyright (c) 2017 Siemens AG
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

import at.ac.tuwien.kr.alpha.core.common.Assignment;
import at.ac.tuwien.kr.alpha.core.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.core.solver.heuristics.activity.BodyActivityProviderFactory.BodyActivityType;

import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomOf;

import java.util.Random;

/**
 * A variant of {@link DependencyDrivenHeuristic} that counts the activity of non-body-representing atoms towards bodies of rules in which they occur.
 */
public class AlphaActiveRuleHeuristic extends DependencyDrivenHeuristic {

	public AlphaActiveRuleHeuristic(Assignment assignment, ChoiceManager choiceManager, int decayPeriod, double decayFactor, Random random) {
		super(assignment, choiceManager, decayPeriod, decayFactor, random, BodyActivityType.DEFAULT);
	}

	public AlphaActiveRuleHeuristic(Assignment assignment, ChoiceManager choiceManager, Random random) {
		super(assignment, choiceManager, random);
	}

	@Override
	protected void incrementActivityCounter(int literal) {
		int atom = atomOf(literal);
		if (choiceManager.isAtomChoice(atom)) {
			activityCounters.compute(atom, (k, v) -> (v == null ? DEFAULT_ACTIVITY : v) + 1);
		} else {
			for (int body : atomsToBodiesAtoms.get(atom)) {
				activityCounters.compute(body, (k, v) -> (v == null ? DEFAULT_ACTIVITY : v) + 1);
			}
		}
	}

}
