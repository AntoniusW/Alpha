/**
 * Copyright (c) 2016-2018, the Alpha Team.
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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.Util;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

public class ChoiceAtom implements Atom {

	public static final Predicate ON = Predicates.getPredicate("ChoiceOn", 1, true, true);
	public static final Predicate OFF = Predicates.getPredicate("ChoiceOff", 1, true, true);

	private final Predicate predicate;
	private final ConstantTerm<String> term;

	private ChoiceAtom(Predicate predicate, ConstantTerm<String> term) {
		this.predicate = predicate;
		this.term = term;
	}

	private ChoiceAtom(Predicate predicate, int id) {
		this(predicate, Terms.newConstant(Integer.toString(id)));
	}

	public static ChoiceAtom on(int id) {
		return new ChoiceAtom(ON, id);
	}

	public static ChoiceAtom off(int id) {
		return new ChoiceAtom(OFF, id);
	}

	@Override
	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public List<Term> getTerms() {
		return Collections.singletonList(term);
	}

	@Override
	public boolean isGround() {
		// NOTE: Term is a ConstantTerm, which is ground by definition.
		return true;
	}

	@Override
	public Literal toLiteral(boolean negated) {
		throw new UnsupportedOperationException("ChoiceAtom cannot be literalized");
	}

	@Override
	public ChoiceAtom substitute(Substitution substitution) {
		return this;
	}

	@Override
	public String toString() {
		return Util.join(predicate.getName() + "(", getTerms(), ")");
	}

	@Override
	public Atom withTerms(List<Term> terms) {
		throw new UnsupportedOperationException("Changing terms is not supported for ChoiceAtoms!");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.predicate == null) ? 0 : this.predicate.hashCode());
		result = prime * result + ((this.getTerms() == null) ? 0 : this.getTerms().hashCode());
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
		if (!(obj instanceof ChoiceAtom)) {
			return false;
		}
		ChoiceAtom other = (ChoiceAtom) obj;
		if (this.predicate == null) {
			if (other.predicate != null) {
				return false;
			}
		} else if (!this.predicate.equals(other.predicate)) {
			return false;
		}
		if (this.getTerms() == null) {
			if (other.getTerms() != null) {
				return false;
			}
		} else if (!this.getTerms().equals(other.getTerms())) {
			return false;
		}
		return true;
	}

	@Override
	public Set<VariableTerm> getOccurringVariables() {
		return Collections.emptySet();
	}

	@Override
	public Atom renameVariables(String newVariablePrefix) {
		throw new UnsupportedOperationException("ChoiceAtom does not have any variables to rename!");
	}
}