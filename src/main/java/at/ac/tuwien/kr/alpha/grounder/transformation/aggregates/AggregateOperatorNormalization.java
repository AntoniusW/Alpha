package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import static at.ac.tuwien.kr.alpha.common.ComparisonOperator.EQ;
import static at.ac.tuwien.kr.alpha.common.ComparisonOperator.LE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonAtom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.terms.ArithmeticTerm;
import at.ac.tuwien.kr.alpha.common.terms.ArithmeticTerm.ArithmeticOperator;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;

/**
 * Transforms an {@link InputProgram} such that, for all aggregate (body-)literals, only the comparison operators "="
 * and "<=" are used.
 * 
 * Rewriting of "#count" and "#sum" aggregates is done using the following equivalences:
 * <ul>
 * <li><code>X < #aggr{...}</code> == <code>XP <= #aggr{...}, XP = X + 1</code></li>
 * <li><code>X != #aggr{...}</code> == <code>not X = #aggr{...}</code></li>
 * <li><code>X > #aggr{...}</code> == <code>not X <= #aggr{...}</code></li>
 * <li><code>X >= #aggr{...}</code> == <code>not XP <= #aggr{...}, XP = X + 1</code></li>
 * <li><code>not X < #aggr{...}</code> == <code>not XP <= #aggr{...}, XP = X + 1</code></li>
 * <li><code>not X != #aggr{...}</code> == <code>X = #aggr{...}</code></li>
 * <li><code>not X > #aggr{...}</code> == <code>X <= #aggr{...}</code></li>
 * <li><code>not X >= #aggr{...}</code> == <code>XP <= #aggr{...}, XP = X + 1</code></li>
 * </ul>
 * Operators for "#min" and "#max" aggregates are not rewritten.
 * 
 * Note that input programs must only contain aggregate literals of form <code>VAR OP #aggr{...}</code>, i.e. with only
 * a left term and operator. When preprocessing programs, apply this transformation AFTER
 * {@link at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.AggregateLiteralSplitting}.
 * 
 * Copyright (c) 2020-2021, the Alpha Team.
 */
public final class AggregateOperatorNormalization {

	private AggregateOperatorNormalization() {
		throw new UnsupportedOperationException("Utility class - cannot instantiate!");
	}

	public static BasicRule normalize(BasicRule rule) {
		List<Literal> rewrittenBody = new ArrayList<>();
		for (Literal lit : rule.getBody()) {
			rewrittenBody.addAll(rewriteLiteral(lit));
		}
		return new BasicRule(rule.getHead(), rewrittenBody);
	}

	private static List<Literal> rewriteLiteral(Literal lit) {
		if (lit instanceof AggregateLiteral) {
			return rewriteAggregateOperator((AggregateLiteral) lit);
		} else {
			return Collections.singletonList(lit);
		}
	}

	private static List<Literal> rewriteAggregateOperator(AggregateLiteral lit) {
		AggregateAtom atom = lit.getAtom();
		if (lit.getAtom().getAggregatefunction() == AggregateFunctionSymbol.MIN || lit.getAtom().getAggregatefunction() == AggregateFunctionSymbol.MAX) {
			// No operator normalization needed for #min/#max aggregates.
			return Collections.singletonList(lit);
		}
		if (atom.getLowerBoundOperator() == EQ || atom.getLowerBoundOperator() == LE) {
			// Nothing to do for operator "=" or "<=".
			return Collections.singletonList(lit);
		} else {
			List<Literal> retVal = new ArrayList<>();
			VariableTerm decrementedBound;
			switch (atom.getLowerBoundOperator()) {
				case LT:
					decrementedBound = VariableTerm.getAnonymousInstance();
					retVal.add(createLowerBoundedAggregateLiteral(LE, decrementedBound, atom, !lit.isNegated()));
					retVal.add(createPlusOneTerm(atom.getLowerBoundTerm(), decrementedBound));
					break;
				case NE:
					retVal.add(createLowerBoundedAggregateLiteral(EQ, atom.getLowerBoundTerm(), atom, lit.isNegated()));
					break;
				case GT:
					retVal.add(createLowerBoundedAggregateLiteral(LE, atom.getLowerBoundTerm(), atom, lit.isNegated()));
					break;
				case GE:
					decrementedBound = VariableTerm.getAnonymousInstance();
					retVal.add(createLowerBoundedAggregateLiteral(LE, decrementedBound, atom, lit.isNegated()));
					retVal.add(createPlusOneTerm(atom.getLowerBoundTerm(), decrementedBound));
					break;
				default:
					throw new IllegalStateException("No operator rewriting logic available for literal: " + lit);
			}
			return retVal;
		}
	}

	private static AggregateLiteral createLowerBoundedAggregateLiteral(ComparisonOperator op, Term lowerBoundTerm, AggregateAtom aggregateAtom, boolean isNegated) {
		return new AggregateLiteral(new AggregateAtom(op, lowerBoundTerm, aggregateAtom.getAggregatefunction(),
			aggregateAtom.getAggregateElements()), isNegated);
	}
	
	/**
	 * Creates a new {@link Literal} that assigns the given target variable to the given (integer) term plus one.
	 * 
	 * @param term
	 * @param targetVariable
	 * @return
	 */
	private static Literal createPlusOneTerm(Term term, VariableTerm targetVariable) {
		Term increment = ArithmeticTerm.getInstance(term, ArithmeticOperator.PLUS, ConstantTerm.getInstance(1));
		ComparisonAtom atom = new ComparisonAtom(targetVariable, increment, ComparisonOperator.EQ);
		return atom.toLiteral();
	}

}
