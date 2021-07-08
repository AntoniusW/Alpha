package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.test.util.RuleParser;

public class AggregateLiteralSplittingTest {

	//@formatter:off
	public static final String LITERAL_SPLITTING_POS1_ASP = 
			"count_between :- "
				+ "X < #count{ N : thing2(N) } < Y, "
				+ "X = #count{ K : thing1(K) },"
				+ "Y = #count{ L : thing3(L) }.";
	public static final String LITERAL_SPLITTING_POS2_ASP = 
			"count_between :- "
				+ "X < #count{ N : thing2(N) } < Y, "
				+ "dom(X),"
				+ "dom1(Y),"
				+ "Y < #count{ L : thing3(L) } = Z.";
	public static final String LITERAL_SPLITTING_NEG1_ASP =
			"count_not_between :- "
			+ "dom(X), dom(Y),"
			+ "not X < #count{N : thing(N)} < Y.";
	public static final String LITERAL_SPLITTING_NEG2_ASP =
			"count_not_between :- "
			+ "dom(X), dom(Y), dom(U), dom(V),"
			+ "not X < #count{N : thing(N)} < Y,"
			+ "not U < #count{K : thing(K)} < V.";
	public static final String LITERAL_SPLITTING_NEG_POS_ASP =
			"count_between_and_not_between(U, V, X, Y) :- "
			+ "dom(X), dom(Y), dom(U), dom(V),"
			+ "U < #count{K : thing(K)} < V,"
			+ "not X < #count{N : thing(N)} < Y.";
	//@formatter:on

	@Test
	public void singlePositiveLiteral() {
		BasicRule inputRule = RuleParser.parse(LITERAL_SPLITTING_POS1_ASP);
		BasicRule expectedRewrittenRule = RuleParser
				.parse("count_between :- X < #count{N : thing2(N)}, Y > #count{N : thing2(N)}, X = #count{ K : thing1(K) }, Y = #count{ L : thing3(L) }.");
		List<BasicRule> rewritten = AggregateLiteralSplitting.split(inputRule);
		Assert.assertEquals(1, rewritten.size());
		Assert.assertEquals(expectedRewrittenRule, rewritten.get(0));
	}

	@Test
	public void multiplePositiveLiterals() {
		BasicRule inputRule = RuleParser.parse(LITERAL_SPLITTING_POS2_ASP);
		BasicRule expectedRewrittenRule = RuleParser
				.parse("count_between :- X < #count{N : thing2(N)}, Y > #count{N : thing2(N)}, Y < #count{L : thing3(L)}, Z = #count{L : thing3(L)}, dom(X), dom1(Y).");
		List<BasicRule> rewritten = AggregateLiteralSplitting.split(inputRule);
		Assert.assertEquals(1, rewritten.size());
		Assert.assertEquals(expectedRewrittenRule, rewritten.get(0));
	}

	@Test
	public void singleNegativeLiteral() {
		BasicRule inputRule = RuleParser.parse(LITERAL_SPLITTING_NEG1_ASP);
		BasicRule expectedRewrittenRule1 = RuleParser
				.parse("count_not_between :- dom(X), dom(Y), not X < #count{N : thing(N)}.");
		BasicRule expectedRewrittenRule2 = RuleParser
				.parse("count_not_between :- dom(X), dom(Y), not Y > #count{N : thing(N)}.");
		List<BasicRule> rewritten = AggregateLiteralSplitting.split(inputRule);
		Assert.assertEquals(2, rewritten.size());
		Assert.assertTrue(rewritten.contains(expectedRewrittenRule1));
		Assert.assertTrue(rewritten.contains(expectedRewrittenRule2));
	}

	@Test
	public void multipleNegativeLiterals() {
		BasicRule inputRule = RuleParser.parse(LITERAL_SPLITTING_NEG2_ASP);
		BasicRule expectedRewrittenRule1 = RuleParser
				.parse("count_not_between :- dom(X), dom(Y), dom(U), dom(V), not X < #count{N : thing(N)}, not U < #count{K : thing(K)}.");
		BasicRule expectedRewrittenRule2 = RuleParser
				.parse("count_not_between :- dom(X), dom(Y), dom(U), dom(V), not X < #count{N : thing(N)}, not V > #count{K : thing(K)}.");
		BasicRule expectedRewrittenRule3 = RuleParser
				.parse("count_not_between :- dom(X), dom(Y), dom(U), dom(V), not Y > #count{N : thing(N)}, not U < #count{K : thing(K)}.");
		BasicRule expectedRewrittenRule4 = RuleParser
				.parse("count_not_between :- dom(X), dom(Y), dom(U), dom(V), not Y > #count{N : thing(N)}, not V > #count{K : thing(K)}.");
		List<BasicRule> rewritten = AggregateLiteralSplitting.split(inputRule);
		Assert.assertEquals(4, rewritten.size());
		Assert.assertTrue(rewritten.contains(expectedRewrittenRule1));
		Assert.assertTrue(rewritten.contains(expectedRewrittenRule2));
		Assert.assertTrue(rewritten.contains(expectedRewrittenRule3));
		Assert.assertTrue(rewritten.contains(expectedRewrittenRule4));
	}

	@Test
	public void negativeAndPositiveLiteral() {
		BasicRule inputRule = RuleParser.parse(LITERAL_SPLITTING_NEG_POS_ASP);
		BasicRule expectedRewrittenRule1 = RuleParser
				.parse("count_between_and_not_between(U, V, X, Y) :- dom(X), dom(Y), dom(U), dom(V), not X < #count{N : thing(N)}, U < #count{K : thing(K)}, V > #count{K : thing(K)}.");
		BasicRule expectedRewrittenRule2 = RuleParser
				.parse("count_between_and_not_between(U, V, X, Y) :- dom(X), dom(Y), dom(U), dom(V), not Y > #count{N : thing(N)}, U < #count{K : thing(K)}, V > #count{K : thing(K)}.");
		List<BasicRule> rewritten = AggregateLiteralSplitting.split(inputRule);
		Assert.assertEquals(2, rewritten.size());
		Assert.assertTrue(rewritten.contains(expectedRewrittenRule1));
		Assert.assertTrue(rewritten.contains(expectedRewrittenRule2));
	}

}
