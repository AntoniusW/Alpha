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
package at.ac.tuwien.kr.alpha.grounder;

import at.ac.tuwien.kr.alpha.common.Program;
import at.ac.tuwien.kr.alpha.common.Rule;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.grounder.transformation.VariableEqualityRemoval;
import org.junit.Test;

import java.io.IOException;

import static at.ac.tuwien.kr.alpha.TestUtil.literal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Copyright (c) 2017-2019, the Alpha Team.
 */
public class RuleGroundingOrderTest {

	private final ProgramParser parser = new ProgramParser();


	@Test
	public void groundingOrder() throws IOException {
		Program program = parser.parse("h(X,C) :- p(X,Y), q(A,B), r(Y,A), s(C)." +
			"j(A,B,X,Y) :- r1(A,B), r1(X,Y), r1(A,X), r1(B,Y), A = B." +
			"p(a) :- b = a.");
		new VariableEqualityRemoval().transform(program);
		Rule rule0 = program.getRules().get(0);
		NonGroundRule nonGroundRule0 = NonGroundRule.constructNonGroundRule(rule0);
		RuleGroundingOrders rgo0 = new RuleGroundingOrders(nonGroundRule0);
		rgo0.computeGroundingOrders();
		assertEquals(4, rgo0.getStartingLiterals().size());

		Rule rule1 = program.getRules().get(1);
		NonGroundRule nonGroundRule1 = NonGroundRule.constructNonGroundRule(rule1);
		RuleGroundingOrders rgo1 = new RuleGroundingOrders(nonGroundRule1);
		rgo1.computeGroundingOrders();
		assertEquals(4, rgo1.getStartingLiterals().size());

		Rule rule2 = program.getRules().get(2);
		NonGroundRule nonGroundRule2 = NonGroundRule.constructNonGroundRule(rule2);
		RuleGroundingOrders rgo2 = new RuleGroundingOrders(nonGroundRule2);
		rgo2.computeGroundingOrders();
		assertTrue(rgo2.fixedInstantiation());
	}

	@Test(expected = RuntimeException.class)
	public void groundingOrderUnsafe() throws IOException {
		Program program = parser.parse("h(X,C) :- X = Y, Y = C .. 3, C = X.");
		new VariableEqualityRemoval().transform(program);
		computeGroundingOrdersForRule(program, 0);
	}
	
	@Test
	public void testPositionFromWhichAllVarsAreBound_ground() {
		Program program = parser.parse("a :- b, not c.");
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(program, 0);
		assertEquals(0, rgo0.getFixedGroundingOrder().getPositionFromWhichAllVarsAreBound());
	}
	
	@Test
	public void testPositionFromWhichAllVarsAreBound_simpleNonGround() {
		Program program = parser.parse("a(X) :- b(X), not c(X).");
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(program, 0);
		assertEquals(1, rgo0.getStartingLiterals().size());
		for (Literal startingLiteral : rgo0.getStartingLiterals()) {
			assertEquals(0, rgo0.orderStartingFrom(startingLiteral).getPositionFromWhichAllVarsAreBound());
		}
	}

	@Test
	public void testPositionFromWhichAllVarsAreBound_longerSimpleNonGround() {
		Program program = parser.parse("a(X) :- b(X), c(X), d(X), not e(X).");
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(program, 0);
		assertEquals(3, rgo0.getStartingLiterals().size());
		for (Literal startingLiteral : rgo0.getStartingLiterals()) {
			assertEquals(0, rgo0.orderStartingFrom(startingLiteral).getPositionFromWhichAllVarsAreBound());
		}
	}

	@Test
	public void testToString_longerSimpleNonGround() {
		Program program = parser.parse("a(X) :- b(X), c(X), d(X), not e(X).");
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(program, 0);
		assertEquals(3, rgo0.getStartingLiterals().size());
		for (Literal startingLiteral : rgo0.getStartingLiterals()) {
			switch (startingLiteral.getPredicate().getName()) {
				case "b": assertEquals("b(X) : | c(X), d(X), not e(X)", rgo0.orderStartingFrom(startingLiteral).toString()); break;
				case "c": assertEquals("c(X) : | b(X), d(X), not e(X)", rgo0.orderStartingFrom(startingLiteral).toString()); break;
				case "d": assertEquals("d(X) : | b(X), c(X), not e(X)", rgo0.orderStartingFrom(startingLiteral).toString()); break;
				default: fail("Unexpected starting literal: " + startingLiteral);
			}
		}
	}

	@Test
	public void testPositionFromWhichAllVarsAreBound_joinedNonGround() {
		Program program = parser.parse("a(X) :- b(X), c(X,Y), d(X,Z), not e(X).");
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(program, 0);
		assertTrue(2 <= rgo0.orderStartingFrom(literal("b", "X")).getPositionFromWhichAllVarsAreBound());
		assertTrue(1 <= rgo0.orderStartingFrom(literal("c", "X", "Y")).getPositionFromWhichAllVarsAreBound());
		assertTrue(1 <= rgo0.orderStartingFrom(literal("d", "X", "Z")).getPositionFromWhichAllVarsAreBound());
	}

	private RuleGroundingOrders computeGroundingOrdersForRule(Program program, int ruleIndex) {
		Rule rule = program.getRules().get(ruleIndex);
		NonGroundRule nonGroundRule = NonGroundRule.constructNonGroundRule(rule);
		RuleGroundingOrders rgo = new RuleGroundingOrders(nonGroundRule);
		rgo.computeGroundingOrders();
		return rgo;
	}

}