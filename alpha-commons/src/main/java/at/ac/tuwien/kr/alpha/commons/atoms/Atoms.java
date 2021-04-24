package at.ac.tuwien.kr.alpha.commons.atoms;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.ComparisonOperator;
import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.AggregateAtom.AggregateElement;
import at.ac.tuwien.kr.alpha.api.programs.atoms.AggregateAtom.AggregateFunction;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.ComparisonAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.ExternalAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.atoms.AggregateAtomImpl.AggregateElementImpl;

public final class Atoms {

	private Atoms() {
		throw new AssertionError("Cannot instantiate utility class");
	}

	/**
	 * Creates a positive BasicAtom over predicate and terms.
	 * 
	 * @param predicate
	 * @param terms
	 */
	public static BasicAtom newBasicAtom(Predicate predicate, List<Term> terms) {
		return new BasicAtomImpl(predicate, terms);
	}

	public static BasicAtom newBasicAtom(Predicate predicate, Term... terms) {
		return new BasicAtomImpl(predicate, Arrays.asList(terms));
	}

	public static BasicAtom newBasicAtom(Predicate predicate) {
		return new BasicAtomImpl(predicate, Collections.emptyList());
	}

	public static AggregateAtom newAggregateAtom(ComparisonOperator lowerBoundOperator, Term lowerBoundTerm, ComparisonOperator upperBoundOperator,
			Term upperBoundTerm, AggregateFunction aggregatefunction, List<AggregateElement> aggregateElements) {
		return new AggregateAtomImpl(lowerBoundOperator, lowerBoundTerm, upperBoundOperator, upperBoundTerm, aggregatefunction, aggregateElements);
	}

	public static AggregateAtom.AggregateElement newAggregateElement(List<Term> elementTerms, List<Literal> elementLiterals) {
		return new AggregateElementImpl(elementTerms, elementLiterals);
	}

	public static ComparisonAtom newComparsionAtom(Term term1, Term term2, ComparisonOperator operator) {
		return new ComparisonAtomImpl(term1, term2, operator);
	}

	public static ExternalAtom newExternalAtom(Predicate predicate, PredicateInterpretation interpretation, List<Term> input, List<Term> output) {
		return new ExternalAtomImpl(predicate, interpretation, input, output);
	}

}
