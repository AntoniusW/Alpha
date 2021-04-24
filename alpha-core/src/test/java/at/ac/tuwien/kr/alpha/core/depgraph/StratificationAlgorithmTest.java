package at.ac.tuwien.kr.alpha.core.depgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.NormalProgram;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.api.programs.analysis.ComponentGraph;
import at.ac.tuwien.kr.alpha.api.programs.analysis.ComponentGraph.SCComponent;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph.Node;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.programs.AnalyzedProgram;
import at.ac.tuwien.kr.alpha.core.programs.transformation.NormalizeProgramTransformation;

public class StratificationAlgorithmTest {

	private ProgramParser parser = new ProgramParserImpl();
	private NormalizeProgramTransformation normalizeTransform = new NormalizeProgramTransformation(false);	
	
	private boolean predicateIsBeforePredicateInOrder(Predicate predBefore, Predicate predAfter, List<SCComponent> order) {
		boolean foundPredBefore = false;
		for (SCComponent component : order) {
			for (Node node : component.getNodes()) {
				if (node.getPredicate() == predBefore) {
					foundPredBefore = true;
				}
				if (node.getPredicate() == predAfter) {
					// Found second predicate, return true if we already found the first predicate.
					return foundPredBefore;
				}
			}
		}
		return false;
	}

	@Test
	public void stratifyOneRuleTest() {
		ASPCore2Program prog = parser.parse("a :- b.");
		NormalProgram normalProg = normalizeTransform.apply(prog);
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normalProg);
		DependencyGraph dg = analyzed.getDependencyGraph();
		ComponentGraph cg = ComponentGraphImpl.buildComponentGraph(dg, StronglyConnectedComponentsAlgorithm.findStronglyConnectedComponents(dg));
		List<SCComponent> strata = StratificationAlgorithm.calculateStratification(cg);

		Predicate a = Predicates.getPredicate("a", 0);
		Predicate b = Predicates.getPredicate("b", 0);

		assertEquals(2, strata.size());
		assertTrue(predicateIsBeforePredicateInOrder(b, a, strata));
	}

	@Test
	public void stratifyTwoRulesTest() {
		StringBuilder bld = new StringBuilder();
		bld.append("b :- a.").append("\n");
		bld.append("c :- b.").append("\n");
		ASPCore2Program prog = parser.parse(bld.toString());
		NormalProgram normalProg = normalizeTransform.apply(prog);
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normalProg);
		DependencyGraph dg = analyzed.getDependencyGraph();
		ComponentGraph cg = ComponentGraphImpl.buildComponentGraph(dg, StronglyConnectedComponentsAlgorithm.findStronglyConnectedComponents(dg));
		List<SCComponent> strata = StratificationAlgorithm.calculateStratification(cg);

		Predicate a = Predicates.getPredicate("a", 0);
		Predicate b = Predicates.getPredicate("b", 0);
		Predicate c = Predicates.getPredicate("c", 0);

		assertEquals(3, strata.size());
		assertTrue(predicateIsBeforePredicateInOrder(a, b, strata));
		assertTrue(predicateIsBeforePredicateInOrder(b, c, strata));
		assertTrue(predicateIsBeforePredicateInOrder(a, c, strata));
	}

	@Test
	public void stratifyWithNegativeDependencyTest() throws IOException {
		StringBuilder bld = new StringBuilder();
		bld.append("b :- a.").append("\n");
		bld.append("c :- b.").append("\n");
		bld.append("d :- not c.").append("\n");
		bld.append("e :- d.").append("\n");
		ASPCore2Program prog = parser.parse(bld.toString());
		NormalProgram normalProg = normalizeTransform.apply(prog);
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normalProg);
		DependencyGraph dg = analyzed.getDependencyGraph();
		ComponentGraph cg = ComponentGraphImpl.buildComponentGraph(dg, StronglyConnectedComponentsAlgorithm.findStronglyConnectedComponents(dg));
		List<SCComponent> strata = StratificationAlgorithm.calculateStratification(cg);

		Predicate a = Predicates.getPredicate("a", 0);
		Predicate b = Predicates.getPredicate("b", 0);
		Predicate c = Predicates.getPredicate("c", 0);
		Predicate d = Predicates.getPredicate("d", 0);
		Predicate e = Predicates.getPredicate("e", 0);

		assertEquals(5, strata.size());
		assertTrue(predicateIsBeforePredicateInOrder(a, b, strata));
		assertTrue(predicateIsBeforePredicateInOrder(b, c, strata));
		assertTrue(predicateIsBeforePredicateInOrder(c, d, strata));
		assertTrue(predicateIsBeforePredicateInOrder(d, e, strata));
	}

	@Test
	public void stratifyWithPositiveCycleTest() {
		StringBuilder bld = new StringBuilder();
		bld.append("ancestor_of(X, Y) :- parent_of(X, Y).");
		bld.append("ancestor_of(X, Z) :- parent_of(X, Y), ancestor_of(Y, Z).");
		ASPCore2Program prog = parser.parse(bld.toString());
		NormalProgram normalProg = normalizeTransform.apply(prog);
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normalProg);
		DependencyGraph dg = analyzed.getDependencyGraph();
		ComponentGraph cg = ComponentGraphImpl.buildComponentGraph(dg, StronglyConnectedComponentsAlgorithm.findStronglyConnectedComponents(dg));
		List<SCComponent> strata = StratificationAlgorithm.calculateStratification(cg);

		Predicate ancestorOf = Predicates.getPredicate("ancestor_of", 2);
		Predicate parentOf = Predicates.getPredicate("parent_of", 2);

		assertEquals(2, strata.size());
		assertTrue(predicateIsBeforePredicateInOrder(parentOf, ancestorOf, strata));
	}

	@Test
	public void stratifyLargeGraphTest() {
		StringBuilder bld = new StringBuilder();
		bld.append("b :- a.");
		bld.append("c :- b.");
		bld.append("d :- c.");
		bld.append("e :- d.");
		bld.append("f :- not e.");
		bld.append("g :- d, j, not f.");
		bld.append("h :- not c.");
		bld.append("i :- h, not j.");
		bld.append("j :- h, not i.");
		bld.append("k :- g, not l.");
		bld.append("l :- g, not k.");
		bld.append("m :- not k, not l.");
		bld.append("n :- m, not i, not j.");
		bld.append("p :- not m, not n.");

		ASPCore2Program prog = parser.parse(bld.toString());
		NormalProgram normalProg = normalizeTransform.apply(prog);
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normalProg);
		DependencyGraph dg = analyzed.getDependencyGraph();
		ComponentGraph cg = ComponentGraphImpl.buildComponentGraph(dg, StronglyConnectedComponentsAlgorithm.findStronglyConnectedComponents(dg));
		List<SCComponent> strata = StratificationAlgorithm.calculateStratification(cg);

		Predicate a = Predicates.getPredicate("a", 0);
		Predicate b = Predicates.getPredicate("b", 0);
		Predicate c = Predicates.getPredicate("c", 0);
		Predicate d = Predicates.getPredicate("d", 0);
		Predicate e = Predicates.getPredicate("e", 0);
		Predicate f = Predicates.getPredicate("f", 0);
		Predicate h = Predicates.getPredicate("h", 0);

		assertTrue(predicateIsBeforePredicateInOrder(a, h, strata));
		assertTrue(predicateIsBeforePredicateInOrder(b, h, strata));
		assertTrue(predicateIsBeforePredicateInOrder(c, h, strata));

		assertTrue(predicateIsBeforePredicateInOrder(a, f, strata));
		assertTrue(predicateIsBeforePredicateInOrder(b, f, strata));
		assertTrue(predicateIsBeforePredicateInOrder(c, f, strata));
		assertTrue(predicateIsBeforePredicateInOrder(d, f, strata));
		assertTrue(predicateIsBeforePredicateInOrder(e, f, strata));
	}

	@Test
	public void stratifyAvoidDuplicatesTest() {
		StringBuilder bld = new StringBuilder();
		bld.append("b :- a.");
		bld.append("c :- b.");
		bld.append("d :- c.");
		bld.append("e :- d.");
		bld.append("f :- not e.");
		bld.append("g :- d, j, not f.");
		bld.append("h :- not c.");
		bld.append("i :- h, not j.");
		bld.append("j :- h, not i.");
		bld.append("k :- g, not l.");
		bld.append("l :- g, not k.");
		bld.append("m :- not k, not l.");
		bld.append("n :- m, not i, not j.");
		bld.append("p :- not m, not n.");

		ASPCore2Program prog = parser.parse(bld.toString());
		NormalProgram normalProg = normalizeTransform.apply(prog);
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normalProg);
		DependencyGraph dg = analyzed.getDependencyGraph();
		ComponentGraph cg = ComponentGraphImpl.buildComponentGraph(dg, StronglyConnectedComponentsAlgorithm.findStronglyConnectedComponents(dg));
		List<SCComponent> strata = StratificationAlgorithm.calculateStratification(cg);

		Predicate a = Predicates.getPredicate("a", 0);
		Predicate b = Predicates.getPredicate("b", 0);
		Predicate c = Predicates.getPredicate("c", 0);
		Predicate d = Predicates.getPredicate("d", 0);
		Predicate e = Predicates.getPredicate("e", 0);
		Predicate f = Predicates.getPredicate("f", 0);
		Predicate h = Predicates.getPredicate("h", 0);


		assertEquals(7, strata.size());
		assertTrue(predicateIsBeforePredicateInOrder(a, b, strata));
		assertTrue(predicateIsBeforePredicateInOrder(b, c, strata));
		assertTrue(predicateIsBeforePredicateInOrder(c, h, strata));
		assertTrue(predicateIsBeforePredicateInOrder(c, d, strata));
		assertTrue(predicateIsBeforePredicateInOrder(d, e, strata));
		assertTrue(predicateIsBeforePredicateInOrder(e, f, strata));
		assertTrue(predicateIsBeforePredicateInOrder(d, f, strata));
	}

	@Test
	public void avoidDuplicatesTest1() {
		StringBuilder bld = new StringBuilder();
		bld.append("b :- a.");
		bld.append("c :- b.");
		bld.append("c :- a.");

		ASPCore2Program prog = parser.parse(bld.toString());
		NormalProgram normalProg = normalizeTransform.apply(prog);
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normalProg);
		DependencyGraph dg = analyzed.getDependencyGraph();
		ComponentGraph cg = ComponentGraphImpl.buildComponentGraph(dg, StronglyConnectedComponentsAlgorithm.findStronglyConnectedComponents(dg));
		List<SCComponent> strata = StratificationAlgorithm.calculateStratification(cg);

		Predicate a = Predicates.getPredicate("a", 0);
		Predicate b = Predicates.getPredicate("b", 0);
		Predicate c = Predicates.getPredicate("c", 0);

		assertTrue(predicateIsBeforePredicateInOrder(a, b, strata));
		assertTrue(predicateIsBeforePredicateInOrder(b, c, strata));
		assertTrue(predicateIsBeforePredicateInOrder(a, c, strata));

		assertEquals(3, strata.size());
	}

}
