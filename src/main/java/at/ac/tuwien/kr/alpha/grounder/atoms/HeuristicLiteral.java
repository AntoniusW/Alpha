/**
 * Copyright (c) 2018 Siemens AG
 * All rights reserved.
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

import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.HashSet;
import java.util.Set;

public class HeuristicLiteral extends Literal {

	public HeuristicLiteral(HeuristicAtom atom, boolean negated) {
		super(atom, negated);
	}
	
	@Override
	public HeuristicAtom getAtom() {
		return (HeuristicAtom)atom;
	}

	@Override
	public HeuristicLiteral negate() {
		return new HeuristicLiteral(getAtom(), !positive);
	}

	@Override
	public HeuristicLiteral substitute(Substitution substitution) {
		return new HeuristicLiteral(getAtom().substitute(substitution), positive);
	}

	@Override
	public Set<VariableTerm> getBindingVariables() {
		Set<VariableTerm> bindingVariables = new HashSet<>();
		for (Term term : getTerms()) {
			bindingVariables.addAll(term.getOccurringVariables());
		}
		return bindingVariables;
	}

	@Override
	public Set<VariableTerm> getNonBindingVariables() {
		Set<VariableTerm> nonbindingVariables = new HashSet<>();
		for (Term term : getTerms()) {
			nonbindingVariables.addAll(term.getOccurringVariables());
		}
		return nonbindingVariables;
	}

}
