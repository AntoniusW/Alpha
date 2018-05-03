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
package at.ac.tuwien.kr.alpha.common;

import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static at.ac.tuwien.kr.alpha.Util.join;
import static at.ac.tuwien.kr.alpha.Util.oops;

/**
 * Represents a non-ground rule or a constraint for the semi-naive grounder.
 * Copyright (c) 2016-2018, the Alpha Team.
 */
public class Rule {
	private final Head head;
	private final List<Literal> body;

	public Rule(Head head, List<Literal> body) {
		this.head = head;
		// Remove duplicate body literals.
		LinkedHashSet<Literal> bodyLiterals = new LinkedHashSet<>(body);
		this.body = new ArrayList<>(bodyLiterals);

		if (!isSafe()) {
			// TODO: safety check needs to be adapted to solver what the solver actually understands. Will change in the future, adapt exception message accordingly.
			throw new RuntimeException("Encountered unsafe rule: " + toString() + System.lineSeparator()
					+ "Notice: A rule is considered safe if all variables occurring in negative literals, builtin atoms, and the head of the rule also occur in some positive literal.");
		}
	}

	public Head getHead() {
		return head;
	}

	public List<Literal> getBody() {
		return body;
	}

	/**
	 * Returns a new Rule that is equal to this one except that all variables are renamed to have the newVariablePostfix appended.
	 * @param newVariablePostfix
	 * @return
	 */
	public Rule renameVariables(String newVariablePostfix) {
		if (!head.isNormal()) {
			throw oops("Trying to rename variables in not-normal rule.");
		}
		List<VariableTerm> occurringVariables = new ArrayList<>();
		Atom headAtom = ((DisjunctiveHead)head).disjunctiveAtoms.get(0);
		occurringVariables.addAll(headAtom.getNonBindingVariables());
		occurringVariables.addAll(headAtom.getBindingVariables());
		for (Literal literal : body) {
			occurringVariables.addAll(literal.getNonBindingVariables());
			occurringVariables.addAll(literal.getBindingVariables());
		}
		Substitution variableReplacement = new Substitution();
		for (VariableTerm occurringVariable : occurringVariables) {
			final String newVariableName = occurringVariable.toString() + newVariablePostfix;
			variableReplacement.put(occurringVariable, VariableTerm.getInstance(newVariableName));
		}
		Atom renamedHeadAtom = headAtom.substitute(variableReplacement);
		ArrayList<Literal> renamedBody = new ArrayList<>(body.size());
		for (Literal literal : body) {
			renamedBody.add((Literal) literal.substitute(variableReplacement));
		}
		return new Rule(new DisjunctiveHead(Collections.singletonList(renamedHeadAtom)), renamedBody);
	}

	/**
	 * Checks whether a rule is safe.
	 * The actual safety condition may vary over the next improvements.
	 * Currently, a rule is safe iff all negated variables and all variables occurring in the
	 * head also occur in the positive body).
	 * @return true if this rule is safe.
	 */
	private boolean isSafe() {
		// TODO: do the real check.
		return true;
		/*Set<VariableTerm> positiveVariables = new HashSet<>();
		Set<VariableTerm> builtinVariables = new HashSet<>();

		// Check that all negative variables occur in the positive body.
		for (Literal literal : body) {
			// FIXME: The following five lines depend on concrete
			// implementations of the Atom interface. Not nice.
			if (literal instanceof BasicAtom) {
				positiveVariables.addAll(literal.getOccurringVariables());
			} else if (literal instanceof BuiltinAtom) {
				builtinVariables.addAll(literal.getOccurringVariables());
			}
		}

		for (Atom negAtom : bodyAtomsNegative) {
			for (VariableTerm term : negAtom.getOccurringVariables()) {
				if (!positiveVariables.contains(term)) {
					return false;
				}
			}
		}
		for (VariableTerm builtinVariable : builtinVariables) {
			if (!positiveVariables.contains(builtinVariable)) {
				return false;
			}
		}

		// Constraint are safe at this point
		if (isConstraint()) {
			return true;
		}

		// Check that all variables of the head occur in the positive body.
		List<VariableTerm> headVariables = head.getOccurringVariables();
		headVariables.removeAll(positiveVariables);
		return headVariables.isEmpty();
		*/
	}

	public boolean isGround() {
		if (!head.isNormal()) {
			throw oops("Called isGround on non-normal rule");
		}
		if (!isConstraint() && !((DisjunctiveHead)head).isGround()) {
			return false;
		}
		for (Literal literal : body) {
			if (!literal.isGround()) {
				return false;
			}
		}
		return true;
	}

	public boolean isConstraint() {
		return head == null;
	}

	@Override
	public String toString() {
		return join((isConstraint() ? "" : head + " ") + ":- ", body, ".");
	}
}