/**
 * Copyright (c) 2017-2019, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.common.Assignment;
import at.ac.tuwien.kr.alpha.common.Literals;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;

/**
 * A BerkMin-like heuristics that uses activity of literals and a fixed-size queue instead of a stack of NoGoods.
 * Copyright (c) 2017-2018, the Alpha Team.
 */
public class BerkMinLiteral extends BerkMin {

	private Deque<Integer> activeLiterals = new LinkedList<>();
	private static final int DEFAULT_QUEUE_SIZE = 32;
	private final int queueSize;

	BerkMinLiteral(Assignment assignment, ChoiceManager choiceManager, int decayPeriod, double decayFactor, Random random, int queueSize) {
		super(assignment, choiceManager, decayPeriod, decayFactor, random);
		this.queueSize = queueSize;
	}

	BerkMinLiteral(Assignment assignment, ChoiceManager choiceManager, Random random) {
		this(assignment, choiceManager, DEFAULT_DECAY_PERIOD, DEFAULT_DECAY_FACTOR, random, DEFAULT_QUEUE_SIZE);
	}

	@Override
	public int chooseAtom(Set<Integer> admissibleChoices) {
		if (admissibleChoices == null) {
			return getMostActiveChoosableAtom(activeLiterals.stream().map(Literals::atomOf));
		} else {
			return getMostActiveChoosableAtom(activeLiterals.stream().map(Literals::atomOf).filter(admissibleChoices::contains));
		}
	}

	private void pushToStack(Integer literal) {
		if (choiceManager.isAtomChoice(atomOf(literal))) {
			activeLiterals.addFirst(literal);
			// Restrict the size of the queue.
			if (activeLiterals.size() > queueSize) {
				activeLiterals.removeLast();
			}
		}
	}

	@Override
	protected void pushToStack(NoGood noGood) {
		if (noGood != null) {
			for (Integer literal : noGood) {
				pushToStack(literal);
			}
		}
	}
}
