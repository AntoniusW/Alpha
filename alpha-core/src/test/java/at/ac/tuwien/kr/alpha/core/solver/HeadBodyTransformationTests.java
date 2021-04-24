/**
 * Copyright (c) 2017 Siemens AG
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
package at.ac.tuwien.kr.alpha.core.solver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.Solver;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;

/**
 * Tests rule transformations described in the following research paper, and their effects on performance:
 * 
 * Anger, Christian; Gebser, Martin; Janhunen, Tomi; Schaub, Torsten (2006): What's a head without a body? In G. Brewka, S. Coradeschi, A. Perini, P. Traverso
 * (Eds.): Proceedings of the Seventeenth European Conference on Artificial Intelligence (ECAI'06): IOS Press, pp. 769–770.
 *
 */
public class HeadBodyTransformationTests extends AbstractSolverTests {

	@Before
	public void printSolverName() {
		System.out.println(solverName);
	}

	@Test(timeout = 10000)
	public void testProgramB_N1() throws IOException {
		test(constructProgramB(1));
	}

	@Test(timeout = 10000)
	public void testProgramB_Transformed_N1() throws IOException {
		test(constructProgramB_TransformationB(1));
	}

	@Test(timeout = 10000)
	public void testProgramA_N1() throws IOException {
		test(constructProgramA(1));
	}

	@Test(timeout = 10000)
	public void testProgramA_Transformed_N1() throws IOException {
		test(constructProgramA_TransformationA(1));
	}

	@Test(timeout = 10000)
	public void testProgramB_N2() throws IOException {
		test(constructProgramB(2));
	}

	@Test(timeout = 10000)
	public void testProgramB_Transformed_N2() throws IOException {
		test(constructProgramB_TransformationB(2));
	}

	@Test(timeout = 10000)
	public void testProgramA_N2() throws IOException {
		test(constructProgramA(2));
	}

	@Test(timeout = 10000)
	public void testProgramA_Transformed_N2() throws IOException {
		test(constructProgramA_TransformationA(2));
	}

	@Test(timeout = 10000)
	public void testProgramB_N4() throws IOException {
		test(constructProgramB(4));
	}

	@Test(timeout = 10000)
	public void testProgramB_Transformed_N4() throws IOException {
		test(constructProgramB_TransformationB(4));
	}

	@Test(timeout = 10000)
	public void testProgramA_N4() throws IOException {
		test(constructProgramA(4));
	}

	@Test(timeout = 10000)
	public void testProgramA_Transformed_N4() throws IOException {
		test(constructProgramA_TransformationA(4));
	}

	@Test(timeout = 10000)
	public void testProgramB_N8() throws IOException {
		test(constructProgramB(8));
	}

	@Test(timeout = 10000)
	public void testProgramB_Transformed_N8() throws IOException {
		test(constructProgramB_TransformationB(8));
	}

	@Test(timeout = 10000)
	public void testProgramA_N8() throws IOException {
		test(constructProgramA(8));
	}

	@Test(timeout = 10000)
	public void testProgramA_Transformed_N8() throws IOException {
		test(constructProgramA_TransformationA(8));
	}

	@Test(timeout = 10000)
	@Ignore("disabled to save resources during CI")
	public void testProgramB_N16() throws IOException {
		test(constructProgramB(16));
	}

	@Test(timeout = 10000)
	@Ignore("disabled to save resources during CI")
	public void testProgramB_Transformed_N16() throws IOException {
		test(constructProgramB_TransformationB(16));
	}

	@Test(timeout = 10000)
	@Ignore("disabled to save resources during CI")
	public void testProgramA_N16() throws IOException {
		test(constructProgramA(16));
	}

	@Test(timeout = 10000)
	@Ignore("disabled to save resources during CI")
	public void testProgramA_Transformed_N16() throws IOException {
		test(constructProgramA_TransformationA(16));
	}

	private void test(ASPCore2Program program) {
		Solver solver = getInstance(program);
		Optional<AnswerSet> answerSet = solver.stream().findFirst();
		assertFalse(answerSet.isPresent());
	}

	/**
	 * Constructs program Pi^n_B from the paper
	 * 
	 * @param n
	 */
	private ASPCore2Program constructProgramB(int n) {
		int numberOfRules = 3 * n + 1;
		List<String> strRules = new ArrayList<>(numberOfRules);
		strRules.add("x :- not x.");
		strRules.addAll(createXRules(n));
		strRules.addAll(createABRules(n));
		return checkNumberOfRulesAndParse(strRules, numberOfRules);
	}

	/**
	 * Constructs program Pi^n_B transformed with Tau_B from the paper
	 * 
	 * @param n
	 */
	private ASPCore2Program constructProgramB_TransformationB(int n) {
		int numberOfRules = 6 * n + 2;
		List<String> strRules = new ArrayList<>(numberOfRules);
		strRules.add("b_notX :- not x.");
		strRules.add("x :- b_notX.");
		strRules.addAll(createXRules_TransformationB(n));
		strRules.addAll(createABRules_TransformationB(n));
		return checkNumberOfRulesAndParse(strRules, numberOfRules);
	}

	/**
	 * Constructs program Pi^n_a from the paper
	 * 
	 * @param n
	 */
	private ASPCore2Program constructProgramA(int n) {
		int numberOfRules = 4 * n + 1;
		List<String> strRules = new ArrayList<>(numberOfRules);
		strRules.add(createXCRule(n));
		strRules.addAll(createCRules(n));
		strRules.addAll(createABRules(n));
		return checkNumberOfRulesAndParse(strRules, numberOfRules);
	}

	/**
	 * Constructs program Pi^n_a from the paper
	 * 
	 * @param n
	 */
	private ASPCore2Program constructProgramA_TransformationA(int n) {
		int numberOfRules = 7 * n + 2;
		List<String> strRules = new ArrayList<>(numberOfRules);
		strRules.addAll(createXCRules_TransformationA(n));
		strRules.addAll(createCRules_TransformationA(n));
		strRules.addAll(createABRules_TransformationA(n));
		return checkNumberOfRulesAndParse(strRules, numberOfRules);
	}

	private ASPCore2Program checkNumberOfRulesAndParse(List<String> strRules, int numberOfRules) {
		assertEquals(numberOfRules, strRules.size());
		String strProgram = strRules.stream().collect(Collectors.joining(System.lineSeparator()));
		ASPCore2Program parsedProgram = new ProgramParserImpl().parse(strProgram);
		assertEquals(numberOfRules, parsedProgram.getRules().size());
		return parsedProgram;
	}

	private Collection<String> createXRules(int n) {
		Collection<String> rules = new ArrayList<>(n);
		for (int i = 1; i <= n; i++) {
			rules.add(String.format("x :- not a%d, not b%d.", i, i));
		}
		return rules;
	}

	private Collection<String> createXRules_TransformationB(int n) {
		Collection<String> rules = new ArrayList<>(n);
		for (int i = 1; i <= n; i++) {
			String beta = String.format("b_nota%dnotb%d", i, i);
			rules.add(String.format("%s :- not a%d, not b%d.", beta, i, i));
			rules.add(String.format("x :- %s.", beta));
		}
		return rules;
	}

	private Collection<String> createABRules(int n) {
		Collection<String> rules = new ArrayList<>(2 * n);
		for (int i = 1; i <= n; i++) {
			rules.add(String.format("a%d :- not b%d.", i, i));
			rules.add(String.format("b%d :- not a%d.", i, i));
		}
		return rules;
	}

	private Collection<String> createABRules_TransformationB(int n) {
		Collection<String> rules = new ArrayList<>(4 * n);
		for (int i = 1; i <= n; i++) {
			rules.add(String.format("b_notb%d :- not b%d.", i, i));
			rules.add(String.format("a%d :- b_notb%d.", i, i));
			rules.add(String.format("b_nota%d :- not a%d.", i, i));
			rules.add(String.format("b%d :- b_nota%d.", i, i));
		}
		return rules;
	}

	private Collection<String> createABRules_TransformationA(int n) {
		Collection<String> rules = new ArrayList<>(4 * n);
		for (int i = 1; i <= n; i++) {
			rules.add(String.format("a_a%d :- not b%d.", i, i));
			rules.add(String.format("a%d :- a_a%d.", i, i));
			rules.add(String.format("a_b%d :- not a%d.", i, i));
			rules.add(String.format("b%d :- a_b%d.", i, i));
		}
		return rules;
	}

	private String createXCRule(int n) {
		StringBuilder stringBuilder = new StringBuilder("x :- ");
		for (int i = 1; i <= n; i++) {
			stringBuilder.append(String.format("c%d, ", i));
		}
		stringBuilder.append("not x.");
		return stringBuilder.toString();
	}

	private Collection<? extends String> createXCRules_TransformationA(int n) {
		Collection<String> rules = new ArrayList<>(2);
		rules.add("x :- a_x.");
		rules.add("a_" + createXCRule(n));
		return rules;
	}

	private Collection<? extends String> createCRules(int n) {
		Collection<String> rules = new ArrayList<>(2 * n);
		for (int i = 1; i <= n; i++) {
			rules.add(String.format("c%d :- not a%d.", i, i));
			rules.add(String.format("c%d :- not b%d.", i, i));
		}
		return rules;
	}

	private Collection<? extends String> createCRules_TransformationA(int n) {
		Collection<String> rules = new ArrayList<>(3 * n);
		for (int i = 1; i <= n; i++) {
			rules.add(String.format("a_c%d :- not a%d.", i, i));
			rules.add(String.format("a_c%d :- not b%d.", i, i));
			rules.add(String.format("c%d :- a_c%d.", i, i));
		}
		return rules;
	}

}
