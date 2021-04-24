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
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.programs.InputProgram;

public class ThreeColouringRandomGraphTest extends AbstractSolverTests {
	@Test(timeout = 1000)
	public void testV3E3() throws IOException {
		testThreeColouring(3, 3);
	}

	@Test(timeout = 10000)
	@Ignore("disabled to save resources during CI")
	public void testV10E18() throws IOException {
		testThreeColouring(10, 18);
	}

	@Test(timeout = 10000)
	@Ignore("disabled to save resources during CI")
	public void testV20E38() throws IOException {
		testThreeColouring(20, 38);
	}

	@Test(timeout = 10000)
	@Ignore("disabled to save resources during CI")
	public void testV30E48() throws IOException {
		testThreeColouring(30, 48);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testV200E300() throws IOException {
		testThreeColouring(200, 300);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testV300E200() throws IOException {
		testThreeColouring(300, 200);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testV300E300() throws IOException {
		testThreeColouring(300, 300);
	}

	private void testThreeColouring(int nVertices, int nEdges) throws IOException {
		ASPCore2Program tmpPrg = new ProgramParserImpl().parse(
				"blue(N) :- v(N), not red(N), not green(N)." +
				"red(N) :- v(N), not blue(N), not green(N)." +
				"green(N) :- v(N), not red(N), not blue(N)." +
				":- e(N1,N2), blue(N1), blue(N2)." +
				":- e(N1,N2), red(N1), red(N2)." +
				":- e(N1,N2), green(N1), green(N2).");
		InputProgram.Builder prgBuilder = InputProgram.builder(tmpPrg);
		prgBuilder.addFacts(createVertices(nVertices));
		prgBuilder.addFacts(createEdges(nVertices, nEdges));
		InputProgram program = prgBuilder.build();
		maybeShuffle(program);

		Optional<AnswerSet> answerSet = getInstance(program).stream().findAny();
		//System.out.println(answerSet);

		// TODO: check correctness of answer set
	}

	private void maybeShuffle(InputProgram program) {

		// TODO: switch on if different rule orderings in the encoding are desired (e.g. for benchmarking purposes)
		// FIXME since InputProgram is immutable this needs to be reworked a bit if used
		// Collections.reverse(program.getRules());
		// Collections.shuffle(program.getRules());
		// Collections.reverse(program.getFacts());
		// Collections.shuffle(program.getFacts());
	}

	private List<Atom> createVertices(int n) {
		List<Atom> facts = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			facts.add(fact("v", i));
		}
		return facts;
	}

	private List<Atom> createEdges(int vertices, int edges) {
		Random rand = new Random(0);
		List<Atom> facts = new ArrayList<>(edges);
		for (int i = 0; i < edges; i++) {
			int v1 = 0;
			int v2 = 0;
			while (v1 == v2) {
				v1 = rand.nextInt(vertices);
				v2 = rand.nextInt(vertices);
			}
			facts.add(fact("e", v1, v2));
			facts.add(fact("e", v2, v1));
		}
		return facts;
	}

	private Atom fact(String predicateName, int... iTerms) {
		List<Term> terms = new ArrayList<>(1);
		for (int i : iTerms) {
			terms.add(Terms.newConstant(i));
		}
		return Atoms.newBasicAtom(Predicates.getPredicate(predicateName, iTerms.length), terms);
	}
}
