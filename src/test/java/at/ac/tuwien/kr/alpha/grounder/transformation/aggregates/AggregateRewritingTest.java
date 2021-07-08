package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class AggregateRewritingTest {

	//@formatter:off
	// Smoke-test case for "X <= #count{...}" aggregate
	private static final String CNT_LE1_ASP = 
			"thing(75..76)."
			+ "candidate(2..4)."
			+ "cnt_le(N) :- N <= #count{X : thing(X)}, candidate(N).";
	// Smoke-test case for "X = #count{...}" aggregate
	private static final String CNT_EQ1_ASP =
			"thing(4..6)."
			+ "cnt_things(N) :- N = #count{X : thing(X)}.";
	// Smoke-test case for non-binding min aggregate
	private static final String MIN_GT1_ASP =
			"thing(4). thing(7). thing(13). thing(3). "
			+ "acceptable(8). acceptable(10). acceptable(5). "
			+ "greater_min_acceptable(T) :- thing(T), T > #min{ A : acceptable(A) }.";
	// Smoke-test case for "X = #sum{...}" aggregate
	private static final String SUM_EQ1_ASP =
			"thing(2). thing(4). thing(6)."
			+ "sum_things(S) :- S = #sum{K : thing(K)}.";
	// Smoke-test case for "X = #sum{...}" aggregate
	private static final String SUM_LE1_ASP =
			"thing(2). thing(4). thing(6). bound(11)."
			+ "bound_le_sum(B) :- B <= #sum{K : thing(K)}, bound(B).";

	// Basic ASP representation of a triangular undirected graph, used across multiple test cases
	private static final String TEST_GRAPH_ASP = 
			"graph(g1)."
			+ "graph_undirected(g1)."
			+ "graph_vertex(g1, 1)."
			+ "graph_vertex(g1, 2)."
			+ "graph_vertex(g1 ,3)."
			+ "graph_edge(g1, e(1, 2))."
			+ "graph_edge(g1, e(2, 3))."
			+ "graph_edge(g1, e(3, 1))."
			+ "graph_directed(G) :- graph(G), not graph_undirected(G)."
			+ "err_directedness(G) :- graph(G), graph_directed(G), graph_undirected(G)."
			+ "err_directedness(G) :- graph(G), not graph_directed(G), not graph_undirected(G)."
			+ ":- err_directedness(_)."
			+ "err_undirected_edges(G, V1, V2) :- graph_edge(G, e(V1, V2)), graph_edge(G, e(V2, V1))."
			+ ":- err_undirected_edges(_, _, _).";
	private static final String VERTEX_DEGREE_ASP = TEST_GRAPH_ASP
			+ "graph_vertex_degree(G, V, D) :-"
			+ "    graph(G),"
			+ "    graph_vertex(G, V),"
			+ "    D = #count{ VN : graph_edge(G, e(V, VN)); VN : graph_edge(G, e(VN, V)) }.";
	private static final String NUM_MAX_DEGREE_VERTICES_ASP = VERTEX_DEGREE_ASP
			+ "graph_max_degree_vertices(G, DMAX, N) :-"
			+ "	   graph(G),"
			+ "	   DMAX = #max{ DV : graph_vertex_degree(G, V, DV)},"
			+ "    N = #count{ V : graph_vertex_degree(G, V, DMAX)}.";
	private static final String COMPLEX_EQUALITY_WITH_GLOBALS =
			"p(1..10)."
			+ "q :- X = #count { Y : p( Y ) }, X = #count { Z : p( Z ) },"
			+ "	Y = #count { X : p( X ) }, 1 <= #count { X : p( X ) }, Z = #max { W : p( W ) }.";
	//@formatter:on

	@Test
	public void countLeSortingGridSimple() {
		Alpha alpha = new Alpha();
		alpha.getConfig().setEvaluateStratifiedPart(false);
		InputProgram input = alpha.readProgramString(CNT_LE1_ASP);
		NormalProgram normalized = alpha.normalizeProgram(input);
		// System.out.println(normalized);
		List<AnswerSet> answerSets = alpha.solve(normalized, (p) -> true).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.get(0);
		Predicate thing = Predicate.getInstance("thing", 1);
		Predicate candidate = Predicate.getInstance("candidate", 1);
		Predicate cntLe = Predicate.getInstance("cnt_le", 1);

		// System.out.println(new SimpleAnswerSetFormatter("\n").format(answerSet));

		Assert.assertTrue(answerSet.getPredicateInstances(thing).contains(new BasicAtom(thing, ConstantTerm.getInstance(75))));
		Assert.assertTrue(answerSet.getPredicateInstances(thing).contains(new BasicAtom(thing, ConstantTerm.getInstance(76))));

		Assert.assertTrue(answerSet.getPredicateInstances(candidate).contains(new BasicAtom(candidate, ConstantTerm.getInstance(2))));
		Assert.assertTrue(answerSet.getPredicateInstances(candidate).contains(new BasicAtom(candidate, ConstantTerm.getInstance(3))));
		Assert.assertTrue(answerSet.getPredicateInstances(candidate).contains(new BasicAtom(candidate, ConstantTerm.getInstance(4))));

		Assert.assertTrue(answerSet.getPredicates().contains(cntLe));
		Assert.assertTrue(answerSet.getPredicateInstances(cntLe).contains(new BasicAtom(cntLe, ConstantTerm.getInstance(2))));
	}

	@Test
	public void countEqSimple() {
		Alpha alpha = new Alpha();
		alpha.getConfig().setEvaluateStratifiedPart(false);
		InputProgram input = alpha.readProgramString(CNT_EQ1_ASP);
		NormalProgram normalized = alpha.normalizeProgram(input);
		// System.out.println(normalized);
		List<AnswerSet> answerSets = alpha.solve(normalized, (p) -> true).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.get(0);
		Predicate thing = Predicate.getInstance("thing", 1);
		Predicate cntThings = Predicate.getInstance("cnt_things", 1);

		// System.out.println(new SimpleAnswerSetFormatter("\n").format(answerSet));

		Assert.assertTrue(answerSet.getPredicateInstances(thing).contains(new BasicAtom(thing, ConstantTerm.getInstance(4))));
		Assert.assertTrue(answerSet.getPredicateInstances(thing).contains(new BasicAtom(thing, ConstantTerm.getInstance(5))));
		Assert.assertTrue(answerSet.getPredicateInstances(thing).contains(new BasicAtom(thing, ConstantTerm.getInstance(6))));

		Assert.assertTrue(answerSet.getPredicates().contains(cntThings));
		Assert.assertTrue(answerSet.getPredicateInstances(cntThings).contains(new BasicAtom(cntThings, ConstantTerm.getInstance(3))));
	}

	@Test
	public void countLeCountingGridSimple() {
		Alpha alpha = new Alpha();
		alpha.getConfig().setEvaluateStratifiedPart(false);
		alpha.getConfig().setUseNormalizationGrid(true);
		InputProgram input = alpha.readProgramString(CNT_LE1_ASP);
		NormalProgram normalized = alpha.normalizeProgram(input);
		// System.out.println(normalized);
		List<AnswerSet> answerSets = alpha.solve(normalized, (p) -> true).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.get(0);
		Predicate thing = Predicate.getInstance("thing", 1);
		Predicate candidate = Predicate.getInstance("candidate", 1);
		Predicate cntLe = Predicate.getInstance("cnt_le", 1);

		// System.out.println(new SimpleAnswerSetFormatter("\n").format(answerSet));

		Assert.assertTrue(answerSet.getPredicateInstances(thing).contains(new BasicAtom(thing, ConstantTerm.getInstance(75))));
		Assert.assertTrue(answerSet.getPredicateInstances(thing).contains(new BasicAtom(thing, ConstantTerm.getInstance(76))));

		Assert.assertTrue(answerSet.getPredicateInstances(candidate).contains(new BasicAtom(candidate, ConstantTerm.getInstance(2))));
		Assert.assertTrue(answerSet.getPredicateInstances(candidate).contains(new BasicAtom(candidate, ConstantTerm.getInstance(3))));
		Assert.assertTrue(answerSet.getPredicateInstances(candidate).contains(new BasicAtom(candidate, ConstantTerm.getInstance(4))));

		Assert.assertTrue(answerSet.getPredicates().contains(cntLe));
		Assert.assertTrue(answerSet.getPredicateInstances(cntLe).contains(new BasicAtom(cntLe, ConstantTerm.getInstance(2))));
	}

	@Test
	public void countEqGlobalVars() {
		Alpha alpha = new Alpha();
		alpha.getConfig().setEvaluateStratifiedPart(false);
		InputProgram input = alpha.readProgramString(VERTEX_DEGREE_ASP);
		NormalProgram normalized = alpha.normalizeProgram(input);
		// System.out.println(normalized);
		List<AnswerSet> answerSets = alpha.solve(normalized, (p) -> true).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.get(0);
		Predicate vertexDegree = Predicate.getInstance("graph_vertex_degree", 3);

		// System.out.println(new SimpleAnswerSetFormatter("\n").format(answerSet));

		Assert.assertTrue(answerSet.getPredicates().contains(vertexDegree));
		Assert.assertTrue(answerSet.getPredicateInstances(vertexDegree)
				.contains(new BasicAtom(vertexDegree, ConstantTerm.getSymbolicInstance("g1"), ConstantTerm.getInstance(1), ConstantTerm.getInstance(2))));
		Assert.assertTrue(answerSet.getPredicateInstances(vertexDegree)
				.contains(new BasicAtom(vertexDegree, ConstantTerm.getSymbolicInstance("g1"), ConstantTerm.getInstance(2), ConstantTerm.getInstance(2))));
		Assert.assertTrue(answerSet.getPredicateInstances(vertexDegree)
				.contains(new BasicAtom(vertexDegree, ConstantTerm.getSymbolicInstance("g1"), ConstantTerm.getInstance(3), ConstantTerm.getInstance(2))));
	}

	@Test
	// Test "count eq" and "max eq" together with global vars
	public void graphVerticesOfMaxDegree() {
		Alpha alpha = new Alpha();
		alpha.getConfig().setEvaluateStratifiedPart(false);
		InputProgram input = alpha.readProgramString(NUM_MAX_DEGREE_VERTICES_ASP);
		NormalProgram normalized = alpha.normalizeProgram(input);
		// System.out.println(normalized);
		List<AnswerSet> answerSets = alpha.solve(normalized, (p) -> true).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.get(0);
		Predicate maxDegreeVertices = Predicate.getInstance("graph_max_degree_vertices", 3);

		// System.out.println(new SimpleAnswerSetFormatter("\n").format(answerSet));

		Assert.assertTrue(answerSet.getPredicates().contains(maxDegreeVertices));
		Assert.assertTrue(answerSet.getPredicateInstances(maxDegreeVertices)
				.contains(new BasicAtom(maxDegreeVertices, ConstantTerm.getSymbolicInstance("g1"), ConstantTerm.getInstance(2), ConstantTerm.getInstance(3))));
	}

	@Test
	public void greaterMin() {
		Alpha alpha = new Alpha();
		alpha.getConfig().setEvaluateStratifiedPart(false);
		InputProgram input = alpha.readProgramString(MIN_GT1_ASP);
		NormalProgram normalized = alpha.normalizeProgram(input);
		// System.out.println(normalized);
		List<AnswerSet> answerSets = alpha.solve(normalized, (p) -> true).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.get(0);
		Predicate greaterMin = Predicate.getInstance("greater_min_acceptable", 1);

		// System.out.println(new SimpleAnswerSetFormatter("\n").format(answerSet));

		Assert.assertTrue(answerSet.getPredicates().contains(greaterMin));
		Assert.assertTrue(answerSet.getPredicateInstances(greaterMin).contains(new BasicAtom(greaterMin, ConstantTerm.getInstance(7))));
		Assert.assertTrue(answerSet.getPredicateInstances(greaterMin).contains(new BasicAtom(greaterMin, ConstantTerm.getInstance(13))));
	}

	@Test
	public void sumEquals1() {
		Alpha alpha = new Alpha();
		alpha.getConfig().setEvaluateStratifiedPart(false);
		InputProgram input = alpha.readProgramString(SUM_EQ1_ASP);
		NormalProgram normalized = alpha.normalizeProgram(input);
		// System.out.println(normalized);
		List<AnswerSet> answerSets = alpha.solve(normalized, (p) -> true).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.get(0);
		Predicate sumThings = Predicate.getInstance("sum_things", 1);

		// System.out.println(new SimpleAnswerSetFormatter("\n").format(answerSet));

		Assert.assertTrue(answerSet.getPredicates().contains(sumThings));
		Assert.assertEquals(1, answerSet.getPredicateInstances(sumThings).size());
		Assert.assertTrue(answerSet.getPredicateInstances(sumThings).contains(new BasicAtom(sumThings, ConstantTerm.getInstance(12))));
	}

	@Test
	public void sumLessOrEqual1() {
		Alpha alpha = new Alpha();
		alpha.getConfig().setEvaluateStratifiedPart(false);
		InputProgram input = alpha.readProgramString(SUM_LE1_ASP);
		NormalProgram normalized = alpha.normalizeProgram(input);
		// System.out.println(normalized);
		List<AnswerSet> answerSets = alpha.solve(normalized, (p) -> true).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.get(0);
		Predicate boundLe = Predicate.getInstance("bound_le_sum", 1);

		// System.out.println(new SimpleAnswerSetFormatter("\n").format(answerSet));

		Assert.assertTrue(answerSet.getPredicates().contains(boundLe));
		Assert.assertTrue(answerSet.getPredicateInstances(boundLe).contains(new BasicAtom(boundLe, ConstantTerm.getInstance(11))));
	}

	@Ignore("Open issue, as dependency analysis includes cyclic output-dependency, which it should not.")
	@Test
	public void setComplexEqualityWithGlobals() {
		Alpha alpha = new Alpha();
		alpha.getConfig().setEvaluateStratifiedPart(false);
		InputProgram input = alpha.readProgramString(COMPLEX_EQUALITY_WITH_GLOBALS);
		NormalProgram normalized = alpha.normalizeProgram(input);
		// System.out.println(normalized);
		List<AnswerSet> answerSets = alpha.solve(normalized, (p) -> true).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.get(0);
		Predicate q = Predicate.getInstance("q", 0);

		// System.out.println(new SimpleAnswerSetFormatter("\n").format(answerSet));

		Assert.assertTrue(answerSet.getPredicates().contains(q));
		Assert.assertTrue(answerSet.getPredicateInstances(q).contains(new BasicAtom(q)));
	}

}
