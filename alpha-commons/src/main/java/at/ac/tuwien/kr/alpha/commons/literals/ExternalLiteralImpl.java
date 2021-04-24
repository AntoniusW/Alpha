/**
 * Copyright (c) 2017-2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.commons.literals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.atoms.ExternalAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.ExternalLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.atoms.AbstractAtom;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;

/**
 * Contains a potentially negated {@link ExternalAtomImpl}.
 */
class ExternalLiteralImpl extends AbstractLiteral implements ExternalLiteral {

	ExternalLiteralImpl(ExternalAtom atom, boolean positive) {
		super(atom, positive);
	}

	@Override
	public ExternalAtom getAtom() {
		return (ExternalAtom) super.getAtom();
	}

	/**
	 * Returns a new copy of this literal whose {@link Literal#isNegated()} status
	 * is inverted
	 */
	@Override
	public ExternalLiteralImpl negate() {
		return new ExternalLiteralImpl(getAtom(), this.isNegated());
	}

	/**
	 * @see AbstractAtom#substitute(BasicSubstitution)
	 */
	@Override
	public ExternalLiteralImpl substitute(Substitution substitution) {
		return new ExternalLiteralImpl((ExternalAtom) getAtom().substitute(substitution), !this.isNegated());
	}

	@Override
	public Set<VariableTerm> getBindingVariables() {
		// If the external atom is negative, then all variables of input and output are non-binding
		// and there are no binding variables (like for ordinary atoms).
		// If the external atom is positive, then variables of output are binding.

		if (this.isNegated()) {
			return Collections.emptySet();
		}

		List<Term> output = getAtom().getOutput();

		Set<VariableTerm> binding = new HashSet<>(output.size());

		for (Term out : output) {
			if (out instanceof VariableTerm) {
				binding.add((VariableTerm) out);
			}
		}

		return binding;
	}

	@Override
	public Set<VariableTerm> getNonBindingVariables() {
		List<Term> input = getAtom().getInput();
		List<Term> output = getAtom().getOutput();

		// External atoms have their input always non-binding, since they cannot
		// be queried without some concrete input.
		Set<VariableTerm> nonbindingVariables = new HashSet<>();
		for (Term term : input) {
			nonbindingVariables.addAll(term.getOccurringVariables());
		}

		// If the external atom is negative, then all variables of input and output are
		// non-binding.
		if (this.isNegated()) {
			for (Term out : output) {
				if (out instanceof VariableTerm) {
					nonbindingVariables.add((VariableTerm) out);
				}
			}
		}

		return nonbindingVariables;
	}

	@Override
	public List<Substitution> getSatisfyingSubstitutions(Substitution partialSubstitution) {
		List<Term> input = getAtom().getInput();
		List<Term> substitutes = new ArrayList<>(input.size());

		// In preparation for evaluating the external atom, set the input values according
		// to the partial substitution supplied by the grounder.
		for (Term t : input) {
			substitutes.add(t.substitute(partialSubstitution));
		}
		Set<List<ConstantTerm<?>>> results = getAtom().getInterpretation().evaluate(substitutes);
		if (results == null) {
			throw new NullPointerException("Predicate " + getPredicate().getName() + " returned null. It must return a Set.");
		}

		if (this.isNegated()) {
			return this.isNegatedLiteralSatisfied(results) ? Collections.singletonList(partialSubstitution) : Collections.emptyList();
		} else {
			return this.buildSubstitutionsForOutputs(partialSubstitution, results);
		}
	}

	/**
	 * Checks whether this negated external literal is satisfied.
	 *
	 * Note that this method must only be called on negated external literals.
	 * In that case, the literal itself does not bind any output variables,
	 * i.e. the underlying atom is satisfied iff the output terms obtained by
	 * evaluating the underlying external atom match the values to which
	 * the respective output variables are bound. The result of this method assumes
	 * that the literal is negated, i.e. for the underlying atom AT, it represents
	 * the truth value !AT.
	 * Furthermore, this method assumes that the output terms of the underlying atom
	 * are {@link ConstantTerm}s, i.e. that the grounder's current partial
	 * substitution has already been applied to them. That assumption is safe since
	 * in case of a negated external literal, the output variables are always
	 * non-binding.
	 *
	 * @param externalMethodResult The term lists obtained from evaluating the external atom
	 *                             (i.e. calling the java method) encapsulated by this literal
	 * @return true iff no list in externalMethodResult equals the external atom's output term
	 *         list as substituted by the grounder, false otherwise
	 */
	private boolean isNegatedLiteralSatisfied(Set<List<ConstantTerm<?>>> externalMethodResult) {
		List<Term> externalAtomOutTerms = this.getAtom().getOutput();
		boolean outputMatches;
		for (List<ConstantTerm<?>> resultTerms : externalMethodResult) {
			outputMatches = true;
			for (int i = 0; i < externalAtomOutTerms.size(); i++) {
				if (!resultTerms.get(i).equals(externalAtomOutTerms.get(i))) {
					outputMatches = false;
					break;
				}
			}
			if (outputMatches) {
				// We found one term list where all terms match the ground output terms of the
				// external atom, therefore the atom is true and the (negative) literal false.
				return false;
			}
		}
		// We checked all term list and none matches the ground output terms, therefore
		// the external atom is false, making the literal true.
		return true;
	}

	private List<Substitution> buildSubstitutionsForOutputs(Substitution partialSubstitution, Set<List<ConstantTerm<?>>> outputs) {
		List<Substitution> retVal = new ArrayList<>();
		List<Term> externalAtomOutputTerms = this.getAtom().getOutput();
		for (List<ConstantTerm<?>> bindings : outputs) {
			if (bindings.size() < externalAtomOutputTerms.size()) {
				throw new RuntimeException(
						"Predicate " + getPredicate().getName() + " returned " + bindings.size() + " terms when at least " + externalAtomOutputTerms.size()
								+ " were expected.");
			}
			BasicSubstitution ith = new BasicSubstitution(partialSubstitution);
			boolean skip = false;
			for (int i = 0; i < externalAtomOutputTerms.size(); i++) {
				Term out = externalAtomOutputTerms.get(i);

				if (out instanceof VariableTerm) {
					ith.put((VariableTerm) out, bindings.get(i));
				} else {
					if (!bindings.get(i).equals(out)) {
						skip = true;
						break;
					}
				}
			}
			if (!skip) {
				retVal.add(ith);
			}
		}
		return retVal;
	}

}
