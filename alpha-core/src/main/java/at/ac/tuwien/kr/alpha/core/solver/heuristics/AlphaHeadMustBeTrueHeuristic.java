/**
 * Copyright (c) 2017-2018 Siemens AG
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
import at.ac.tuwien.kr.alpha.core.solver.ThriceTruth;
import at.ac.tuwien.kr.alpha.core.solver.heuristics.activity.BodyActivityProviderFactory.BodyActivityType;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A variant of {@link DependencyDrivenHeuristic} that prefers to choose atoms representing bodies of rules whose heads
 * are assigned {@link at.ac.tuwien.kr.alpha.core.solver.ThriceTruth#MBT}.
 */
public class AlphaHeadMustBeTrueHeuristic extends DependencyDrivenHeuristic {

	private int rememberedAtom;

	public AlphaHeadMustBeTrueHeuristic(Assignment assignment, ChoiceManager choiceManager, int decayPeriod, double decayFactor, Random random) {
		super(assignment, choiceManager, decayPeriod, decayFactor, random, BodyActivityType.DEFAULT);
	}

	public AlphaHeadMustBeTrueHeuristic(Assignment assignment, ChoiceManager choiceManager, Random random) {
		super(assignment, choiceManager, random);
	}

	@Override
	public int chooseLiteral() {
		Set<Integer> heads = headToBodies.keySet();
		Stream<Integer> bodiesOfMbtHeads = heads.stream().filter(a -> assignment.getTruth(a) == ThriceTruth.MBT).flatMap(h -> headToBodies.get(h).stream());
		Optional<Integer> mostActiveBody = bodiesOfMbtHeads.filter(this::isUnassigned).filter(choiceManager::isActiveChoiceAtom)
				.max(Comparator.comparingDouble(bodyActivity::get));
		if (mostActiveBody.isPresent()) {
			rememberedAtom = mostActiveBody.get();
			return rememberedAtom;
		}
		return super.chooseLiteral();
	}

}
