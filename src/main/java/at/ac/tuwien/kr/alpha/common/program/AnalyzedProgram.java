package at.ac.tuwien.kr.alpha.common.program;

import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.depgraph.ComponentGraph;
import at.ac.tuwien.kr.alpha.common.depgraph.DependencyGraph;
import at.ac.tuwien.kr.alpha.common.depgraph.StronglyConnectedComponentsAlgorithm;
import at.ac.tuwien.kr.alpha.common.rule.InternalRule;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.List;


/**
 * An {@link InternalProgram} with dependency information.
 *
 * Copyright (c) 2019-2020, the Alpha Team
 */
public class AnalyzedProgram extends InternalProgram {

	private final DependencyGraph dependencyGraph;
	private final ComponentGraph componentGraph;

	public AnalyzedProgram(List<InternalRule> rules, List<Atom> facts, Boolean containsWeakConstraints) {
		super(rules, facts, containsWeakConstraints);
		dependencyGraph = DependencyGraph.buildDependencyGraph(getRulesById());
		componentGraph = buildComponentGraph(dependencyGraph);
	}

	public static AnalyzedProgram analyzeNormalProgram(NormalProgram prog) {
		ImmutableTriple<List<InternalRule>, List<Atom>, Boolean> rulesAndFacts = InternalProgram.internalizeRulesAndFacts(prog);
		return new AnalyzedProgram(rulesAndFacts.left, rulesAndFacts.middle, rulesAndFacts.right);
	}

	private ComponentGraph buildComponentGraph(DependencyGraph depGraph) {
		StronglyConnectedComponentsAlgorithm.SccResult sccResult = StronglyConnectedComponentsAlgorithm.findStronglyConnectedComponents(depGraph);
		return ComponentGraph.buildComponentGraph(depGraph, sccResult);
	}

	public ComponentGraph getComponentGraph() {
		return componentGraph;
	}

	public DependencyGraph getDependencyGraph() {
		return dependencyGraph;
	}

}
