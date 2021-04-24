/**
 * Copyright (c) 2016-2020, the Alpha Team.
 * All rights reserved.
 * <p>
 * Additional changes made by Siemens.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.commons.atoms;

import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

/**
 * An Atom is the common superclass of all representations of ASP atoms used by Alpha.
 */
// TODO make public as implementation template?
public abstract class AbstractAtom implements Atom {

	/**
	 * Creates a new Atom that represents this Atom, but has the given term list instead.
	 *
	 * @param terms the terms to set.
	 * @return a new Atom with the given terms set.
	 */
	@Override
	public abstract Atom withTerms(List<Term> terms);

	/**
	 * Returns the set of all variables occurring in the Atom.
	 */
	@Override
	public Set<VariableTerm> getOccurringVariables() {
		return toLiteral().getOccurringVariables();
	}

	/**
	 * This method applies a substitution to the atom. Note that, depending on the atom and the substitution, the
	 * resulting atom may still contain variables.
	 *
	 * @param substitution the variable substitution to apply.
	 * @return the atom resulting from the application of the substitution.
	 */
	@Override
	public abstract Atom substitute(Substitution substitution);

	@Override
	public abstract List<Term> getTerms();

	@Override
	public abstract Predicate getPredicate();

	/**
	 * Returns whether this atom is ground, i.e., variable-free.
	 *
	 * @return true iff the terms of this atom contain no {@link VariableTermImpl}.
	 */
	@Override
	public abstract boolean isGround();

	@Override
	public abstract Literal toLiteral(boolean positive);

	@Override
	public Atom renameVariables(String newVariablePrefix) {
		return this.substitute(Terms.renameVariables(getOccurringVariables(), newVariablePrefix));
	}

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

}
