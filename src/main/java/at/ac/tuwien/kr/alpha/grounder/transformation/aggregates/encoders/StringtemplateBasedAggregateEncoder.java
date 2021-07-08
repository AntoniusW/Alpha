package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.grounder.transformation.EnumerationRewriting;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.AggregateRewritingContext.AggregateInfo;
import org.apache.commons.collections4.ListUtils;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Abstract base class for aggregate encoders making use of stringtemplates in their rewriting workflow.
 * The base implementation encodes the core (i.e. aggregate element-independent) part of an aggregate literal's encoding
 * by way of rendering a stringtemplate that is expected to have the arguments: "id" (the aggregate id to encode),
 * "result_predicate" (the symbol of the predicate by which the respective predicate got replaced in it's source rule),
 * "element_tuple" (the predicate name for tuples to aggregate over) and "bound" (the predicate name of the bound atom
 * to use in the encoding).
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public abstract class StringtemplateBasedAggregateEncoder extends AbstractAggregateEncoder {

	private final ProgramParser parser = new ProgramParser();
	private final ST encodingTemplate;
	private final boolean needsBoundRule;

	protected StringtemplateBasedAggregateEncoder(AggregateFunctionSymbol aggregateFunctionToEncode, ComparisonOperator acceptedOperator, ST encodingTemplate) {
		super(aggregateFunctionToEncode, Collections.singleton(acceptedOperator));
		this.encodingTemplate = encodingTemplate;
		if (acceptedOperator == ComparisonOperator.EQ) {
			this.needsBoundRule = false;
		} else if (acceptedOperator == ComparisonOperator.LE) {
			this.needsBoundRule = true;
		} else {
			throw new IllegalArgumentException("This encoder is incompatible with comparison operator: " + acceptedOperator);
		}
	}

	@Override
	protected InputProgram encodeAggregateResult(AggregateInfo aggregateToEncode) {
		String aggregateId = aggregateToEncode.getId();

		/*
		 * Create a rule deriving a "bound" value for the core aggregate encoding.
		 * The bound is (in case of encodings for "<=" comparisons) the value that should be tested for being a lower bound, or
		 * else zero.
		 */
		BasicRule boundRule = null;
		if (this.needsBoundRule) {
			boundRule = this.buildBoundRule(aggregateToEncode);
		} else {
			/*
			 * Even if we don't have to create a bound rule because the aggregate encoding generates its own candidate values,
			 * we still generate a rule deriving zero as a bound, so that sums and counts over empty sets correctly return 0.
			 */
			boundRule = this.buildZeroBoundRule(aggregateToEncode);
		}

		// Generate encoding
		ST coreEncodingTemplate = new ST(this.encodingTemplate);
		coreEncodingTemplate.add("result_predicate", aggregateToEncode.getOutputAtom().getPredicate().getName());
		coreEncodingTemplate.add("id", aggregateId);
		coreEncodingTemplate.add("element_tuple", this.getElementTuplePredicateSymbol(aggregateId));
		coreEncodingTemplate.add("bound", this.getBoundPredicateName(aggregateId));
		String coreEncodingAsp = coreEncodingTemplate.render();

		// Create the basic program
		InputProgram coreEncoding = new EnumerationRewriting().apply(parser.parse(coreEncodingAsp));

		// Add the programatically created bound rule and return
		return new InputProgram(ListUtils.union(coreEncoding.getRules(), Collections.singletonList(boundRule)), coreEncoding.getFacts(),
				new InlineDirectives());
	}

	private String getBoundPredicateName(String aggregateId) {
		return aggregateId + "_bound";
	}

	private BasicRule buildZeroBoundRule(AggregateInfo aggregateToEncode) {
		BasicAtom bound = new BasicAtom(Predicate.getInstance(getBoundPredicateName(aggregateToEncode.getId()), 2),
				aggregateToEncode.getAggregateArguments(), ConstantTerm.getInstance(0));
		return new BasicRule(new NormalHead(bound), new ArrayList<>(aggregateToEncode.getDependencies()));
	}

	private BasicRule buildBoundRule(AggregateInfo aggregateToEncode) {
		BasicAtom bound = new BasicAtom(Predicate.getInstance(getBoundPredicateName(aggregateToEncode.getId()), 2),
				aggregateToEncode.getAggregateArguments(), aggregateToEncode.getLiteral().getAtom().getLowerBoundTerm());
		return new BasicRule(new NormalHead(bound), new ArrayList<>(aggregateToEncode.getDependencies()));
	}

}
