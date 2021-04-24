/**
 * Copyright (c) 2018 Siemens AG
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
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

import java.util.List;

import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.NormalProgram;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.atoms.Literals;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.programs.CompiledProgram;
import at.ac.tuwien.kr.alpha.core.programs.InternalProgram;
import at.ac.tuwien.kr.alpha.core.programs.transformation.NormalizeProgramTransformation;
import at.ac.tuwien.kr.alpha.core.rules.CompiledRule;
import at.ac.tuwien.kr.alpha.core.rules.InternalRule;

/**
 * Tests {@link NoGoodGenerator}
 */
public class NoGoodGeneratorTest {

	private static final ProgramParser PARSER = new ProgramParserImpl();
	private static final NormalizeProgramTransformation NORMALIZE_TRANSFORM = new NormalizeProgramTransformation(false);

	private static final ConstantTerm<String> A = Terms.newSymbolicConstant("a");
	private static final ConstantTerm<String> B = Terms.newSymbolicConstant("b");

	private static final VariableTerm X = Terms.newVariable("X");
	private static final VariableTerm Y = Terms.newVariable("Y");

	/**
	 * Calls {@link NoGoodGenerator#collectNegLiterals(InternalRule, Substitution)}, which puts the atom occurring
	 * negatively in a rule into the atom store. It is then checked whether the atom in the atom store is positive.
	 */
	@Test
	public void collectNeg_ContainsOnlyPositiveLiterals() {
		ASPCore2Program input = PARSER.parse("p(a,b). " 
				+ "q(a,b) :- not nq(a,b). " 
				+ "nq(a,b) :- not q(a,b).");
		NormalProgram normal = NORMALIZE_TRANSFORM.apply(input);
		CompiledProgram program = InternalProgram.fromNormalProgram(normal);

		CompiledRule rule = program.getRules().get(1);
		AtomStore atomStore = new AtomStoreImpl();
		Grounder grounder = GrounderFactory.getInstance("naive", program, atomStore, true);
		NoGoodGenerator noGoodGenerator = ((NaiveGrounder) grounder).noGoodGenerator;
		Substitution substitution = new BasicSubstitution();
		substitution.put(X, A);
		substitution.put(Y, B);
		List<Integer> collectedNeg = noGoodGenerator.collectNegLiterals(rule, substitution);
		assertEquals(1, collectedNeg.size());
		String negAtomString = atomStore.atomToString(Literals.atomOf(collectedNeg.get(0)));
		assertEquals("q(a, b)", negAtomString);
	}

}
