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
package at.ac.tuwien.kr.alpha.core.atoms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.literals.FixedInterpretationLiteral;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.literals.AbstractLiteral;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;
import at.ac.tuwien.kr.alpha.commons.terms.IntervalTerm;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

/**
 * @see IntervalAtom
 */
public class IntervalLiteral extends AbstractLiteral implements FixedInterpretationLiteral {

	public IntervalLiteral(IntervalAtom atom) {
		super(atom, true);
	}

	@Override
	public IntervalAtom getAtom() {
		return (IntervalAtom) atom;
	}

	@Override
	public IntervalLiteral negate() {
		throw new UnsupportedOperationException("IntervalLiteral cannot be negated");
	}

	private List<Substitution> getIntervalSubstitutions(Substitution partialSubstitution) {
		List<Substitution> substitutions = new ArrayList<>();
		List<Term> terms = getTerms();
		Term intervalRepresentingVariable = terms.get(1);
		IntervalTerm intervalTerm = (IntervalTerm) terms.get(0);
		// Check whether intervalRepresentingVariable is bound already.
		if (intervalRepresentingVariable instanceof VariableTerm) {
			// Still a variable, generate all elements in the interval.
			for (int i = intervalTerm.getLowerBound(); i <= intervalTerm.getUpperBound(); i++) {
				Substitution ith = new BasicSubstitution(partialSubstitution);
				ith.put((VariableTerm) intervalRepresentingVariable, Terms.newConstant(i));
				substitutions.add(ith);
			}
			return substitutions;
		} else {
			// The intervalRepresentingVariable is bound already, check if it is in the interval.
			if (!(intervalRepresentingVariable instanceof ConstantTerm)
					|| !(((ConstantTerm<?>) intervalRepresentingVariable).getObject() instanceof Integer)) {
				// Term is not bound to an integer constant, not in the interval.
				return Collections.emptyList();
			}
			// TODO to avoid that type of unchecked cast, we may want interval terms to not extend AbstractTerm
			@SuppressWarnings("unchecked")
			Integer integer = ((ConstantTerm<Integer>) intervalRepresentingVariable).getObject();
			if (intervalTerm.getLowerBound() <= integer && integer <= intervalTerm.getUpperBound()) {
				return Collections.singletonList(partialSubstitution);
			}
			return Collections.emptyList();
		}
	}

	@Override
	public Set<VariableTerm> getBindingVariables() {
		Term term = getTerms().get(1);
		if (term instanceof VariableTerm) {
			return Collections.singleton((VariableTerm) term);
		}
		return Collections.emptySet();
	}

	@Override
	public Set<VariableTerm> getNonBindingVariables() {
		return Sets.newHashSet(getTerms().get(0).getOccurringVariables());
	}

	@Override
	public List<Substitution> getSatisfyingSubstitutions(Substitution partialSubstitution) {
		// Substitute variables occurring in the interval itself.
		IntervalLiteral groundInterval = substitute(partialSubstitution);
		// Generate all substitutions for the interval representing variable.
		return groundInterval.getIntervalSubstitutions(partialSubstitution);
	}

	@Override
	public IntervalLiteral substitute(Substitution substitution) {
		return new IntervalLiteral(getAtom().substitute(substitution));
	}
}
