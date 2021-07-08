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
package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.head.DisjunctiveHead;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Unifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Removes variable equalities from rules by replacing one variable with the other.
 *
 * Copyright (c) 2017-2021, the Alpha Team.
 */
public class VariableEqualityRemoval extends ProgramTransformation<InputProgram, InputProgram> {

	@Override
	public InputProgram apply(InputProgram inputProgram) {
		List<BasicRule> rewrittenRules = new ArrayList<>();
		for (BasicRule rule : inputProgram.getRules()) {
			rewrittenRules.add(findAndReplaceVariableEquality(rule));
		}
		return new InputProgram(rewrittenRules, inputProgram.getFacts(), inputProgram.getInlineDirectives());
	}

	private BasicRule findAndReplaceVariableEquality(BasicRule rule) {
		// Collect all equal variables.
		HashMap<VariableTerm, HashSet<VariableTerm>> variableToEqualVariables = new LinkedHashMap<>();
		HashSet<Literal> equalitiesToRemove = new HashSet<>();
		for (Literal bodyElement : rule.getBody()) {
			if (!(bodyElement instanceof ComparisonLiteral)) {
				continue;
			}
			ComparisonLiteral comparisonLiteral = (ComparisonLiteral) bodyElement;
			if (!comparisonLiteral.isNormalizedEquality()) {
				continue;
			}
			if (comparisonLiteral.getTerms().get(0) instanceof VariableTerm && comparisonLiteral.getTerms().get(1) instanceof VariableTerm) {
				VariableTerm leftVariable = (VariableTerm) comparisonLiteral.getTerms().get(0);
				VariableTerm rightVariable = (VariableTerm) comparisonLiteral.getTerms().get(1);
				HashSet<VariableTerm> leftEqualVariables = variableToEqualVariables.get(leftVariable);
				HashSet<VariableTerm> rightEqualVariables = variableToEqualVariables.get(rightVariable);
				if (leftEqualVariables == null && rightEqualVariables == null) {
					HashSet<VariableTerm> equalVariables = new LinkedHashSet<>(Arrays.asList(leftVariable, rightVariable));
					variableToEqualVariables.put(leftVariable, equalVariables);
					variableToEqualVariables.put(rightVariable, equalVariables);
				}
				if (leftEqualVariables == null && rightEqualVariables != null) {
					rightEqualVariables.add(leftVariable);
					variableToEqualVariables.put(leftVariable, rightEqualVariables);
				}
				if (leftEqualVariables != null && rightEqualVariables == null) {
					leftEqualVariables.add(rightVariable);
					variableToEqualVariables.put(rightVariable, leftEqualVariables);
				}
				if (leftEqualVariables != null && rightEqualVariables != null) {
					leftEqualVariables.addAll(rightEqualVariables);
					for (VariableTerm rightEqualVariable : rightEqualVariables) {
						variableToEqualVariables.put(rightEqualVariable, leftEqualVariables);
					}
				}
				equalitiesToRemove.add(comparisonLiteral);
			}
		}
		if (variableToEqualVariables.isEmpty()) {
			// Skip rule if there is no equality between variables.
			return rule;
		}

		List<Literal> rewrittenBody = new ArrayList<>(rule.getBody());
		if (!rule.isConstraint() && rule.getHead() instanceof DisjunctiveHead) {
			throw new UnsupportedOperationException("VariableEqualityRemoval cannot be applied to rule with DisjunctiveHead, yet.");
		}
		NormalHead rewrittenHead = rule.isConstraint() ? null : new NormalHead(((NormalHead)rule.getHead()).getAtom());

		// Use substitution for actual replacement.
		Unifier replacementSubstitution = new Unifier();
		// For each set of equal variables, take the first variable and replace all others by it.
		for (Map.Entry<VariableTerm, HashSet<VariableTerm>> variableEqualityEntry : variableToEqualVariables.entrySet()) {
			VariableTerm variableToReplace = variableEqualityEntry.getKey();
			VariableTerm replacementVariable = variableEqualityEntry.getValue().iterator().next();
			if (variableToReplace == replacementVariable) {
				continue;
			}
			replacementSubstitution.put(variableToReplace, replacementVariable);
		}
		// Replace/Substitute in each literal every term where one of the common variables occurs.
		Iterator<Literal> bodyIterator = rewrittenBody.iterator();
		while (bodyIterator.hasNext()) {
			Literal literal = bodyIterator.next();
			if (equalitiesToRemove.contains(literal)) {
				bodyIterator.remove();
			}
			for (int i = 0; i < literal.getTerms().size(); i++) {
				Term replaced = literal.getTerms().get(i).substitute(replacementSubstitution);
				literal.getTerms().set(i, replaced);
			}
		}
		// Replace variables in head.
		if (rewrittenHead != null) {
			Atom headAtom = rewrittenHead.getAtom();
			for (int i = 0; i < headAtom.getTerms().size(); i++) {
				Term replaced = headAtom.getTerms().get(i).substitute(replacementSubstitution);
				headAtom.getTerms().set(i, replaced);
			}
		}
		return new BasicRule(rewrittenHead, rewrittenBody);
	}
}
