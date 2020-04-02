/**
 * Copyright (c) 2016-2020, the Alpha Team.
 * All rights reserved.
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
package at.ac.tuwien.kr.alpha.api.test.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.api.test.AspTestRunner;
import at.ac.tuwien.kr.alpha.api.test.exception.TestExecutionException;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.Program;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.lang.test.TestCase;
import at.ac.tuwien.kr.alpha.lang.test.TestCaseResult;
import at.ac.tuwien.kr.alpha.lang.test.TestSuite;
import at.ac.tuwien.kr.alpha.lang.test.TestSuiteResult;

/**
 * Default single-threaded implementation of {@link AspTestRunner}.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public class DefaultAspTestRunner implements AspTestRunner {

	private final Alpha alpha;

	public DefaultAspTestRunner(Alpha alpha) {
		this.alpha = alpha;
	}

	@Override
	public TestSuiteResult runSuite(TestSuite suite) throws TestExecutionException {
		Program testedUnit = suite.getTestedUnit();
		TestCaseResult caseResult;
		for (TestCase testCase : suite.getTestCases()) {
			caseResult = this.runTestCase(testedUnit, testCase);
		}
		return null;
	}

	private TestCaseResult runTestCase(Program testedUnit, TestCase testCase) throws TestExecutionException {
		List<Atom> testProgramFacts = new ArrayList<>(testCase.getInput());
		testProgramFacts.addAll(testedUnit.getFacts());
		Program testProgram = new Program(testedUnit.getRules(), testProgramFacts, testedUnit.getInlineDirectives());
		Set<AnswerSet> testProgramAnswers = this.alpha.solve(testProgram).collect(Collectors.toSet());
		if (testProgramAnswers.size() != testCase.getExpectedAnswerSets()) {
			// TODO return appropriate result
		}
		Program verifier;
		for(AnswerSet as : testProgramAnswers) {
			verifier = null; // TODO verifier = union(as, rewriteConstraints(testCase.getVerifier()));
			Set<AnswerSet> verifierAnswers = this.alpha.solve(verifier).collect(Collectors.toSet());
			if(verifierAnswers.size() != 1) {
				throw new TestExecutionException(testCase, "Invalid number of answer sets for verifier: " + verifierAnswers.size());
			}
			// TODO map answer set to assertion errors!
		}
		return null;
	}

}
