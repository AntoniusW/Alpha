/**
 * Copyright (c) 2019, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.grounder.transformation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.Solver;
import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.config.SystemConfig;
import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.externals.Externals;
import at.ac.tuwien.kr.alpha.core.grounder.Grounder;
import at.ac.tuwien.kr.alpha.core.grounder.GrounderFactory;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.programs.AnalyzedProgram;
import at.ac.tuwien.kr.alpha.core.programs.CompiledProgram;
import at.ac.tuwien.kr.alpha.core.programs.InternalProgram;
import at.ac.tuwien.kr.alpha.core.programs.Programs;
import at.ac.tuwien.kr.alpha.core.programs.transformation.NormalizeProgramTransformation;
import at.ac.tuwien.kr.alpha.core.programs.transformation.StratifiedEvaluation;
import at.ac.tuwien.kr.alpha.core.solver.SolverFactory;
import at.ac.tuwien.kr.alpha.test.util.TestUtils;

public class StratifiedEvaluationTest {

	private final ProgramParser parser = new ProgramParserImpl();
	private final NormalizeProgramTransformation normalizer = new NormalizeProgramTransformation(false);
	private final StratifiedEvaluation evaluator = new StratifiedEvaluation();
	private final Function<String, CompiledProgram> parseAndEvaluate = (str) -> {
		return evaluator.apply(AnalyzedProgram.analyzeNormalProgram(normalizer.apply(parser.parse(str))));
	};

	private final Function<CompiledProgram, Set<AnswerSet>> solveCompiledProg = (prog) -> {
		AtomStore atomStore = new AtomStoreImpl();
		Grounder grounder = GrounderFactory.getInstance("naive", prog, atomStore, false);
		Solver solver = SolverFactory.getInstance(new SystemConfig(), atomStore, grounder);
		return solver.collectSet();
	};

	@Test
	public void testDuplicateFacts() {
		String aspStr = "p(a). p(b). q(b). q(X) :- p(X).";
		CompiledProgram evaluated = parseAndEvaluate.apply(aspStr);
		Instance qOfB = new Instance(TestUtils.basicAtomWithSymbolicTerms("q", "b").getTerms());
		Set<Instance> facts = evaluated.getFactsByPredicate().get(Predicates.getPredicate("q", 1));
		int numQOfB = 0;
		for (Instance at : facts) {
			if (at.equals(qOfB)) {
				numQOfB++;
			}
		}
		assertEquals(1, numQOfB);
	}

	@Test
	public void testEqualityWithConstantTerms() {
		String aspStr = "equal :- 1 = 1.";
		CompiledProgram evaluated = parseAndEvaluate.apply(aspStr);
		Atom equal = TestUtils.basicAtomWithSymbolicTerms("equal");
		assertTrue(evaluated.getFacts().contains(equal));
	}

	@Test
	public void testEqualityWithVarTerms() {
		String aspStr = "a(1). a(2). a(3). b(X) :- a(X), X = 1. c(X) :- a(X), X = 2. d(X) :- X = 3, a(X).";
		CompiledProgram evaluated = parseAndEvaluate.apply(aspStr);
		Set<AnswerSet> answerSets = solveCompiledProg.apply(evaluated);
		TestUtils.assertAnswerSetsEqual("a(1), a(2), a(3), b(1), c(2), d(3)", answerSets);
	}

	@Test
	public void testNonGroundableRule() {
		String asp = "p(a). q(a, b). s(X, Y) :- p(X), q(X, Y), r(Y).";
		CompiledProgram evaluated = parseAndEvaluate.apply(asp);
		Set<AnswerSet> answerSets = solveCompiledProg.apply(evaluated);
		TestUtils.assertAnswerSetsEqual("p(a), q(a,b)", answerSets);
	}

	@Test
	public void testCountAggregate() {
		String asp = "a. b :- 1 <= #count { 1 : a }.";
		CompiledProgram evaluated = parseAndEvaluate.apply(asp);
		Set<AnswerSet> answerSets = solveCompiledProg.apply(evaluated);
		TestUtils.assertAnswerSetsEqual("a, b", answerSets);
	}

	@Test
	public void testIntervalFact() {
		String asp = "a(1..3).";
		CompiledProgram evaluated = parseAndEvaluate.apply(asp);
		Set<AnswerSet> answerSets = solveCompiledProg.apply(evaluated);
		TestUtils.assertAnswerSetsEqual("a(1), a(2), a(3)", answerSets);
	}

	@Test
	public void testAggregateSpecial() {
		String asp = "thing(1..3).\n" + "% choose exactly one - since Alpha doesn't support bounds,\n" + "% this needs two constraints\n"
				+ "{ chosenThing(X) : thing(X) }.\n" + "chosenSomething :- chosenThing(X).\n" + ":- not chosenSomething.\n"
				+ ":- chosenThing(X), chosenThing(Y), X != Y.\n" + "allThings :- 3 <= #count{ X : thing(X)}. \n"
				+ "chosenMaxThing :- allThings, chosenThing(3).\n" + ":- not chosenMaxThing.";
		CompiledProgram evaluated = parseAndEvaluate.apply(asp);
		assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("allThings")));
		Set<AnswerSet> answerSets = solveCompiledProg.apply(evaluated);
		TestUtils.assertAnswerSetsEqual("allThings, thing(1), thing(2), thing(3), chosenMaxThing, chosenSomething, chosenThing(3)", answerSets);
	}

	@Test
	public void testNegatedFixedInterpretationLiteral() {
		String asp = "stuff(1). stuff(2). smallStuff(X) :- stuff(X), not X > 1.";
		CompiledProgram evaluated = parseAndEvaluate.apply(asp);
		Set<AnswerSet> answerSets = solveCompiledProg.apply(evaluated);
		TestUtils.assertAnswerSetsEqual("stuff(1), stuff(2), smallStuff(1)", answerSets);
	}

	@at.ac.tuwien.kr.alpha.api.externals.Predicate
	public static boolean sayTrue(Object o) {
		return true;
	}

	@Test
	public void testNegatedExternalLiteral() throws Exception {
		String asp = "claimedTruth(bla). truth(X) :- claimedTruth(X), &sayTrue[X]. lie(X) :- claimedTruth(X), not &sayTrue[X].";
		Map<String, PredicateInterpretation> externals = new HashMap<>();
		externals.put("sayTrue", Externals.processPredicateMethod(this.getClass().getMethod("sayTrue", Object.class)));
		ProgramParser parserWithExternals = new ProgramParserImpl();
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normalizer.apply(parserWithExternals.parse(asp, externals)));
		CompiledProgram evaluated = new StratifiedEvaluation().apply(analyzed);
		Set<AnswerSet> answerSets = solveCompiledProg.apply(evaluated);
		TestUtils.assertAnswerSetsEqual("claimedTruth(bla), truth(bla)", answerSets);
	}

	/**
	 * Tests an encoding associated with the partner units problem (PUP) that computes a topological order to be used by
	 * domain-specific heuristics. The entire program can be solved by stratified evaluation.
	 */
	@Test
	public void testPartnerUnitsProblemTopologicalOrder() throws IOException {
		ASPCore2Program prg = parser.parse(StratifiedEvaluationTest.class.getResourceAsStream("/partial-eval/pup_topological_order.asp"));
		CompiledProgram evaluated = new StratifiedEvaluation().apply(AnalyzedProgram.analyzeNormalProgram(normalizer.apply(prg)));
		assertTrue("Not all rules eliminated by stratified evaluation", evaluated.getRules().isEmpty());
		assertEquals(57, evaluated.getFacts().size());
	}

	/**
	 * Verifies correct handling of negated basic literals in StratifiedEvaluation.
	 * For details, see comments in test program
	 * 
	 * @throws IOException
	 */
	@Test
	public void testNegatedLiteralInRecursiveRule() throws IOException {
		//@formatter:off
		String expectedAnswerSet = "basefact1(1), basefact2(1), max_value(10), min_value(1), "
				+ "basefact1(9), basefact2(9), base(1), base(9), "
				+ "inc_value(1), inc_value(2), inc_value(2), inc_value(3), "
				+ "inc_value(4), inc_value(5), inc_value(6), inc_value(7), "
				+ "inc_value(8)";
		//@formatter:on
		ASPCore2Program prog = Programs.fromInputStream(
				StratifiedEvaluationTest.class.getResourceAsStream("/partial-eval/recursive_w_negated_condition.asp"),
				new HashMap<>());
		
		// Run stratified evaluation and solve
		CompiledProgram inputStratEval = new StratifiedEvaluation().apply(AnalyzedProgram.analyzeNormalProgram(normalizer.apply(prog)));
		Set<AnswerSet> asStrat = solveCompiledProg.apply(inputStratEval);
		TestUtils.assertAnswerSetsEqual(expectedAnswerSet, asStrat);
		
		// Solve without stratified evaluation
		CompiledProgram inputNoStratEval = InternalProgram.fromNormalProgram(normalizer.apply(prog));
		Set<AnswerSet> as = solveCompiledProg.apply(inputNoStratEval);
		TestUtils.assertAnswerSetsEqual(expectedAnswerSet, as);
	}

	@Test
	public void testRecursiveRanking() {
		//@formatter:off
		String asp = "thing(a).\n" + 
				"thing(b).\n" + 
				"thing(c).\n" + 
				"thing_before(a, b).\n" + 
				"thing_before(b, c).\n" + 
				"has_prev_thing(X) :- thing(X), thing_succ(_, X).\n" + 
				"first_thing(X) :- thing(X), not has_prev_thing(X).\n" + 
				"thing_not_succ(X, Y) :-\n" + 
				"	thing(X),\n" + 
				"	thing(Y),\n" + 
				"	thing(INTM),\n" + 
				"	thing_before(X, Y),\n" + 
				"	thing_before(X, INTM),\n" + 
				"	thing_before(INTM, X).\n" + 
				"thing_succ(X, Y) :-\n" + 
				"	thing(X),\n" + 
				"	thing(Y),\n" + 
				"	thing_before(X, Y),\n" + 
				"	not thing_not_succ(X, Y).\n" + 
				"thing_rank(X, 1) :- first_thing(X).\n" + 
				"thing_rank(X, R) :-\n" + 
				"	thing(X),\n" + 
				"	thing_succ(Y, X),\n" + 
				"	thing_rank(Y, K),\n" + 
				"	R = K + 1.";
		//@formatter:on
		CompiledProgram evaluated = parseAndEvaluate.apply(asp);
		Predicate rank = Predicates.getPredicate("thing_rank", 2);
		BasicAtom rank1 = Atoms.newBasicAtom(rank, Terms.newSymbolicConstant("a"), Terms.newConstant(1));
		BasicAtom rank2 = Atoms.newBasicAtom(rank, Terms.newSymbolicConstant("b"), Terms.newConstant(2));
		BasicAtom rank3 = Atoms.newBasicAtom(rank, Terms.newSymbolicConstant("c"), Terms.newConstant(3));
		List<Atom> evaluatedFacts = evaluated.getFacts();
		Assert.assertTrue(evaluatedFacts.contains(rank1));
		Assert.assertTrue(evaluatedFacts.contains(rank2));
		Assert.assertTrue(evaluatedFacts.contains(rank3));
	}

}
