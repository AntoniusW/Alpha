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

import static at.ac.tuwien.kr.alpha.Util.join;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

/**
 * Represents ordinary ASP atoms.
 */
public class BasicAtom extends Atom implements VariableNormalizableAtom {
	private final Predicate predicate;
	private final List<Term> terms;
	private final boolean ground;

	/**
	 * Creates a positive BasicAtom over predicate and terms.
	 * 
	 * @param predicate
	 * @param terms
	 */
	public BasicAtom(Predicate predicate, List<Term> terms) {
		if (terms.size() != predicate.getArity()) {
			throw new IllegalArgumentException(
					"Cannot create Atom with terms: " + terms + " - Predicate " + predicate.getName() + " has incompatible arity " + predicate.getArity());
		}

		this.predicate = predicate;
		this.terms = terms;

		boolean ground = true;
		for (Term term : terms) {
			if (!term.isGround()) {
				ground = false;
				break;
			}
		}

		this.ground = ground;
	}

	public BasicAtom(Predicate predicate, Term... terms) {
		this(predicate, Arrays.asList(terms));
	}

	public BasicAtom(Predicate predicate) {
		this(predicate, Collections.emptyList());
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
		return ground;
	}

	@Override
	public BasicAtom substitute(Substitution substitution) {
		return new BasicAtom(predicate, terms.stream()
				.map(t -> t.substitute(substitution))
				.collect(Collectors.toList()));
	}

	@Override
	public BasicAtom normalizeVariables(String prefix, int counterStartingValue) {
		List<Term> renamedTerms = Term.renameTerms(terms, prefix, counterStartingValue);
		return new BasicAtom(predicate, renamedTerms);
	}

	@Override
	public BasicLiteral toLiteral(boolean positive) {
		return new BasicLiteral(this, positive);
	}

	@Override
	public String toString() {
		final String prefix = predicate.getName();
		if (terms.isEmpty()) {
			return prefix;
		}

		return join(prefix + "(", terms, ")");
	}

	@Override
	public int compareTo(Atom o) {
		if (this.terms.size() != o.getTerms().size()) {
			return this.terms.size() - o.getTerms().size();
		}

		int result = this.predicate.compareTo(o.getPredicate());

		if (result != 0) {
			return result;
		}

		for (int i = 0; i < terms.size(); i++) {
			result = terms.get(i).compareTo(o.getTerms().get(i));
			if (result != 0) {
				return result;
			}
		}

		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		BasicAtom that = (BasicAtom) o;

		return predicate.equals(that.predicate) && terms.equals(that.terms);
	}

	@Override
	public int hashCode() {
		return 31 * predicate.hashCode() + terms.hashCode();
	}

	@Override
	public Atom withTerms(List<Term> terms) {
		return new BasicAtom(predicate, terms);
	}

}
