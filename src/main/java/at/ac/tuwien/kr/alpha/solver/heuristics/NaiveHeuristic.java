/**
 * Copyright (c) 2016-2018, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.solver.heuristics;

import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.solver.learning.GroundConflictNoGoodLearner.ConflictAnalysisResult;

import java.util.Collection;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.common.Literals.atomToLiteral;

/**
 * The default heuristic that had been used by {@link at.ac.tuwien.kr.alpha.solver.DefaultSolver} before {@link BerkMin} was implemented.
 *
 */
public class NaiveHeuristic implements BranchingHeuristic {

	private final ChoiceManager choiceManager;

	public NaiveHeuristic(ChoiceManager choiceManager) {
		this.choiceManager = choiceManager;
	}

	@Override
	public void violatedNoGood(NoGood violatedNoGood) {
	}

	@Override
	public void analyzedConflict(ConflictAnalysisResult analysisResult) {
	}

	@Override
	public void newNoGood(NoGood newNoGood) {
	}

	@Override
	public void newNoGoods(Collection<NoGood> newNoGoods) {
	}

	@Override
	public int chooseAtom() {
		return atomToLiteral(choiceManager.getNextActiveChoiceAtom());
	}

	@Override
	public int chooseAtom(Set<Integer> admissibleChoices) {
		if (admissibleChoices == null) {
			return chooseLiteral();
		}

		for (int choice : admissibleChoices) {
			if (choiceManager.isActiveChoiceAtom(choice)) {
				return choice;
			}
		}
		return DEFAULT_CHOICE_ATOM;
	}

	@Override
	public int chooseLiteral() {
		return chooseAtom();
	}

	@Override
	public int chooseLiteral(Set<Integer> admissibleChoices) {
		return chooseAtom(admissibleChoices);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
