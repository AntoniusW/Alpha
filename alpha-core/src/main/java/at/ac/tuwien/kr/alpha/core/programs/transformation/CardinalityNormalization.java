package at.ac.tuwien.kr.alpha.core.programs.transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.AggregateLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.rules.Head;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.api.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.substitutions.Unifier;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.programs.InputProgram;
import at.ac.tuwien.kr.alpha.core.rules.BasicRule;
import at.ac.tuwien.kr.alpha.core.rules.heads.NormalHeadImpl;

/**
 * Copyright (c) 2017-2020, the Alpha Team.
 */
public class CardinalityNormalization extends ProgramTransformation<ASPCore2Program, ASPCore2Program> {

	private int aggregateCount;
	private ProgramParserImpl parser = new ProgramParserImpl();
	private final boolean useSortingCircuitEncoding;

	public CardinalityNormalization() {
		this(true);
	}

	public CardinalityNormalization(boolean useSortingCircuitEncoding) {
		this.useSortingCircuitEncoding = useSortingCircuitEncoding;
	}

	private ASPCore2Program parse(String program) {
		return parser.parse(program);
	}

	@Override
	public ASPCore2Program apply(ASPCore2Program inputProgram) {
		if (!this.rewritingNecessary(inputProgram)) {
			return inputProgram;
		}
		InputProgram.Builder programBuilder = InputProgram.builder();
		programBuilder.addFacts(inputProgram.getFacts());
		programBuilder.addInlineDirectives(inputProgram.getInlineDirectives());
		//@formatter:off
		String cardinalityCountingGrid =
			"span(R,1..I1) :- I1 = I-1, sorting_network_input_number(R,I).\n" +
			"sum(R,0,0)    :- sorting_network_input_number(R,_).\n" +
			"sum(R,I,S)    :- sum(R,I1,S), I1 = I-1, span(R,I).\n" +
			"sum(R,I,S1)   :- sum(R,I1,S),S1 = S+1, I1 = I-1, sorting_network_input_number(R,I),\n" +
			"                  sorting_network_bound(R,K), S < K.\n" +
			"sorting_network_output(R,K) :- sorting_network_bound(R,K), K <= S, sum(R,_,S).\n";

		// Transforms all cardinality-aggregates into normal logic rules employing a lazy-grounded sorting circuit.
		String phi = "N = (I - 1) / S - ((I - 1) / S / B) * B, S = 2 ** (P - L), B = 2 ** L";
		String cardinalitySortingCircuit =
			"sorting_network_span(R,I) :- sorting_network_input_number(R,I).\n" +
			"sorting_network_span(R,Im1) :- sorting_network_span(R,I), 1<I, Im1=I-1.\n" +
			"sorting_network_v(R,I,D) :- sorting_network_input_number(R,I), D=0.\n" +
			"sorting_network_v(R,I,D) :- sorting_network_v(R,I,D1), D1=D-1, sorting_network_comp(I,_,D), sorting_network_dh(R,D).\n" +
			"sorting_network_v(R,I,D) :- sorting_network_v(R,J,D1), D1=D-1, sorting_network_comp(I,J,D), sorting_network_dh(R,D).\n" +
			"sorting_network_v(R,J,D) :- sorting_network_v(R,I,D1), D1=D-1, sorting_network_comp(I,J,D), sorting_network_dh(R,D), sorting_network_v(R,J,D1).\n" +
			"sorting_network_v(R,I,D) :- sorting_network_v(R,I,D1), D1=D-1, sorting_network_pass(I,D), sorting_network_dh(R,D).\n" +
			"sorting_network_output(R,K) :- sorting_network_bound(R,K), sorting_network_v(R,K,D), sorting_network_done(N,D), K<=N.\n" +
			"sorting_network_output(R,K) :- sorting_network_bound(R,K), K<=0.\n" +

			"sorting_network_span_project(I) :- sorting_network_span(_,I).\n" +
			"sorting_network_part(P) :- sorting_network_span_project(I), Im1=I-1, sorting_network_log2(Im1,P1), P=P1+1.\n" +
			"sorting_network_lvl(1,1,1) :- sorting_network_part(1).\n" +
			"sorting_network_lvl(L,P1,DL) :- sorting_network_lvl(P,P,D), P1=P+1, sorting_network_part(P1), L=1..P1, DL=D+L.\n" +
			"sorting_network_comp(I,J,D) :- sorting_network_lvl(1,P,D), sorting_network_span_project(I), I<J, J=((I-1)^(2**(P-1)))+1.\n" +
			"sorting_network_comp(I,J,D) :- sorting_network_lvl(L,P,D), sorting_network_span_project(I), J=I+S, 1<L, N!=0, N!=B-1, N \\ 2 = 1, " + phi + ".\n" +
			"sorting_network_pass(I,D) :- sorting_network_lvl(L,P,D), sorting_network_span_project(I), 1<L, N=0, " + phi + ".\n" +
			"sorting_network_pass(I,D) :- sorting_network_lvl(L,P,D), sorting_network_span_project(I), 1<L, N=B-1, " + phi + ".\n" +
			"sorting_network_dh(R,1..D) :- sorting_network_span(R,N1), N1=N+1, sorting_network_done(N,_), N2=N*2, sorting_network_done(N2,D).\n" +
			"sorting_network_done(N,D) :- sorting_network_log2(N,P), sorting_network_lvl(P,P,D).\n" +
			"sorting_network_done(1,0).\n" +
			"sorting_network_log2(Ip2, I) :- Ip2 = 2 ** I, I = 0..30.";
		//@formatter:on

		// Connect/Rewrite every aggregate in each rule.
		List<Rule<Head>> rewrittenRules = rewriteAggregates(inputProgram.getRules());

		String usedCardinalityEncoding = useSortingCircuitEncoding ? cardinalitySortingCircuit : cardinalityCountingGrid;
		ASPCore2Program cardinalityEncoding = PredicateInternalizer.makePredicatesInternal(new ProgramParserImpl().parse(usedCardinalityEncoding));
		programBuilder.addRules(rewrittenRules);

		// Add enumeration rule that uses the special EnumerationAtom.
		// The enumeration rule is: "sorting_network_input_number(A, I) :- sorting_network_input(A, X),
		// sorting_network_index(A, X, I)."
		Rule<Head> tmpEnumRule = PredicateInternalizer.makePredicatesInternal(parse(
				"sorting_network_input_number(A, I) :- sorting_network_input(A, X).")).getRules().get(0);
		EnumerationAtom enumerationAtom = new EnumerationAtom(Terms.newVariable("A"), Terms.newVariable("X"), Terms.newVariable("I"));
		List<Literal> enumerationRuleBody = new ArrayList<>(tmpEnumRule.getBody());
		enumerationRuleBody.add(enumerationAtom.toLiteral());
		BasicRule enumerationRule = new BasicRule(tmpEnumRule.getHead(), enumerationRuleBody);
		programBuilder.addRule(enumerationRule);

		// Add cardinality encoding to program.
		programBuilder.accumulate(cardinalityEncoding);
		return programBuilder.build();
	}

	/**
	 * Checks if rewriting of count aggregates is necessary for the given program, i.e. if such aggregates exist.
	 * 
	 * @param program the program.
	 * @return true if count aggregates occur, false otherwise.
	 */
	private boolean rewritingNecessary(ASPCore2Program program) {
		for (Rule<Head> rule : program.getRules()) {
			for (Literal lit : rule.getBody()) {
				if (lit instanceof AggregateLiteral) {
					AggregateAtom aggregateAtom = ((AggregateLiteral) lit).getAtom();
					if (aggregateAtom.getAggregateFunction() == AggregateAtom.AggregateFunction.COUNT) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private List<Rule<Head>> rewriteAggregates(List<Rule<Head>> srcRules) {
		List<Rule<Head>> retVal = new ArrayList<>();
		for (Rule<Head> rule : srcRules) {
			retVal.addAll(rewriteAggregatesInRule(rule));
		}
		return retVal;
	}

	private List<Rule<Head>> rewriteAggregatesInRule(Rule<Head> rule) {
		// Example rewriting/connection:
		// num(K) :- K <= #count {X,Y,Z : p(X,Y,Z) }, dom(K).
		// is rewritten into:
		// num(K) :- sorting_network_output(aggregate_arguments(-731776545), K), dom(K).
		// sorting_network_input(aggregate_arguments(-731776545), element_tuple(X, Y, Z)) :- p(X, Y, Z).
		// sorting_network_bound(aggregate_arguments(-731776545), K) :- dom(K).

		// Create interface atoms to the aggregate encoding.
		final BasicAtom aggregateOutputAtom = (BasicAtom) PredicateInternalizer
				.makePredicatesInternal(parse("sorting_network_output(aggregate_arguments(AGGREGATE_ID), LOWER_BOUND).")).getFacts().get(0);
		final BasicAtom aggregateInputAtom = (BasicAtom) PredicateInternalizer
				.makePredicatesInternal(parse("sorting_network_input(aggregate_arguments(AGGREGATE_ID), ELEMENT_TUPLE).")).getFacts().get(0);
		final BasicAtom lowerBoundAtom = (BasicAtom) PredicateInternalizer
				.makePredicatesInternal(parse("sorting_network_bound(aggregate_arguments(AGGREGATE_ID), LOWER_BOUND).")).getFacts().get(0);

		ArrayList<Literal> aggregateOutputAtoms = new ArrayList<>();
		int aggregatesInRule = 0; // Only needed for limited rewriting.
		ArrayList<Rule<Head>> additionalRules = new ArrayList<>();

		List<Literal> rewrittenBody = new ArrayList<>(rule.getBody());

		for (Iterator<Literal> iterator = rewrittenBody.iterator(); iterator.hasNext();) {
			Literal bodyElement = iterator.next();
			// Skip non-aggregates.
			if (!(bodyElement instanceof AggregateLiteral)) {
				continue;
			}
			AggregateLiteral aggregateLiteral = (AggregateLiteral) bodyElement;
			AggregateAtom aggregateAtom = aggregateLiteral.getAtom();

			// Check that aggregate is limited to what we currently can deal with.
			if (aggregateLiteral.isNegated() || aggregateAtom.getUpperBoundOperator() != null
					|| (aggregateAtom.getAggregateFunction() != AggregateAtom.AggregateFunction.COUNT
							&& aggregateAtom.getAggregateFunction() != AggregateAtom.AggregateFunction.SUM)
					|| aggregatesInRule++ > 0) {
				throw new UnsupportedOperationException(
						"Only limited #count/#sum aggregates without upper bound are currently supported." + "No rule may have more than one aggregate.");
			}

			// Only treat count aggregates.
			if (aggregateAtom.getAggregateFunction() != AggregateAtom.AggregateFunction.COUNT) {
				continue;
			}
			// Remove aggregate from rule body.
			iterator.remove();

			// Prepare aggregate parameters.
			aggregateCount++;
			Substitution aggregateSubstitution = new Unifier();
			Collection<Term> globalVariables = getGlobalVariables(rewrittenBody, aggregateAtom);
			if (globalVariables.isEmpty()) {
				aggregateSubstitution.put(Terms.newVariable("AGGREGATE_ID"), Terms.newConstant(aggregateCount));
			} else {
				// In case some variables are not local to the aggregate, add them to the aggregate identifier
				ArrayList<Term> globalVariableTermlist = new ArrayList<>(globalVariables);
				globalVariableTermlist.add(Terms.newConstant(aggregateCount));
				aggregateSubstitution.put(Terms.newVariable("AGGREGATE_ID"), Terms.newFunctionTerm("agg", globalVariableTermlist));
			}
			aggregateSubstitution.put(Terms.newVariable("LOWER_BOUND"), aggregateAtom.getLowerBoundTerm());

			// Create new output atom for addition to rule body instead of the aggregate.
			aggregateOutputAtoms.add(aggregateOutputAtom.substitute(aggregateSubstitution).toLiteral());

			// Create input to sorting network from aggregate elements.
			for (AggregateAtom.AggregateElement aggregateElement : aggregateAtom.getAggregateElements()) {
				// Prepare element substitution.
				List<Term> elementTerms = aggregateElement.getElementTerms();
				FunctionTerm elementTuple = Terms.newFunctionTerm("element_tuple", elementTerms);
				Substitution elementSubstitution = new Unifier(aggregateSubstitution);
				elementSubstitution.put(Terms.newVariable("ELEMENT_TUPLE"), elementTuple);

				// Create new rule for input.
				Atom inputHeadAtom = aggregateInputAtom.substitute(elementSubstitution);
				List<Literal> elementLiterals = new ArrayList<>(aggregateElement.getElementLiterals());

				// If there are global variables used inside the aggregate, add original rule body
				// (minus the aggregate itself) to input rule.
				if (!globalVariables.isEmpty()) {
					elementLiterals.addAll(rewrittenBody);
				}
				BasicRule inputRule = new BasicRule(new NormalHeadImpl(inputHeadAtom), elementLiterals);
				additionalRules.add(inputRule);
			}

			// Create lower bound for the aggregate.
			Atom lowerBoundHeadAtom = lowerBoundAtom.substitute(aggregateSubstitution);
			List<Literal> lowerBoundBody = rewrittenBody; // Note: this is only correct if no other aggregate occurs in the rule.
			additionalRules.add(new BasicRule(new NormalHeadImpl(lowerBoundHeadAtom), lowerBoundBody));

		}
		rewrittenBody.addAll(aggregateOutputAtoms);
		Rule<Head> rewrittenSrcRule = new BasicRule(rule.getHead(), rewrittenBody);
		additionalRules.add(rewrittenSrcRule);
		return additionalRules;
	}

	static Collection<Term> getGlobalVariables(List<Literal> ruleBody, AggregateAtom aggregateAtom) {
		// Hacky way to get all global variables: take all variables inside the aggregate that occur also in the
		// rest of the rule.
		HashSet<Term> occurringVariables = new LinkedHashSet<>();
		for (Literal element : ruleBody) {
			if (element instanceof AggregateLiteral) {
				continue;
			}
			occurringVariables.addAll(element.getBindingVariables());
			occurringVariables.addAll(element.getNonBindingVariables());
		}
		LinkedHashSet<Term> globalVariables = new LinkedHashSet<>();
		for (Term aggVariable : aggregateAtom.getAggregateVariables()) {
			if (occurringVariables.contains(aggVariable)) {
				globalVariables.add(aggVariable);
			}
		}
		return globalVariables;
	}
}
