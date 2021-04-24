/**
 * Copyright (c) 2017-2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.commons.atoms;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import at.ac.tuwien.kr.alpha.api.ComparisonOperator;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.ComparisonAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.literals.Literals;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

/**
 * Represents a builtin comparison atom according to the standard.
 */
class ComparisonAtomImpl extends AbstractAtom implements ComparisonAtom {

	private final Predicate predicate;
	private final ComparisonOperator operator;
	private final List<Term> terms;

	private ComparisonAtomImpl(List<Term> terms, ComparisonOperator operator) {
		this.terms = terms;
		this.operator = operator;
		this.predicate = operator.toPredicate();
	}

	ComparisonAtomImpl(Term term1, Term term2, ComparisonOperator operator) {
		this(Arrays.asList(term1, term2), operator);
	}

	@Override
	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public List<Term> getTerms() {
		return terms;
	}

	@Override
	public boolean isGround() {
		return terms.get(0).isGround() && terms.get(1).isGround();
	}

	@Override
	public ComparisonAtom substitute(Substitution substitution) {
		List<Term> substitutedTerms = getTerms().stream().map(t -> t.substitute(substitution)).collect(Collectors.toList());
		return new ComparisonAtomImpl(substitutedTerms, operator);
	}

	@Override
	public ComparisonLiteral toLiteral(boolean positive) {
		return Literals.fromAtom(this, positive);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(terms.get(0));
		sb.append(" ");
		sb.append(operator);
		sb.append(" ");
		sb.append(terms.get(1));
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ComparisonAtomImpl that = (ComparisonAtomImpl) o;

		if (operator != that.operator) {
			return false;
		}
		return terms.equals(that.terms);
	}

	@Override
	public int hashCode() {
		return 31 * (31 * operator.hashCode() + terms.hashCode());
	}

	@Override
	public ComparisonAtomImpl normalizeVariables(String prefix, int counterStartingValue) {
		List<Term> renamedTerms = Terms.renameTerms(terms, prefix, counterStartingValue);
		return new ComparisonAtomImpl(renamedTerms.get(0), renamedTerms.get(1), operator);
	}

	@Override
	public Atom withTerms(List<Term> terms) {
		return new ComparisonAtomImpl(terms, operator);
	}

	@Override
	public ComparisonOperator getOperator() {
		return this.operator;
	}

}
