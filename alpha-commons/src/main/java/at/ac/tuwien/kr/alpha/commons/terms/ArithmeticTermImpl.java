/**
 * Copyright (c) 2017-2019, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.commons.terms;

import java.util.LinkedHashSet;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.terms.ArithmeticOperator;
import at.ac.tuwien.kr.alpha.api.terms.ArithmeticTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.util.Interner;

/**
 * This class represents an arithmetic expression occurring as a term.
 * Copyright (c) 2017-2019, the Alpha Team.
 */
class ArithmeticTermImpl extends AbstractTerm implements ArithmeticTerm {
	private static final Interner<ArithmeticTermImpl> INTERNER = new Interner<>();
	protected final Term left;
	private final ArithmeticOperator arithmeticOperator;
	private final Term right;

	private ArithmeticTermImpl(Term left, ArithmeticOperator arithmeticOperator, Term right) {
		this.left = left;
		this.arithmeticOperator = arithmeticOperator;
		this.right = right;
	}

	static Term getInstance(Term left, ArithmeticOperator arithmeticOperator, Term right) {
		// Evaluate ground arithmetic terms immediately and return result.
		if (left.isGround() && right.isGround()) {
			Integer result = new ArithmeticTermImpl(left, arithmeticOperator, right).evaluateExpression();
			return ConstantTermImpl.getInstance(result);
		}
		return INTERNER.intern(new ArithmeticTermImpl(left, arithmeticOperator, right));
	}

	@Override
	public boolean isGround() {
		return left.isGround() && right.isGround();
	}

	@Override
	public Set<VariableTerm> getOccurringVariables() {
		LinkedHashSet<VariableTerm> occurringVariables = new LinkedHashSet<>(left.getOccurringVariables());
		occurringVariables.addAll(right.getOccurringVariables());
		return occurringVariables;
	}

	@Override
	public Term substitute(Substitution substitution) {
		return getInstance(left.substitute(substitution), arithmeticOperator, right.substitute(substitution));
	}

	@Override
	public Term renameVariables(String renamePrefix) {
		return getInstance(left.renameVariables(renamePrefix), arithmeticOperator, right.renameVariables(renamePrefix));
	}

	@Override
	public Term normalizeVariables(String renamePrefix, Term.RenameCounter counter) {
		Term normalizedLeft = left.normalizeVariables(renamePrefix, counter);
		Term normalizedRight = right.normalizeVariables(renamePrefix, counter);
		return ArithmeticTermImpl.getInstance(normalizedLeft, arithmeticOperator, normalizedRight);

	}

	@Override
	public String toString() {
		return left + " " + arithmeticOperator + " " + right;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ArithmeticTermImpl that = (ArithmeticTermImpl) o;

		if (left != that.left) {
			return false;
		}
		if (arithmeticOperator != that.arithmeticOperator) {
			return false;
		}
		return right == that.right;
	}

	@Override
	public int hashCode() {
		return 31 * (31 * left.hashCode() + arithmeticOperator.hashCode()) + right.hashCode();
	}
	
	@Override
	public ArithmeticOperator getOperator() {
		return arithmeticOperator;
	}
	
	@Override
	public Term getLeftOperand() {
		return left;
	}
	
	@Override
	public Term getRightOperand() {
		return right;
	}

	@Override
	public Integer evaluateExpression() {
		Integer leftInt = Terms.evaluateGroundTermHelper(left);
		Integer rightInt = Terms.evaluateGroundTermHelper(right);
		if (leftInt == null || rightInt == null) {
			return null;
		}
		return arithmeticOperator.eval(leftInt, rightInt);
	}
	
	// FIXME it doesn't seem like this class is really needed, could be handled by an if in ArithmeticTermImpl#getInstance
	public static class MinusTerm extends ArithmeticTermImpl {

		private MinusTerm(Term term) {
			super(term, null, null);
		}

		static Term getInstance(Term term) {
			// Evaluate ground arithmetic terms immediately and return result.
			if (term.isGround()) {
				Integer result = Terms.evaluateGroundTerm(term) * -1;
				return ConstantTermImpl.getInstance(result);
			}
			return INTERNER.intern(new MinusTerm(term));
		}

		@Override
		public Integer evaluateExpression() {
			return Terms.evaluateGroundTermHelper(left) * -1;
		}

		@Override
		public boolean isGround() {
			return left.isGround();
		}

		@Override
		public Set<VariableTerm> getOccurringVariables() {
			return left.getOccurringVariables();
		}

		@Override
		public Term substitute(Substitution substitution) {
			return getInstance(left.substitute(substitution));
		}

		@Override
		public Term renameVariables(String renamePrefix) {
			return getInstance(left.renameVariables(renamePrefix));
		}

		@Override
		public Term normalizeVariables(String renamePrefix, Term.RenameCounter counter) {
			Term normalizedLeft = left.normalizeVariables(renamePrefix, counter);
			return MinusTerm.getInstance(normalizedLeft);
		}

		@Override
		public String toString() {
			return "-" + left;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			MinusTerm that = (MinusTerm) o;

			return left == that.left;
		}

		@Override
		public int hashCode() {
			return 31 * left.hashCode();
		}
	}
}
