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
package at.ac.tuwien.kr.alpha.config;

import at.ac.tuwien.kr.alpha.grounder.heuristics.GrounderHeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.solver.BinaryNoGoodPropagationEstimation;
import at.ac.tuwien.kr.alpha.solver.heuristics.BranchingHeuristicFactory.Heuristic;
import at.ac.tuwien.kr.alpha.solver.heuristics.PhaseInitializerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SystemConfig {

	// Note: Defining constants for default values here rather than just
	// initializing from those values in order to have the values accessible in
	// contexts where no AlphaConfig instance exists (e.g. argument parsing from
	// command line)
	public static final String DEFAULT_GROUNDER_NAME = "naive";
	public static final String DEFAULT_SOLVER_NAME = "default";
	public static final String DEFAULT_NOGOOD_STORE_NAME = "alphaRoaming";
	public static final Heuristic DEFAULT_BRANCHING_HEURISTIC = Heuristic.VSIDS;
	public static final BinaryNoGoodPropagationEstimation.Strategy DEFAULT_MOMS_STRATEGY = BinaryNoGoodPropagationEstimation.Strategy.CountBinaryWatches;
	public static final long DEFAULT_SEED = System.nanoTime();
	public static final boolean DEFAULT_DETERMINISTIC = false;
	public static final boolean DEFAULT_PRINT_STATS = false;
	public static final boolean DEFAULT_QUIET = false;
	public static final boolean DEFAULT_DISABLE_JUSTIFICATION_SEARCH = false;
	public static final boolean DEFAULT_DEBUG_INTERNAL_CHECKS = false;
	public static final boolean DEFAULT_USE_NORMALIZATION_GRID = false;
	public static final boolean DEFAULT_SORT_ANSWER_SETS = false;
	public static final List<Integer> DEFAULT_REPLAY_CHOICES = Collections.emptyList();
	public static final boolean DEFAULT_DISABLE_NOGOOD_DELETION = false;
	public static final String DEFAULT_GROUNDER_TOLERANCE_CONSTRAINTS = GrounderHeuristicsConfiguration.STRICT_STRING;
	public static final String DEFAULT_GROUNDER_TOLERANCE_RULES = GrounderHeuristicsConfiguration.STRICT_STRING;
	public static final boolean DEFAULT_GROUNDER_ACCUMULATOR_ENABLED = false;
	public static final boolean DEFAULT_ENABLE_RESTARTS = false;
	public static final PhaseInitializerFactory.InitialPhase DEFAULT_PHASE_INITIALIZER = PhaseInitializerFactory.InitialPhase.RULESTRUEATOMSFALSE;

	private String grounderName = SystemConfig.DEFAULT_GROUNDER_NAME;
	private String solverName = SystemConfig.DEFAULT_SOLVER_NAME;
	private String nogoodStoreName = SystemConfig.DEFAULT_NOGOOD_STORE_NAME;
	private boolean deterministic = SystemConfig.DEFAULT_DETERMINISTIC;
	private long seed = SystemConfig.DEFAULT_SEED;
	private boolean debugInternalChecks = SystemConfig.DEFAULT_DEBUG_INTERNAL_CHECKS;
	private Heuristic branchingHeuristic = SystemConfig.DEFAULT_BRANCHING_HEURISTIC;
	private BinaryNoGoodPropagationEstimation.Strategy momsStrategy = SystemConfig.DEFAULT_MOMS_STRATEGY;
	private boolean quiet = SystemConfig.DEFAULT_QUIET;
	private boolean printStats = SystemConfig.DEFAULT_PRINT_STATS;
	private boolean disableJustificationSearch = SystemConfig.DEFAULT_DISABLE_JUSTIFICATION_SEARCH;
	private boolean useNormalizationGrid = SystemConfig.DEFAULT_USE_NORMALIZATION_GRID;
	private boolean sortAnswerSets = SystemConfig.DEFAULT_SORT_ANSWER_SETS;
	private List<Integer> replayChoices = SystemConfig.DEFAULT_REPLAY_CHOICES;
	private boolean disableNoGoodDeletion = SystemConfig.DEFAULT_DISABLE_NOGOOD_DELETION;
	private String grounderToleranceConstraints = DEFAULT_GROUNDER_TOLERANCE_CONSTRAINTS;
	private String grounderToleranceRules = DEFAULT_GROUNDER_TOLERANCE_RULES;
	private boolean grounderAccumulatorEnabled = DEFAULT_GROUNDER_ACCUMULATOR_ENABLED;
	private boolean areRestartsEnabled = SystemConfig.DEFAULT_ENABLE_RESTARTS;
	private PhaseInitializerFactory.InitialPhase phaseInitializer = SystemConfig.DEFAULT_PHASE_INITIALIZER;

	public String getGrounderName() {
		return this.grounderName;
	}

	public void setGrounderName(String grounderName) {
		this.grounderName = grounderName;
	}

	public String getSolverName() {
		return this.solverName;
	}

	public void setSolverName(String solverName) {
		this.solverName = solverName;
	}

	public String getNogoodStoreName() {
		return this.nogoodStoreName;
	}

	public void setNogoodStoreName(String nogoodStoreName) {
		this.nogoodStoreName = nogoodStoreName;
	}

	public boolean isDeterministic() {
		return this.deterministic;
	}

	public void setDeterministic(boolean deterministic) {
		this.deterministic = deterministic;
	}

	public long getSeed() {
		return this.seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public boolean isDebugInternalChecks() {
		return this.debugInternalChecks;
	}

	public void setDebugInternalChecks(boolean debugInternalChecks) {
		this.debugInternalChecks = debugInternalChecks;
	}

	public Heuristic getBranchingHeuristic() {
		return this.branchingHeuristic;
	}

	public void setBranchingHeuristic(Heuristic branchingHeuristic) {
		this.branchingHeuristic = branchingHeuristic;
	}

	public void setBranchingHeuristicName(String branchingHeuristicName) {
		this.branchingHeuristic = Heuristic.valueOf(branchingHeuristicName.replace("-", "_").toUpperCase());
	}

	public BinaryNoGoodPropagationEstimation.Strategy getMomsStrategy() {
		return momsStrategy;
	}

	public void setMomsStrategy(BinaryNoGoodPropagationEstimation.Strategy momsStrategy) {
		this.momsStrategy = momsStrategy;
	}
	
	public void setMomsStrategyName(String momsStrategyName) {
		this.momsStrategy = BinaryNoGoodPropagationEstimation.Strategy.valueOf(momsStrategyName);
	}

	public boolean isQuiet() {
		return this.quiet;
	}

	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	public boolean isPrintStats() {
		return this.printStats;
	}

	public void setPrintStats(boolean printStats) {
		this.printStats = printStats;
	}

	public boolean isDisableJustificationSearch() {
		return this.disableJustificationSearch;
	}

	public void setDisableJustificationSearch(boolean disableJustificationSearch) {
		this.disableJustificationSearch = disableJustificationSearch;
	}

	public boolean isUseNormalizationGrid() {
		return this.useNormalizationGrid;
	}

	public void setUseNormalizationGrid(boolean useNormalizationGrid) {
		this.useNormalizationGrid = useNormalizationGrid;
	}

	public boolean isSortAnswerSets() {
		return this.sortAnswerSets;
	}

	public void setSortAnswerSets(boolean sortAnswerSets) {
		this.sortAnswerSets = sortAnswerSets;
	}

	public List<Integer> getReplayChoices() {
		return replayChoices;
	}

	public void setReplayChoices(List<Integer> replayChoices) {
		this.replayChoices = replayChoices;
	}

	public void setReplayChoices(String replayChoices) {
		this.replayChoices = Arrays.stream(replayChoices.split(",")).map(String::trim).map(Integer::valueOf).collect(Collectors.toList());
	}

	public boolean isDisableNoGoodDeletion() {
		return this.disableNoGoodDeletion;
	}

	public void setDisableNoGoodDeletion(boolean disableNoGoodDeletion) {
		this.disableNoGoodDeletion = disableNoGoodDeletion;
	}

	public String getGrounderToleranceConstraints() {
		return grounderToleranceConstraints;
	}

	public void setGrounderToleranceConstraints(String grounderToleranceConstraints) {
		this.grounderToleranceConstraints = grounderToleranceConstraints;
	}

	public String getGrounderToleranceRules() {
		return grounderToleranceRules;
	}

	public void setGrounderToleranceRules(String grounderToleranceRules) {
		this.grounderToleranceRules = grounderToleranceRules;
	}

	public boolean isGrounderAccumulatorEnabled() {
		return grounderAccumulatorEnabled;
	}

	public void setGrounderAccumulatorEnabled(boolean grounderAccumulatorEnabled) {
		this.grounderAccumulatorEnabled = grounderAccumulatorEnabled;
	}
	public boolean areRestartsEnabled() {
		return areRestartsEnabled;
	}

	public void setRestartsEnabled(boolean areRestartsEnabled) {
		this.areRestartsEnabled = areRestartsEnabled;
	}

	public PhaseInitializerFactory.InitialPhase getPhaseInitializer() {
		return phaseInitializer;
	}

	public void setPhaseInitializer(PhaseInitializerFactory.InitialPhase phaseInitializer) {
		this.phaseInitializer = phaseInitializer;
	}

	public void setPhaseInitializerName(String phaseInitializerName) {
		this.phaseInitializer = PhaseInitializerFactory.InitialPhase.valueOf(phaseInitializerName.toUpperCase());
	}

}
