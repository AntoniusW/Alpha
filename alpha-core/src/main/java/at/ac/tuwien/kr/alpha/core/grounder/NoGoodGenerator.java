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

import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomOf;
import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomToLiteral;
import static at.ac.tuwien.kr.alpha.core.atoms.Literals.negateLiteral;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.FixedInterpretationLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.core.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.core.atoms.RuleAtom;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.programs.CompiledProgram;
import at.ac.tuwien.kr.alpha.core.rules.CompiledRule;

/**
 * Class to generate ground NoGoods out of non-ground rules and grounding substitutions.
 * Copyright (c) 2017-2018, the Alpha Team.
 */
public class NoGoodGenerator {
	private final AtomStore atomStore;
	private final ChoiceRecorder choiceRecorder;
	private final Map<Predicate, LinkedHashSet<Instance>> factsFromProgram;
	private final CompiledProgram programAnalysis;
	private final Set<CompiledRule> uniqueGroundRulePerGroundHead;

	NoGoodGenerator(AtomStore atomStore, ChoiceRecorder recorder, Map<Predicate, LinkedHashSet<Instance>> factsFromProgram, CompiledProgram programAnalysis, Set<CompiledRule> uniqueGroundRulePerGroundHead) {
		this.atomStore = atomStore;
		this.choiceRecorder = recorder;
		this.factsFromProgram = factsFromProgram;
		this.programAnalysis = programAnalysis;
		this.uniqueGroundRulePerGroundHead = uniqueGroundRulePerGroundHead;
	}

	/**
	 * Generates all NoGoods resulting from a non-ground rule and a variable substitution.
	 * 
	 * @param nonGroundRule
	 *          the non-ground rule.
	 * @param substitution
	 *          the grounding substitution, i.e., applying substitution to nonGroundRule results in a ground rule.
	 *          Assumption: atoms with fixed interpretation evaluate to true under the substitution.
	 * @return a list of the NoGoods corresponding to the ground rule.
	 */
	List<NoGood> generateNoGoodsFromGroundSubstitution(final CompiledRule nonGroundRule, final Substitution substitution) {
		final List<Integer> posLiterals = collectPosLiterals(nonGroundRule, substitution);
		final List<Integer> negLiterals = collectNegLiterals(nonGroundRule, substitution);

		if (posLiterals == null || negLiterals == null) {
			return emptyList();
		}

		// A constraint is represented by exactly one nogood.
		if (nonGroundRule.isConstraint()) {
			return singletonList(NoGood.fromConstraint(posLiterals, negLiterals));
		}

		final List<NoGood> result = new ArrayList<>();

		final Atom groundHeadAtom = nonGroundRule.getHeadAtom().substitute(substitution);
		final int headId = atomStore.putIfAbsent(groundHeadAtom);
		
		// Prepare atom representing the rule body.
		final RuleAtom bodyAtom = new RuleAtom(nonGroundRule, substitution);

		// Check uniqueness of ground rule by testing whether the
		// body representing atom already has an id.
		if (atomStore.contains(bodyAtom)) {
			// The current ground instance already exists,
			// therefore all nogoods have already been created.
			return emptyList();
		}

		final int bodyRepresentingLiteral = atomToLiteral(atomStore.putIfAbsent(bodyAtom));
		final int headLiteral = atomToLiteral(atomStore.putIfAbsent(nonGroundRule.getHeadAtom().substitute(substitution)));

		choiceRecorder.addHeadToBody(headId, atomOf(bodyRepresentingLiteral));
		
		// Create a nogood for the head.
		result.add(NoGood.headFirst(negateLiteral(headLiteral), bodyRepresentingLiteral));

		final NoGood ruleBody = NoGood.fromBody(posLiterals, negLiterals, bodyRepresentingLiteral);
		result.add(ruleBody);

		// Nogoods such that the atom representing the body is true iff the body is true.
		for (int j = 1; j < ruleBody.size(); j++) {
			result.add(new NoGood(bodyRepresentingLiteral, negateLiteral(ruleBody.getLiteral(j))));
		}

		// If the rule head is unique, add support.
		if (uniqueGroundRulePerGroundHead.contains(nonGroundRule)) {
			result.add(NoGood.support(headLiteral, bodyRepresentingLiteral));
		}

		// If the body of the rule contains negation, add choices.
		if (!negLiterals.isEmpty()) {
			result.addAll(choiceRecorder.generateChoiceNoGoods(posLiterals, negLiterals, bodyRepresentingLiteral));
		}

		return result;
	}

	List<Integer> collectNegLiterals(final CompiledRule nonGroundRule, final Substitution substitution) {
		final List<Integer> bodyLiteralsNegative = new ArrayList<>();
		for (Literal lit : nonGroundRule.getNegativeBody()) {
			Atom groundAtom = lit.getAtom().substitute(substitution);
			
			final Set<Instance> factInstances = factsFromProgram.get(groundAtom.getPredicate());

			if (factInstances != null && factInstances.contains(new Instance(groundAtom.getTerms()))) {
				// Negative atom that is always true encountered, skip whole rule as it will never fire.
				return null;
			}

			if (!existsRuleWithPredicateInHead(groundAtom.getPredicate())) {
				// Negative atom is no fact and no rule defines it, it is always false, skip it.
				continue;
			}

			bodyLiteralsNegative.add(atomToLiteral(atomStore.putIfAbsent(groundAtom)));
		}
		return bodyLiteralsNegative;
	}

	private List<Integer> collectPosLiterals(final CompiledRule nonGroundRule, final Substitution substitution) {
		final List<Integer> bodyLiteralsPositive = new ArrayList<>();
		for (Literal lit  : nonGroundRule.getPositiveBody()) {
			if (lit instanceof FixedInterpretationLiteral) {
				// TODO: conversion of atom to literal is ugly. NonGroundRule could manage atoms instead of literals, cf. FIXME there
				// Atom has fixed interpretation, hence was checked earlier that it
				// evaluates to true under the given substitution.
				// FixedInterpretationAtoms need not be shown to the solver, skip it.
				continue;
			}
			final Atom atom = lit.getAtom();
			// Skip the special enumeration atom.
			if (atom instanceof EnumerationAtom) {
				continue;
			}

			final Atom groundAtom = atom.substitute(substitution);

			// Consider facts to eliminate ground atoms from the generated nogoods that are always true
			// and eliminate nogoods that are always satisfied due to facts.
			Set<Instance> factInstances = factsFromProgram.get(groundAtom.getPredicate());
			if (factInstances != null && factInstances.contains(new Instance(groundAtom.getTerms()))) {
				// Skip positive atoms that are always true.
				continue;
			}

			if (!existsRuleWithPredicateInHead(groundAtom.getPredicate())) {
				// Atom is no fact and no rule defines it, it cannot be derived (i.e., is always false), skip whole rule as it will never fire.
				return null;
			}

			bodyLiteralsPositive.add(atomToLiteral(atomStore.putIfAbsent(groundAtom)));
		}
		return bodyLiteralsPositive;
	}

	private boolean existsRuleWithPredicateInHead(final Predicate predicate) {
		final HashSet<CompiledRule> definingRules = programAnalysis.getPredicateDefiningRules().get(predicate);
		return definingRules != null && !definingRules.isEmpty();
	}
}
