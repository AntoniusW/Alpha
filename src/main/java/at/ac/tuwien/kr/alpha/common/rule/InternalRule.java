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
package at.ac.tuwien.kr.alpha.common.rule;

import static at.ac.tuwien.kr.alpha.Util.join;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.IntIdGenerator;
import at.ac.tuwien.kr.alpha.grounder.RuleGroundingOrders;
import at.ac.tuwien.kr.alpha.grounder.Unifier;

/**
 * Represents a normal rule or a constraint for the semi-naive grounder. A normal rule has one (or no if it's a constraint) atom in it's head.
 */
public class InternalRule extends NormalRule {

	private static final IntIdGenerator ID_GENERATOR = new IntIdGenerator();

	private final int ruleId;

	private final List<Atom> bodyAtomsPositive;
	private final List<Atom> bodyAtomsNegative;
	private final List<Predicate> occurringPredicates;

	private final RuleGroundingOrders groundingOrders;

	public InternalRule(NormalHead head, List<Literal> body) {
		super(head, body);
		if (body.isEmpty()) {
			throw new IllegalArgumentException(
					"Empty bodies are not supported for InternalRule! (Head = " + (head == null ? "NULL" : head.getAtom().toString()) + ")");
		}
		this.ruleId = InternalRule.ID_GENERATOR.getNextId();

		final List<Atom> pos = new ArrayList<>(body.size() / 2);
		final List<Atom> neg = new ArrayList<>(body.size() / 2);

		this.occurringPredicates = new ArrayList<>();
		if (!isConstraint()) {
			this.occurringPredicates.add(this.getHeadAtom().getPredicate());
		}

		for (Literal literal : body) {
			if (literal instanceof AggregateLiteral) {
				throw new IllegalArgumentException("AggregateLiterals aren't supported in InternalRules! (lit: " + literal.toString() + ")");
			}
			(literal.isNegated() ? neg : pos).add(literal.getAtom());
			this.occurringPredicates.add(literal.getPredicate());
		}

		// Sort for better join order.
		this.bodyAtomsPositive = Collections.unmodifiableList(pos);

		// Since rule is safe, all variables in the negative body are already bound,
		// i.e., joining them cannot degenerate into cross-product.
		// Hence, there is no need to sort them.
		this.bodyAtomsNegative = Collections.unmodifiableList(neg);

		// not needed, done in AbstractRule! Leaving it commented out for future reference since this might actually be the proper place to put it
		// this.checkSafety();

		this.groundingOrders = new RuleGroundingOrders(this);
		this.groundingOrders.computeGroundingOrders();
	}

	@VisibleForTesting
	public static void resetIdGenerator() {
		InternalRule.ID_GENERATOR.resetGenerator();
	}	
	
	public static InternalRule fromNormalRule(NormalRule rule) {
		return new InternalRule(rule.isConstraint() ? null : new NormalHead(rule.getHead().getAtom()), new ArrayList<>(rule.getBody()));
	}

	/**
	 * Returns a new Rule that is equal to this one except that all variables are renamed to have the newVariablePostfix appended.
	 * 
	 * @param newVariablePostfix
	 * @return
	 */
	public InternalRule renameVariables(String newVariablePostfix) {
		if (!this.getHead().isNormal()) {
			throw Util.oops("Trying to rename variables in not-normal rule.");
		}
		List<VariableTerm> occurringVariables = new ArrayList<>();
		Atom headAtom = this.getHeadAtom();
		occurringVariables.addAll(headAtom.getOccurringVariables());
		for (Literal literal : this.getBody()) {
			occurringVariables.addAll(literal.getOccurringVariables());
		}
		Unifier variableReplacement = new Unifier();
		for (VariableTerm occurringVariable : occurringVariables) {
			final String newVariableName = occurringVariable.toString() + newVariablePostfix;
			variableReplacement.put(occurringVariable, VariableTerm.getInstance(newVariableName));
		}
		Atom renamedHeadAtom = headAtom.substitute(variableReplacement);
		ArrayList<Literal> renamedBody = new ArrayList<>(this.getBody().size());
		for (Literal literal : this.getBody()) {
			renamedBody.add(literal.substitute(variableReplacement));
		}
		return new InternalRule(new NormalHead(renamedHeadAtom), renamedBody);
	}

	/**
	 *
	 * @return a list of all ordinary predicates occurring in the rule (may contain duplicates, does not contain builtin atoms).
	 */
	public List<Predicate> getOccurringPredicates() {
		return this.occurringPredicates;
	}

	@Override
	public String toString() {
		//@formatter:off
		return join(
				join(
						(isConstraint() ? "" : this.getHeadAtom() + " ") + ":- ", 
						this.bodyAtomsPositive, 
						this.bodyAtomsNegative.size() > 0 ? ", not " : ""
				),
				this.bodyAtomsNegative, 
				", not ", 
				".");
		//@formatter:on
	}

	public List<Atom> getBodyAtomsPositive() {
		return this.bodyAtomsPositive;
	}

	public List<Atom> getBodyAtomsNegative() {
		return this.bodyAtomsNegative;
	}

	public RuleGroundingOrders getGroundingOrders() {
		return this.groundingOrders;
	}

	public int getRuleId() {
		return this.ruleId;
	}
}
