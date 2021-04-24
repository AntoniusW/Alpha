/**
 * Copyright (c) 2018 Siemens AG
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
package at.ac.tuwien.kr.alpha.common.atoms;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.rules.Head;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.comparisons.ComparisonOperators;
import at.ac.tuwien.kr.alpha.core.common.fixedinterpretations.IntPredicateInterpretation;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;

/**
 * Tests the behaviour of {@link Literal#getBindingVariables()} and {@link Literal#getNonBindingVariables()}
 * on classes implementing {@link Atom}.
 *
 */
public class LiteralBindingNonBindingVariablesTest {

	private final Map<String, PredicateInterpretation> externals = new HashMap<>();
	private final ProgramParser parser = new ProgramParserImpl();

	@Test
	public void testPositiveBasicLiteral() {
		Literal literal = parser.parse("p(X,Y) :- q(X,Y).").getRules().get(0).getBody().stream().findFirst().get();
		assertEquals(false, literal.isNegated());
		expectVariables(literal.getBindingVariables(), "X", "Y");
		expectVariables(literal.getNonBindingVariables());
	}

	@Test
	public void testNegativeBasicLiteral() {
		Literal literal = parser.parse("p(X,Y) :- q(X,Y), not r(X,Y).").getRules().get(0).getNegativeBody().stream().findFirst().get();
		assertEquals(true, literal.isNegated());
		expectVariables(literal.getBindingVariables());
		expectVariables(literal.getNonBindingVariables(), "X", "Y");
	}

	@Test
	public void testPositiveComparisonLiteral_EQ_LeftAssigning() {
		Rule<Head> rule = parser.parse("p(X) :- q(X,Y), Y = 5.").getRules().get(0);
		Literal literal = rule.getBody().stream().filter((lit) -> lit.getPredicate() == ComparisonOperators.EQ.toPredicate()).findFirst().get();
		assertEquals(false, literal.isNegated());
		expectVariables(literal.getBindingVariables(), "Y");
		expectVariables(literal.getNonBindingVariables());
	}

	@Test
	public void testNegativeComparisonLiteral_EQ_LeftAssigning() {
		Rule<Head> rule = parser.parse("p(X) :- q(X,Y), not Y = 5.").getRules().get(0);
		Literal literal = rule.getNegativeBody().stream().findFirst().get();
		assertEquals(true, literal.isNegated());
		expectVariables(literal.getBindingVariables());
		expectVariables(literal.getNonBindingVariables(), "Y");
	}

	@Test
	public void testPositiveComparisonLiteral_EQ_RightAssigning() {
		Rule<Head> rule = parser.parse("p(X) :- q(X,Y), 5 = Y.").getRules().get(0);
		Literal literal = rule.getBody().stream().filter((lit) -> lit.getPredicate() == ComparisonOperators.EQ.toPredicate()).findFirst().get();
		assertEquals(false, literal.isNegated());
		expectVariables(literal.getBindingVariables(), "Y");
		expectVariables(literal.getNonBindingVariables());
	}

	@Test
	public void testNegativeComparisonLiteral_EQ_RightAssigning() {
		Literal literal = parser.parse("p(X) :- q(X,Y), not 5 = Y.").getRules().get(0).getNegativeBody().stream().findFirst().get();
		assertEquals(true, literal.isNegated());
		expectVariables(literal.getBindingVariables());
		expectVariables(literal.getNonBindingVariables(), "Y");
	}

	@Test
	@Ignore("Literals of this kind are compiled away by VariableEqualityRemoval")
	public void testPositiveComparisonLiteral_EQ_Bidirectional() {
		Rule<Head> rule = parser.parse("p(X) :- q(X,Y), X = Y.").getRules().get(0);
		Literal literal = rule.getBody().stream().filter((lit) -> lit.getPredicate() == ComparisonOperators.EQ.toPredicate()).findFirst().get();
		assertEquals(false, literal.isNegated());
		expectVariables(literal.getBindingVariables());
		expectVariables(literal.getNonBindingVariables(), "X", "Y");
	}

	@Test
	public void testNegativeComparisonLiteral_EQ_Bidirectional() {
		Literal literal = parser.parse("p(X) :- q(X,Y), not X = Y.").getRules().get(0).getNegativeBody().stream().findFirst().get();
		assertEquals(true, literal.isNegated());
		expectVariables(literal.getBindingVariables());
		expectVariables(literal.getNonBindingVariables(), "X", "Y");
	}

	@Test
	public void testPositiveComparisonLiteral_NEQ_LeftAssigning() {
		Rule<Head> rule = parser.parse("p(X) :- q(X,Y), Y != 5.").getRules().get(0);
		Literal literal = rule.getBody().stream().filter((lit) -> lit.getPredicate() == ComparisonOperators.NE.toPredicate()).findFirst().get();
		assertEquals(false, literal.isNegated());
		expectVariables(literal.getBindingVariables());
		expectVariables(literal.getNonBindingVariables(), "Y");
	}

	@Test
	public void testNegativeComparisonLiteral_NEQ_LeftAssigning() {
		Literal literal = parser.parse("p(X) :- q(X,Y), not Y != 5.").getRules().get(0).getNegativeBody().stream().findFirst().get();
		assertEquals(true, literal.isNegated());
		expectVariables(literal.getBindingVariables(), "Y");
		expectVariables(literal.getNonBindingVariables());
	}

	@Test
	public void testPositiveComparisonLiteral_NEQ_RightAssigning() {
		Rule<Head> rule = parser.parse("p(X) :- q(X,Y), 5 != Y.").getRules().get(0);
		Literal literal = rule.getBody().stream().filter((lit) -> lit.getPredicate() == ComparisonOperators.NE.toPredicate()).findFirst().get();
		assertEquals(false, literal.isNegated());
		expectVariables(literal.getBindingVariables());
		expectVariables(literal.getNonBindingVariables(), "Y");
	}

	@Test
	public void testNegativeComparisonLiteral_NEQ_RightAssigning() {
		Literal literal = parser.parse("p(X) :- q(X,Y), not 5 != Y.").getRules().get(0).getNegativeBody().stream().findFirst().get();
		assertEquals(true, literal.isNegated());
		expectVariables(literal.getBindingVariables(), "Y");
		expectVariables(literal.getNonBindingVariables());
	}

	@Test
	public void testPositiveComparisonLiteral_NEQ_Bidirectional() {
		Rule<Head> rule = parser.parse("p(X) :- q(X,Y), X != Y.").getRules().get(0);
		Literal literal = rule.getBody().stream().filter((lit) -> lit.getPredicate() == ComparisonOperators.NE.toPredicate()).findFirst().get();
		assertEquals(false, literal.isNegated());
		expectVariables(literal.getBindingVariables());
		expectVariables(literal.getNonBindingVariables(), "X", "Y");
	}

	@Test
	@Ignore("Literals of this kind are compiled away by VariableEqualityRemoval")
	public void testNegativeComparisonLiteral_NEQ_Bidirectional() {
		Literal literal = parser.parse("p(X) :- q(X,Y), not X != Y.").getRules().get(0).getNegativeBody().stream().findFirst().get();
		assertEquals(true, literal.isNegated());
		expectVariables(literal.getBindingVariables(), "X", "Y");
		expectVariables(literal.getNonBindingVariables());
	}

	@Test
	public void testPositiveExternalLiteral() {
		externals.put("ext", new IntPredicateInterpretation(i -> i > 0));
		Rule<Head> rule = parser.parse("p(X) :- q(Y), &ext[Y](X).", externals).getRules().get(0);
		Literal literal = rule.getBody().stream().filter((lit) -> lit.getPredicate().getName().equals("ext")).findFirst().get();
		assertEquals(false, literal.isNegated());
		expectVariables(literal.getBindingVariables(), "X");
		expectVariables(literal.getNonBindingVariables(), "Y");
	}

	@Test
	public void testNegativeExternalLiteral() {
		externals.put("ext", new IntPredicateInterpretation(i -> i > 0));
		Literal literal = parser.parse("p(X) :- q(Y), not &ext[Y](X).", externals).getRules().get(0).getNegativeBody().stream().findFirst().get();
		assertEquals(true, literal.isNegated());
		expectVariables(literal.getBindingVariables());
		expectVariables(literal.getNonBindingVariables(), "X", "Y");
	}

	private void expectVariables(Collection<VariableTerm> variables, String... expectedVariableNames) {
		Set<String> setActualVariableNames = variables.stream().map(VariableTerm::toString).collect(Collectors.toSet());
		Set<String> setExpectedVariableNames = Arrays.stream(expectedVariableNames).collect(Collectors.toSet());
		assertEquals(setExpectedVariableNames, setActualVariableNames);
	}

}
