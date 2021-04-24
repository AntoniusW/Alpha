package at.ac.tuwien.kr.alpha.core.atoms;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.literals.AbstractLiteral;

/**
 * Copyright (c) 2018, the Alpha Team.
 */
public class EnumerationLiteral extends AbstractLiteral {

	public EnumerationLiteral(EnumerationAtom atom) {
		super(atom, true);
	}

	@Override
	public EnumerationAtom getAtom() {
		return (EnumerationAtom) super.getAtom();
	}

	@Override
	public EnumerationLiteral negate() {
		throw new UnsupportedOperationException("EnumerationLiteral cannot be negated");
	}

	@Override
	public EnumerationLiteral substitute(Substitution substitution) {
		return new EnumerationLiteral(getAtom().substitute(substitution));
	}

	@Override
	public Set<VariableTerm> getBindingVariables() {
		return Collections.singleton((VariableTerm) getTerms().get(2));
	}

	@Override
	public Set<VariableTerm> getNonBindingVariables() {
		Set<VariableTerm> ret = new HashSet<>(2);
		Term idTerm = getTerms().get(0);
		Term enumTerm = getTerms().get(1);
		if (idTerm instanceof VariableTerm) {
			ret.add((VariableTerm) idTerm);
		}
		if (enumTerm instanceof VariableTerm) {
			ret.add((VariableTerm) enumTerm);
		}
		return ret;

	}

	public Substitution addEnumerationIndexToSubstitution(Substitution partialSubstitution) {
		return this.getAtom().addEnumerationIndexToSubstitution(partialSubstitution);
	}
}
