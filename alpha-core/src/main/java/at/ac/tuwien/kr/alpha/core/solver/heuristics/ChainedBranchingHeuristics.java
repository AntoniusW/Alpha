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

import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.solver.learning.GroundConflictNoGoodLearner.ConflictAnalysisResult;

import static at.ac.tuwien.kr.alpha.api.Util.oops;

import java.util.*;

/**
 * A "chained" list of branching heuristics in which the entry at position n+1 is used as a fallback if the entry at position n cannot make a decision. 
 */
public class ChainedBranchingHeuristics implements BranchingHeuristic {
	
	private List<BranchingHeuristic> chain = new LinkedList<>();
	private int[] decisionCounters;
	
	private ChainedBranchingHeuristics(BranchingHeuristic... branchingHeuristics) {
		for (BranchingHeuristic element : branchingHeuristics) {
			add(element);
		}
		this.decisionCounters = new int[chain.size()];
	}
	
	@Override
	public void violatedNoGood(NoGood violatedNoGood) {
		for (BranchingHeuristic element : chain) {
			element.violatedNoGood(violatedNoGood);
		}
	}

	@Override
	public void analyzedConflict(ConflictAnalysisResult analysisResult) {
		for (BranchingHeuristic element : chain) {
			element.analyzedConflict(analysisResult);
		}
	}

	@Override
	public void newNoGood(NoGood newNoGood) {
		for (BranchingHeuristic element : chain) {
			element.newNoGood(newNoGood);
		}
	}
	
	@Override
	public int chooseLiteral() {
		for (int i = 0; i < chain.size(); i++) {
			BranchingHeuristic element = chain.get(i);
			int chosenLiteral = element.chooseLiteral();
			if (chosenLiteral != DEFAULT_CHOICE_LITERAL) {
				decisionCounters[i]++;
				return chosenLiteral;
			}
		}
		return DEFAULT_CHOICE_LITERAL;
	}

	@Override
	public void growForMaxAtomId(int maxAtomId) {
		for (BranchingHeuristic element : chain) {
			element.growForMaxAtomId(maxAtomId);
		}
	}

	public void add(BranchingHeuristic element) {
		if (chain.contains(element)) {
			throw oops("Cycle detected in chain of branching heuristics");
		}
		chain.add(element);
		if (decisionCounters != null) {
			decisionCounters = Arrays.copyOf(decisionCounters, decisionCounters.length + 1);
		}
	}

	public BranchingHeuristic getLastElement() {
		return chain.get(chain.size() - 1);
	}
	
	public static ChainedBranchingHeuristics chainOf(BranchingHeuristic... branchingHeuristics) {
		return new ChainedBranchingHeuristics(branchingHeuristics);
	}

	/**
	 * Returns a mapping from individual heuristics to number of decisions made by them.
	 */
	public Map<BranchingHeuristic, Integer> getNumberOfDecisions() {
		Map<BranchingHeuristic, Integer> map = new HashMap<>();
		for (int i = 0; i < chain.size(); i++) {
			map.put(chain.get(i), decisionCounters[i]);
		}
		return map;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + chain;
	}

}
