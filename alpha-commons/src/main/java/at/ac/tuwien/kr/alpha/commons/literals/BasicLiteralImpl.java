/**
 * Copyright (c) 2017-2018, the Alpha Team.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.BasicLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.atoms.AbstractAtom;

/**
 * Contains a potentially negated {@link BasicAtomImpl}.
 *
 * Copyright (c) 2017-2018, the Alpha Team.
 */
class BasicLiteralImpl extends AbstractLiteral implements BasicLiteral { // TODO could we parameterize Literal with Atom type?

	BasicLiteralImpl(BasicAtom atom, boolean positive) {
		super(atom, positive);
	}

	@Override
	public BasicAtom getAtom() {
		return (BasicAtom) atom;
	}

	/**
	 * Returns a new copy of this literal whose {@link Literal#isNegated()} status is inverted
	 */
	@Override
	public BasicLiteralImpl negate() {
		return new BasicLiteralImpl(getAtom(), !positive);
	}

	/**
	 * @see AbstractAtom#substitute(Substitution)
	 */
	@Override
	public BasicLiteralImpl substitute(Substitution substitution) {
		return new BasicLiteralImpl((BasicAtom) getAtom().substitute(substitution), positive);
	}

	/**
	 * Set of all variables occurring in the Atom that are potentially binding, i.e., variables in positive atoms.
	 *
	 * @return
	 */
	@Override
	public Set<VariableTerm> getBindingVariables() {
		if (!positive) {
			// Negative literal has no binding variables.
			return Collections.emptySet();
		}
		Set<VariableTerm> bindingVariables = new HashSet<>();
		for (Term term : atom.getTerms()) {
			bindingVariables.addAll(term.getOccurringVariables());
		}
		return bindingVariables;
	}

	/**
	 * Set of all variables occurring in the Atom that are never binding, not even in positive atoms, e.g., variables in intervals or built-in
	 * atoms.
	 *
	 * @return
	 */
	@Override
	public Set<VariableTerm> getNonBindingVariables() {
		if (positive) {
			// Positive literal has only binding variables.
			return Collections.emptySet();
		}
		Set<VariableTerm> nonbindingVariables = new HashSet<>();
		for (Term term : atom.getTerms()) {
			nonbindingVariables.addAll(term.getOccurringVariables());
		}
		return nonbindingVariables;
	}
}
