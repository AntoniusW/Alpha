package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.AggregateRewritingContext.AggregateInfo;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class AggregateRewritingContextTest {

	//@formatter:off
	private static final String CTX_TEST_MIN_EQ_ASP =
			"thing(1..3)."
			+ "min_thing(M) :- thing(M), M = #min{N : thing(N)}.";
	
	private static final String CTX_TEST_CNT_EQ_ASP =
			"thing(1..3)."
			+ "cnt_things(N) :- N = #count{X : thing(X)}.";
	
	private static final String CTX_TEST_GRAPH_ASP = 
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
			+ ":- err_undirected_edges(_, _, _)."
			+ "graph_vertex_degree(G, V, D) :-"
			+ "    graph(G),"
			+ "    graph_vertex(G, V),"
			+ "    D = #count{ VN : graph_edge(G, e(V, VN)); VN : graph_edge(G, e(VN, V)) }."
			+ "graph_max_degree_vertices(G, DMAX, N) :-"
			+ "	   graph(G),"
			+ "	   DMAX = #max{ DV : graph_vertex_degree(G, V, DV)},"
			+ "    N = #count{ V : graph_vertex_degree(G, V, DMAX)}.";
	//@formatter:on

	private static final AggregateRewritingContext rewritingContextForAspString(String asp) {
		InputProgram program = new Alpha().readProgramString(asp);
		AggregateRewritingContext ctx = new AggregateRewritingContext();
		for (BasicRule rule : program.getRules()) {
			ctx.registerRule(rule);
		}
		return ctx;
	}

	@Test
	public void minEqAggregateNoGlobalVars() {
		AggregateRewritingContext ctx = rewritingContextForAspString(CTX_TEST_MIN_EQ_ASP);
		Map<ImmutablePair<AggregateFunctionSymbol, ComparisonOperator>, Set<AggregateInfo>> functionsToRewrite = ctx.getAggregateFunctionsToRewrite();
		Assert.assertEquals(1, functionsToRewrite.size());
		ImmutablePair<AggregateFunctionSymbol, ComparisonOperator> minEq = new ImmutablePair<>(AggregateFunctionSymbol.MIN, ComparisonOperator.EQ);
		Assert.assertTrue(functionsToRewrite.containsKey(minEq));
		Set<AggregateInfo> minEqAggregateInfos = functionsToRewrite.get(minEq);
		Assert.assertEquals(1, minEqAggregateInfos.size());
		AggregateInfo info = minEqAggregateInfos.iterator().next();
		Assert.assertTrue(info.getGlobalVariables().isEmpty());
	}

	@Test
	public void countEqAggregateNoGlobalVars() {
		AggregateRewritingContext ctx = rewritingContextForAspString(CTX_TEST_CNT_EQ_ASP);
		Map<ImmutablePair<AggregateFunctionSymbol, ComparisonOperator>, Set<AggregateInfo>> functionsToRewrite = ctx.getAggregateFunctionsToRewrite();
		Assert.assertEquals(1, functionsToRewrite.size());
		ImmutablePair<AggregateFunctionSymbol, ComparisonOperator> cntEq = new ImmutablePair<>(AggregateFunctionSymbol.COUNT, ComparisonOperator.EQ);
		Assert.assertTrue(functionsToRewrite.containsKey(cntEq));
		Set<AggregateInfo> cntEqAggregateInfos = functionsToRewrite.get(cntEq);
		Assert.assertEquals(1, cntEqAggregateInfos.size());
		AggregateInfo info = cntEqAggregateInfos.iterator().next();
		Assert.assertTrue(info.getGlobalVariables().isEmpty());
	}

	@Test
	public void countEqMaxEqGlobalVars() {
		AggregateRewritingContext ctx = rewritingContextForAspString(CTX_TEST_GRAPH_ASP);
		Map<ImmutablePair<AggregateFunctionSymbol, ComparisonOperator>, Set<AggregateInfo>> functionsToRewrite = ctx.getAggregateFunctionsToRewrite();
		Assert.assertEquals(2, functionsToRewrite.size());
		ImmutablePair<AggregateFunctionSymbol, ComparisonOperator> cntEq = new ImmutablePair<>(AggregateFunctionSymbol.COUNT, ComparisonOperator.EQ);
		ImmutablePair<AggregateFunctionSymbol, ComparisonOperator> maxEq = new ImmutablePair<>(AggregateFunctionSymbol.MAX, ComparisonOperator.EQ);
		Assert.assertTrue(functionsToRewrite.containsKey(cntEq));
		Assert.assertTrue(functionsToRewrite.containsKey(maxEq));
		Set<AggregateInfo> cntEqIds = functionsToRewrite.get(cntEq);
		Set<AggregateInfo> maxEqIds = functionsToRewrite.get(maxEq);
		Assert.assertEquals(2, cntEqIds.size());
		Assert.assertEquals(1, maxEqIds.size());
		Predicate<AggregateInfo> vertexDegreeCount = (info) -> {
			if (info.getLiteral().getAtom().getAggregateElements().size() != 2) {
				return false;
			}
			Set<VariableTerm> globalVars = info.getGlobalVariables();
			if (globalVars.size() != 2) {
				return false;
			}
			if (!globalVars.contains(VariableTerm.getInstance("G"))) {
				return false;
			}
			if (!globalVars.contains(VariableTerm.getInstance("V"))) {
				return false;
			}
			return true;
		};
		Predicate<AggregateInfo> maxDegreeVerticesCount = (info) -> {
			if (info.getLiteral().getAtom().getAggregateElements().size() != 1) {
				return false;
			}
			Set<VariableTerm> globalVars = info.getGlobalVariables();
			if (globalVars.size() != 2) {
				return false;
			}
			if (!globalVars.contains(VariableTerm.getInstance("G"))) {
				return false;
			}
			if (!globalVars.contains(VariableTerm.getInstance("DMAX"))) {
				return false;
			}
			return true;
		};
		boolean verifiedDegreeCount = false;
		boolean verifiedMaxDegreeVerticesCount = false;
		for (AggregateInfo id : cntEqIds) {
			if (vertexDegreeCount.test(id)) {
				verifiedDegreeCount = true;
			} else if (maxDegreeVerticesCount.test(id)) {
				verifiedMaxDegreeVerticesCount = true;
			}
		}
		Assert.assertTrue(verifiedDegreeCount);
		Assert.assertTrue(verifiedMaxDegreeVerticesCount);
	}

}
