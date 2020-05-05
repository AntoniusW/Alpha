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
package at.ac.tuwien.kr.alpha.api.test.runner;

import at.ac.tuwien.kr.alpha.api.test.exception.TestExecutionException;
import at.ac.tuwien.kr.alpha.lang.test.TestSuite;
import at.ac.tuwien.kr.alpha.lang.test.TestSuiteResult;

/**
 * A component that is capable of executing tests written in ASP code as
 * specified in the
 * <a href="https://github.com/alpha-asp/Alpha/issues/237">corresponding github
 * issue</a>.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public interface AspTestRunner {

	/**
	 * Executes all {@link TestCase}s contained in the given suite
	 * 
	 * @param tests a test suite
	 * @return a {@link TestSuiteResult} representing the results of running all
	 *         test cases in the suite
	 * @throws TestExecutionException in case of internal errors during test
	 *                                execution
	 */
	TestSuiteResult runSuite(TestSuite suite) throws TestExecutionException;

}
