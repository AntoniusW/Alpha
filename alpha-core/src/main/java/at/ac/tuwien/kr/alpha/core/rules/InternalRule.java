/**
 * Copyright (c) 2016-2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.core.rules;

import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.AggregateLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.rules.NormalHead;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.substitutions.Unifier;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.commons.util.IntIdGenerator;
import at.ac.tuwien.kr.alpha.core.grounder.RuleGroundingInfoImpl;
import at.ac.tuwien.kr.alpha.core.rules.heads.NormalHeadImpl;

/**
 * Represents a normal rule or a constraint for the semi-naive grounder.
 * A normal rule has no head atom if it represents a constraint, otherwise it has one atom in its head.
 */
public class InternalRule extends NormalRuleImpl implements CompiledRule {

	private static final IntIdGenerator ID_GENERATOR = new IntIdGenerator();

	private final int ruleId;

	private final List<Predicate> occurringPredicates;

	private final RuleGroundingInfoImpl groundingOrders;

	public InternalRule(NormalHead head, List<Literal> body) {
		super(head, body);
		if (body.isEmpty()) {
			throw new IllegalArgumentException(
					"Empty bodies are not supported for InternalRule! (Head = " + (head == null ? "NULL" : head.getAtom().toString()) + ")");
		}
		this.ruleId = InternalRule.ID_GENERATOR.getNextId();

		this.occurringPredicates = new ArrayList<>();
		if (!isConstraint()) {
			this.occurringPredicates.add(this.getHeadAtom().getPredicate());
		}

		for (Literal literal : body) {
			if (literal instanceof AggregateLiteral) {
				throw new IllegalArgumentException("AggregateLiterals aren't supported in InternalRules! (lit: " + literal.toString() + ")");
			}
			this.occurringPredicates.add(literal.getPredicate());
		}

		// not needed, done in AbstractRule! Leaving it commented out for future reference since this might actually be the
		// proper place to put it
		// this.checkSafety();

		this.groundingOrders = new RuleGroundingInfoImpl(this);
		this.groundingOrders.computeGroundingOrders();
	}

	@VisibleForTesting
	public static void resetIdGenerator() {
		InternalRule.ID_GENERATOR.resetGenerator();
	}

	public static CompiledRule fromNormalRule(Rule<NormalHead> rule) {
		return new InternalRule(rule.isConstraint() ? null : new NormalHeadImpl(rule.getHeadAtom()), new ArrayList<>(rule.getBody()));
	}

	/**
	 * Returns a new Rule that is equal to this one except that all variables are renamed to have the newVariablePostfix
	 * appended.
	 * 
	 * @param newVariablePostfix
	 * @return
	 */
	@Override
	public InternalRule renameVariables(String newVariablePostfix) {
		List<VariableTerm> occurringVariables = new ArrayList<>();
		Atom headAtom = this.getHeadAtom();
		occurringVariables.addAll(headAtom.getOccurringVariables());
		for (Literal literal : this.getBody()) {
			occurringVariables.addAll(literal.getOccurringVariables());
		}
		Unifier variableReplacement = new Unifier();
		for (VariableTerm occurringVariable : occurringVariables) {
			final String newVariableName = occurringVariable.toString() + newVariablePostfix;
			variableReplacement.put(occurringVariable, Terms.newVariable(newVariableName));
		}
		Atom renamedHeadAtom = headAtom.substitute(variableReplacement);
		ArrayList<Literal> renamedBody = new ArrayList<>(this.getBody().size());
		for (Literal literal : this.getBody()) {
			renamedBody.add(literal.substitute(variableReplacement));
		}
		return new InternalRule(new NormalHeadImpl(renamedHeadAtom), renamedBody);
	}

	/**
	 * Returns the predicates occurring in this rule.
	 * @return a list of all predicates occurring in the rule (may contain duplicates and builtin atoms).
	 */
	@Override
	public List<Predicate> getOccurringPredicates() {
		return this.occurringPredicates;
	}

	@Override
	public RuleGroundingInfoImpl getGroundingInfo() {
		return this.groundingOrders;
	}

	@Override
	public int getRuleId() {
		return this.ruleId;
	}

}
