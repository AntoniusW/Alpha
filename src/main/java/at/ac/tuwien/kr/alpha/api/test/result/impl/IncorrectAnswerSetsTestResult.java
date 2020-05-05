package at.ac.tuwien.kr.alpha.api.test.result.impl;

import at.ac.tuwien.kr.alpha.api.test.result.TestCaseResult;

public class IncorrectAnswerSetsTestResult extends TestCaseResult {

	private final int expected;
	private final int actual;

	public IncorrectAnswerSetsTestResult(int expected, int actual) {
		this.expected = expected;
		this.actual = actual;
	}

	public int getExpected() {
		return this.expected;
	}

	public int getActual() {
		return this.actual;
	}

}
