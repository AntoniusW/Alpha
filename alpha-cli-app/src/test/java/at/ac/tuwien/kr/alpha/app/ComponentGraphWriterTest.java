package at.ac.tuwien.kr.alpha.app;

import java.io.ByteArrayOutputStream;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.api.DebugSolvingContext;
import at.ac.tuwien.kr.alpha.api.impl.AlphaImpl;
import at.ac.tuwien.kr.alpha.api.programs.analysis.ComponentGraph;

public class ComponentGraphWriterTest {
	private static final String LS = System.lineSeparator();

	@Test
	public void smokeTest() {
		// Note: rather than testing correct implementation of dot file format,
		// (which would be a lot of work), just test correct dot code generation
		// for one component graph that has all possible "special" node constellations,
		// i.e. positive and negative dependencies, cycle through negation, constraints.
		String asp = "p(X) :- q(X), r(X)." + LS +
				"s(X) :- p(X), q(X), not r(X)." + LS +
				"t(X) :- p(X), not u(X)." + LS +
				"u(X) :- p(X), not t(X)." + LS +
				":- p(X), not q(X), not r(X).";
		String expectedGraph = "digraph componentGraph" + LS +
				"{" + LS +
				"splines=false;" + LS +
				"ranksep=4.0;" + LS +
				"label = <" + LS +
				"	<table border = '1' cellborder = '0'>" + LS +
				"		<tr><td>Component Id</td><td>0</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td></tr>" + LS +
				"		<tr><td>Predicates</td><td>q/1<br/></td><td>r/1<br/></td><td>p/1<br/></td><td>[constr_1]/0<br/></td><td>t/1<br/>u/1<br/></td><td>s/1<br/></td></tr>" + LS
				+
				"	</table>" + LS +
				">" + LS +
				"" + LS +
				"n0 [label = C0]" + LS +
				"n1 [label = C1]" + LS +
				"n2 [label = C2]" + LS +
				"n0 -> n2 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n1 -> n2 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n3 [label = C3]" + LS +
				"n0 -> n3 [xlabel=\"-\" labeldistance=0.1]" + LS +
				"n1 -> n3 [xlabel=\"-\" labeldistance=0.1]" + LS +
				"n2 -> n3 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n4 [label = C4]" + LS +
				"n2 -> n4 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n5 [label = C5]" + LS +
				"n0 -> n5 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"n1 -> n5 [xlabel=\"-\" labeldistance=0.1]" + LS +
				"n2 -> n5 [xlabel=\"+\" labeldistance=0.1]" + LS +
				"}" + LS;
		Alpha alpha = new AlphaImpl();
		DebugSolvingContext dbgResult = alpha.prepareDebugSolve(alpha.readProgramString(asp));
		ComponentGraph compgraph = dbgResult.getComponentGraph();
		ComponentGraphWriter writer = new ComponentGraphWriter();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writer.writeAsDot(compgraph, out);
		String actualGraph = out.toString();
		System.out.println(actualGraph);
		Assert.assertEquals(expectedGraph, actualGraph);
	}

}
