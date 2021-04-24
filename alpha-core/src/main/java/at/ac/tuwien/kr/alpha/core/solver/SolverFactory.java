/**
 * Copyright (c) 2016-2017, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.core.solver;

import at.ac.tuwien.kr.alpha.api.Solver;
import at.ac.tuwien.kr.alpha.api.config.SystemConfig;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.grounder.Grounder;
import at.ac.tuwien.kr.alpha.core.solver.heuristics.HeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.core.solver.heuristics.HeuristicsConfigurationBuilder;

import java.util.Random;

public final class SolverFactory {
	public static Solver getInstance(SystemConfig config, AtomStore atomStore, Grounder grounder) {
		final String solverName = config.getSolverName();
		final String nogoodStoreName = config.getNogoodStoreName();
		final Random random = new Random(config.getSeed());
		final boolean debugInternalChecks = config.isDebugInternalChecks();
		final HeuristicsConfiguration heuristicsConfiguration = buildHeuristicsConfiguration(config);
		final WritableAssignment assignment = new TrailAssignment(atomStore, debugInternalChecks);

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
				return new DefaultSolver(atomStore, grounder, store, assignment, random, config, heuristicsConfiguration);
		}
		throw new IllegalArgumentException("Unknown solver requested.");
	}

	private static HeuristicsConfiguration buildHeuristicsConfiguration(SystemConfig config) {
		HeuristicsConfigurationBuilder heuristicsConfigurationBuilder = HeuristicsConfiguration.builder();
		heuristicsConfigurationBuilder.setHeuristic(config.getBranchingHeuristic());
		heuristicsConfigurationBuilder.setMomsStrategy(config.getMomsStrategy());
		heuristicsConfigurationBuilder.setReplayChoices(config.getReplayChoices());
		return heuristicsConfigurationBuilder.build();
	}
}
