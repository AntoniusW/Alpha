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

import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;

/**
 * A potentially negated {@link AbstractAtom}
 *
 * Copyright (c) 2017-2018, the Alpha Team.
 */
public abstract class AbstractLiteral implements Literal {

	protected final Atom atom;
	protected final boolean positive;

	public AbstractLiteral(Atom atom, boolean positive) {
		this.atom = atom;
		this.positive = positive;
	}

	@Override
	public Atom getAtom() {
		return atom;
	}

	@Override
	public boolean isNegated() {
		return !positive;
	}

	@Override
	public abstract Literal negate();

	@Override
	public abstract Literal substitute(Substitution substitution);

	@Override
	public abstract Set<VariableTerm> getBindingVariables();

	@Override
	public abstract Set<VariableTerm> getNonBindingVariables();

	/**
	 * Union of {@link #getBindingVariables()} and {@link #getNonBindingVariables()}
	 */
	@Override
	public Set<VariableTerm> getOccurringVariables() {
		return SetUtils.union(getBindingVariables(), getNonBindingVariables());
	}

	/**
	 * @see AbstractAtom#getPredicate()
	 */
	@Override
	public Predicate getPredicate() {
		return atom.getPredicate();
	}

	/**
	 * @see AbstractAtom#getTerms()
	 */
	@Override
	public List<Term> getTerms() {
		return atom.getTerms();
	}

	/**
	 * @see AbstractAtom#isGround()
	 */
	@Override
	public boolean isGround() {
		return atom.isGround();
	}

	@Override
	public String toString() {
		return (positive ? "" : "not ") + atom.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Literal that = (Literal) o;

		return atom.equals(that.getAtom()) && positive == !that.isNegated();
	}

	@Override
	public int hashCode() {
		return 12 * atom.hashCode() + (positive ? 1 : 0);
	}

}
