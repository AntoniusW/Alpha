/*
 * Copyright (c) 2017-2020 Siemens AG
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
package at.ac.tuwien.kr.alpha.common;

import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProgramTest {

	public static final String LS = System.lineSeparator();

	@Test
	public void testToString() {
		InputProgram parsedProgram = new ProgramParser().parse(
				"#heuristic q(X) : p(X). [X@2]" + LS +
					"p(a)." + LS +
					"q(X) :- p(X)." + LS +
					"p(b).");
		assertEquals(
				"p(a)." + LS +
					"p(b)." + LS +
					"q(X) :- p(X)." + LS +
					"#heuristic T q(X) : TM p(X). [X@2]" + LS,
				parsedProgram.toString());
	}

	@Test
	public void testHeuristicDefaultWeight() {
		InputProgram parsedProgram = new ProgramParser().parse(
				"#heuristic q(X) : p(X).");
		assertEquals(ConstantTerm.getInstance(0), ((HeuristicDirective)parsedProgram.getInlineDirectives().getDirectives().iterator().next()).getWeightAtLevel().getWeight());
	}

	@Test
	public void testHeuristicDefaultLevel() {
		InputProgram parsedProgram = new ProgramParser().parse(
				"#heuristic q(X) : p(X).");
		assertEquals(ConstantTerm.getInstance(0), ((HeuristicDirective)parsedProgram.getInlineDirectives().getDirectives().iterator().next()).getWeightAtLevel().getLevel());
	}

	@Test
	public void testAccumulation() {
		InputProgram program1 = new ProgramParser().parse(
				"a." + LS +
				"b :- a, not c." + LS +
				"#heuristic b : not c. [1@2]"
		);
		InputProgram program2 = new ProgramParser().parse(
			"c :- a, not b." + LS +
				"#heuristic c : not b. [2@3]"
		);
		program1 = InputProgram.builder(program1).accumulate(program2).build();
		assertEquals(1, program1.getFacts().size());
		assertEquals(2, program1.getRules().size());
		assertEquals(2, program1.getInlineDirectives().getDirectives().size());
	}
}