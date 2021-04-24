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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.Solver;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.programs.InputProgram;

/**
 * Tests {@link AbstractSolver} using some three-coloring test cases, as described in:
 * Lefèvre, Claire; Béatrix, Christopher; Stéphan, Igor; Garcia, Laurent (2017):
 * ASPeRiX, a first-order forward chaining approach for answer set computing.
 * In Theory and Practice of Logic Programming, pp. 1-45.
 * DOI: 10.1017/S1471068416000569
 */
public class ThreeColouringWheelTest extends AbstractSolverTests {
	@Test(timeout = 1000)
	public void testN4() throws IOException {
		testThreeColouring(4);
	}

	@Test(timeout = 1000)
	public void testN5() throws IOException {
		testThreeColouring(5);
	}

	@Test(timeout = 6000)
	@Ignore("disabled to save resources during CI")
	public void testN6() throws IOException {
		testThreeColouring(6);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testN3() throws IOException {
		testThreeColouring(3);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testN7() throws IOException {
		testThreeColouring(7);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testN11() throws IOException {
		testThreeColouring(11);
	}

	private void testThreeColouring(int n) throws IOException {
		ASPCore2Program tmpPrg = new ProgramParserImpl().parse(
				"col(V,C) :- v(V), c(C), not ncol(V,C)." +
				"ncol(V,C) :- col(V,D), c(C), C != D." +
				":- e(V,U), col(V,C), col(U,C).");
		InputProgram.Builder prgBuilder = InputProgram.builder(tmpPrg);
		prgBuilder.addFacts(createColors("red", "blue", "green"));
		prgBuilder.addFacts(createVertices(n));
		prgBuilder.addFacts(createEdges(n));
		InputProgram program = prgBuilder.build();

		maybeShuffle(program);

		Solver solver = getInstance(program);

		Optional<AnswerSet> answerSet = solver.stream().findAny();
		//System.out.println(answerSet);

		// TODO: check correctness of answer set
	}

	private void maybeShuffle(InputProgram program) {
		// FIXME since InputProgram is immutable this needs to be reworked a bit if used
		// No shuffling here.
	}

	private List<Atom> createColors(String... colours) {
		List<Atom> facts = new ArrayList<>(colours.length);
		Predicate predicate = Predicates.getPredicate("c", 1);
		for (String colour : colours) {
			List<Term> terms = new ArrayList<>(1);
			terms.add(Terms.newConstant(colour));
			facts.add(Atoms.newBasicAtom(predicate, terms));
		}
		return facts;
	}

	private List<Atom> createVertices(int n) {
		List<Atom> facts = new ArrayList<>(n);
		for (int i = 1; i <= n; i++) {
			facts.add(fact("v", i));
		}
		return facts;
	}

	private List<Atom> createEdges(int n) {
		List<Atom> facts = new ArrayList<>(n);
		for (int i = 2; i <= n; i++) {
			facts.add(fact("e", 1, i));
		}
		for (int i = 2; i <= n - 1; i++) {
			facts.add(fact("e", i, i + 1));
		}
		facts.add(fact("e", n, 2));
		return facts;
	}

	private Atom fact(String predicateName, int... iTerms) {
		List<Term> terms = new ArrayList<>(1);
		Predicate predicate = Predicates.getPredicate(predicateName, iTerms.length);
		for (int i : iTerms) {
			terms.add(Terms.newConstant(i));
		}
		return Atoms.newBasicAtom(predicate, terms);
	}
}
