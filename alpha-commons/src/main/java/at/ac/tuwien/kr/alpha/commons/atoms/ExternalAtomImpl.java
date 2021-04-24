/**
 * Copyright (c) 2017-2019, the Alpha Team.
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;

import at.ac.tuwien.kr.alpha.api.Util;
import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.ExternalAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.ExternalLiteral;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.literals.Literals;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

class ExternalAtomImpl extends AbstractAtom implements ExternalAtom {

	private final List<Term> input;
	private final List<Term> output;

	protected final Predicate predicate;
	protected final PredicateInterpretation interpretation;

	ExternalAtomImpl(Predicate predicate, PredicateInterpretation interpretation, List<Term> input, List<Term> output) {
		if (predicate == null) {
			throw new IllegalArgumentException("predicate must not be null!");
		}
		if (interpretation == null) {
			throw new IllegalArgumentException("interpretation must not be null!");
		}
		if (input == null) {
			throw new IllegalArgumentException("input must not be null!");
		}
		if (output == null) {
			throw new IllegalArgumentException("output must not be null!");
		}
		this.predicate = predicate;
		this.interpretation = interpretation;
		this.input = input;
		this.output = output;
	}

	@Override
	public boolean hasOutput() {
		return !output.isEmpty();
	}

	@Override
	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public PredicateInterpretation getInterpretation() {
		return interpretation;
	}

	@Override
	public List<Term> getInput() {
		return Collections.unmodifiableList(input);
	}

	@Override
	public List<Term> getOutput() {
		return Collections.unmodifiableList(output);
	}

	@Override
	public List<Term> getTerms() {
		return ListUtils.union(input, output);
	}

	@Override
	public boolean isGround() {
		for (Term t : input) {
			if (!t.isGround()) {
				return false;
			}
		}
		for (Term t : output) {
			if (!t.isGround()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ExternalAtomImpl substitute(Substitution substitution) {
		List<Term> substitutedInput = this.input.stream().map(t -> t.substitute(substitution)).collect(Collectors.toList());
		List<Term> substitutedOutput = this.output.stream().map(t -> t.substitute(substitution)).collect(Collectors.toList());
		return new ExternalAtomImpl(predicate, interpretation, substitutedInput, substitutedOutput);
	}

	@Override
	public ExternalLiteral toLiteral(boolean positive) {
		return Literals.fromAtom(this, positive);
	}

	@Override
	public String toString() {
		String result = "&" + predicate.getName();
		if (!input.isEmpty()) {
			result += Util.join("[", input, "]");
		}
		if (!output.isEmpty()) {
			result += Util.join("(", output, ")");
		}
		return result;
	}

	/**
	 * Creates a new {@link ExternalAtom} with input and output term lists set according to the given term list.
	 * Callers are responsible for making sure the given term list contains the right number of terms. For an external atom with n input and m
	 * output terms, terms[0..n-1] is interpreted as input terms for the new atom, terms[n..(n + m)-1] are taken to be output terms.
	 */
	@Override
	public ExternalAtom withTerms(List<Term> terms) {
		if (terms.size() != this.input.size() + this.output.size()) {
			throw new IllegalArgumentException(
					"Cannot apply term list " + terms.toString() + " to external atom " + this.toString() + ", terms has invalid size!");
		}
		List<Term> newInput = terms.subList(0, this.input.size());
		List<Term> newOutput = terms.subList(this.input.size(), terms.size());
		return new ExternalAtomImpl(this.predicate, this.interpretation, newInput, newOutput);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.input.hashCode();
		result = prime * result + this.output.hashCode();
		result = prime * result + this.predicate.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ExternalAtom)) {
			return false;
		}
		ExternalAtom other = (ExternalAtom) obj;
		if (!this.input.equals(other.getInput())) {
			return false;
		}
		if (!this.output.equals(other.getOutput())) {
			return false;
		}
		if (!this.predicate.equals(other.getPredicate())) {
			return false;
		}
		return true;
	}

	@Override
	public ExternalAtomImpl normalizeVariables(String prefix, int counterStartingValue) {
		List<Term> renamedInput = Terms.renameTerms(this.input, prefix + "_IN_", counterStartingValue);
		List<Term> renamedOutput = Terms.renameTerms(this.output, prefix + "_OUT_", counterStartingValue);
		return new ExternalAtomImpl(this.predicate, this.interpretation, renamedInput, renamedOutput);
	}

}
