/*
 * Copyright (c) 2019-2020, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.grounder.atoms.RuleAtom;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Factory returning atom phase initializers, which determine the initial phase given to atoms that were previously
 * unassigned.
 *
 * Copyright (c) 2019, the Alpha Team.
 */
public abstract class PhaseInitializerFactory {

	public enum InitialPhase {
		ALLTRUE,
		ALLFALSE,
		RANDOM,
		RULESTRUEATOMSFALSE;

		public static String listAllowedValues() {
			return Arrays.stream(values()).map(InitialPhase::toString).map(String::toLowerCase).collect(Collectors.joining(", "));
		}
	}

	public abstract static class PhaseInitializer {
		public abstract boolean getNextInitialPhase(int atom);
	}

	public static PhaseInitializer getInstance(InitialPhase initialPhase, Random random, AtomStore atomStore) {
		switch (initialPhase) {
			case ALLTRUE:
				return getPhaseInitializerAllTrue();
			case ALLFALSE:
				return getPhaseInitializerAllFalse();
			case RANDOM:
				return getPhaseInitializerRandom(random);
			case RULESTRUEATOMSFALSE:
				return getPhaseInitializerRulesTrueRestFalse(atomStore);
			default:
				throw new IllegalArgumentException("Unknown phase initializer requested:" + initialPhase);
		}
	}

	private static PhaseInitializer getPhaseInitializerAllTrue() {
		return new PhaseInitializer() {
			@Override
			public boolean getNextInitialPhase(int atom) {
				return true;
			}
		};
	}

	private static PhaseInitializer getPhaseInitializerAllFalse() {
		return new PhaseInitializer() {
			@Override
			public boolean getNextInitialPhase(int atom) {
				return false;
			}
		};
	}

	private static PhaseInitializer getPhaseInitializerRandom(Random random) {
		return new PhaseInitializer() {
			private final Random rand = random;
			@Override
			public boolean getNextInitialPhase(int atom) {
				return rand.nextBoolean();
			}
		};
	}

	private static PhaseInitializer getPhaseInitializerRulesTrueRestFalse(AtomStore atomStore) {
		return new PhaseInitializer() {
			@Override
			public boolean getNextInitialPhase(int atom) {
				return atomStore.get(atom) instanceof RuleAtom;	// Return true for RuleAtoms, otherwise false.
			}
		};
	}
}
