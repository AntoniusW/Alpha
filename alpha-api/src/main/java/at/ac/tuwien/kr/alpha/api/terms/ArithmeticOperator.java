package at.ac.tuwien.kr.alpha.api.terms;

import com.google.common.math.IntMath;

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