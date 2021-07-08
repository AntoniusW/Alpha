package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.terms.ArithmeticTerm;
import at.ac.tuwien.kr.alpha.common.terms.ArithmeticTerm.ArithmeticOperator;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.test.util.RuleParser;

public class AggregateOperatorNormalizationTest {

	//@formatter:off
	public static final String OPERATOR_NORMALIZATION_GT_POS_ASP = 
			"bla :- dom(X), X > #count{N : thing(N)}.";
	public static final String OPERATOR_NORMALIZATION_LT_POS_ASP =
			"bla :- dom(X), X < #count{N : thing(N)}.";
	public static final String OPERATOR_NORMALIZATION_NE_POS_ASP =
			"bla :- dom(X), X != #count{N : thing(N)}.";
	public static final String OPERATOR_NORMALIZATION_GE_POS_ASP =
			"bla :- dom(X), X >= #count{N : thing(N)}.";
	public static final String OPERATOR_NORMALIZATION_LT_NEG_ASP =
			"bla :- dom(X), not X < #count{N : thing(N)}.";
	public static final String OPERATOR_NORMALIZATION_NE_NEG_ASP =
			"bla :- dom(X), not X != #count{N : thing(N)}.";
	public static final String OPERATOR_NORMALIZATION_GT_NEG_ASP = 
			"bla :- dom(X), not X > #count{N : thing(N)}.";
	public static final String OPERATOR_NORMALIZATION_GE_NEG_ASP =
			"bla :- dom(X), not X >= #count{N : thing(N)}.";
	//@formatter:on

	@Test
	public void gtPositive() {
		BasicRule inputRule = RuleParser.parse(OPERATOR_NORMALIZATION_GT_POS_ASP);
		BasicRule rewritten = AggregateOperatorNormalization.normalize(inputRule);
		BasicRule expectedRewrittenRule = RuleParser.parse("bla :- dom(X), not X <= #count{N : thing(N)}.");
		Assert.assertEquals(expectedRewrittenRule, rewritten);
	}

	@Test
	public void ltPositive() {
		BasicRule inputRule = RuleParser.parse(OPERATOR_NORMALIZATION_LT_POS_ASP);
		BasicRule rewritten = AggregateOperatorNormalization.normalize(inputRule);
		assertOperatorNormalized(rewritten, ComparisonOperator.LE, true);
		assertAggregateBoundIncremented(inputRule, rewritten);
	}

	@Test
	public void nePositive() {
		BasicRule inputRule = RuleParser.parse(OPERATOR_NORMALIZATION_NE_POS_ASP);
		BasicRule rewritten = AggregateOperatorNormalization.normalize(inputRule);
		BasicRule expectedRewrittenRule = RuleParser.parse("bla :- dom(X), not X = #count{N : thing(N)}.");
		Assert.assertEquals(expectedRewrittenRule, rewritten);
	}

	@Test
	public void gePositive() {
		BasicRule inputRule = RuleParser.parse(OPERATOR_NORMALIZATION_GE_POS_ASP);
		BasicRule rewritten = AggregateOperatorNormalization.normalize(inputRule);
		assertOperatorNormalized(rewritten, ComparisonOperator.LE, false);
		assertAggregateBoundIncremented(inputRule, rewritten);
	}

	@Test
	public void ltNegative() {
		BasicRule inputRule = RuleParser.parse(OPERATOR_NORMALIZATION_LT_NEG_ASP);
		BasicRule rewritten = AggregateOperatorNormalization.normalize(inputRule);
		assertOperatorNormalized(rewritten, ComparisonOperator.LE, false);
		assertAggregateBoundIncremented(inputRule, rewritten);
	}

	@Test
	public void neNegative() {
		BasicRule inputRule = RuleParser.parse(OPERATOR_NORMALIZATION_NE_NEG_ASP);
		BasicRule rewritten = AggregateOperatorNormalization.normalize(inputRule);
		BasicRule expectedRewrittenRule = RuleParser.parse("bla :- dom(X), X = #count{N : thing(N)}.");
		Assert.assertEquals(expectedRewrittenRule, rewritten);
	}

	@Test
	public void gtNegative() {
		BasicRule inputRule = RuleParser.parse(OPERATOR_NORMALIZATION_GT_NEG_ASP);
		BasicRule rewritten = AggregateOperatorNormalization.normalize(inputRule);
		BasicRule expectedRewrittenRule = RuleParser.parse("bla :- dom(X), X <= #count{N : thing(N)}.");
		Assert.assertEquals(expectedRewrittenRule, rewritten);
	}

	@Test
	public void geNegative() {
		BasicRule inputRule = RuleParser.parse(OPERATOR_NORMALIZATION_GE_NEG_ASP);
		BasicRule rewritten = AggregateOperatorNormalization.normalize(inputRule);
		assertOperatorNormalized(rewritten, ComparisonOperator.LE, true);
		assertAggregateBoundIncremented(inputRule, rewritten);
	}

	private static void assertOperatorNormalized(BasicRule rewrittenRule, ComparisonOperator expectedRewrittenOperator,
			boolean expectedRewrittenLiteralPositive) {
		AggregateLiteral rewrittenAggregate = null;
		for (Literal lit : rewrittenRule.getBody()) {
			if (lit instanceof AggregateLiteral) {
				rewrittenAggregate = (AggregateLiteral) lit;
			}
		}
		Assert.assertNotNull(rewrittenAggregate);
		Assert.assertEquals(expectedRewrittenOperator, rewrittenAggregate.getAtom().getLowerBoundOperator());
		Assert.assertTrue(expectedRewrittenLiteralPositive == !rewrittenAggregate.isNegated());
	}

	private static void assertAggregateBoundIncremented(BasicRule sourceRule, BasicRule rewrittenRule) {
		AggregateLiteral sourceAggregate = null;
		for (Literal lit : sourceRule.getBody()) {
			if (lit instanceof AggregateLiteral) {
				sourceAggregate = (AggregateLiteral) lit;
			}
		}
		AggregateLiteral rewrittenAggregate = null;
		ComparisonLiteral addedComparisonLiteral = null;
		for (Literal lit : rewrittenRule.getBody()) {
			if (lit instanceof AggregateLiteral) {
				rewrittenAggregate = (AggregateLiteral) lit;
			} else if (lit instanceof ComparisonLiteral) {
				addedComparisonLiteral = (ComparisonLiteral) lit;
			}
		}
		Assert.assertNotNull(addedComparisonLiteral);
		Assert.assertEquals(addedComparisonLiteral.getAtom().getTerms().get(0), rewrittenAggregate.getAtom().getLowerBoundTerm());
		Term comparisonRightHandTerm = addedComparisonLiteral.getAtom().getTerms().get(1);
		Assert.assertTrue(comparisonRightHandTerm instanceof ArithmeticTerm);
		ArithmeticTerm incrementTerm = (ArithmeticTerm) comparisonRightHandTerm;
		Assert.assertEquals(ArithmeticOperator.PLUS, incrementTerm.getArithmeticOperator());
		Assert.assertEquals(ConstantTerm.getInstance(1), incrementTerm.getRight());
		Assert.assertEquals(sourceAggregate.getAtom().getLowerBoundTerm(), incrementTerm.getLeft());
	}

}
