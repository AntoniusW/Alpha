/**
 * Copyright (c) 2019 Siemens AG
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

import at.ac.tuwien.kr.alpha.core.atoms.Literals;
import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.core.solver.learning.GroundConflictNoGoodLearner.ConflictAnalysisResult;

import java.util.Iterator;
import java.util.List;

/**
 * A heuristic that replays a fixed list of choices.
 */
public class ReplayHeuristic implements BranchingHeuristic {

	private final Iterator<Integer> choicesIterator;
	private final ChoiceManager choiceManager;

	/**
	 * Initializes the heuristic
	 * @param choices a list of signed atoms (positive if atom shall be made true, negative otherwise)
	 * @param choiceManager
	 */
	public ReplayHeuristic(List<Integer> choices, ChoiceManager choiceManager) {
		super();
		this.choicesIterator = choices.iterator();
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
	public int chooseLiteral() {
		if (!choicesIterator.hasNext()) {
			return DEFAULT_CHOICE_LITERAL;
		}
		int replayChoiceSignedAtom = choicesIterator.next();
		if (replayChoiceSignedAtom == 0) {
			// Use 0 to signal no more choices.
			return DEFAULT_CHOICE_LITERAL;
		}
		int replayChoiceAtom = Math.abs(replayChoiceSignedAtom);
		int replayChoiceLiteral = Literals.atomToLiteral(replayChoiceAtom, replayChoiceSignedAtom > 0);
		if (!choiceManager.isActiveChoiceAtom(replayChoiceAtom)) {
			throw new IllegalStateException("Replay choice is not an active choice point: " + replayChoiceAtom);
		}
		return replayChoiceLiteral;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
