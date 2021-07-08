package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits aggregate literals with both "lower" and "upper" bound operators into literals with only one operator each.
 * 
 * Follows The <a href="https://www.mat.unical.it/aspcomp2013/files/ASP-CORE-2.03c.pdf">ASP-Core2 Standard</a> (Section
 * 3.3) for splitting logic.<br/>
 * Aggregate (body-)literals of format <code>LT LO #aggr{...} RO RT</code> are rewritten into two separate literals,
 * <code>LT LO #aggr{...}</code> and <code>RT inv(RO) #aggr{...}</code>, where <code>inv(OP)</code> for an operator OP
 * denotes the inverse comparison operator as defined in section 3.3 of the standard. <br/>
 * Negative aggregate (body-)literals of format <code>not LT LO #aggr{...} RO RT</code> require the source rule to be
 * split into rules:
 * <ul>
 * <li>H :- b1,...,bn, not LT LO #aggr{...}.</li>
 * <li>H :- b1,...,bn, not RT inv(RO) #aggr{...}.</li>
 * </ul>
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public final class AggregateLiteralSplitting {

	private AggregateLiteralSplitting() {
		throw new UnsupportedOperationException("Utility class - cannot instantiate!");
	}

	public static List<BasicRule> split(BasicRule sourceRule) {
		// Check if body contains aggregates that need to be split.
		for (Literal lit : sourceRule.getBody()) {
			if (lit instanceof AggregateLiteral && shouldRewrite((AggregateLiteral) lit)) {
				return splitAggregatesInRule(sourceRule);
			}
		}
		// No aggregate in the body that needs splitting, return original rule.
		return Collections.singletonList(sourceRule);
	}

	private static List<BasicRule> splitAggregatesInRule(BasicRule sourceRule) {
		// Rule contains some aggregates that need splitting.
		// Aggregates may require splitting in two literals, or in two rules.
		List<Literal> commonBodyLiterals = new ArrayList<>();
		List<Literal> twoLiteralsSplitAggregates = new ArrayList<>();
		List<ImmutablePair<Literal, Literal>> twoRulesSplitAggregates = new ArrayList<>();
		// First, sort literals of the rule and also compute splitting.
		for (Literal literal : sourceRule.getBody()) {
			if (literal instanceof AggregateLiteral && shouldRewrite((AggregateLiteral) literal)) {
				splitCombinedAggregateLiteral(literal, twoLiteralsSplitAggregates, twoRulesSplitAggregates);
			} else {
				// Literal is no aggregate that needs splitting.
				commonBodyLiterals.add(literal);
			}
		}
		// Second, compute rule bodies of splitting result.
		List<Literal> commonBody = new ArrayList<>(commonBodyLiterals);
		commonBody.addAll(twoLiteralsSplitAggregates);
		List<List<Literal>> rewrittenBodies = new ArrayList<>();
		rewrittenBodies.add(commonBody);	// Initialize list of rules with the common body.
		// For n twoRulesSplitAggregates we need 2^n rules, so
		// for each of the n pairs in twoRulesSplitAggregates we duplicate the list of rewritten bodies.
		for (ImmutablePair<Literal, Literal> ruleSplitAggregate : twoRulesSplitAggregates) {
			int numBodiesBeforeDuplication = rewrittenBodies.size();
			for (int i = 0; i < numBodiesBeforeDuplication; i++) {
				List<Literal> originalBody = rewrittenBodies.get(i);
				List<Literal> duplicatedBody = new ArrayList<>(originalBody);
				// Extend bodies of original and duplicate with splitting results.
				originalBody.add(ruleSplitAggregate.left);
				duplicatedBody.add(ruleSplitAggregate.right);
				rewrittenBodies.add(duplicatedBody);
			}
		}
		// Third, turn computed bodies into rules again.
		List<BasicRule> rewrittenRules = new ArrayList<>();
		for (List<Literal> rewrittenBody : rewrittenBodies) {
			rewrittenRules.add(new BasicRule(sourceRule.getHead(), rewrittenBody));
		}
		return rewrittenRules;
	}

	private static void splitCombinedAggregateLiteral(Literal literal, List<Literal> twoLiteralsSplitAggregates, List<ImmutablePair<Literal, Literal>> twoRulesSplitAggregates) {
		AggregateLiteral aggLit = (AggregateLiteral) literal;
		ImmutablePair<AggregateAtom, AggregateAtom> splitAggregate = splitCombinedAggregateAtom(aggLit.getAtom());
		if (literal.isNegated()) {
			// Negated aggregate require splitting in two rules.
			twoRulesSplitAggregates.add(new ImmutablePair<>(
				splitAggregate.left.toLiteral(false),
				splitAggregate.right.toLiteral(false)));
		} else {
			// Positive aggregate requires two literals in the body.
			twoLiteralsSplitAggregates.add(splitAggregate.left.toLiteral(true));
			twoLiteralsSplitAggregates.add(splitAggregate.right.toLiteral(true));
		}
	}

	private static boolean shouldRewrite(AggregateLiteral lit) {
		return lit.getAtom().getLowerBoundTerm() != null && lit.getAtom().getUpperBoundTerm() != null;
	}

	private static ImmutablePair<AggregateAtom, AggregateAtom> splitCombinedAggregateAtom(AggregateAtom atom) {
		AggregateAtom leftHandAtom = new AggregateAtom(atom.getLowerBoundOperator(), atom.getLowerBoundTerm(), atom.getAggregatefunction(),
				atom.getAggregateElements());
		AggregateAtom rightHandAtom = new AggregateAtom(switchOperands(atom.getUpperBoundOperator()), atom.getUpperBoundTerm(),
				atom.getAggregatefunction(), atom.getAggregateElements());
		return new ImmutablePair<>(leftHandAtom, rightHandAtom);
	}

	private static ComparisonOperator switchOperands(ComparisonOperator op) {
		switch (op) {
			case EQ:
				return op;
			case NE:
				return op;
			case LT:
				return ComparisonOperator.GT;
			case LE:
				return ComparisonOperator.GE;
			case GT:
				return ComparisonOperator.LT;
			case GE:
				return ComparisonOperator.LE;
			default:
				throw new IllegalArgumentException("Unsupported ComparisonOperator " + op + "!");
		}
	}

}
