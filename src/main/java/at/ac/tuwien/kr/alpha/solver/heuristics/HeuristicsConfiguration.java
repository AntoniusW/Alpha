/**
 * Copyright (c) 2018 Siemens AG
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

import at.ac.tuwien.kr.alpha.solver.heuristics.BranchingHeuristicFactory.Heuristic;
import at.ac.tuwien.kr.alpha.solver.heuristics.MOMs.Strategy;

/**
 * Configuration class holding parameters for {@link BranchingHeuristic}s.
 */
public class HeuristicsConfiguration {
	
	private Heuristic heuristic;
	private MOMs.Strategy momsStrategy;
	
	/**
	 * @param heuristic
	 * @param momsStrategy
	 */
	public HeuristicsConfiguration(Heuristic heuristic, Strategy momsStrategy) {
		super();
		this.heuristic = heuristic;
		this.momsStrategy = momsStrategy;
	}

	/**
	 * @return the heuristic
	 */
	public Heuristic getHeuristic() {
		return heuristic;
	}

	/**
	 * @param heuristic the heuristic to set
	 */
	public void setHeuristic(Heuristic heuristic) {
		this.heuristic = heuristic;
	}

	/**
	 * @return the momsStrategy
	 */
	public MOMs.Strategy getMomsStrategy() {
		return momsStrategy;
	}

	/**
	 * @param momsStrategy the momsStrategy to set
	 */
	public void setMomsStrategy(MOMs.Strategy momsStrategy) {
		this.momsStrategy = momsStrategy;
	}
	
	public static HeuristicsConfigurationBuilder builder() {
		return new HeuristicsConfigurationBuilder();
	}

}
