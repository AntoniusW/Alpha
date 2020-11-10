package at.ac.tuwien.kr.alpha.grounder.transformation;

import org.apache.commons.collections4.SetUtils;
import org.stringtemplate.v4.ST;

import java.util.Collections;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateElement;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.grounder.transformation.AggregateRewritingContext.AggregateInfo;

public class CountEqualsAggregateEncoder extends AbstractAggregateEncoder {

	private static final String ELEMENT_TUPLE_FUNCTION_SYMBOL = "tuple";

	//@formatter:off
	private static final ST CNT_EQ_LITERAL_ENCODING = Util.aspStringTemplate(
				"#enumeration_predicate_is $enumeration$."
				+ "$aggregate_result$(ARGS, VAL) :- $leq$(ARGS, VAL), not $leq$(ARGS, NEXTVAL), NEXTVAL = VAL + 1."
				+ "$leq$($aggregate_arguments$, $value_var$) :- $value_leq_cnt_lit$, $cnt_candidate_lit$."
				+ "$cnt_candidate$(ARGS, ORDINAL) :- $element_tuple$(ARGS, TUPLE), $enumeration$(ARGS, TUPLE, ORDINAL).");
	//@formatter:on

	private final ProgramParser parser = new ProgramParser();

	public CountEqualsAggregateEncoder() {
		super(AggregateFunctionSymbol.COUNT, SetUtils.hashSet(ComparisonOperator.EQ));
	}

	@Override
	// TODO look into generalizing this!
	protected InputProgram encodeAggregateResult(AggregateInfo aggregateToEncode, AggregateRewritingContext ctx) {
		String aggregateId = aggregateToEncode.getId();
		AggregateLiteral lit = aggregateToEncode.getLiteral();
		AggregateAtom sourceAtom = lit.getAtom();
		VariableTerm valueVar = VariableTerm.getInstance("CNT");
		// Build a new AggregateLiteral representing the "CNT <= #count{...}" part of the encoding.
		AggregateLiteral candidateLeqCount = new AggregateLiteral(
				new AggregateAtom(ComparisonOperator.LE, valueVar, AggregateFunctionSymbol.COUNT, sourceAtom.getAggregateElements()), true);
		String cntCandidatePredicateSymbol = aggregateId + "_candidate";
		BasicLiteral cntCandidate = new BasicLiteral(
				new BasicAtom(Predicate.getInstance(cntCandidatePredicateSymbol, 2), aggregateToEncode.getAggregateArguments(), valueVar), true);
		// Create an encoder for the newly built " <= #count{..}" literal and rewrite it.
		// Note that the literal itself is not written into the encoding of the original literal,
		// but only its substitute "aggregate result" literal.
		AggregateRewritingContext candidateLeqCountCtx = new AggregateRewritingContext(ctx);
		String candidateLeqCntId = candidateLeqCountCtx.registerAggregateLiteral(candidateLeqCount, Collections.singleton(cntCandidate));
		// The encoder won't encode AggregateElements of the newly created literal separately but alias them
		// with the element encoding predicates for the original literal.
		AbstractAggregateEncoder candidateLeqEncoder = new CountLessOrEqualDelegateAggregateEncoder(aggregateId);
		InputProgram candidateLeqEncoding = candidateLeqEncoder
				.encodeAggregateLiteral(candidateLeqCountCtx.getAggregateInfo(candidateLeqCntId), candidateLeqCountCtx);
		// Create a fresh template to make sure attributes are empty at each call to encodeAggregateResult.
		ST encodingTemplate = new ST(CNT_EQ_LITERAL_ENCODING);
		encodingTemplate.add("aggregate_result", aggregateToEncode.getOutputAtom().getPredicate().getName());
		encodingTemplate.add("leq", aggregateId + "_leq");
		encodingTemplate.add("cnt_candidate", cntCandidatePredicateSymbol);
		encodingTemplate.add("value_var", valueVar.toString());
		encodingTemplate.add("value_leq_cnt_lit", candidateLeqCountCtx.getAggregateInfo(candidateLeqCntId).getOutputAtom().toString());
		encodingTemplate.add("cnt_candidate_lit", cntCandidate.toString());
		encodingTemplate.add("element_tuple", aggregateId + "_element_tuple");
		encodingTemplate.add("enumeration", aggregateId + "_enum");
		encodingTemplate.add("aggregate_arguments", aggregateToEncode.getAggregateArguments());
		String resultEncodingAsp = encodingTemplate.render();
		InputProgram resultEncoding = PredicateInternalizer.makePrefixedPredicatesInternal(new EnumerationRewriting().apply(parser.parse(resultEncodingAsp)),
				candidateLeqCntId);
		return InputProgram.builder(resultEncoding).accumulate(candidateLeqEncoding).build();
	}

	@Override
	protected Atom buildElementRuleHead(String aggregateId, AggregateElement element, AggregateRewritingContext ctx) {
		AggregateInfo aggregate = ctx.getAggregateInfo(aggregateId);
		Term aggregateArguments = aggregate.getAggregateArguments();
		FunctionTerm elementTuple = FunctionTerm.getInstance(ELEMENT_TUPLE_FUNCTION_SYMBOL, element.getElementTerms());
		return new BasicAtom(Predicate.getInstance(aggregateId + "_element_tuple", 2), aggregateArguments, elementTuple);
	}

}