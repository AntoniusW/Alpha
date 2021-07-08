/*
 * Copyright (c) 2017-2020, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.InternalProgram;
import at.ac.tuwien.kr.alpha.common.rule.InternalRule;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramPartParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Copyright (c) 2017-2019, the Alpha Team.
 */
public class RuleGroundingOrderTest {

	private static final ProgramPartParser PROGRAM_PART_PARSER = new ProgramPartParser();

	@Test
	public void groundingOrder() {
		String aspStr = "h(X,C) :- p(X,Y), q(A,B), r(Y,A), s(C)." +
				"j(A,B,X,Y) :- r1(A,B), r1(X,Y), r1(A,X), r1(B,Y), A = B." +
				"p(a) :- b = a.";
		Alpha system = new Alpha();
		system.getConfig().setEvaluateStratifiedPart(false);
		InternalProgram internalPrg = InternalProgram.fromNormalProgram(system.normalizeProgram(system.readProgramString(aspStr)));
		InternalRule rule0 = internalPrg.getRules().get(0);
		RuleGroundingOrders rgo0 = new RuleGroundingOrders(rule0);
		rgo0.computeGroundingOrders();
		assertEquals(4, rgo0.getStartingLiterals().size());

		InternalRule rule1 = internalPrg.getRules().get(1);
		RuleGroundingOrders rgo1 = new RuleGroundingOrders(rule1);
		rgo1.computeGroundingOrders();
		assertEquals(4, rgo1.getStartingLiterals().size());

		InternalRule rule2 = internalPrg.getRules().get(2);
		RuleGroundingOrders rgo2 = new RuleGroundingOrders(rule2);
		rgo2.computeGroundingOrders();
		assertTrue(rgo2.fixedInstantiation());
	}

	@Test(expected = RuntimeException.class)
	public void groundingOrderUnsafe() {
		String aspStr = "h(X,C) :- X = Y, Y = C .. 3, C = X.";
		Alpha system = new Alpha();
		system.getConfig().setEvaluateStratifiedPart(false);
		InternalProgram internalPrg = InternalProgram.fromNormalProgram(system.normalizeProgram(system.readProgramString(aspStr)));
		computeGroundingOrdersForRule(internalPrg, 0);
	}

	@Test
	public void testPositionFromWhichAllVarsAreBound_ground() {
		String aspStr = "a :- b, not c.";
		Alpha system = new Alpha();
		system.getConfig().setEvaluateStratifiedPart(false);
		InternalProgram internalPrg = InternalProgram.fromNormalProgram(system.normalizeProgram(system.readProgramString(aspStr)));
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(internalPrg, 0);
		assertEquals(0, rgo0.getFixedGroundingOrder().getPositionFromWhichAllVarsAreBound());
	}

	@Test
	public void testPositionFromWhichAllVarsAreBound_simpleNonGround() {
		String aspStr = "a(X) :- b(X), not c(X).";
		Alpha system = new Alpha();
		system.getConfig().setEvaluateStratifiedPart(false);
		InternalProgram internalPrg = InternalProgram.fromNormalProgram(system.normalizeProgram(system.readProgramString(aspStr)));
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(internalPrg, 0);
		assertEquals(1, rgo0.getStartingLiterals().size());
		for (Literal startingLiteral : rgo0.getStartingLiterals()) {
			assertEquals(0, rgo0.orderStartingFrom(startingLiteral).getPositionFromWhichAllVarsAreBound());
		}
	}

	@Test
	public void testPositionFromWhichAllVarsAreBound_longerSimpleNonGround() {
		String aspStr = "a(X) :- b(X), c(X), d(X), not e(X).";
		Alpha system = new Alpha();
		system.getConfig().setEvaluateStratifiedPart(false);
		InternalProgram internalPrg = InternalProgram.fromNormalProgram(system.normalizeProgram(system.readProgramString(aspStr)));
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(internalPrg, 0);
		assertEquals(3, rgo0.getStartingLiterals().size());
		for (Literal startingLiteral : rgo0.getStartingLiterals()) {
			assertEquals(0, rgo0.orderStartingFrom(startingLiteral).getPositionFromWhichAllVarsAreBound());
		}
	}

	@Test
	public void testToString_longerSimpleNonGround() {
		String aspStr = "a(X) :- b(X), c(X), d(X), not e(X).";
		Alpha system = new Alpha();
		system.getConfig().setEvaluateStratifiedPart(false);
		InternalProgram internalPrg = InternalProgram.fromNormalProgram(system.normalizeProgram(system.readProgramString(aspStr)));
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(internalPrg, 0);
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
		String aspStr = "a(X) :- b(X), c(X,Y), d(X,Z), not e(X).";
		Alpha system = new Alpha();
		system.getConfig().setEvaluateStratifiedPart(false);
		InternalProgram internalPrg = InternalProgram.fromNormalProgram(system.normalizeProgram(system.readProgramString(aspStr)));
		RuleGroundingOrders rgo0 = computeGroundingOrdersForRule(internalPrg, 0);
final Literal litBX = PROGRAM_PART_PARSER.parseLiteral("b(X)");
		final Literal litCXY = PROGRAM_PART_PARSER.parseLiteral("c(X,Y)");
		final Literal litDXZ = PROGRAM_PART_PARSER.parseLiteral("d(X,Z)");
		assertTrue(2 <= rgo0.orderStartingFrom(litBX).getPositionFromWhichAllVarsAreBound());
		assertTrue(1 <= rgo0.orderStartingFrom(litCXY).getPositionFromWhichAllVarsAreBound());
		assertTrue(1 <= rgo0.orderStartingFrom(litDXZ).getPositionFromWhichAllVarsAreBound());
	}

	private RuleGroundingOrders computeGroundingOrdersForRule(InternalProgram program, int ruleIndex) {
		InternalRule rule = program.getRules().get(ruleIndex);
		RuleGroundingOrders rgo = new RuleGroundingOrders(rule);
		rgo.computeGroundingOrders();
		return rgo;
	}

}
