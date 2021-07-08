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
package at.ac.tuwien.kr.alpha.solver.heuristics;

import at.ac.tuwien.kr.alpha.solver.BinaryNoGoodPropagationEstimation;
import at.ac.tuwien.kr.alpha.solver.heuristics.BranchingHeuristicFactory.Heuristic;

import java.util.List;

/**
 * Builder for {@link HeuristicsConfiguration} objects
 */
public class HeuristicsConfigurationBuilder {

	private Heuristic heuristic;
	private boolean respectDomspecHeuristics = true;
	private BinaryNoGoodPropagationEstimation.Strategy momsStrategy;
	private List<Integer> replayChoices;

	/**
	 * @param heuristic the heuristic to set
	 */
	public HeuristicsConfigurationBuilder setHeuristic(Heuristic heuristic) {
		this.heuristic = heuristic;
		return this;
	}

	public HeuristicsConfigurationBuilder setRespectDomspecHeuristics(boolean respectDomspecHeuristics) {
		this.respectDomspecHeuristics = respectDomspecHeuristics;
		return this;
	}

	/**
	 * @param momsStrategy the momsStrategy to set
	 */
	public HeuristicsConfigurationBuilder setMomsStrategy(BinaryNoGoodPropagationEstimation.Strategy momsStrategy) {
		this.momsStrategy = momsStrategy;
		return this;
	}

	/**
	 * @param replayChoices the replayChoices to set
	 */
	public HeuristicsConfigurationBuilder setReplayChoices(List<Integer> replayChoices) {
		this.replayChoices = replayChoices;
		return this;
	}

	public HeuristicsConfiguration build() {
		return new HeuristicsConfiguration(heuristic, respectDomspecHeuristics, momsStrategy, replayChoices);
	}
}
