package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.*;
import at.ac.tuwien.kr.alpha.common.atoms.*;
import at.ac.tuwien.kr.alpha.common.rule.head.impl.DisjunctiveHead;
import at.ac.tuwien.kr.alpha.common.rule.impl.Rule;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Substitution;
import at.ac.tuwien.kr.alpha.grounder.Unifier;
import at.ac.tuwien.kr.alpha.grounder.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;

import java.util.*;

/**
 * Copyright (c) 2017, the Alpha Team.
 */
public class CardinalityNormalization implements ProgramTransformation {

	private int aggregateCount;
	private ProgramParser parser = new ProgramParser();
	private final boolean useSortingCircuitEncoding;

	public CardinalityNormalization() {
		this(true);
	}

	public CardinalityNormalization(boolean useSortingCircuitEncoding) {
		this.useSortingCircuitEncoding = useSortingCircuitEncoding;
	}

	private Program parse(String program) {
		return parser.parse(program);
	}

	@Override
	public void transform(Program inputProgram) {
		String cardinalityCountingGrid =
			"span(R,1..I1) :- I1 = I-1, sorting_network_input_number(R,I).\n" +
			"sum(R,0,0)    :- sorting_network_input_number(R,_).\n" +
			"sum(R,I,S)    :- sum(R,I1,S), I1 = I-1, span(R,I).\n" +
			"sum(R,I,S1)   :- sum(R,I1,S),S1 = S+1, I1 = I-1, sorting_network_input_number(R,I),\n" +
			"                  sorting_network_bound(R,K), S < K.\n" +
			"sorting_network_output(R,K) :- sorting_network_bound(R,K), K <= S, sum(R,_,S).\n";

		// Transforms all cardinality-aggregates into normal logic rules employing a lazy-grounded sorting circuit.
		String cardinalitySortingCircuit =
			"sorting_network_wire_value(R, I, D) :- sorting_network_input_number(R, I), D = 0.\n" +
			"sorting_network_wire_value(R, I, D) :- sorting_network_wire_value(R, I, D1), D1 = D - 1, sorting_network_comparator(I, _, D), sorting_network_relevant_depth(R, D).\n" +
			"sorting_network_wire_value(R, I, D) :- sorting_network_wire_value(R, J, D1), D1 = D - 1, sorting_network_comparator(I, J, D), sorting_network_relevant_depth(R, D).\n" +
			"sorting_network_wire_value(R, J, D) :- sorting_network_wire_value(R, I, D1), D1 = D - 1, sorting_network_wire_value(R, J, D1), sorting_network_comparator(I, J, D), sorting_network_relevant_depth(R, D).\n" +
			"sorting_network_wire_value(R, I, D) :- sorting_network_wire_value(R, I, D1), D1 = D - 1, sorting_network_passthrough(I, D), sorting_network_relevant_depth(R, D).\n" +
			"sorting_network_input_range(R, 1..I) :- sorting_network_input_number(R, I).\n" +
			"sorting_network_relevant_depth(R, D) :- sorting_network_odd_even_level(R, _, _, D).\n" +
			"sorting_network_part(R, G) :- sorting_network_input_range(R, I), I1 = I - 1, G = G1 + 1, sorting_network_log2(I1, G1).\n" +
			"sorting_network_output(R, K) :- sorting_network_bound(R, K), sorting_network_wire_value(R, K, D), sorting_network_sorted_count(N, D), K <= N.\n" +
			"sorting_network_output(R, K) :- sorting_network_bound(R, K), K <= 0.\n" +
			"sorting_network_odd_even_level(R, 1, 1, 1) :- sorting_network_part(R, 1).\n" +
			"sorting_network_odd_even_level(R, L, P1, DL) :- P1 = P + 1, L = 1..P1, DL = D + L, sorting_network_odd_even_level(R, P, P, D), sorting_network_part(R, P1).\n" +
			"sorting_network_odd_even_comparator(1, P, I, J) :- sorting_network_odd_even_level(_, 1, P, _), sorting_network_input_range(_, I), I < J, J = ((I - 1) ^ 2 ** (P - 1)) + 1.\n" +
			"sorting_network_odd_even_comparator(L, P, I, J) :- sorting_network_odd_even_level(_, L, P, _), sorting_network_input_range(_, I), J = I + S, 1 < L, N != 0, N != B - 1, N \\ 2 = 1, N = (I - 1) / S - ((I - 1) / S / B) * B, S = 2 ** (P - L), B = 2 ** L.\n" +
			"sorting_network_odd_even_passthrough(L, P, I) :- sorting_network_odd_even_level(_, L, P, _), sorting_network_input_range(_, I), 1 < L, N = 0, N = (I - 1) / S - ((I - 1) / S / B) * B, S = 2 ** (P - L), B = 2 ** L.\n" +
			"sorting_network_odd_even_passthrough(L, P, I) :- sorting_network_odd_even_level(_, L, P, _), sorting_network_input_range(_, I), 1 < L, N = B - 1, N = (I - 1) / S - ((I - 1) / S / B) * B, S = 2 ** (P - L), B = 2 ** L.\n" +
			"sorting_network_comparator(I, J, D) :- sorting_network_odd_even_comparator(L, P, I, J), sorting_network_odd_even_level(_, L, P, D).\n" +
			"sorting_network_passthrough(I, D) :- sorting_network_odd_even_passthrough(L, P, I), sorting_network_odd_even_level(_, L, P, D).\n" +
			"sorting_network_sorted_count(1, 0).\n" +
			"sorting_network_sorted_count(N, D) :- sorting_network_log2(N, P), sorting_network_odd_even_level(_, P, P, D).\n" +
			"sorting_network_log2(Ip2, I) :- Ip2 = 2 ** I, I = 0..30.\n";

		// Connect/Rewrite every aggregate in each rule.
		ArrayList<Rule> additionalRules = new ArrayList<>();
		for (Rule rule : inputProgram.getRules()) {
			additionalRules.addAll(rewriteAggregates(rule));
		}
		// Leave program as-is if no aggregates occur.
		if (additionalRules.isEmpty()) {
			return;
		}
		String usedCardinalityEncoding = useSortingCircuitEncoding ? cardinalitySortingCircuit : cardinalityCountingGrid;
		Program cardinalityEncoding = PredicateInternalizer.makePredicatesInternal(new ProgramParser().parse(usedCardinalityEncoding));
		cardinalityEncoding.accumulate(additionalRules);

		// Add enumeration rule that uses the special EnumerationAtom.
		// The enumeration rule is: "sorting_network_input_number(A, I) :- sorting_network_input(A, X), sorting_network_index(A, X, I)."
		Rule enumerationRule = PredicateInternalizer.makePredicatesInternal(parse("sorting_network_input_number(A, I) :- sorting_network_input(A, X).")).getRules().get(0);
		EnumerationAtom enumerationAtom = new EnumerationAtom(parse("sorting_network_index(A, X, I).").getFacts().get(0).getTerms());
		enumerationRule.getBody().add(enumerationAtom.toLiteral());
		cardinalityEncoding.accumulate(enumerationRule);

		// Add cardinality encoding to program.
		inputProgram.accumulate(cardinalityEncoding);
	}

	private List<Rule> rewriteAggregates(Rule rule) {

		// Example rewriting/connection:
		// num(K) :-  K <= #count {X,Y,Z : p(X,Y,Z) }, dom(K).
		// is rewritten into:
		// num(K) :- sorting_network_output(aggregate_arguments(-731776545), K), dom(K).
		// sorting_network_input(aggregate_arguments(-731776545), element_tuple(X, Y, Z)) :- p(X, Y, Z).
		// sorting_network_bound(aggregate_arguments(-731776545), K) :- dom(K).

		// Create interface atoms to the aggregate encoding.
		final BasicAtom aggregateOutputAtom = (BasicAtom) PredicateInternalizer.makePredicatesInternal(parse(
			"sorting_network_output(aggregate_arguments(AGGREGATE_ID), LOWER_BOUND).")).getFacts().get(0);
		final BasicAtom aggregateInputAtom = (BasicAtom) PredicateInternalizer.makePredicatesInternal(parse(
			"sorting_network_input(aggregate_arguments(AGGREGATE_ID), ELEMENT_TUPLE).")).getFacts().get(0);
		final BasicAtom lowerBoundAtom = (BasicAtom) PredicateInternalizer.makePredicatesInternal(parse(
			"sorting_network_bound(aggregate_arguments(AGGREGATE_ID), LOWER_BOUND).")).getFacts().get(0);

		ArrayList<Literal> aggregateOutputAtoms = new ArrayList<>();
		int aggregatesInRule = 0;	// Only needed for limited rewriting.
		ArrayList<Rule> additionalRules = new ArrayList<>();

		for (Iterator<Literal> iterator = rule.getBody().iterator(); iterator.hasNext();) {
			Literal bodyElement = iterator.next();
			// Skip non-aggregates.
			if (!(bodyElement instanceof AggregateLiteral)) {
				continue;
			}
			AggregateLiteral aggregateLiteral = (AggregateLiteral) bodyElement;
			AggregateAtom aggregateAtom = aggregateLiteral.getAtom();

			// Check that aggregate is limited to what we currently can deal with.
			if (aggregateLiteral.isNegated() || aggregateAtom.getUpperBoundOperator() != null
				|| (aggregateAtom.getAggregatefunction() != AggregateAtom.AGGREGATEFUNCTION.COUNT
					&& aggregateAtom.getAggregatefunction() != AggregateAtom.AGGREGATEFUNCTION.SUM)
				|| aggregatesInRule++ > 0) {
				throw new UnsupportedOperationException("Only limited #count/#sum aggregates without upper bound are currently supported." +
					"No rule may have more than one aggregate.");
			}

			// Only treat count aggregates.
			if (aggregateAtom.getAggregatefunction() != AggregateAtom.AGGREGATEFUNCTION.COUNT) {
				continue;
			}
			// Remove aggregate from rule body.
			iterator.remove();

			// Prepare aggregate parameters.
			aggregateCount++;
			Substitution aggregateSubstitution = new Unifier();
			Collection<Term> globalVariables = getGlobalVariables(rule, aggregateAtom);
			if (globalVariables.isEmpty()) {
				aggregateSubstitution.put(VariableTerm.getInstance("AGGREGATE_ID"), ConstantTerm.getInstance(aggregateCount));
			} else {
				// In case some variables are not local to the aggregate, add them to the aggregate identifier
				ArrayList<Term> globalVariableTermlist = new ArrayList<>(globalVariables);
				globalVariableTermlist.add(ConstantTerm.getInstance(aggregateCount));
				aggregateSubstitution.put(VariableTerm.getInstance("AGGREGATE_ID"), FunctionTerm.getInstance("agg", globalVariableTermlist));
			}
			aggregateSubstitution.put(VariableTerm.getInstance("LOWER_BOUND"), aggregateAtom.getLowerBoundTerm());

			// Create new output atom for addition to rule body instead of the aggregate.
			aggregateOutputAtoms.add(aggregateOutputAtom.substitute(aggregateSubstitution).toLiteral());

			// Create input to sorting network from aggregate elements.
			for (AggregateAtom.AggregateElement aggregateElement : aggregateAtom.getAggregateElements()) {
				// Prepare element substitution.
				List<Term> elementTerms = aggregateElement.getElementTerms();
				FunctionTerm elementTuple = FunctionTerm.getInstance("element_tuple", elementTerms);
				Substitution elementSubstitution = new Unifier(aggregateSubstitution);
				elementSubstitution.put(VariableTerm.getInstance("ELEMENT_TUPLE"), elementTuple);

				// Create new rule for input.
				BasicAtom inputHeadAtom = aggregateInputAtom.substitute(elementSubstitution);
				List<Literal> elementLiterals = new ArrayList<>(aggregateElement.getElementLiterals());

				// If there are global variables used inside the aggregate, add original rule body (minus the aggregate itself) to input rule.
				if (!globalVariables.isEmpty()) {
					elementLiterals.addAll(rule.getBody());
				}
				Rule inputRule = new Rule(new DisjunctiveHead(Collections.singletonList(inputHeadAtom)), elementLiterals);
				additionalRules.add(inputRule);
			}

			// Create lower bound for the aggregate.
			BasicAtom lowerBoundHeadAtom = lowerBoundAtom.substitute(aggregateSubstitution);
			List<Literal> lowerBoundBody = rule.getBody();	// Note: this is only correct if no other aggregate occurs in the rule.
			additionalRules.add(new Rule(new DisjunctiveHead(Collections.singletonList(lowerBoundHeadAtom)), lowerBoundBody));

		}
		rule.getBody().addAll(aggregateOutputAtoms);
		return additionalRules;
	}

	static Collection<Term> getGlobalVariables(Rule rule, AggregateAtom aggregateAtom) {
		// Hacky way to get all global variables: take all variables inside the aggregate that occur also in the rest of the rule.
		HashSet<Term> occurringVariables = new HashSet<>();
		for (Literal element : rule.getBody()) {
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
