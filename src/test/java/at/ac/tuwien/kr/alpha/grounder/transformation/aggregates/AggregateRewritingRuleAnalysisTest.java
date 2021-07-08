package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateElement;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonAtom;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.ArithmeticTerm;
import at.ac.tuwien.kr.alpha.common.terms.ArithmeticTerm.ArithmeticOperator;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.test.util.RuleParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AggregateRewritingRuleAnalysisTest {

	//@formatter:off
	private static final String BINDING_AGGREGATE_NO_GLOBALS = 
			"p(X) :- X = #max{N : q(N)}, X < 10, p(X, Y).";
	private static final String NONBINDING_AGGREGATE_NO_GLOBALS_1 = 
			"p(X) :- X < #max{N : q(N)}, X < 10, p(X, Y).";
	private static final String NONBINDING_AGGREGATE_NO_GLOBALS_2 = 
			"p(X) :- X < #max{N : q(N)}, X < 10, X = 3 + Y, Y = Z + 4, r(S, Z).";
	private static final String BINDING_AGGREGATES_NO_GLOBALS_INCLUDED =
			"p :- not p(X), 3 < #max { Y : q(Y) }, X = #count { Y : q(Y) }.";
	private static final String BINDING_AGGREGATE_WITH_GLOBALS_1 =
			"graph_vertex_degree(G, V, D) :-"
			+ "    graph(G),"
			+ "    graph_vertex(G, V),"
			+ "    D = #count{ VN : graph_edge(G, e(V, VN)); VN : graph_edge(G, e(VN, V)) }.";
	private static final String BINDING_AGGREGATE_WITH_GLOBALS_2 = 
			"graph_max_degree_vertices(G, DMAX, N) :-"
			+ "	   graph(G),"
			+ "	   DMAX = #max{ DV : graph_vertex_degree(G, V, DV)},"
			+ "    N = #count{ V : graph_vertex_degree(G, V, DMAX)}.";
	//@formatter:on

	private static final AggregateRewritingRuleAnalysis analyze(String rule) {
		return AggregateRewritingRuleAnalysis.analyzeRuleDependencies(RuleParser.parse(rule));
	}

	@Test
	public void bindingAggregateNoGlobals() {
		AggregateRewritingRuleAnalysis analysis = analyze(BINDING_AGGREGATE_NO_GLOBALS);
		Assert.assertEquals(1, analysis.globalVariablesPerAggregate.size());
		Assert.assertEquals(1, analysis.dependenciesPerAggregate.size());

		AggregateLiteral aggregate = new ArrayList<>(analysis.globalVariablesPerAggregate.keySet()).get(0);
		Assert.assertTrue(analysis.globalVariablesPerAggregate.get(aggregate).isEmpty());
		Assert.assertEquals(0, analysis.dependenciesPerAggregate.get(aggregate).size());
	}

	@Test
	public void nonBindingAggregateNoGlobals1() {
		AggregateRewritingRuleAnalysis analysis = analyze(NONBINDING_AGGREGATE_NO_GLOBALS_1);
		Assert.assertEquals(1, analysis.globalVariablesPerAggregate.size());
		Assert.assertEquals(1, analysis.dependenciesPerAggregate.size());

		AggregateLiteral aggregate = new ArrayList<>(analysis.globalVariablesPerAggregate.keySet()).get(0);
		Assert.assertTrue(analysis.globalVariablesPerAggregate.get(aggregate).isEmpty());
		Assert.assertFalse(analysis.dependenciesPerAggregate.get(aggregate).isEmpty());

		Set<Literal> dependencies = analysis.dependenciesPerAggregate.get(aggregate);
		Assert.assertEquals(1, dependencies.size());

		Literal pXY = new BasicLiteral(new BasicAtom(Predicate.getInstance("p", 2), VariableTerm.getInstance("X"), VariableTerm.getInstance("Y")), true);
		Assert.assertTrue(dependencies.contains(pXY));
	}

	@Test
	public void nonBindingAggregateNoGlobals2() {
		AggregateRewritingRuleAnalysis analysis = analyze(NONBINDING_AGGREGATE_NO_GLOBALS_2);
		Assert.assertEquals(1, analysis.globalVariablesPerAggregate.size());
		Assert.assertEquals(1, analysis.dependenciesPerAggregate.size());

		AggregateLiteral aggregate = new ArrayList<>(analysis.globalVariablesPerAggregate.keySet()).get(0);
		Assert.assertTrue(analysis.globalVariablesPerAggregate.get(aggregate).isEmpty());
		Assert.assertFalse(analysis.dependenciesPerAggregate.get(aggregate).isEmpty());

		Set<Literal> dependencies = analysis.dependenciesPerAggregate.get(aggregate);
		Assert.assertEquals(3, dependencies.size());

		Literal threePlusY = new ComparisonLiteral(
				new ComparisonAtom(VariableTerm.getInstance("X"),
						ArithmeticTerm.getInstance(ConstantTerm.getInstance(3), ArithmeticOperator.PLUS, VariableTerm.getInstance("Y")),
						ComparisonOperator.EQ),
				true);
		Assert.assertTrue(dependencies.contains(threePlusY));

		Literal zPlusFour = new ComparisonLiteral(
				new ComparisonAtom(VariableTerm.getInstance("Y"),
						ArithmeticTerm.getInstance(VariableTerm.getInstance("Z"), ArithmeticOperator.PLUS, ConstantTerm.getInstance(4)),
						ComparisonOperator.EQ),
				true);
		Assert.assertTrue(dependencies.contains(zPlusFour));

		Literal rSZ = new BasicLiteral(new BasicAtom(Predicate.getInstance("r", 2), VariableTerm.getInstance("S"), VariableTerm.getInstance("Z")), true);
		Assert.assertTrue(dependencies.contains(rSZ));
	}

	@Test
	public void bindingAggregateWithGlobals1() {
		AggregateRewritingRuleAnalysis analysis = analyze(BINDING_AGGREGATE_WITH_GLOBALS_1);
		Assert.assertEquals(1, analysis.globalVariablesPerAggregate.size());
		Assert.assertEquals(1, analysis.dependenciesPerAggregate.size());

		AggregateLiteral aggregate = new ArrayList<>(analysis.globalVariablesPerAggregate.keySet()).get(0);
		Assert.assertFalse(analysis.globalVariablesPerAggregate.get(aggregate).isEmpty());
		Assert.assertFalse(analysis.dependenciesPerAggregate.get(aggregate).isEmpty());

		Set<VariableTerm> globalVars = analysis.globalVariablesPerAggregate.get(aggregate);
		Assert.assertTrue(globalVars.contains(VariableTerm.getInstance("G")));
		Assert.assertTrue(globalVars.contains(VariableTerm.getInstance("V")));

		Set<Literal> dependencies = analysis.dependenciesPerAggregate.get(aggregate);
		Assert.assertEquals(2, dependencies.size());

		Literal graph = new BasicLiteral(new BasicAtom(Predicate.getInstance("graph", 1), VariableTerm.getInstance("G")), true);
		Assert.assertTrue(dependencies.contains(graph));

		Literal graphVertex = new BasicLiteral(
				new BasicAtom(Predicate.getInstance("graph_vertex", 2), VariableTerm.getInstance("G"), VariableTerm.getInstance("V")), true);
		Assert.assertTrue(dependencies.contains(graphVertex));
	}

	@Test
	public void bindingAggregateWithGlobals2() {
		AggregateRewritingRuleAnalysis analysis = analyze(BINDING_AGGREGATE_WITH_GLOBALS_2);
		Assert.assertEquals(2, analysis.globalVariablesPerAggregate.size());
		Assert.assertEquals(2, analysis.dependenciesPerAggregate.size());

		// Verify correct analysis of max aggregate
		List<Term> vertexDegreeTerms = Collections.singletonList(VariableTerm.getInstance("DV"));
		Literal vertexDegreeLiteral = new BasicLiteral(new BasicAtom(Predicate.getInstance("graph_vertex_degree", 3), VariableTerm.getInstance("G"),
				VariableTerm.getInstance("V"), VariableTerm.getInstance("DV")), true);
		List<Literal> vertexDegreeLiterals = Collections.singletonList(vertexDegreeLiteral);
		AggregateElement vertexDegree = new AggregateElement(vertexDegreeTerms, vertexDegreeLiterals);
		AggregateLiteral maxAggregate = new AggregateLiteral(
				new AggregateAtom(ComparisonOperator.EQ, VariableTerm.getInstance("DMAX"), AggregateFunctionSymbol.MAX,
						Collections.singletonList(vertexDegree)),
				true);
		Assert.assertTrue(analysis.globalVariablesPerAggregate.containsKey(maxAggregate));

		Set<VariableTerm> maxAggrGlobalVars = analysis.globalVariablesPerAggregate.get(maxAggregate);
		Assert.assertEquals(1, maxAggrGlobalVars.size());
		Assert.assertTrue(maxAggrGlobalVars.contains(VariableTerm.getInstance("G")));

		Assert.assertTrue(analysis.dependenciesPerAggregate.containsKey(maxAggregate));
		Set<Literal> maxAggrDependencies = analysis.dependenciesPerAggregate.get(maxAggregate);
		Assert.assertEquals(1, maxAggrDependencies.size());
		Literal graph = new BasicLiteral(new BasicAtom(Predicate.getInstance("graph", 1), VariableTerm.getInstance("G")), true);
		Assert.assertTrue(maxAggrDependencies.contains(graph));

		// Verify correct analysis of count aggregate
		List<Term> maxVertexDegreeTerms = Collections.singletonList(VariableTerm.getInstance("V"));
		Literal maxVertexDegreeLiteral = new BasicLiteral(new BasicAtom(Predicate.getInstance("graph_vertex_degree", 3), VariableTerm.getInstance("G"),
				VariableTerm.getInstance("V"), VariableTerm.getInstance("DMAX")), true);
		List<Literal> maxVertexDegreeLiterals = Collections.singletonList(maxVertexDegreeLiteral);
		AggregateElement maxVertexDegree = new AggregateElement(maxVertexDegreeTerms, maxVertexDegreeLiterals);
		AggregateLiteral countAggregate = new AggregateLiteral(
				new AggregateAtom(ComparisonOperator.EQ, VariableTerm.getInstance("N"), AggregateFunctionSymbol.COUNT,
						Collections.singletonList(maxVertexDegree)),
				true);
		Assert.assertTrue(analysis.globalVariablesPerAggregate.containsKey(countAggregate));
		Set<VariableTerm> cntAggrGlobalVars = analysis.globalVariablesPerAggregate.get(countAggregate);
		Assert.assertEquals(2, cntAggrGlobalVars.size());
		Assert.assertTrue(cntAggrGlobalVars.contains(VariableTerm.getInstance("G")));
		Assert.assertTrue(cntAggrGlobalVars.contains(VariableTerm.getInstance("DMAX")));

		Assert.assertTrue(analysis.dependenciesPerAggregate.containsKey(countAggregate));
		Set<Literal> cntAggrDependencies = analysis.dependenciesPerAggregate.get(countAggregate);
		Assert.assertEquals(2, cntAggrDependencies.size());
		Assert.assertTrue(cntAggrDependencies.contains(graph));
		Assert.assertTrue(cntAggrDependencies.contains(maxAggregate));
	}

	@Test
	public void bindingAggregateGlobalsNotIncluded() {
		AggregateRewritingRuleAnalysis analysis = analyze(BINDING_AGGREGATES_NO_GLOBALS_INCLUDED);
		Assert.assertEquals(2, analysis.globalVariablesPerAggregate.size());
		Assert.assertEquals(2, analysis.dependenciesPerAggregate.size());

		// Check that the #max aggregate does not include "not p(X)" as its dependency.
		for (Map.Entry<AggregateLiteral, Set<Literal>> aggregateDependencies : analysis.dependenciesPerAggregate.entrySet()) {
			if (aggregateDependencies.getKey().getAtom().getAggregatefunction() == AggregateFunctionSymbol.MAX) {
				for (Literal dependency : aggregateDependencies.getValue()) {
					Assert.assertFalse(dependency.isNegated());
				}
			}
		}
	}

}
