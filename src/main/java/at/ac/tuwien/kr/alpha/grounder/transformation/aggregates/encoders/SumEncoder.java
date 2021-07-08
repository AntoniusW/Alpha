package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateElement;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

/**
 * Abstract base class for aggregate encoders handling sum aggregates.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public final class SumEncoder extends StringtemplateBasedAggregateEncoder {

	private static final STGroup AGGREGATE_ENCODINGS = Util.loadStringTemplateGroup(
			SumEncoder.class.getResource("/stringtemplates/aggregate-encodings.stg"));

	private static final ST SUM_LE_TEMPLATE = AGGREGATE_ENCODINGS.getInstanceOf("sum_le");
	private static final ST SUM_EQ_TEMPLATE = AGGREGATE_ENCODINGS.getInstanceOf("sum_eq");

	private SumEncoder(ComparisonOperator acceptedOperator, ST encodingTemplate) {
		super(AggregateFunctionSymbol.SUM, acceptedOperator, encodingTemplate);
	}

	public static SumEncoder buildSumLessOrEqualEncoder() {
		return new SumEncoder(ComparisonOperator.LE, SUM_LE_TEMPLATE);
	}

	public static SumEncoder buildSumEqualsEncoder() {
		return new SumEncoder(ComparisonOperator.EQ, SUM_EQ_TEMPLATE);
	}

	/**
	 * In contrast to encoders for other aggregate functions, the "element tuple" atom for sum encodings is ternary - apart
	 * from the aggregate arguments and the variable tuple identifying the aggregated element, it also holds the value to
	 * add to the result sum as its third argument.
	 */
	@Override
	protected Atom buildElementRuleHead(String aggregateId, AggregateElement element, Term aggregateArguments) {
		Predicate headPredicate = Predicate.getInstance(this.getElementTuplePredicateSymbol(aggregateId), 3);
		FunctionTerm elementTuple = FunctionTerm.getInstance(AbstractAggregateEncoder.ELEMENT_TUPLE_FUNCTION_SYMBOL, element.getElementTerms());
		return new BasicAtom(headPredicate, aggregateArguments, elementTuple, element.getElementTerms().get(0));
	}

}