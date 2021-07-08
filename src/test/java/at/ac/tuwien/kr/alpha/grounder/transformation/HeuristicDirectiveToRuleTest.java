/*
 * Copyright (c) 2018-2021 Siemens AG
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
package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfigurationBuilder;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link HeuristicDirectiveToRule}.
 */
public class HeuristicDirectiveToRuleTest {
	private final ProgramParser parser = new ProgramParser();
	private final HeuristicsConfiguration heuristicsConfiguration = new HeuristicsConfigurationBuilder().setRespectDomspecHeuristics(true).build();
	
	@Test
	public void testPositiveDirectiveWithBodyWeightAndLevel() {
		InputProgram program = parser.parse("a(1)."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic b(N) : a(N). [N@2]");

		program = new HeuristicDirectiveToRule(heuristicsConfiguration).apply(program);
		assertEquals(emptyList(), program.getInlineDirectives().getDirectives());
		assertEquals("_h(N, 2, true, b(N), condpos(tm(a(N))), condneg) :- a(N).", program.getRules().get(program.getRules().size() - 1).toString());
	}
	
	@Test
	public void testNegativeDirectiveWithBodyWeightAndLevel() {
		InputProgram program = parser.parse("a(1)."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic F b(N) : T a(N), not F b(N). [N@2]");

		program = new HeuristicDirectiveToRule(heuristicsConfiguration).apply(program);
		assertEquals(emptyList(), program.getInlineDirectives().getDirectives());
		assertEquals("_h(N, 2, false, b(N), condpos(t(a(N))), condneg(f(b(N)))) :- a(N).", program.getRules().get(program.getRules().size() - 1).toString());
	}

	@Test
	public void testPositiveComparisonAtom() {
		InputProgram program = parser.parse("a(1). a(2)."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic F b(N) : T a(N), Nm1 = N - 1, not F b(Nm1). [N@2]");

		program = new HeuristicDirectiveToRule(heuristicsConfiguration).apply(program);
		assertEquals(emptyList(), program.getInlineDirectives().getDirectives());
		assertEquals("_h(N, 2, false, b(N), condpos(t(a(N))), condneg(f(b(Nm1)))) :- a(N), Nm1 = N - 1.", program.getRules().get(program.getRules().size() - 1).toString());
	}

	@Test
	public void testNegativeComparisonAtom() {
		InputProgram program = parser.parse("a(1). a(2)."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic F b(N) : T a(N), T b(M), not N < M. [N@2]");

		program = new HeuristicDirectiveToRule(heuristicsConfiguration).apply(program);
		assertEquals(emptyList(), program.getInlineDirectives().getDirectives());
		assertEquals("_h(N, 2, false, b(N), condpos(t(a(N)), t(b(M))), condneg) :- a(N), b(M), not N < M.", program.getRules().get(program.getRules().size() - 1).toString());
	}

}
