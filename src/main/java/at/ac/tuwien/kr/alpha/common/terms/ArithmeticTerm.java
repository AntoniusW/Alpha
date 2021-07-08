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
package at.ac.tuwien.kr.alpha.common.terms;

import com.google.common.math.IntMath;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import at.ac.tuwien.kr.alpha.common.Interner;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

/**
 * This class represents an arithmetic expression occurring as a term.
 * Copyright (c) 2017-2019, the Alpha Team.
 */
public class ArithmeticTerm extends Term {
	private static final Interner<ArithmeticTerm> INTERNER = new Interner<>();
	protected final Term left;
	private final ArithmeticOperator arithmeticOperator;
	private final Term right;

	private ArithmeticTerm(Term left, ArithmeticOperator arithmeticOperator, Term right) {
		this.left = left;
		this.arithmeticOperator = arithmeticOperator;
		this.right = right;
	}

	public static Term getInstance(Term left, ArithmeticOperator arithmeticOperator, Term right) {
		// Evaluate ground arithmetic terms immediately and return result.
		if (left.isGround() && right.isGround()) {
			Integer result = new ArithmeticTerm(left, arithmeticOperator, right).evaluateExpression();
			return ConstantTerm.getInstance(result);
		}
		return INTERNER.intern(new ArithmeticTerm(left, arithmeticOperator, right));
	}

	@Override
	public boolean isGround() {
		return left.isGround() && right.isGround();
	}

	@Override
	public List<VariableTerm> getOccurringVariables() {
		LinkedHashSet<VariableTerm> occurringVariables = new LinkedHashSet<>(left.getOccurringVariables());
		occurringVariables.addAll(right.getOccurringVariables());
		return new ArrayList<>(occurringVariables);
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
	public Term normalizeVariables(String renamePrefix, RenameCounter counter) {
		Term normalizedLeft = left.normalizeVariables(renamePrefix, counter);
		Term normalizedRight = right.normalizeVariables(renamePrefix, counter);
		return ArithmeticTerm.getInstance(normalizedLeft, arithmeticOperator, normalizedRight);

	}

	public static Integer evaluateGroundTerm(Term term) {
		if (!term.isGround()) {
			throw new RuntimeException("Cannot evaluate arithmetic term since it is not ground: " + term);
		}
		return evaluateGroundTermHelper(term);
	}

	private static Integer evaluateGroundTermHelper(Term term) {
		if (term instanceof ConstantTerm
				&& ((ConstantTerm<?>) term).getObject() instanceof Integer) {
			// Extract integer from the constant.
			return (Integer) ((ConstantTerm<?>) term).getObject();
		} else if (term instanceof ArithmeticTerm) {
			return ((ArithmeticTerm) term).evaluateExpression();
		} else {
			// ASP Core 2 standard allows non-integer terms in arithmetic expressions, result is to simply ignore the ground
			// instance.
			return null;
		}
	}

	Integer evaluateExpression() {
		Integer leftInt = evaluateGroundTermHelper(left);
		Integer rightInt = evaluateGroundTermHelper(right);
		if (leftInt == null || rightInt == null) {
			return null;
		}
		return arithmeticOperator.eval(leftInt, rightInt);
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

		ArithmeticTerm that = (ArithmeticTerm) o;

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

	public Term getLeft() {
		return left;
	}

	public ArithmeticOperator getArithmeticOperator() {
		return arithmeticOperator;
	}

	public Term getRight() {
		return right;
	}

	public enum ArithmeticOperator {
		PLUS("+"),
		MINUS("-"),
		TIMES("*"),
		DIV("/"),
		POWER("**"),
		MODULO("\\"),
		BITXOR("^");

		private String asString;

		ArithmeticOperator(String asString) {
			this.asString = asString;
		}

		@Override
		public String toString() {
			return asString;
		}

		public Integer eval(Integer left, Integer right) {
			switch (this) {
				case PLUS:
					return left + right;
				case MINUS:
					return left - right;
				case TIMES:
					return left * right;
				case DIV:
					return left / right;
				case POWER:
					return IntMath.checkedPow(left, right);
				case MODULO:
					return left % right;
				case BITXOR:
					return left ^ right;
				default:
					throw new RuntimeException("Unknown arithmetic operator encountered.");

			}
		}
	}

	public static class MinusTerm extends ArithmeticTerm {

		private MinusTerm(Term term) {
			super(term, null, null);
		}

		public static Term getInstance(Term term) {
			// Evaluate ground arithmetic terms immediately and return result.
			if (term.isGround()) {
				Integer result = evaluateGroundTermHelper(term) * -1;
				return ConstantTerm.getInstance(result);
			}
			return INTERNER.intern(new MinusTerm(term));
		}

		@Override
		Integer evaluateExpression() {
			return evaluateGroundTermHelper(left) * -1;
		}

		@Override
		public boolean isGround() {
			return left.isGround();
		}

		@Override
		public List<VariableTerm> getOccurringVariables() {
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
		public Term normalizeVariables(String renamePrefix, RenameCounter counter) {
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
