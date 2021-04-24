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
package at.ac.tuwien.kr.alpha.core.programs.transformation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.rules.ChoiceHead;
import at.ac.tuwien.kr.alpha.api.rules.Head;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.terms.IntervalTerm;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.programs.InputProgram;
import at.ac.tuwien.kr.alpha.core.rules.BasicRule;
import at.ac.tuwien.kr.alpha.core.rules.heads.ChoiceHeadImpl;
import at.ac.tuwien.kr.alpha.core.rules.heads.NormalHeadImpl;

/**
 * Copyright (c) 2017-2020, the Alpha Team.
 */
// TODO this could already give NormalProgram as result type
public class ChoiceHeadToNormal extends ProgramTransformation<ASPCore2Program, ASPCore2Program> {
	private final static String PREDICATE_NEGATION_PREFIX = "_n";

	@Override
	public ASPCore2Program apply(ASPCore2Program inputProgram) {
		InputProgram.Builder programBuilder = InputProgram.builder();
		List<Rule<Head>> additionalRules = new ArrayList<>();

		List<Rule<Head>> srcRules = new ArrayList<>(inputProgram.getRules());
		Iterator<Rule<Head>> ruleIterator = srcRules.iterator();
		while (ruleIterator.hasNext()) {
			Rule<Head> rule = ruleIterator.next();

			Head ruleHead = rule.getHead();
			if (!(ruleHead instanceof ChoiceHead)) {
				// Rule is constraint or without choice in the head. Leave as is.
				continue;
			}

			// Remove this rule, as it will be transformed.
			ruleIterator.remove();

			ChoiceHeadImpl choiceHead = (ChoiceHeadImpl) ruleHead;
			// Choice rules with boundaries are not yet supported.
			if (choiceHead.getLowerBound() != null || choiceHead.getUpperBound() != null) {
				throw new UnsupportedOperationException("Found choice rule with bounds, which are not yet supported. Rule is: " + rule);
			}

			// Only rewrite rules with a choice in their head.
			for (ChoiceHeadImpl.ChoiceElement choiceElement : choiceHead.getChoiceElements()) {
				// Create two guessing rules for each choiceElement.

				// Construct common body to both rules.
				Atom head = choiceElement.getChoiceAtom();
				List<Literal> ruleBody = new ArrayList<>(rule.getBody());
				ruleBody.addAll(choiceElement.getConditionLiterals());

				if (containsIntervalTerms(head)) {
					throw new RuntimeException("Program contains a choice rule with interval terms in its head. This is not supported (yet).");
				}

				// Construct head atom for the choice.
				Predicate headPredicate = head.getPredicate();

				Predicate negPredicate = Predicates.getPredicate(PREDICATE_NEGATION_PREFIX + headPredicate.getName(), headPredicate.getArity() + 1, true);
				List<Term> headTerms = new ArrayList<>(head.getTerms());
				headTerms.add(0, Terms.newConstant("1")); // FIXME: when introducing classical negation, this is 1 for classical positive atoms and 0 for
																	// classical negative atoms.
				Atom negHead = Atoms.newBasicAtom(negPredicate, headTerms);

				// Construct two guessing rules.
				List<Literal> guessingRuleBodyWithNegHead = new ArrayList<>(ruleBody);
				guessingRuleBodyWithNegHead.add(Atoms.newBasicAtom(head.getPredicate(), head.getTerms()).toLiteral(false));
				additionalRules.add(new BasicRule(new NormalHeadImpl(negHead), guessingRuleBodyWithNegHead));

				List<Literal> guessingRuleBodyWithHead = new ArrayList<>(ruleBody);
				guessingRuleBodyWithHead.add(Atoms.newBasicAtom(negPredicate, headTerms).toLiteral(false));
				additionalRules.add(new BasicRule(new NormalHeadImpl(head), guessingRuleBodyWithHead));

				// TODO: when cardinality constraints are possible, process the boundaries by adding a constraint with a cardinality check.
			}
		}
		return programBuilder.addRules(srcRules).addRules(additionalRules).addFacts(inputProgram.getFacts())
				.addInlineDirectives(inputProgram.getInlineDirectives()).build();
	}

	private static boolean containsIntervalTerms(Atom atom) {
		for (Term term : atom.getTerms()) {
			if (IntervalTerm.termContainsIntervalTerm(term)) {
				return true;
			}
		}
		return false;
	}
}
