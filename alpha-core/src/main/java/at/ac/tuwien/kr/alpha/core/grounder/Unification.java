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
package at.ac.tuwien.kr.alpha.core.grounder;

import static at.ac.tuwien.kr.alpha.api.Util.oops;

import java.util.Set;

import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.ArithmeticTerm;
import at.ac.tuwien.kr.alpha.api.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.substitutions.Unifier;

/**
 * Copyright (c) 2017, the Alpha Team.
 */
public class Unification {

	/**
	 * Computes the most-general-unifier of two atoms that share no common variables.
	 * @param left the one atom to unify with
	 * @param right to other atom to unify with
	 * @return the most-general-unifier of the two given atoms if it exists, and null otherwise.
	 */
	public static Unifier unifyAtoms(Atom left, Atom right) {
		return unifyAtoms(left, right, false);
	}

	/**
	 * Instantiates the general Atom to match the specific one and returns the corresponding substitution.
	 * @param general the general Atom to instantiate.
	 * @param specific the specific Atom.
	 * @return a Unifier sigma such that specific == general.substitute(sigma), returns null if no such sigma exists.
	 */
	public static Unifier instantiate(Atom general, Atom specific) {
		return unifyAtoms(specific, general, true);
	}

	private static Unifier unifyAtoms(Atom left, Atom right, boolean keepLeftAsIs) {
		Set<VariableTerm> leftOccurringVariables = left.getOccurringVariables();
		Set<VariableTerm> rightOccurringVaribles = right.getOccurringVariables();
		boolean leftSmaller = leftOccurringVariables.size() < rightOccurringVaribles.size();
		Set<VariableTerm> smallerSet = leftSmaller ? leftOccurringVariables : rightOccurringVaribles;
		Set<VariableTerm> largerSet = leftSmaller ? rightOccurringVaribles : leftOccurringVariables;
		for (VariableTerm variableTerm : smallerSet) {
			if (largerSet.contains(variableTerm)) {
				throw oops("Left and right atom share variables.");
			}
		}
		Unifier mgu = new Unifier();
		if (!left.getPredicate().equals(right.getPredicate())) {
			return null;
		}
		for (int i = 0; i < left.getPredicate().getArity(); i++) {
			final Term leftTerm = left.getTerms().get(i);
			final Term rightTerm = right.getTerms().get(i);
			if (!unifyTerms(leftTerm, rightTerm, mgu, keepLeftAsIs)) {
				return null;
			}
		}
		return mgu;
	}

	private static boolean unifyTerms(Term left, Term right, Unifier currentSubstitution, boolean keepLeftAsIs) {
		final Term leftSubs = left.substitute(currentSubstitution);
		final Term rightSubs = right.substitute(currentSubstitution);
		if (leftSubs == rightSubs) {
			return true;
		}
		if (!keepLeftAsIs && leftSubs instanceof VariableTerm && !currentSubstitution.isVariableSet((VariableTerm) leftSubs)) {
			currentSubstitution.put((VariableTerm) leftSubs, rightSubs);
			return true;
		}
		if (rightSubs instanceof VariableTerm && !currentSubstitution.isVariableSet((VariableTerm) rightSubs)) {
			currentSubstitution.put((VariableTerm) rightSubs, leftSubs);
			return true;
		}
		if (leftSubs instanceof FunctionTerm && rightSubs instanceof FunctionTerm) {
			final FunctionTerm leftFunction = (FunctionTerm) leftSubs;
			final FunctionTerm rightFunction = (FunctionTerm) rightSubs;
			if (!leftFunction.getSymbol().equals(rightFunction.getSymbol())
				|| leftFunction.getTerms().size() != rightFunction.getTerms().size()) {
				return false;
			}
			for (int i = 0; i < leftFunction.getTerms().size(); i++) {
				final Term leftTerm = leftFunction.getTerms().get(i);
				final Term rightTerm = rightFunction.getTerms().get(i);
				if (!unifyTerms(leftTerm, rightTerm, currentSubstitution, keepLeftAsIs)) {
					return false;
				}
			}
			return true;
		}
		if (leftSubs instanceof ArithmeticTerm && rightSubs instanceof ArithmeticTerm) {
			// ArithmeticTerms are similar to FunctionTerms, i.e. if the operator is the same and its subterms unify, the ArithmeticTerms unify.
			final ArithmeticTerm leftArithmeticTerm = (ArithmeticTerm) leftSubs;
			final ArithmeticTerm rightArithmeticTerm = (ArithmeticTerm) rightSubs;
			if (!leftArithmeticTerm.getOperator().equals(rightArithmeticTerm.getOperator())) {
				return false;
			}
			final Term leftTermLeftSubterm = leftArithmeticTerm.getLeftOperand();
			final Term rightTermLeftSubterm = rightArithmeticTerm.getLeftOperand();
			if (!unifyTerms(leftTermLeftSubterm, rightTermLeftSubterm, currentSubstitution, keepLeftAsIs)) {
				return false;
			}
			final Term leftTermRightSubterm = leftArithmeticTerm.getRightOperand();
			final Term rightTermRightSubterm = rightArithmeticTerm.getRightOperand();
			if (!unifyTerms(leftTermRightSubterm, rightTermRightSubterm, currentSubstitution, keepLeftAsIs)) {
				return false;
			}
			return true;
		}
		return false;
	}
}
