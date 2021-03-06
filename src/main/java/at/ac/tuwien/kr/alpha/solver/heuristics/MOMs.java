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
import at.ac.tuwien.kr.alpha.solver.BinaryNoGoodPropagationEstimation.Strategy;

/**
 * The well-known MOMs (Maximum Occurrences in clauses of Minimum size) heuristic
 * used to initialize atom scores.
 * This implementation is inspired by the MOMs implementation in <a href="https://github.com/potassco/clasp">clasp</a>
 * but differs from it in several ways, e.g.:
 * <ul>
 * 	<li>The default strategy is {@link Strategy#CountBinaryWatches}, not {@link Strategy#BinaryNoGoodPropagation}.</li>
 * 	<li>{@link Strategy#BinaryNoGoodPropagation} does not do only one iteration of propagation, but exhaustive propagation.</li>
 * </ul>
 *
 */
public class MOMs {
	
	static final Strategy DEFAULT_STRATEGY = Strategy.CountBinaryWatches;
	
	private BinaryNoGoodPropagationEstimation bnpEstimation;
	private Strategy strategy = DEFAULT_STRATEGY;

	public MOMs(BinaryNoGoodPropagationEstimation bnpEstimation) {
		super();
		this.bnpEstimation = bnpEstimation;
	}

	/**
	 * @param atom
	 * @return
	 */
	public double getScore(Integer atom) {
		int s1 = bnpEstimation.estimate(atom, true, strategy);
		int s2 = bnpEstimation.estimate(atom, false, strategy);
		return ((s1 * s2) << 10) + s1 + s2;
	}

	public void setStrategy(Strategy strategy) {
		this.strategy = strategy != null ? strategy : DEFAULT_STRATEGY;
	}

}
