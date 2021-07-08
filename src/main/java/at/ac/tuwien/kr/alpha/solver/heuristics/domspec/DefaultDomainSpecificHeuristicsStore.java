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
package at.ac.tuwien.kr.alpha.solver.heuristics.domspec;

import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues.PriorityComparator;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.solver.InfluenceManager;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Stores a mapping between heuristic IDs and their corresponding domain-specific heuristic values.
 */
public class DefaultDomainSpecificHeuristicsStore implements DomainSpecificHeuristicsStore {

	private final Map<Integer, HeuristicDirectiveValues> mapHeuristicToHeuristicValue = new HashMap<>();
	private final PriorityQueue<Integer> prioritisedHeuristics = new PriorityQueue<>(new HeuristicPriorityComparator().reversed());

	@Override
	public void addInfo(int heuristicId, HeuristicDirectiveValues values) {
		mapHeuristicToHeuristicValue.put(heuristicId, values);
	}

	@Override
	public HeuristicDirectiveValues poll() {
		Integer heuristicId = prioritisedHeuristics.poll();
		return heuristicId == null ? null : mapHeuristicToHeuristicValue.get(heuristicId);
	}

	@Override
	public void setChoiceManager(ChoiceManager choiceManager) {
		if (choiceManager != null) {
			choiceManager.setHeuristicActivityListener(new HeuristicActivityListener());
		}
	}

	private class HeuristicActivityListener implements InfluenceManager.ActivityListener {

		@Override
		public void callbackOnChanged(int atom, boolean active) {
			if (active) {
				prioritisedHeuristics.add(atom);
			} else {
				prioritisedHeuristics.remove(atom);
			}
		}
	}
	
	private class HeuristicPriorityComparator implements Comparator<Integer> {
		
		private final PriorityComparator priorityComparator = new PriorityComparator();

		@Override
		public int compare(Integer heu1, Integer heu2) {
			return priorityComparator.compare(mapHeuristicToHeuristicValue.get(heu1), mapHeuristicToHeuristicValue.get(heu2));
		}

	}

}
