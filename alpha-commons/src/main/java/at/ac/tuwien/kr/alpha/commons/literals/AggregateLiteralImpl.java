package at.ac.tuwien.kr.alpha.commons.literals;

import java.util.HashSet;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.ComparisonOperator;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.AggregateLiteral;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.atoms.AbstractAtom;
import at.ac.tuwien.kr.alpha.commons.comparisons.ComparisonOperators;

/**
 * Copyright (c) 2018, the Alpha Team.
 */
class AggregateLiteralImpl extends AbstractLiteral implements AggregateLiteral {
	
	AggregateLiteralImpl(AggregateAtom atom, boolean positive) {
		super(atom, positive);
	}

	@Override
	public AggregateAtom getAtom() {
		return (AggregateAtom) atom;
	}

	@Override
	public AggregateLiteralImpl negate() {
		return new AggregateLiteralImpl(getAtom(), !positive);
	}

	/**
	 * @see AbstractAtom#substitute(Substitution)
	 */
	@Override
	public AggregateLiteralImpl substitute(Substitution substitution) {
		//return new AggregateLiteral(getAtom().substitute(substitution), positive);
		// TODO either remove substitute method from abstract type or don't extend
		throw new UnsupportedOperationException("Cannot substitute AggregateLiteral!");
	}

	@Override
	public Set<VariableTerm> getBindingVariables() {
		Set<VariableTerm> bindingVariables = new HashSet<>();
		if (boundBindingVariable(getAtom().getLowerBoundOperator(), getAtom().getLowerBoundTerm(), positive) != null) {
			bindingVariables.add((VariableTerm) getAtom().getLowerBoundTerm());
		}
		if (boundBindingVariable(getAtom().getUpperBoundOperator(), getAtom().getUpperBoundTerm(), positive) != null) {
			bindingVariables.add((VariableTerm) getAtom().getUpperBoundTerm());
		}
		return bindingVariables;
	}

	@Override
	public Set<VariableTerm> getNonBindingVariables() {
		// Note: every local variable that also occurs globally in the rule is a nonBindingVariable, hence an
		// aggregate literal alone cannot detect its non-binding (i.e. global) variables.
		throw new UnsupportedOperationException();
	}

	private static VariableTerm boundBindingVariable(ComparisonOperator op, Term bound, boolean positive) {
		boolean isNormalizedEquality = op == ComparisonOperators.EQ && positive || op == ComparisonOperators.NE && !positive;
		if (isNormalizedEquality &&  bound instanceof VariableTerm) {
			return (VariableTerm) bound;
		}
		return null;
	}

}
