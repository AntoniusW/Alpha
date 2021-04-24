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

import java.util.Random;

/**
 * One weakness of Alpha that has to be addressed in future work is its lack of support nogoods (except in one special case).
 * This means that the solver cannot recognize when an atom occurring negatively in a rule body cannot be satisfied anymore,
 * thus exploring large portions of the search space in search for a witness.
 * Therefore, Alpha currently benefits in many cases from heuristics that assign true to choice points whenever they can.
 * We apply this modification to {@link GeneralizedDependencyDrivenHeuristic}, which then always assigns true first and tries false
 * only when backtracking, and call these variants <i>pyromaniacal</i> since they prefer the firing of a rule over its non-firing.

 * Copyright (c) 2017 Siemens AG
 *
 */
public class GeneralizedDependencyDrivenPyroHeuristic extends GeneralizedDependencyDrivenHeuristic {

	public GeneralizedDependencyDrivenPyroHeuristic(Assignment assignment, ChoiceManager choiceManager, int decayPeriod, double decayFactor, Random random,
							BodyActivityType bodyActivityType) {
		super(assignment, choiceManager, decayPeriod, decayFactor, random, bodyActivityType);
	}

	public GeneralizedDependencyDrivenPyroHeuristic(Assignment assignment, ChoiceManager choiceManager, Random random, BodyActivityType bodyActivityType) {
		super(assignment, choiceManager, random, bodyActivityType);
	}

	public GeneralizedDependencyDrivenPyroHeuristic(Assignment assignment, ChoiceManager choiceManager, Random random) {
		super(assignment, choiceManager, random);
	}

	@Override
	public boolean chooseSign(int atom) {
		return true;
	}

}
