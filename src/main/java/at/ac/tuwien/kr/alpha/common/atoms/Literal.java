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
package at.ac.tuwien.kr.alpha.common.atoms;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.Substitutable;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Substitution;
import org.apache.commons.collections4.SetUtils;

import java.util.List;
import java.util.Set;

/**
 * A potentially negated {@link Atom}
 * 
 * Copyright (c) 2017-2018, the Alpha Team.
 */
public abstract class Literal implements Substitutable<Literal> {
	
	protected final Atom atom;
	protected final boolean positive;
	
	public Literal(Atom atom, boolean positive) {
		this.atom = atom;
		this.positive = positive;
	}

	public Atom getAtom() {
		return atom;
	}

	public boolean isNegated() {
		return !positive;
	}
	
	public abstract Literal negate();
	
	public abstract Literal substitute(Substitution substitution);
	
	public abstract Set<VariableTerm> getBindingVariables();
	
	public abstract Set<VariableTerm> getNonBindingVariables();
	
	/**
	 * Union of {@link #getBindingVariables()} and {@link #getNonBindingVariables()}
	 */
	public Set<VariableTerm> getOccurringVariables() {
		return SetUtils.union(getBindingVariables(), getNonBindingVariables());
	}

	/**
	 * @see Atom#getPredicate()
	 */
	public Predicate getPredicate() {
		return atom.getPredicate();
	}

	/**
	 * @see Atom#getTerms()
	 */
	public List<Term> getTerms() {
		return atom.getTerms();
	}

	/**
	 * @see Atom#isGround()
	 */
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

		return atom.equals(that.atom) && positive == that.positive;
	}

	@Override
	public int hashCode() {
		return 12 * atom.hashCode() + (positive ? 1 : 0);
	}

}
