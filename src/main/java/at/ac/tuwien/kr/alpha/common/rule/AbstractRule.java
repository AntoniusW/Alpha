/*
 * Copyright (c) 2019-2021, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.rule.head.Head;
import org.apache.commons.collections4.SetUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * An abstract representation of a rule with a specific type of @{link Head} (type parameter H)
 * 
 * @param <H> the type of head for this rule
 * 
 *            Copyright (c) 2017-2019, the Alpha Team.
 */
public abstract class AbstractRule<H extends Head> {

	private final H head;
	private final Set<Literal> bodyLiteralsPositive;
	private final Set<Literal> bodyLiteralsNegative;

	public AbstractRule(H head, Iterable<Literal> body) {
		this.head = head;
		Set<Literal> positiveBody = new LinkedHashSet<>();
		Set<Literal> negativeBody = new LinkedHashSet<>();
		for (Literal bodyLiteral : body) {
			if (bodyLiteral.isNegated()) {
				negativeBody.add(bodyLiteral);
			} else {
				positiveBody.add(bodyLiteral);
			}
		}
		this.bodyLiteralsPositive = Collections.unmodifiableSet(positiveBody);
		this.bodyLiteralsNegative = Collections.unmodifiableSet(negativeBody);

		if (!this.isSafe()) {
			// TODO: safety check needs to be adapted to solver what the solver actually understands. Will change in the future,
			// adapt exception message accordingly.
			throw new RuntimeException("Encountered unsafe rule: " + toString() + System.lineSeparator()
					+ "Notice: A rule is considered safe if all variables occurring in negative literals, builtin atoms, and the head of the rule also occur in some positive literal.");
		}
	}

	/**
	 * Checks whether a rule is safe. The actual safety condition may vary over the next improvements. Currently, a rule is
	 * safe iff all negated variables and
	 * all variables occurring in the head also occur in the positive body).
	 * 
	 * @return true if this rule is safe.
	 */
	private boolean isSafe() {
		// TODO: do the real check.
		// Note that - once a proper safety check is implemented - that check should probably be specific for each rule
		// implementation, therefore this method should be "protected abstract" here and implemented in each subclass.
		return true;
		/*
		 * Set<VariableTerm> positiveVariables = new HashSet<>(); Set<VariableTerm> builtinVariables = new HashSet<>();
		 * 
		 * // Check that all negative variables occur in the positive body. for (Literal literal : body) { // FIXME: The
		 * following five lines depend on concrete
		 * // implementations of the Atom interface. Not nice. if (literal instanceof BasicAtom) {
		 * positiveVariables.addAll(literal.getOccurringVariables()); }
		 * else if (literal instanceof BuiltinAtom) { builtinVariables.addAll(literal.getOccurringVariables()); } }
		 * 
		 * for (Atom negAtom : bodyAtomsNegative) { for (VariableTerm term : negAtom.getOccurringVariables()) { if
		 * (!positiveVariables.contains(term)) { return
		 * false; } } } for (VariableTerm builtinVariable : builtinVariables) { if
		 * (!positiveVariables.contains(builtinVariable)) { return false; } }
		 * 
		 * // Constraint are safe at this point if (isConstraint()) { return true; }
		 * 
		 * // Check that all variables of the head occur in the positive body. List<VariableTerm> headVariables =
		 * head.getOccurringVariables();
		 * headVariables.removeAll(positiveVariables); return headVariables.isEmpty();
		 */
	}

	public boolean isConstraint() {
		return head == null;
	}

	@Override
	public String toString() {
		return Util.join((isConstraint() ? "" : head.toString() + " ") + ":- ", getBody(), ".");
	}

	public H getHead() {
		return head;
	}

	public Set<Literal> getBody() {
		return SetUtils.union(this.bodyLiteralsPositive, this.bodyLiteralsNegative);
	}

	public Set<Literal> getPositiveBody() {
		return this.bodyLiteralsPositive;
	}

	public Set<Literal> getNegativeBody() {
		return this.bodyLiteralsNegative;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bodyLiteralsNegative, bodyLiteralsPositive, head);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AbstractRule)) {
			return false;
		}
		AbstractRule<?> other = (AbstractRule<?>) obj;
		return Objects.equals(this.bodyLiteralsNegative, other.bodyLiteralsNegative)
				&& Objects.equals(this.bodyLiteralsPositive, other.bodyLiteralsPositive)
				&& Objects.equals(this.head, other.head);
	}

}
