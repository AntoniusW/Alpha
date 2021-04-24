package at.ac.tuwien.kr.alpha.core.atoms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.Util;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.AbstractAtom;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

/**
 * Represents a ground-instance enumeration atom of form:
 * enum(enumId, groundTerm, sequenceNo).
 * 
 * The semantics of this is:
 * if enum(A,T1, N1) and enum(A,T2,N2) are both true and T1 != T2, then N1 != N2.
 * Furthermore, If enum(A,T1,N1) is true with N1 > 0 then enum(A,T2,N1 - 1) is true for some T1 != T2 and
 * both, T1 and T2, are ground instances the grounder encountered during the search so far.
 *
 * Copyright (c) 2017, the Alpha Team.
 */
public class EnumerationAtom extends AbstractAtom implements BasicAtom {
	public static final Predicate ENUMERATION_PREDICATE = Predicates.getPredicate("_Enumeration", 3);
	private static final HashMap<Term, HashMap<Term, Integer>> ENUMERATIONS = new HashMap<>();

	private final Term enumIdTerm;
	private final Term valueTerm;
	private final VariableTerm indexTerm;

	public EnumerationAtom(Term enumIdTerm, Term valueTerm, VariableTerm indexTerm) {
		this.enumIdTerm = enumIdTerm;
		this.valueTerm = valueTerm;
		this.indexTerm = indexTerm;
	}
	
	public static void resetEnumerations() {
		ENUMERATIONS.clear();
	}

	private Integer getEnumerationIndex(Term identifier, Term enumerationTerm) {
		ENUMERATIONS.putIfAbsent(identifier, new HashMap<>());
		HashMap<Term, Integer> enumeratedTerms = ENUMERATIONS.get(identifier);
		Integer assignedInteger = enumeratedTerms.get(enumerationTerm);
		if (assignedInteger == null) {
			int enumerationIndex = enumeratedTerms.size() + 1;
			enumeratedTerms.put(enumerationTerm, enumerationIndex);
			return enumerationIndex;
		} else {
			return assignedInteger;
		}

	}

	/**
	 * Based on a given substitution, substitutes the first two terms of this {@link EnumerationAtom} with the values from the substitution,
	 * and returns a new substitution with all mappings from the input substitution plus a binding for the third term of the enum atom to the
	 * integer index that is mapped to the first two terms in the internal <code>ENUMERATIONS</code> map.
	 * 
	 * @param substitution an input substitution which must provide ground terms for the first two terms of the enumeration atom.
	 * @return a new substitution where the third term of the enumeration atom is bound to an integer.
	 */
	public Substitution addEnumerationIndexToSubstitution(Substitution substitution) {
		Term idTerm = this.getTerms().get(0).substitute(substitution);
		Term enumerationTerm = this.getTerms().get(1).substitute(substitution);
		if (!enumerationTerm.isGround()) {
			throw new RuntimeException("Enumeration term is not ground after substitution. Should not happen.");
		}
		Integer enumerationIndex = getEnumerationIndex(idTerm, enumerationTerm);
		BasicSubstitution retVal = new BasicSubstitution(substitution);
		retVal.put((VariableTerm) getTerms().get(2), Terms.newConstant(enumerationIndex));
		return retVal;
	}

	@Override
	public EnumerationAtom substitute(Substitution substitution) {
		Term substEnumIdTerm = enumIdTerm.substitute(substitution);
		Term substValueTerm = valueTerm.substitute(substitution);
		Term substIndexTerm = indexTerm.substitute(substitution);
		return new EnumerationAtom(substEnumIdTerm, substValueTerm, (VariableTerm) substIndexTerm);
	}

	@Override
	public EnumerationLiteral toLiteral(boolean positive) {
		if (!positive) {
			throw Util.oops("IntervalLiteral cannot be negated");
		}
		return new EnumerationLiteral(this);
	}

	@Override
	public EnumerationLiteral toLiteral() {
		return toLiteral(true);
	}

	@Override
	public Predicate getPredicate() {
		return ENUMERATION_PREDICATE;
	}

	@Override
	public List<Term> getTerms() {
		List<Term> lst = new ArrayList<>();
		lst.add(enumIdTerm);
		lst.add(valueTerm);
		lst.add(indexTerm);
		return lst;
	}

	@Override
	public boolean isGround() {
		// An enumeration atom always has a variable as its third argument which represents the internal index of the atom (i.e. enumeration value).
		// It is therefore, by definition, not ground.
		return false;
	}

	@Override
	public Set<VariableTerm> getOccurringVariables() {
		return toLiteral().getOccurringVariables();
	}

	@Override
	public Atom renameVariables(String newVariablePrefix) {
		return this.substitute(Terms.renameVariables(getOccurringVariables(), newVariablePrefix));
	}

	@Override
	public EnumerationAtom withTerms(List<Term> terms) {
		if (terms.size() != 3) {
			throw new IllegalArgumentException("EnumerationAtom must have exactly 3 terms!");
		}
		if (!(terms.get(2) instanceof VariableTerm)) {
			throw new IllegalArgumentException("Third argument of EnumerationAtom must be a variable!");
		}
		return new EnumerationAtom(terms.get(0), terms.get(1), (VariableTerm) terms.get(2));
	}

	@Override
	public Atom normalizeVariables(String prefix, int counterStartingValue) {
		List<Term> renamedTerms = Terms.renameTerms(getTerms(), prefix, counterStartingValue);
		return withTerms(renamedTerms);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		EnumerationAtom that = (EnumerationAtom) o;

		return ENUMERATION_PREDICATE.equals(that.getPredicate()) && this.getTerms().equals(that.getTerms());
	}

	@Override
	public int hashCode() {
		return 31 * ENUMERATION_PREDICATE.hashCode() + getTerms().hashCode();
	}
	
}
