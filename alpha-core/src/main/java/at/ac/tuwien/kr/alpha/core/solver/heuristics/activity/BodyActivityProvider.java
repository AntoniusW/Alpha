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
package at.ac.tuwien.kr.alpha.core.solver.heuristics.activity;

import org.apache.commons.collections4.MultiValuedMap;

import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomOf;

import java.util.Map;

public abstract class BodyActivityProvider {

	protected final MultiValuedMap<Integer, Integer> bodyToLiterals;
	protected final Map<Integer, Double> activityCounters;
	protected final double defaultActivity;

	public BodyActivityProvider(MultiValuedMap<Integer, Integer> bodyToLiterals, Map<Integer, Double> activityCounters, double defaultActivity) {
		this.bodyToLiterals = bodyToLiterals;
		this.activityCounters = activityCounters;
		this.defaultActivity = defaultActivity;
	}

	public abstract double get(int bodyRepresentingAtom);

	protected double getActivity(int literal) {
		return activityCounters.getOrDefault(atomOf(literal), defaultActivity);
	}

}
