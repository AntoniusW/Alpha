package at.ac.tuwien.kr.alpha.core.programs.transformation;

import static at.ac.tuwien.kr.alpha.core.programs.transformation.PredicateInternalizer.makePredicatesInternal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
 * Rewrites #sum aggregates into normal rules.
 * Note: Currently only works for a restricted form.
 *
 * Copyright (c) 2018-2020, the Alpha Team.
 */
public class SumNormalization extends ProgramTransformation<ASPCore2Program, ASPCore2Program> {

	private int aggregateCount;
	private ProgramParserImpl parser = new ProgramParserImpl();

	private ASPCore2Program parse(String program) {
		return parser.parse(program);
	}

	@Override
	public ASPCore2Program apply(ASPCore2Program inputProgram) {
		if (!rewritingNecessary(inputProgram)) {
			return inputProgram;
		}
		String summationSubprogram = "interesting_number(R, 1..I1) :- input_number_with_first(R, I, _), I1 = I - 1.\n"
				+ "prefix_subset_sum(R, 0, 0) :- input_number_with_first(R, _, _).\n"
				+ "prefix_subset_sum(R, I, S) :- prefix_subset_sum(R, I1, S), I1 = I - 1, interesting_number(R, I).\n"
				+ "prefix_subset_sum(R, I, SF) :- prefix_subset_sum(R, I1, S), I1 = I - 1, SF = S + F, input_number_with_first(R, I, F),  bound(R, K), SF < K.\n"
				+ "output(R, K) :- bound(R, K), K <= 0."
				+ "output(R, K) :- prefix_subset_sum(R, I1, S), I1 = I - 1, input_number_with_first(R, I, F), bound(R, K), K <= S + F.";

		// Connect/Rewrite every aggregate in each rule.
		List<Rule<Head>> rewrittenRules = rewriteAggregates(inputProgram.getRules());

		InputProgram.Builder prgBuilder = InputProgram.builder();
		prgBuilder.addFacts(inputProgram.getFacts());
		ASPCore2Program summationEncoding = makePredicatesInternal(new ProgramParserImpl().parse(summationSubprogram));
		prgBuilder.accumulate(summationEncoding);
		prgBuilder.addRules(rewrittenRules);

		// Add enumeration rule that uses the special EnumerationAtom.
		// The enumeration rule is: "input_number_with_first(A, I, F) :- input_with_first(A, X, F), _index(A, X, I)."
		Rule<Head> tmpEnumRule = makePredicatesInternal(parse("input_number_with_first(A, I, F) :- input_with_first(A, X, F).")).getRules().get(0);
		EnumerationAtom enumerationAtom = new EnumerationAtom(Terms.newVariable("A"), Terms.newVariable("X"), Terms.newVariable("I"));
		List<Literal> enumerationRuleBody = new ArrayList<>(tmpEnumRule.getBody());
		enumerationRuleBody.add(enumerationAtom.toLiteral());
		BasicRule enumerationRule = new BasicRule(tmpEnumRule.getHead(), enumerationRuleBody);
		prgBuilder.addRule(enumerationRule);

		return prgBuilder.build();
	}

	/**
	 * Checks if rewriting of sum aggregates is necessary for the given program, i.e. if such aggregates exist.
	 * 
	 * @param program the program.
	 * @return true if sum aggregates occur, false otherwise.
	 */
	private boolean rewritingNecessary(ASPCore2Program program) {
		for (Rule<Head> rule : program.getRules()) {
			for (Literal lit : rule.getBody()) {
				if (lit instanceof AggregateLiteral) {
					AggregateAtom aggregateAtom = ((AggregateLiteral) lit).getAtom();
					if (aggregateAtom.getAggregateFunction() == AggregateAtom.AggregateFunction.SUM) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private List<Rule<Head>> rewriteAggregates(List<Rule<Head>> srcRules) {
		List<Rule<Head>> rewrittenRules = new ArrayList<>();
		for (Rule<Head> rule : srcRules) {
			rewrittenRules.addAll(rewriteAggregatesInRule(rule));
		}
		return rewrittenRules;
	}

	private List<Rule<Head>> rewriteAggregatesInRule(Rule<Head> rule) {

		// Example rewriting/connection:
		// x :- 6 <= #sum {3,a:a; 4,b:b; 5,c:c}.
		// is rewritten to:
		// x :- output(aggregate(1), 6).
		// input_with_first(aggregate(1), element_tuple(3, a), 3) :- a.
		// input_with_first(aggregate(1), element_tuple(4, b), 4) :- b.
		// input_with_first(aggregate(1), element_tuple(5, c), 5) :- c.
		// bound(aggregate(1), 6).

		// Create interface atoms to the aggregate encoding.
		final BasicAtom aggregateOutputAtom = (BasicAtom) makePredicatesInternal(parse(
				"output(aggregate(AGGREGATE_ID), LOWER_BOUND).")).getFacts().get(0);
		final BasicAtom aggregateInputAtom = (BasicAtom) makePredicatesInternal(parse(
				"input_with_first(aggregate(AGGREGATE_ID), ELEMENT_TUPLE, FIRST_VARIABLE).")).getFacts().get(0);
		final BasicAtom lowerBoundAtom = (BasicAtom) makePredicatesInternal(parse(
				"bound(aggregate(AGGREGATE_ID), LOWER_BOUND).")).getFacts().get(0);

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

			// Only treat sum aggregates.
			if (aggregateAtom.getAggregateFunction() != AggregateAtom.AggregateFunction.SUM) {
				continue;
			}
			// Remove aggregate from rule body.
			iterator.remove();

			// Prepare aggregate parameters.
			aggregateCount++;
			Unifier aggregateUnifier = new Unifier();
			Collection<Term> globalVariables = CardinalityNormalization.getGlobalVariables(rewrittenBody, aggregateAtom);
			if (globalVariables.isEmpty()) {
				aggregateUnifier.put(Terms.newVariable("AGGREGATE_ID"), Terms.newConstant(aggregateCount));
			} else {
				// In case some variables are not local to the aggregate, add them to the aggregate identifier
				ArrayList<Term> globalVariableTermlist = new ArrayList<>(globalVariables);
				globalVariableTermlist.add(Terms.newConstant(aggregateCount));
				aggregateUnifier.put(Terms.newVariable("AGGREGATE_ID"), Terms.newFunctionTerm("agg", globalVariableTermlist));
			}
			aggregateUnifier.put(Terms.newVariable("LOWER_BOUND"), aggregateAtom.getLowerBoundTerm());

			// Create new output atom for addition to rule body instead of the aggregate.
			aggregateOutputAtoms.add(aggregateOutputAtom.substitute(aggregateUnifier).toLiteral());

			// Create input to sorting network from aggregate elements.
			for (AggregateAtom.AggregateElement aggregateElement : aggregateAtom.getAggregateElements()) {
				// Prepare element substitution.
				List<Term> elementTerms = aggregateElement.getElementTerms();
				FunctionTerm elementTuple = Terms.newFunctionTerm("element_tuple", elementTerms);
				Unifier elementUnifier = new Unifier(aggregateUnifier);
				elementUnifier.put(Terms.newVariable("ELEMENT_TUPLE"), elementTuple);
				elementUnifier.put(Terms.newVariable("FIRST_VARIABLE"), elementTuple.getTerms().get(0));

				// Create new rule for input.
				Atom inputHeadAtom = aggregateInputAtom.substitute(elementUnifier);
				List<Literal> elementLiterals = new ArrayList<>(aggregateElement.getElementLiterals());

				// If there are global variables used inside the aggregate, add original rule body (minus the aggregate itself) to input rule.
				if (!globalVariables.isEmpty()) {
					elementLiterals.addAll(rewrittenBody);
				}
				BasicRule inputRule = new BasicRule(new NormalHeadImpl(inputHeadAtom), elementLiterals);
				additionalRules.add(inputRule);
			}

			// Create lower bound for the aggregate.
			Atom lowerBoundHeadAtom = lowerBoundAtom.substitute(aggregateUnifier);
			List<Literal> lowerBoundBody = rewrittenBody; // Note: this is only correct if no other aggregate occurs in the rule.
			additionalRules.add(new BasicRule(new NormalHeadImpl(lowerBoundHeadAtom), lowerBoundBody));
		}
		if (aggregatesInRule > 0) {
			rewrittenBody.addAll(aggregateOutputAtoms);
			additionalRules.add(new BasicRule(rule.getHead(), rewrittenBody));
		} else {
			// Return original rule if no aggregate occurs in it.
			additionalRules.add(rule);
		}
		return additionalRules;
	}
}
