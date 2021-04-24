package at.ac.tuwien.kr.alpha.core.programs;

import java.util.List;

import at.ac.tuwien.kr.alpha.api.programs.analysis.ComponentGraph;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;
import at.ac.tuwien.kr.alpha.core.depgraph.DependencyGraphImpl;
import org.apache.commons.lang3.tuple.ImmutablePair;

import at.ac.tuwien.kr.alpha.api.programs.NormalProgram;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.core.depgraph.ComponentGraphImpl;
import at.ac.tuwien.kr.alpha.core.depgraph.StronglyConnectedComponentsAlgorithm;
import at.ac.tuwien.kr.alpha.core.rules.CompiledRule;


/**
 * An {@link InternalProgram} with dependency information.
 *
 * Copyright (c) 2019-2020, the Alpha Team
 */
public class AnalyzedProgram extends InternalProgram {

	private final DependencyGraph dependencyGraph;
	private final ComponentGraph  componentGraph;

	public AnalyzedProgram(List<CompiledRule> rules, List<Atom> facts) {
		super(rules, facts);
		dependencyGraph = DependencyGraphImpl.buildDependencyGraph(getRulesById());
		componentGraph = buildComponentGraph(dependencyGraph);
	}

	public static AnalyzedProgram analyzeNormalProgram(NormalProgram prog) {
		ImmutablePair<List<CompiledRule>, List<Atom>> rulesAndFacts = InternalProgram.internalizeRulesAndFacts(prog);
		return new AnalyzedProgram(rulesAndFacts.left, rulesAndFacts.right);
	}

	private ComponentGraph buildComponentGraph(DependencyGraph depGraph) {
		StronglyConnectedComponentsAlgorithm.SccResult sccResult = StronglyConnectedComponentsAlgorithm.findStronglyConnectedComponents(depGraph);
		return ComponentGraphImpl.buildComponentGraph(depGraph, sccResult);
	}

	public ComponentGraph getComponentGraph() {
		return componentGraph;
	}

	public DependencyGraph getDependencyGraph() {
		return dependencyGraph;
	}

}
