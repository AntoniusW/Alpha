/**
 * Copyright (c) 2017-2018, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.core.atoms;

import java.util.Arrays;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.Util;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.VariableNormalizableAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.AbstractAtom;
import at.ac.tuwien.kr.alpha.commons.terms.IntervalTerm;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

/**
 * Helper for treating IntervalTerms in rules.
 *
 * Each IntervalTerm is replaced by a variable and this special IntervalAtom is added to the rule body for generating
 * all bindings of the variable.
 *
 * The first term of this atom is an IntervalTerm while the second term is any Term (if it is a VariableTerm, this will
 * bind to all elements of the interval, otherwise it is a simple check whether the Term is a ConstantTerm<Integer> with
 * the Integer being inside the interval.
 * 
 * Copyright (c) 2017, the Alpha Team.
 */
public class IntervalAtom extends AbstractAtom implements VariableNormalizableAtom {
	private static final Predicate PREDICATE = Predicates.getPredicate("_interval", 2, true);

	private final List<Term> terms;

	public IntervalAtom(IntervalTerm intervalTerm, Term intervalRepresentingVariable) {
		this.terms = Arrays.asList(intervalTerm, intervalRepresentingVariable);
	}

	@Override
	public Predicate getPredicate() {
		return PREDICATE;
	}

	@Override
	public List<Term> getTerms() {
		return terms;
	}

	@Override
	public boolean isGround() {
		for (Term t : this.terms) {
			if (!t.isGround()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public IntervalLiteral toLiteral(boolean positive) {
		if (!positive) {
			throw Util.oops("IntervalLiteral cannot be negated");
		}
		return new IntervalLiteral(this);
	}

	@Override
	public IntervalLiteral toLiteral() {
		return toLiteral(true);
	}

	@Override
	public String toString() {
		return Util.join(PREDICATE.getName() + "(", terms, ")");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IntervalAtom that = (IntervalAtom) o;

		return terms.equals(that.terms);
	}

	@Override
	public int hashCode() {
		return terms.hashCode();
	}

	@Override
	public IntervalAtom substitute(Substitution substitution) {
		return new IntervalAtom((IntervalTerm) terms.get(0).substitute(substitution), terms.get(1).substitute(substitution));
	}

	@Override
	public IntervalAtom normalizeVariables(String prefix, int counterStartingValue) {
		List<Term> renamedTerms = Terms.renameTerms(terms, prefix, counterStartingValue);
		return new IntervalAtom((IntervalTerm) renamedTerms.get(0), renamedTerms.get(1));
	}

	@Override
	public Atom withTerms(List<Term> newTerms) {
		if (newTerms.size() != 2) {
			throw new IllegalArgumentException("Cannot set term list " + Util.join("[", newTerms, "]") + " for interval atom!");
		}
		if (!(newTerms.get(0) instanceof IntervalTerm)) {
			throw new IllegalArgumentException("IntervalAtom requires first term to be an interval term!");
		}
		return new IntervalAtom((IntervalTerm) newTerms.get(0), newTerms.get(1));
	}

}
