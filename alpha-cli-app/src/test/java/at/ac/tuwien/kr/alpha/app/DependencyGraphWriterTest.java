package at.ac.tuwien.kr.alpha.app;

import java.io.ByteArrayOutputStream;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.api.DebugSolvingContext;
import at.ac.tuwien.kr.alpha.api.impl.AlphaImpl;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;

public class DependencyGraphWriterTest {
	private static final String LS = System.lineSeparator();

	@Test
	public void smokeTest() {
		// Note: rather than testing correct implementation of dot file format,
		// (which would be a lot of work), just test correct dot code generation
		// for one dependency graph that has all possible "special" node constellations,
		// i.e. positive and negative dependencies, cycle through negation, constraints.
		String asp = "p(X) :- q(X), r(X)." + LS +
				"s(X) :- p(X), q(X), not r(X)." + LS +
				"t(X) :- p(X), not u(X)." + LS +
				"u(X) :- p(X), not t(X)." + LS +
				":- p(X), not q(X), not r(X).";
		String expectedGraph = "digraph dependencyGraph" + LS +
				"{" + LS +
				"splines=false;" + LS +
				"ranksep=4.0;" + LS +
				"n0 [label = \"r/1\"]" + LS +
				"n1 [label = \"q/1\"]" + LS +
				"n2 [label = \"t/1\"]" + LS +
				"n3 [label = \"s/1\"]" + LS +
				"n4 [label = \"u/1\"]" + LS +
				"n5 [label = \"[constr_1]/0\"]" + LS +
				"n6 [label = \"p/1\"]" + LS +
				"n0 -> n6 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n0 -> n3 [xlabel=\"-\" labeldistance=0.1]" + LS +
				"n0 -> n5 [xlabel=\"-\" labeldistance=0.1]" + LS +
				"n1 -> n6 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n1 -> n3 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n1 -> n5 [xlabel=\"-\" labeldistance=0.1]" + LS +
				"n2 -> n4 [xlabel=\"-\" labeldistance=0.1]" + LS +
				"n4 -> n2 [xlabel=\"-\" labeldistance=0.1]" + LS +
				"n5 -> n5 [xlabel=\"-\" labeldistance=0.1]" + LS +
				"n6 -> n3 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n6 -> n2 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n6 -> n4 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n6 -> n5 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"}" + LS;
		Alpha alpha = new AlphaImpl();
		DebugSolvingContext dbgResult = alpha.prepareDebugSolve(alpha.readProgramString(asp));
		DependencyGraph depgraph = dbgResult.getDependencyGraph();
		DependencyGraphWriter writer = new DependencyGraphWriter();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writer.writeAsDot(depgraph, out);
		String actualGraph = out.toString();
		Assert.assertEquals(expectedGraph, actualGraph);
	}

}
