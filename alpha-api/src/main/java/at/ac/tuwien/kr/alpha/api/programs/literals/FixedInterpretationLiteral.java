package at.ac.tuwien.kr.alpha.api.programs.literals;

import java.util.List;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;

/**
 * Represents a literal whose ground truth value(s) are independent of the
 * current assignment.
 * Examples of atoms underlying such literals are builtin atoms and external
 * atoms.
 * Copyright (c) 2017-2021, the Alpha Team.
 */
public interface FixedInterpretationLiteral extends Literal {

	/**
	 * Creates a list of {@link Substitution}s based on the given partial substitution, such that this {@link FixedInterpretationLiteral} is
	 * true in every returned substitution. In cases where this is not possible (because of conflicting variable assignments in the partial
	 * substitution), getSatisfyingSubstitutions is required to return an empty list
	 * 
	 * @param partialSubstitution a partial substitution that is required to bind
	 *                            all variables that are non-binding in this literal
	 * @return a list of substitutions, in each of which this literal is true, or an
	 *         empty list if no such substitution exists
	 */
	public abstract List<Substitution> getSatisfyingSubstitutions(Substitution partialSubstitution);

}
