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
import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.core.solver.heuristics.activity.BodyActivityProviderFactory.BodyActivityType;

import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomOf;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * {@link DependencyDrivenHeuristic} needs to know a lot about the structure of nogoods, i.e. the role their members play in rules.
 * However, the solving component of Alpha usually deals only with nogoods and cannot naturally access this information.
 * Therefore, {@link GeneralizedDependencyDrivenHeuristic} both generalizes and simplifies {@link DependencyDrivenHeuristic}
 * by redefining the set of atoms dependent on a choice point.
 * This set is constructed by adding to it, every time a new nogood containing a choice point is added to the stack,
 * all other atoms in the nogood.
 * To choose an atom, {@link GeneralizedDependencyDrivenHeuristic} then proceeds as {@link DependencyDrivenHeuristic}.
 * Because {@link GeneralizedDependencyDrivenHeuristic} does not know the head belonging to a choice point,
 * it just uses the {@link BerkMin} method to choose a truth value.

 * Copyright (c) 2017 Siemens AG
 *
 */
public class GeneralizedDependencyDrivenHeuristic extends DependencyDrivenHeuristic {

	public GeneralizedDependencyDrivenHeuristic(Assignment assignment, ChoiceManager choiceManager, int decayPeriod, double decayFactor, Random random, BodyActivityType bodyActivityType) {
		super(assignment, choiceManager, decayPeriod, decayFactor, random, bodyActivityType);
	}

	public GeneralizedDependencyDrivenHeuristic(Assignment assignment, ChoiceManager choiceManager, Random random, BodyActivityType bodyActivityType) {
		super(assignment, choiceManager, random, bodyActivityType);
	}

	public GeneralizedDependencyDrivenHeuristic(Assignment assignment, ChoiceManager choiceManager, Random random) {
		super(assignment, choiceManager, random);
	}

	@Override
	protected void recordAtomRelationships(NoGood noGood) {
		// TODO: use HeapOfActiveChoicePoints.recordAtomRelationships, which does similar things
		int body = DEFAULT_CHOICE_ATOM;
		Set<Integer> others = new HashSet<>();
		for (int literal : noGood) {
			int atom = atomOf(literal);
			if (body == DEFAULT_CHOICE_ATOM && choiceManager.isAtomChoice(atom)) {
				body = atom;
			} else {
				others.add(atom);
			}
		}
		for (Integer atom : others) {
			atomsToBodiesAtoms.put(atom, body);
			bodyAtomToLiterals.put(body, atom);
		}
	}

	@Override
	protected int getAtomForChooseSign(int atom) {
		return atom;
	}

}
