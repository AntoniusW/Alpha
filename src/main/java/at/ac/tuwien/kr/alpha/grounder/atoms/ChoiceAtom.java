/**
 * Copyright (c) 2016-2019, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.grounder.atoms;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.Collections;
import java.util.List;

import static at.ac.tuwien.kr.alpha.Util.join;

public class ChoiceAtom extends BasicAtom {
	public static final Predicate ON = Predicate.getInstance("ChoiceOn", 1, true, true);
	public static final Predicate OFF = Predicate.getInstance("ChoiceOff", 1, true, true);

	private ChoiceAtom(Predicate predicate, Term term) {
		super(predicate, Collections.singletonList(term));
	}

	private ChoiceAtom(Predicate predicate, int id) {
		this(predicate, ConstantTerm.getInstance(Integer.toString(id)));
	}

	public static ChoiceAtom on(int id) {
		return new ChoiceAtom(ON, id);
	}

	public static ChoiceAtom off(int id) {
		return new ChoiceAtom(OFF, id);
	}

	@Override
	public boolean isGround() {
		// NOTE: Term is a ConstantTerm, which is ground by definition.
		return true;
	}

	@Override
	public BasicLiteral toLiteral(boolean negated) {
		throw new UnsupportedOperationException(this.getClass().getName() + " cannot be literalized");
	}

	@Override
	public ChoiceAtom substitute(Substitution substitution) {
		return this;
	}

	@Override
	public String toString() {
		return join(predicate.getName() + "(", terms, ")");
	}

	@Override
	public ChoiceAtom withTerms(List<Term> terms) {
		throw new UnsupportedOperationException("Changing terms is not supported for ChoiceAtoms!");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.predicate == null) ? 0 : this.predicate.hashCode());
		result = prime * result + ((this.terms == null) ? 0 : this.terms.hashCode());
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
		if (this.terms == null) {
			if (other.terms != null) {
				return false;
			}
		} else if (!this.terms.equals(other.terms)) {
			return false;
		}
		return true;
	}
}