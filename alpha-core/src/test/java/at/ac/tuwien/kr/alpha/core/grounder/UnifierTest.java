/*
 * Copyright (c) 2018, 2020, the Alpha Team.
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

package at.ac.tuwien.kr.alpha.core.grounder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.substitutions.Unifier;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;

public class UnifierTest extends SubstitutionTest {

	@Test
	public void extendUnifier() {
		VariableTerm varX = Terms.newVariable("X");
		VariableTerm varY = Terms.newVariable("Y");
		Unifier sub1 = new Unifier();
		sub1.put(varX, varY);
		Unifier sub2 = new Unifier();
		sub2.put(varY, Terms.newConstant("a"));

		sub1.extendWith(sub2);
		BasicAtom atom1 = parseAtom("p(X)");

		Atom atomSubstituted = atom1.substitute(sub1);
		assertEquals(Terms.newConstant("a"), atomSubstituted.getTerms().get(0));
	}

	@Test
	public void mergeUnifierIntoLeft() {
		VariableTerm varX = Terms.newVariable("X");
		VariableTerm varY = Terms.newVariable("Y");
		VariableTerm varZ = Terms.newVariable("Z");
		Term constA = Terms.newConstant("a");
		Unifier left = new Unifier();
		left.put(varX, varY);
		left.put(varZ, varY);
		Unifier right = new Unifier();
		right.put(varX, constA);
		Unifier merged = Unifier.mergeIntoLeft(left, right);
		assertEquals(constA, merged.eval(varY));
		assertEquals(constA, merged.eval(varZ));
	}

	private BasicAtom parseAtom(String atom) {
		ProgramParser programParser = new ProgramParserImpl();
		ASPCore2Program program = programParser.parse(atom + ".");
		return (BasicAtom) program.getFacts().get(0);
	}
}
