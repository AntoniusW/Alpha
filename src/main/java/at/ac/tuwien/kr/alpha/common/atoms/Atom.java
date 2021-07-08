/**
 * Copyright (c) 2016-2020, the Alpha Team.
 * All rights reserved.
 *
 * Additional changes made by Siemens.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
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
package at.ac.tuwien.kr.alpha.common.atoms;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.Substitutable;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Substitution;
import at.ac.tuwien.kr.alpha.grounder.Unifier;

import java.util.List;
import java.util.Set;

/**
 * An Atom is the common superclass of all representations of ASP atoms used by Alpha.
 */
public abstract class Atom implements Comparable<Atom>, Substitutable<Atom> {

	public abstract Predicate getPredicate();

	public abstract List<Term> getTerms();

	/**
	 * Creates a new Atom that represents this Atom, but has the given term list instead.
	 * 
	 * @param terms the terms to set.
	 * @return a new Atom with the given terms set.
	 */
	public abstract Atom withTerms(List<Term> terms);

	/**
	 * Returns whether this atom is ground, i.e., variable-free.
	 *
	 * @return true iff the terms of this atom contain no {@link VariableTerm}.
	 */
	public abstract boolean isGround();

	/**
	 * Returns the set of all variables occurring in the Atom.
	 */
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
	public abstract Atom substitute(Substitution substitution);

	/**
	 * Creates a non-negated literal containing this atom.
	 */
	public Literal toLiteral() {
		return toLiteral(true);
	}

	/**
	 * Creates a literal containing this atom which will be negated if {@code positive} is {@code false}.
	 * 
	 * @param positive the polarity of the resulting literal.
	 * @return a literal that is positive iff the given parameter is true.
	 */
	public abstract Literal toLiteral(boolean positive);

	/**
	 * Converts this atom to a function term (which is needed if the atom is to be nested in another term)
	 */
	public FunctionTerm toFunctionTerm() {
		throw new UnsupportedOperationException(this + " cannot be converted to a function term.");
	}

	public Atom renameVariables(String newVariablePrefix) {
		Unifier renamingSubstitution = new Unifier();
		int counter = 0;
		for (VariableTerm variable : getOccurringVariables()) {
			renamingSubstitution.put(variable, VariableTerm.getInstance(newVariablePrefix + counter++));
		}
		return this.substitute(renamingSubstitution);
	}

	@Override
	public int compareTo(Atom o) {
		if (o == null) {
			return 1;
		}

		final List<Term> aTerms = this.getTerms();
		final List<Term> bTerms = o.getTerms();

		if (aTerms.size() != bTerms.size()) {
			return Integer.compare(aTerms.size(), bTerms.size());
		}

		int result = this.getPredicate().compareTo(o.getPredicate());

		if (result != 0) {
			return result;
		}

		for (int i = 0; i < aTerms.size(); i++) {
			result = aTerms.get(i).compareTo(o.getTerms().get(i));
			if (result != 0) {
				return result;
			}
		}

		return 0;
	}

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

}
