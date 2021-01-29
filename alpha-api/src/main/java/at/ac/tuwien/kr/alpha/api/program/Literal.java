package at.ac.tuwien.kr.alpha.api.program;

import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;

public interface Literal {
	Atom getAtom();

	boolean isNegated();

	Literal negate();

	Predicate getPredicate();

	List<Term> getTerms();

	Set<VariableTerm> getOccurringVariables();

	boolean isGround();

	Set<VariableTerm> getBindingVariables();

	Set<VariableTerm> getNonBindingVariables();

	Literal substitute(Substitution substitution);
}