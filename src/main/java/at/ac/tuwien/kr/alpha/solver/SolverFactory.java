/**
 * Copyright (c) 2016-2018, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.config.SystemConfig;
import at.ac.tuwien.kr.alpha.grounder.Grounder;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfiguration;

import java.util.Random;

public final class SolverFactory {
	public static Solver getInstance(SystemConfig config, AtomStore atomStore, Grounder grounder, HeuristicsConfiguration heuristicsConfiguration) {
		final String solverName = config.getSolverName();
		final String nogoodStoreName = config.getNogoodStoreName();
		final Random random = new Random(config.getSeed());
		final boolean debugInternalChecks = config.isDebugInternalChecks();
		final TrailAssignment assignment = new TrailAssignment(atomStore, debugInternalChecks);
		assignment.setChecksEnabled(debugInternalChecks);

		NoGoodStore store;

		switch (nogoodStoreName.toLowerCase()) {
			case "naive":
				store = new NaiveNoGoodStore(assignment);
				break;
			case "alpharoaming":
				store = new NoGoodStoreAlphaRoaming(assignment, debugInternalChecks);
				break;
			default:
				throw new IllegalArgumentException("Unknown store requested.");
		}

		switch (solverName.toLowerCase()) {
			case "naive" :
				return new NaiveSolver(atomStore, grounder);
			case "default":
				DefaultSolver solver = new DefaultSolver(atomStore, grounder, store, assignment, random, config, heuristicsConfiguration);
				solver.setChecksEnabled(debugInternalChecks);
				return solver;
		}
		throw new IllegalArgumentException("Unknown solver requested.");
	}
}
