/*
 * Copyright (c) 2018-2021 Siemens AG
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
import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.Literals;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.solver.heuristics.domspec.DomainSpecificHeuristicsStore;
import at.ac.tuwien.kr.alpha.solver.learning.GroundConflictNoGoodLearner.ConflictAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;

/**
 * Analyses information obtained from {@link HeuristicDirective}s to follow domain-specific heuristics specified within the input program.
 */
public class DomainSpecific implements BranchingHeuristic {
	private static final Logger LOGGER = LoggerFactory.getLogger(DomainSpecific.class);

	private final Assignment assignment;
	private final ChoiceManager choiceManager;
	private final BranchingHeuristic fallbackHeuristic;

	DomainSpecific(Assignment assignment, ChoiceManager choiceManager, BranchingHeuristic fallbackHeuristic) {
		this.assignment = assignment;
		this.choiceManager = choiceManager;
		this.fallbackHeuristic = fallbackHeuristic;
	}

	@Override
	public void violatedNoGood(NoGood violatedNoGood) {
		fallbackHeuristic.violatedNoGood(violatedNoGood);
	}

	@Override
	public void analyzedConflict(ConflictAnalysisResult analysisResult) {
		fallbackHeuristic.analyzedConflict(analysisResult);
	}

	@Override
	public void newNoGood(NoGood newNoGood) {
		fallbackHeuristic.newNoGood(newNoGood);
	}

	@Override
	public void newNoGoods(Collection<NoGood> newNoGoods) {
		fallbackHeuristic.newNoGoods(newNoGoods);
	}

	@Override
	public int chooseAtom(Set<Integer> admissibleChoices) {
		return atomOf(chooseLiteral(admissibleChoices));
	}

	@Override
	public int chooseLiteral(Set<Integer> admissibleChoices) {
		DomainSpecificHeuristicsStore heuristicsStore = choiceManager.getDomainSpecificHeuristics();
		if (admissibleChoices != null) {
			throw new UnsupportedOperationException("DomainSpecific does not support restricting guesses to admissible choices.");
		}

		HeuristicDirectiveValues currentValues;
		int chosenLiteral = DEFAULT_CHOICE_LITERAL;
		int discardedBecauseNoBody = 0;
		while ((currentValues = heuristicsStore.poll()) != null) {
			if (assignment.isUnassignedOrMBT(currentValues.getHeadAtomId())) {
				Optional<Integer> body = chooseLiteralForValues(currentValues);
				if (body.isPresent()) {
					chosenLiteral = body.get();
					LOGGER.debug(String.format("Weight of chosen heuristic: %d@%d", currentValues.getWeight(), currentValues.getLevel()));
					break;
				} else {
					LOGGER.warn("Ground heuristic directive with head {} not applicable because no active rule can derive it.", currentValues.getGroundHeadAtom());
					discardedBecauseNoBody++;
				}
			} else {
				throw oops("Heuristic chosen whose head is already assigned");
			}
		}
		LOGGER.debug("{} HeuristicDirectiveValues discarded because no active body found", discardedBecauseNoBody);

		return chosenLiteral;
	}

	private Optional<Integer> chooseLiteralForValues(HeuristicDirectiveValues values) {
		Set<Integer> activeChoiceAtomsDerivingHead = choiceManager.getActiveChoiceAtomsDerivingHead(values.getHeadAtomId());
		int atom;
		if (activeChoiceAtomsDerivingHead.isEmpty()) {
			return Optional.empty();
		} else if (activeChoiceAtomsDerivingHead.size() == 1) {
			atom = activeChoiceAtomsDerivingHead.iterator().next();
		} else {
			throw new UnsupportedOperationException("Atom " + values.getGroundHeadAtom() + " chosen by domain-specific heuristic is the head of more than one applicable rule.");
		}
		return Optional.of(Literals.atomToLiteral(atom, values.getSign()));
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
