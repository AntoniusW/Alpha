/**
 * Copyright (c) 2016, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.solver;

import static at.ac.tuwien.kr.alpha.test.util.TestUtils.assertRegressionTestAnswerSet;
import static at.ac.tuwien.kr.alpha.test.util.TestUtils.assertRegressionTestAnswerSets;
import static at.ac.tuwien.kr.alpha.test.util.TestUtils.assertRegressionTestAnswerSetsWithBase;
import static at.ac.tuwien.kr.alpha.test.util.TestUtils.buildSolverForRegressionTest;
import static at.ac.tuwien.kr.alpha.test.util.TestUtils.collectRegressionTestAnswerSets;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.antlr.v4.runtime.CharStreams;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import at.ac.tuwien.kr.alpha.AnswerSetsParser;
import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.AnswerSetBuilder;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.config.SystemConfig;
import at.ac.tuwien.kr.alpha.grounder.ChoiceGrounder;
import at.ac.tuwien.kr.alpha.grounder.DummyGrounder;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.solver.heuristics.BranchingHeuristicFactory;
import junit.framework.TestCase;

public class SolverTests {
	
	private static class Thingy implements Comparable<Thingy> {
		@Override
		public String toString() {
			return "thingy";
		}

		@Override
		public int compareTo(Thingy o) {
			return 0;
		}
	}

	@RegressionTest
	public void testObjectProgram(RegressionTestConfig cfg) throws IOException {
		final Thingy thingy = new Thingy();

		final Atom fact = new BasicAtom(Predicate.getInstance("foo", 1), ConstantTerm.getInstance(thingy));

		final InputProgram program = new InputProgram(
			Collections.emptyList(),
			Collections.singletonList(fact),
			new InlineDirectives()
		);

		assertEquals(singleton(new AnswerSetBuilder()
			.predicate("foo").instance(thingy)
			.build()), collectRegressionTestAnswerSets(program, cfg));
	}

	@RegressionTest
	public void testFactsOnlyProgram(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet(
			"p(a). p(b). foo(13). foo(16). q(a). q(c).",

			"q(a), q(c), p(a), p(b), foo(13), foo(16)",
			cfg
		);
	}

	@RegressionTest
	public void testSimpleRule(RegressionTestConfig cfg) throws Exception {
		assertRegressionTestAnswerSet(
			"p(a). p(b). r(X) :- p(X).",

			"p(a), p(b), r(a), r(b)",
			cfg
		);
	}

	@RegressionTest
	public void testSimpleRuleWithGroundPart(RegressionTestConfig cfg) throws Exception {
		assertRegressionTestAnswerSet(
			"p(1)." +
				"p(2)." +
				"q(X) :-  p(X), p(1).",

			"q(1), q(2), p(1), p(2)",
			cfg
		);
	}

	@RegressionTest
	public void testProgramZeroArityPredicates(RegressionTestConfig cfg) throws Exception {
		assertRegressionTestAnswerSet(
			"a. p(X) :- b, r(X).",

		"a",
		cfg
		);
	}

	@RegressionTest
	public void testChoiceGroundProgram(RegressionTestConfig cfg) throws Exception {
		assertRegressionTestAnswerSets(
			cfg,
			"a :- not b. b :- not a.",

			"a",
			"b"
		);
	}

	@RegressionTest
	public void testChoiceProgramNonGround(RegressionTestConfig cfg) throws Exception {
		assertRegressionTestAnswerSetsWithBase(
			cfg,
			"dom(1). dom(2). dom(3)." +
			"p(X) :- dom(X), not q(X)." +
			"q(X) :- dom(X), not p(X).",

			"dom(1), dom(2), dom(3)",

			"q(1), q(2), p(3)",
			"q(1), p(2), p(3)",
			"p(1), q(2), p(3)",
			"p(1), p(2), p(3)",
			"q(1), q(2), q(3)",
			"q(1), p(2), q(3)",
			"p(1), q(2), q(3)",
			"p(1), p(2), q(3)"
		);
	}

	@RegressionTest
	public void choiceProgram3Way(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"a :- not b, not c." +
			"b :- not a, not c." +
			"c :- not a, not b.",

			"a",
			"b",
			"c"
		);
	}

	@RegressionTest
	public void emptyProgramYieldsEmptyAnswerSet(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(cfg, "", "");
	}

	@RegressionTest
	public void chooseMultipleAnswerSets(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"a :- not nota." +
			"nota :- not a." +
			"b :- not notb." +
			"notb :- not b." +
			"c :- not notc." +
			"notc :- not c." +
			":- nota,notb,notc.",

			"a, b, c",
			"nota, b, c",
			"a, notb, c",
			"nota, notb, c",
			"a, b, notc",
			"nota, b, notc",
			"a, notb, notc"
		);
	}

	@RegressionTest
	public void builtinAtoms(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet(
			"dom(1). dom(2). dom(3). dom(4). dom(5)." +
			"p(X) :- dom(X), X = 4." +
			"r(Y) :- dom(Y), Y <= 2.",

			"dom(1), dom(2), dom(3), dom(4), dom(5), p(4), r(1), r(2)",
			cfg
		);
	}

	@RegressionTest
	public void builtinAtomsGroundRule(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet(
			"a :- 13 != 4." +
			"b :- 2 != 3, 2 = 3." +
			"c :- 2 <= 3, not 2 > 3.",

			"a, c",
			cfg
		);
	}

	
	@RegressionTest
	public void choiceProgramConstraintSimple(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet(
				"fact(a).\n" + 
				"choice(either, X) :- fact(X), not choice(or, X).\n" + 
				"choice(or, X) :- fact(X), not choice(either, X).\n" + 
				":- choice(or, X).",
				
				"fact(a), choice(either, a)",
				cfg
		);
	}
	
	@RegressionTest
	public void choiceProgramConstraintSimple2(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet(
				"fact(a).\n" + 
				"desired(either).\n" + 
				"choice(either, X) :- fact(X), not choice(or, X).\n" + 
				"choice(or, X) :- fact(X), not choice(either, X).\n" + 
				":- choice(C, X), not desired(C).",
				
				"fact(a), desired(either), choice(either, a)",
				cfg
		);
	}
	
	@RegressionTest
	public void choiceProgramConstraint(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSetsWithBase(
			cfg,
			"eq(1,1)." +
			"eq(2,2)." +
			"eq(3,3)." +
			"var(1)." +
			"var(2)." +
			"var(3)." +
			"val(VAR,1):-var(VAR),not val(VAR,2),not val(VAR,3)." +
			"val(VAR,2):-var(VAR),not val(VAR,1),not val(VAR,3)." +
			"val(VAR,3):-var(VAR),not val(VAR,1),not val(VAR,2)." +
			":- eq(VAL1,VAL2), not eq(VAR1,VAR2), val(VAR1,VAL1), val(VAR2,VAL2).",

			"eq(1, 1), eq(2, 2), eq(3, 3), var(1), var(2), var(3)",

			"val(1, 1), val(2, 2), val(3, 3)",
			"val(1, 1), val(3, 2), val(2, 3)",
			"val(2, 1), val(1, 2), val(3, 3)",
			"val(2, 1), val(3, 2), val(1, 3)",
			"val(3, 1), val(1, 2), val(2, 3)",
			"val(3, 1), val(2, 2), val(1, 3)"
		);
	}

	@RegressionTest
	public void choiceProgramConstraintPermutation(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSetsWithBase(
		cfg,
		"eq(1,1)." +
			"eq(2,2)." +
			"eq(3,3)." +
			"var(1)." +
			"var(2)." +
			"var(3)." +
			"val(VAR,1):-var(VAR),not val(VAR,2),not val(VAR,3)." +
			"val(VAR,2):-var(VAR),not val(VAR,1),not val(VAR,3)." +
			"val(VAR,3):-var(VAR),not val(VAR,1),not val(VAR,2)." +
			":- val(VAR1,VAL1), val(VAR2,VAL2), eq(VAL1,VAL2), not eq(VAR1,VAR2).",

			"eq(1,1), eq(2,2), eq(3,3), var(1), var(2), var(3)",

			"val(1,1), val(2,2), val(3,3)",
			"val(1,1), val(3,2), val(2,3)",
			"val(2,1), val(1,2), val(3,3)",
			"val(2,1), val(3,2), val(1,3)",
			"val(3,1), val(1,2), val(2,3)",
			"val(3,1), val(2,2), val(1,3)"
		);
	}

	@RegressionTest
	public void simpleNoPropagation(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet(
			"val(1,1)." +
			"val(2,2)." +
			"something:- val(VAR1,VAL1), val(VAR2,VAL2), anything(VAL1,VAL2).",

			"val(1, 1), val(2, 2)",
			cfg
		);
	}

	@RegressionTest
	public void choiceAndPropagationAfterwards(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSetsWithBase(
			cfg,
			"node(a)." +
			"node(b)." +
			"in(X) :- not out(X), node(X)." +
			"out(X) :- not in(X), node(X)." +
			"pair(X,Y) :- in(X), in(Y).",

			"node(a), node(b)",

			"in(a), in(b), pair(a,a), pair(a,b), pair(b,a), pair(b,b)",
			"in(b), out(a), pair(b,b)",
			"in(a), out(b), pair(a,a)",
			"out(a), out(b)"
		);
	}

	@RegressionTest
	public void choiceAndConstraints(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSetsWithBase(
			cfg,
			"node(a)." +
			"node(b)." +
			"edge(b,a)." +
			"in(X) :- not out(X), node(X)." +
			"out(X) :- not in(X), node(X)." +
			":- in(X), in(Y), edge(X,Y).",

			"node(a), node(b), edge(b,a)",

			"in(b), out(a)",
			"in(a), out(b)",
			"out(a), out(b)"
		);
	}

	@RegressionTest
	public void testUnsatisfiableProgram(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(cfg, "p(a). p(b). :- p(a), p(b).");
	}

	@RegressionTest
	public void testFunctionTermEquality(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet(
			"r1(f(a,b)). r2(f(a,b)). a :- r1(X), r2(Y), X = Y.",

			"r1(f(a,b)), r2(f(a,b)), a",
			cfg
		);
	}

	@RegressionTest
	public void builtinInequality(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSetsWithBase(
			cfg,
			"location(a1)." +
			"region(r1)." +
			"region(r2)." +
			"assign(L,R) :- location(L), region(R), not nassign(L,R)." +
			"nassign(L,R) :- location(L), region(R), not assign(L,R)." +
			":- assign(L,R1), assign(L,R2), R1 != R2." +
			"aux_ext_assign(a1,r1)." +
			"aux_ext_assign(a1,r2)." +
			"aux_not_assign(L,R) :- aux_ext_assign(L,R), not assign(L,R)." +
			":- aux_not_assign(L,R), assign(L,R).",

			"location(a1), region(r1), region(r2), aux_ext_assign(a1,r1), aux_ext_assign(a1,r2)",

			"assign(a1,r2), nassign(a1,r1), aux_not_assign(a1,r1)",
			"assign(a1,r1), nassign(a1,r2), aux_not_assign(a1,r2)",
			"nassign(a1,r1), nassign(a1,r2), aux_not_assign(a1,r1), aux_not_assign(a1,r2)"
		);
	}

	@RegressionTest
	public void choiceConstraintsInequality(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSetsWithBase(
			cfg,
			"assign(L, R) :- not nassign(L, R), possible(L, R)." +
			"nassign(L, R) :- not assign(L, R), possible(L, R)." +
			"assigned(L) :- assign(L, R)." +
			":- possible(L,_), not assigned(L)." +
			":- assign(L, R1), assign(L, R2), R1 != R2." +
			"possible(l1, r1). possible(l3, r3). possible(l4, r1). possible(l4, r3). possible(l5, r4). possible(l6, r2). possible(l7, r3). possible(l8, r2). possible(l9, r1). possible(l9, r4).",

			"possible(l1,r1), " +
			"possible(l3,r3), " +
			"possible(l4,r1), " +
			"possible(l4,r3), " +
			"possible(l5,r4), " +
			"possible(l6,r2), " +
			"possible(l7,r3), " +
			"possible(l8,r2), " +
			"possible(l9,r1), " +
			"possible(l9,r4), " +
			"assign(l1,r1), " +
			"assign(l3,r3), " +
			"assign(l5,r4), " +
			"assign(l6,r2), " +
			"assign(l7,r3), " +
			"assign(l8,r2), " +
			"assigned(l1), " +
			"assigned(l3), " +
			"assigned(l4), " +
			"assigned(l5), " +
			"assigned(l6), " +
			"assigned(l7), " +
			"assigned(l8), " +
			"assigned(l9)",

			"assign(l4,r1), " +
			"assign(l9,r4), " +
			"nassign(l4,r3), " +
			"nassign(l9,r1)",

			"assign(l4,r1), " +
			"assign(l9,r1), " +
			"nassign(l4,r3), " +
			"nassign(l9,r4)",

			"assign(l4,r3), " +
			"assign(l9,r4), " +
			"nassign(l4,r1), " +
			"nassign(l9,r1)",

			"assign(l4,r3), " +
			"assign(l9,r1), " +
			"nassign(l4,r1), " +
			"nassign(l9,r4)"
		);
	}
	
	@RegressionTest
	public void sameVariableTwiceInAtom(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"p(a, a)." +
			"q(X) :- p(X, X).",

			"p(a,a), q(a)"
		);
	}

	@RegressionTest
	public void sameVariableTwiceInAtomConstraint(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"p(a, a)." +
			":- p(X, X)."
		);
	}

	@RegressionTest
	public void noPositiveSelfFounding(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"a :- b." +
			"b:- a." +
			":- not b."
		);
	}

	@RegressionTest
	public void noPositiveCycleSelfFoundingChoice(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"c :- not d." +
			"d :- not c." +
			"a :- b, not c." +
			"b:- a." +
			":- not b."
		);
	}

	@RegressionTest
	public void conflictFromUnaryNoGood(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet(
			"d(b)." +
			"sel(X) :- not nsel(X), d(X)." +
			"nsel(X) :- not sel(X), d(X)." +
			"t(a) :- sel(b)." +
			":- t(X).",

			"d(b), nsel(b)",
			cfg
		);
	}

	@RegressionTest
	public void intervalsInFacts(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"a." +
			"facta(1..3)." +
			"factb(t, 5..8, u)." +
			"factc(1..3, w, 2 .. 4)." +
			"b(1,2)." +
			"b(3,4).",

			"facta(1), " +
			"facta(2), " +
			"facta(3), " +

			"factb(t, 5, u)," +
			"factb(t, 6, u)," +
			"factb(t, 7, u)," +
			"factb(t, 8, u)," +

			"factc(1, w, 2)," +
			"factc(2, w, 2)," +
			"factc(3, w, 2)," +
			"factc(1, w, 3)," +
			"factc(2, w, 3)," +
			"factc(3, w, 3)," +
			"factc(1, w, 4)," +
			"factc(2, w, 4)," +
			"factc(3, w, 4)," +

			"a," +

			"b(1, 2)," +
			"b(3, 4)"
		);
	}

	@RegressionTest
	public void intervalInRules(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"a :- 3 = 1..4 ." +
			"p(X, 1..X) :- dom(X), X != 2." +
			"dom(1). dom(2). dom(3).",

			"dom(1)," +
				"dom(2)," +
				"dom(3)," +

				"p(1, 1)," +
				"p(3, 1)," +
				"p(3, 2)," +
				"p(3, 3)," +

				"a"
		);
	}

	@RegressionTest
	public void emptyIntervals(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"p(3..1)." +
				"dom(5)." +
				"p(X) :- dom(X), X = 7..2 .",
			"dom(5)"
		);
	}

	@RegressionTest
	public void intervalInFunctionTermsInRules(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,
			"a :- q(f(1..3,g(4..5)))." +
			"q(f(2,g(4)))." +
			"q(f(1,g(5)))." +
			"p(f(1..3,g(4..5))) :- b." +
			"b.",

			"a, " +
			"b, " +

			"q(f(2,g(4))), " +
			"q(f(1,g(5))), " +

			"p(f(1,g(4))), " +
			"p(f(1,g(5))), " +
			"p(f(2,g(4))), " +
			"p(f(2,g(5))), " +
			"p(f(3,g(4))), " +
			"p(f(3,g(5)))"
		);
	}

	@RegressionTest
	public void groundAtomInRule(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet(
			"p :- dom(X), q, q2." +
				"dom(1)." +
				"q :- not nq." +
				"nq :- not q." +
				"q2 :- not nq2." +
				"nq2 :- not q2." +
				":- not p.",

			"dom(1), p, q, q2",
			cfg
		);
	}

	@RegressionTest
	public void simpleChoiceRule(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSetsWithBase(
			cfg,
			"{ a; b; c} :- d." +
				"d.",

			"d",
			"",
			"a",
			"a, b",
			"a, c",
			"a, b, c",
			"b",
			"b, c",
			"c"
		);
	}

	@RegressionTest
	public void conditionalChoiceRule(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSetsWithBase(
			cfg,
			"dom(1..3)." +
				"{ p(X): not q(X); r(Y): p(Y)} :- dom(X), q(Y)." +
				"q(2).",

			"dom(1)," +
				"dom(2)," +
				"dom(3)," +
				"q(2)",

			"p(1)," +
				"p(3)",

			"",

			"p(3)",

			"p(1)"
		);
	}

	@RegressionTest
	public void doubleChoiceRule(RegressionTestConfig cfg) throws IOException {
		Solver solver = buildSolverForRegressionTest("{ a }. { a }.", cfg);
		// Make sure that no superfluous answer sets that only differ on hidden atoms occur.
		List<AnswerSet> actual = solver.collectList();
		assertEquals(2, actual.size());
		assertEquals(AnswerSetsParser.parse("{} { a }"), new HashSet<>(actual));
	}

	@RegressionTest
	public void simpleArithmetics(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet("eight(X) :- X = 4 + 5 - 1." +
			"three(X) :- X = Z, Y = 1..10, Z = Y / 3, Z > 2, Z < 4.",

			"eight(8), three(3)",
			cfg);
	}

	@RegressionTest
	public void arithmeticsMultiplicationBeforeAddition(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSet("seven(X) :- 1+2 * 3 = X.",

			"seven(7)",
			cfg);
	}

	/**
	 * Tests the fix for issue #101
	 */
	@RegressionTest
	public void involvedUnsatisfiableProgram(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSets(
			cfg,	
			"x :- c1, c2, not x." +
			"c1 :- not a1." +
			"c1 :- not b1." +
			"c2 :- not a2." +
			"c2 :- not b2." +
			"a1 :- not b1." +
			"b1 :- not a1." +
			"a2 :- not b2." +
			"b2 :- not a2.");
	}

	@RegressionTest
	public void instanceEnumerationAtom(RegressionTestConfig cfg) throws IOException {
		Set<AnswerSet> answerSets = buildSolverForRegressionTest("# enumeration_predicate_is enum." +
			"dom(1). dom(2). dom(3)." +
			"p(X) :- dom(X)." +
			"q(Y) :- p(Y)." +
			"unique_position(Term,Pos) :- q(Term), enum(id0,Term,Pos)." +
			"wrong_double_occurrence :- unique_position(T1,P), unique_position(T2,P), T1 != T2.", cfg).collectSet();
		// Since enumeration depends on evaluation, we do not know which unique_position is actually assigned.
		// Check manually that there is one answer set, wrong_double_occurrence has not been derived, and enum yielded a unique position for each term.
		assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.iterator().next();
		assertEquals(null, answerSet.getPredicateInstances(Predicate.getInstance("wrong_double_occurrence", 0)));
		SortedSet<Atom> positions = answerSet.getPredicateInstances(Predicate.getInstance("unique_position", 2));
		assertEnumerationPositions(positions, 3);
	}

	@RegressionTest
	public void instanceEnumerationArbitraryTerms(RegressionTestConfig cfg) throws IOException {
		Set<AnswerSet> answerSets = buildSolverForRegressionTest("# enumeration_predicate_is enum." +
			"dom(a). dom(f(a,b)). dom(d)." +
			"p(X) :- dom(X)." +
			"q(Y) :- p(Y)." +
			"unique_position(Term,Pos) :- q(Term), enum(id0,Term,Pos)." +
			"wrong_double_occurrence :- unique_position(T1,P), unique_position(T2,P), T1 != T2.", cfg).collectSet();
		// Since enumeration depends on evaluation, we do not know which unique_position is actually assigned.
		// Check manually that there is one answer set, wrong_double_occurrence has not been derived, and enum yielded a unique position for each term.
		assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.iterator().next();
		assertPropositionalPredicateFalse(answerSet, Predicate.getInstance("wrong_double_occurrence", 0));
		SortedSet<Atom> positions = answerSet.getPredicateInstances(Predicate.getInstance("unique_position", 2));
		assertEnumerationPositions(positions, 3);
	}

	@RegressionTest
	public void instanceEnumerationMultipleIdentifiers(RegressionTestConfig cfg) throws IOException {
		Set<AnswerSet> answerSets = buildSolverForRegressionTest("# enumeration_predicate_is enum." +
			"dom(a). dom(b). dom(c). dom(d)." +
			"p(X) :- dom(X)." +
			"unique_position1(Term,Pos) :- p(Term), enum(id,Term,Pos)." +
			"unique_position2(Term,Pos) :- p(Term), enum(otherid,Term,Pos)." +
			"wrong_double_occurrence :- unique_position(T1,P), unique_position(T2,P), T1 != T2." +
			"wrong_double_occurrence :- unique_position2(T1,P), unique_position(T2,P), T1 != T2.", cfg).collectSet();
		// Since enumeration depends on evaluation, we do not know which unique_position is actually assigned.
		// Check manually that there is one answer set, wrong_double_occurrence has not been derived, and enum yielded a unique position for each term.
		assertEquals(1, answerSets.size());
		AnswerSet answerSet = answerSets.iterator().next();
		assertPropositionalPredicateFalse(answerSet, Predicate.getInstance("wrong_double_occurrence", 0));
		SortedSet<Atom> positions = answerSet.getPredicateInstances(Predicate.getInstance("unique_position1", 2));
		assertEnumerationPositions(positions, 4);
		SortedSet<Atom> positions2 = answerSet.getPredicateInstances(Predicate.getInstance("unique_position2", 2));
		assertEnumerationPositions(positions2, 4);
	}

	private void assertPropositionalPredicateFalse(AnswerSet answerSet, Predicate predicate) {
		assertEquals(null, answerSet.getPredicateInstances(predicate));
	}

	private void assertEnumerationPositions(SortedSet<Atom> positions, int numPositions) {
		assertEquals(numPositions, positions.size());
		boolean usedPositions[] = new boolean[numPositions];
		for (Atom position : positions) {
			@SuppressWarnings("unchecked")
			Integer atomPos = ((ConstantTerm<Integer>) position.getTerms().get(1)).getObject() - 1;
			assertTrue(atomPos < numPositions);
			usedPositions[atomPos] = true;
		}
		for (int i = 0; i < numPositions; i++) {
			assertTrue(usedPositions[i]);
		}
	}

	@RegressionTest
	public void smallCardinalityAggregate(RegressionTestConfig cfg) throws IOException {
		assertRegressionTestAnswerSetsWithBase(
			cfg,
			"dom(1..3)." +
				"bound(1..4)." +
				"{ value(X) : dom(X) }." +
				"num(K) :-  K <= #count {X : value(X) }, bound(K).",

			"dom(1), dom(2), dom(3), bound(1), bound(2), bound(3), bound(4)",
			"",
			"",
			"value(1), num(1)",
			"value(1), value(2), num(1), num(2)",
			"value(1), value(2), value(3), num(1), num(2), num(3)",
			"value(1), value(3), num(1), num(2)",
			"value(2), num(1)",
			"value(2), value(3), num(1), num(2)",
			"value(3), num(1)"
		);
	}

	// TODO move this out!
	@Test
	public void testLearnedUnaryNoGoodCausingOutOfOrderLiteralsConflict() throws IOException {
		final ProgramParser parser = new ProgramParser();
		InputProgram.Builder bld = InputProgram.builder();
		bld.accumulate(parser.parse(CharStreams.fromPath(Paths.get("src", "test", "resources", "HanoiTower_Alpha.asp"))));
		bld.accumulate(parser.parse(CharStreams.fromPath(Paths.get("src", "test", "resources", "HanoiTower_instances", "simple.asp"))));
		InputProgram parsedProgram = bld.build();
		
		SystemConfig config = new SystemConfig();
		config.setSolverName("default");
		config.setNogoodStoreName("alpharoaming");
		config.setSeed(0);
		config.setBranchingHeuristic(BranchingHeuristicFactory.Heuristic.valueOf("VSIDS"));
		config.setDebugInternalChecks(true);
		config.setDisableJustificationSearch(false);
		config.setEvaluateStratifiedPart(false);
		config.setReplayChoices(Arrays.asList(21, 26, 36, 56, 91, 96, 285, 166, 101, 290, 106, 451, 445, 439, 448,
			433, 427, 442, 421, 415, 436, 409, 430, 397, 391, 424, 385, 379,
			418, 373, 412, 406, 394, 388, 382, 245, 232, 208
		));
		Alpha alpha = new Alpha(config);
		Optional<AnswerSet> answerSet = alpha.solve(parsedProgram).findFirst();
		assertTrue(answerSet.isPresent());
	}


	@RegressionTest
	public void dummyGrounder(RegressionTestConfig cfg) {
		AtomStore atomStore = new AtomStoreImpl();
		TestCase.assertEquals(DummyGrounder.EXPECTED, buildSolverForRegressionTest(atomStore, new DummyGrounder(atomStore), cfg).collectSet());
	}

	@RegressionTest
	public void choiceGrounder(RegressionTestConfig cfg) {
		AtomStore atomStore = new AtomStoreImpl();
		TestCase.assertEquals(ChoiceGrounder.EXPECTED, buildSolverForRegressionTest(atomStore, new ChoiceGrounder(atomStore), cfg).collectSet());
	}

}
