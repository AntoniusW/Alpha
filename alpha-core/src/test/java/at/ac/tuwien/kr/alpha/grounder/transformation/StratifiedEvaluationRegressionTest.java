package at.ac.tuwien.kr.alpha.grounder.transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.Solver;
import at.ac.tuwien.kr.alpha.api.config.SystemConfig;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.grounder.Grounder;
import at.ac.tuwien.kr.alpha.core.grounder.GrounderFactory;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.programs.AnalyzedProgram;
import at.ac.tuwien.kr.alpha.core.programs.CompiledProgram;
import at.ac.tuwien.kr.alpha.core.programs.transformation.NormalizeProgramTransformation;
import at.ac.tuwien.kr.alpha.core.programs.transformation.StratifiedEvaluation;
import at.ac.tuwien.kr.alpha.core.solver.SolverFactory;
import at.ac.tuwien.kr.alpha.test.util.TestUtils;

@RunWith(Parameterized.class)
public class StratifiedEvaluationRegressionTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(StratifiedEvaluationRegressionTest.class);

	private static final String STRATIFIED_NEG_ASP = "base(X) :- req(X), not incomp(X).\n"
			+ "depend_base(X, Y) :- base(X), base(Y).\n"
			+ "dep_b_hlp(X) :- depend_base(X, _).\n"
			+ "fallback_base(X) :- base(X), not dep_b_hlp(X).\n"
			+ "depend_further(X) :- depend_base(_, X).\n"
			+ "depend_further(X) :- fallback_base(X).";

	private static final String BASIC_TEST_ASP = "a. b:- a.";
	private static final String BASIC_MULTI_INSTANCE_ASP = "p(a). p(b). q(X) :- p(X).";
	private static final String BASIC_NEGATION_ASP = "p(a). q(b). p(c). q(d). r(c). s(X, Y) :- p(X), q(Y), not r(X).";
	private static final String PART_STRATIFIED_ASP = "p(a). q(a). p(b). m(c). n(d).\n" + "r(X) :- p(X), q(X).\n" + "s(X, Y, Z) :- r(X), m(Y), n(Z).\n"
			+ "t(X, Y) :- p(X), q(X), p(Y), not q(Y).\n" + "either(X) :- t(X, _), not or(X).\n" + "or(X) :- t(X, _), not either(X).";
	private static final String POSITIVE_RECURSION_ASP = "num(0).\n" + "max_num(10).\n" + "num(S) :- num(N), S = N + 1, S <= M, max_num(M).";
	private static final String EMPTY_PROG_ASP = "";
	private static final String FACTS_ONLY_ASP = "a. b. c. p(a). q(b, c). r(c, c, a). s(b).";
	private static final String STRATIFIED_NO_FACTS_ASP = STRATIFIED_NEG_ASP;
	private static final String STRATIFIED_W_FACTS_ASP = "req(a). req(b). incomp(b).\n" + STRATIFIED_NEG_ASP;
	private static final String EQUALITY_ASP = "equal :- 1 = 1.";
	private static final String EQUALITY_WITH_VAR_ASP = "a(1). a(2). a(3). b(X) :- a(X), X = 1. c(X) :- a(X), X = 2. d(X) :- X = 3, a(X).";

	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> BASIC_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramBasic, StratifiedEvaluationRegressionTest::verifyAnswerSetsBasic);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> BASIC_MULTI_INSTANCE_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramBasicMultiInstance, StratifiedEvaluationRegressionTest::verifyAnswerSetsBasicMultiInstance);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> BASIC_NEGATION_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramBasicNegation, StratifiedEvaluationRegressionTest::verifyAnswerSetsBasicNegation);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> PART_STRATIFIED_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramPartStratified, StratifiedEvaluationRegressionTest::verifyAnswerSetsPartStratified);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> POSITIVE_RECURSIVE_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramPositiveRecursive, StratifiedEvaluationRegressionTest::verifyAnswerSetsPositiveRecursive);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> EMPTY_PROG_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramEmptyProg, StratifiedEvaluationRegressionTest::verifyAnswerSetsEmptyProg);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> FACTS_ONLY_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramFactsOnly, StratifiedEvaluationRegressionTest::verifyAnswerSetsFactsOnly);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> STRATIFIED_NO_FACTS_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramStratNoFacts, StratifiedEvaluationRegressionTest::verifyAnswerSetsStratNoFacts);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> STRATIFIED_W_FACTS_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramStratWithFacts, StratifiedEvaluationRegressionTest::verifyAnswerSetsStratWithFacts);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> EQUALITY_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramEquality, StratifiedEvaluationRegressionTest::verifyAnswerSetsEquality);
	private static final ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>> EQUALITY_WITH_VAR_VERIFIERS = new ImmutablePair<>(
			StratifiedEvaluationRegressionTest::verifyProgramEqualityWithVar, StratifiedEvaluationRegressionTest::verifyAnswerSetsEqualityWithVar);

	@Parameters(name = "Run {index}: aspString={0}, programVerifier={1}, answerSetsVerifier={2}")
	public static Iterable<Object[]> params() {
		List<ImmutablePair<String, ImmutablePair<Consumer<CompiledProgram>, Consumer<Set<AnswerSet>>>>> testCases = new ArrayList<>();
		List<Object[]> paramList = new ArrayList<>();
		testCases.add(new ImmutablePair<>(BASIC_TEST_ASP, BASIC_VERIFIERS));
		testCases.add(new ImmutablePair<>(BASIC_MULTI_INSTANCE_ASP, BASIC_MULTI_INSTANCE_VERIFIERS));
		testCases.add(new ImmutablePair<>(BASIC_NEGATION_ASP, BASIC_NEGATION_VERIFIERS));
		testCases.add(new ImmutablePair<>(PART_STRATIFIED_ASP, PART_STRATIFIED_VERIFIERS));
		testCases.add(new ImmutablePair<>(POSITIVE_RECURSION_ASP, POSITIVE_RECURSIVE_VERIFIERS));
		testCases.add(new ImmutablePair<>(EMPTY_PROG_ASP, EMPTY_PROG_VERIFIERS));
		testCases.add(new ImmutablePair<>(FACTS_ONLY_ASP, FACTS_ONLY_VERIFIERS));
		testCases.add(new ImmutablePair<>(STRATIFIED_NO_FACTS_ASP, STRATIFIED_NO_FACTS_VERIFIERS));
		testCases.add(new ImmutablePair<>(STRATIFIED_W_FACTS_ASP, STRATIFIED_W_FACTS_VERIFIERS));
		testCases.add(new ImmutablePair<>(EQUALITY_ASP, EQUALITY_VERIFIERS));
		testCases.add(new ImmutablePair<>(EQUALITY_WITH_VAR_ASP, EQUALITY_WITH_VAR_VERIFIERS));

		testCases.forEach((pair) -> paramList.add(new Object[] {pair.left, pair.right.left, pair.right.right }));
		return paramList;
	}

	private String aspString;
	private Consumer<CompiledProgram> programVerifier;
	private Consumer<Set<AnswerSet>> answerSetsVerifier;

	public StratifiedEvaluationRegressionTest(String aspString, Consumer<CompiledProgram> programVerifier, Consumer<Set<AnswerSet>> answerSetsVerifier) {
		this.aspString = aspString;
		this.programVerifier = programVerifier;
		this.answerSetsVerifier = answerSetsVerifier;
	}

	@Test
	public void runTest() {
		String aspStr = this.aspString;
		// Parse and pre-evaulate program
		ProgramParser parser = new ProgramParserImpl();
		ASPCore2Program prog = parser.parse(aspStr);
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(new NormalizeProgramTransformation(false).apply(prog));
		CompiledProgram evaluated = new StratifiedEvaluation().apply(analyzed);
		// Verify stratified evaluation result
		this.programVerifier.accept(evaluated);
		// Solve remaining program
		AtomStore atomStore = new AtomStoreImpl();
		Grounder grounder = GrounderFactory.getInstance("naive", evaluated, atomStore, false);
		Solver solver = SolverFactory.getInstance(new SystemConfig(), atomStore, grounder);
		Set<AnswerSet> answerSets = solver.collectSet();
		this.answerSetsVerifier.accept(answerSets);
	}

	private static void verifyProgramBasic(CompiledProgram evaluated) {
		TestUtils.assertFactsContainedInProgram(evaluated, TestUtils.basicAtomWithSymbolicTerms("a"), TestUtils.basicAtomWithSymbolicTerms("b"));
		Assert.assertEquals(2, evaluated.getFacts().size());
		Assert.assertTrue(evaluated.getRules().size() == 0);
	}

	private static void verifyAnswerSetsBasic(Set<AnswerSet> answerSets) {
		TestUtils.assertAnswerSetsEqual("a, b", answerSets);
	}

	private static void verifyProgramBasicMultiInstance(CompiledProgram evaluated) {
		TestUtils.assertFactsContainedInProgram(evaluated, TestUtils.basicAtomWithSymbolicTerms("q", "a"), TestUtils.basicAtomWithSymbolicTerms("q", "b"));
		Assert.assertTrue(evaluated.getRules().size() == 0);
	}

	private static void verifyAnswerSetsBasicMultiInstance(Set<AnswerSet> answerSets) {
		TestUtils.assertAnswerSetsEqual("p(a), p(b), q(a), q(b)", answerSets);
	}

	private static void verifyProgramBasicNegation(CompiledProgram evaluated) {
		TestUtils.assertFactsContainedInProgram(evaluated, TestUtils.basicAtomWithSymbolicTerms("s", "a", "b"),
				TestUtils.basicAtomWithSymbolicTerms("s", "a", "d"));
		Assert.assertEquals(7, evaluated.getFacts().size());
		Assert.assertEquals(0, evaluated.getRules().size());
	}

	private static void verifyAnswerSetsBasicNegation(Set<AnswerSet> answerSets) {
		TestUtils.assertAnswerSetsEqual("p(a), q(b), p(c), q(d), r(c), s(a,b), s(a,d)", answerSets);
	}

	private static void verifyProgramPartStratified(CompiledProgram evaluated) {
		TestUtils.assertFactsContainedInProgram(evaluated, TestUtils.basicAtomWithSymbolicTerms("p", "a"), TestUtils.basicAtomWithSymbolicTerms("q", "a"),
				TestUtils.basicAtomWithSymbolicTerms("p", "b"),
				TestUtils.basicAtomWithSymbolicTerms("m", "c"), TestUtils.basicAtomWithSymbolicTerms("n", "d"), TestUtils.basicAtomWithSymbolicTerms("r", "a"),
				TestUtils.basicAtomWithSymbolicTerms("s", "a", "c", "d"),
				TestUtils.basicAtomWithSymbolicTerms("t", "a", "b"));
		LOGGER.debug("part stratified evaluated prog is:\n{}", evaluated.toString());
		Assert.assertEquals(2, evaluated.getRules().size());
	}

	private static void verifyAnswerSetsPartStratified(Set<AnswerSet> answerSets) {
		TestUtils.assertAnswerSetsEqual(new String[] {"p(a), q(a), p(b), m(c), n(d), r(a), s(a,c,d), t(a,b), either(a)",
				"p(a), q(a), p(b), m(c), n(d), r(a), s(a,c,d), t(a,b), or(a)" }, answerSets);
	}

	private static void verifyProgramPositiveRecursive(CompiledProgram evaluated) {
		Predicate num = Predicates.getPredicate("num", 1);
		TestUtils.assertFactsContainedInProgram(evaluated, Atoms.newBasicAtom(Predicates.getPredicate("max_num", 1), Terms.newConstant(10)),
				Atoms.newBasicAtom(num, Terms.newConstant(0)),
				Atoms.newBasicAtom(num, Terms.newConstant(1)), Atoms.newBasicAtom(num, Terms.newConstant(2)),
				Atoms.newBasicAtom(num, Terms.newConstant(3)), Atoms.newBasicAtom(num, Terms.newConstant(4)),
				Atoms.newBasicAtom(num, Terms.newConstant(5)), Atoms.newBasicAtom(num, Terms.newConstant(6)),
				Atoms.newBasicAtom(num, Terms.newConstant(7)), Atoms.newBasicAtom(num, Terms.newConstant(8)),
				Atoms.newBasicAtom(num, Terms.newConstant(9)), Atoms.newBasicAtom(num, Terms.newConstant(10)));
		LOGGER.debug("Recursive program evaluated is:\n{}", evaluated.toString());
		Assert.assertEquals(0, evaluated.getRules().size());
	}

	private static void verifyAnswerSetsPositiveRecursive(Set<AnswerSet> answerSets) {
		TestUtils.assertAnswerSetsEqual("max_num(10), num(0), num(1), num(2), num(3), num(4), num(5), num(6), num(7), num(8), num(9), num(10)", answerSets);
	}

	private static void verifyProgramEmptyProg(CompiledProgram evaluated) {
		Assert.assertTrue(evaluated.getRules().isEmpty());
		Assert.assertTrue(evaluated.getRulesById().isEmpty());
		Assert.assertTrue(evaluated.getPredicateDefiningRules().isEmpty());
		Assert.assertTrue(evaluated.getFacts().isEmpty());
		Assert.assertTrue(evaluated.getFactsByPredicate().isEmpty());
	}

	private static void verifyAnswerSetsEmptyProg(Set<AnswerSet> answerSets) {
		Assert.assertEquals(1, answerSets.size());
		Assert.assertTrue(answerSets.iterator().next().isEmpty());
	}

	private static void verifyProgramFactsOnly(CompiledProgram evaluated) {
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("a")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("b")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("c")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("p", "a")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("q", "b", "c")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("r", "c", "c", "a")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("s", "b")));
	}

	private static void verifyAnswerSetsFactsOnly(Set<AnswerSet> answerSets) {
		TestUtils.assertAnswerSetsEqual("a, b, c, p(a), q(b,c), r(c,c,a), s(b)", answerSets);
	}

	private static void verifyProgramStratNoFacts(CompiledProgram evaluated) {
		Assert.assertTrue(evaluated.getFacts().isEmpty());
	}

	private static void verifyAnswerSetsStratNoFacts(Set<AnswerSet> answerSets) {
		Assert.assertEquals(1, answerSets.size());
		Assert.assertTrue(answerSets.iterator().next().isEmpty());
	}

	private static void verifyProgramStratWithFacts(CompiledProgram evaluated) {
		// rules should all be taken care of at this point
		Assert.assertTrue(evaluated.getRules().isEmpty());
		Assert.assertTrue(evaluated.getRulesById().isEmpty());
		Assert.assertTrue(evaluated.getPredicateDefiningRules().isEmpty());

		// facts should be the full answer set
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("req", "a")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("req", "b")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("incomp", "b")));

		// below facts from stratified evaluation
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("base", "a")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("depend_base", "a", "a")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("dep_b_hlp", "a")));
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("depend_further", "a")));
	}

	private static void verifyAnswerSetsStratWithFacts(Set<AnswerSet> answerSets) {
		TestUtils.assertAnswerSetsEqual("req(a), req(b), incomp(b), base(a), depend_base(a,a), dep_b_hlp(a), depend_further(a)", answerSets);
	}

	private static void verifyProgramEquality(CompiledProgram evaluated) {
		Assert.assertEquals(0, evaluated.getRules().size());
		Assert.assertTrue(evaluated.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("equal")));
	}

	private static void verifyAnswerSetsEquality(Set<AnswerSet> answerSets) {
		TestUtils.assertAnswerSetsEqual("equal", answerSets);
	}

	private static void verifyProgramEqualityWithVar(CompiledProgram evaluated) {
		Assert.assertEquals(0, evaluated.getRules().size());
		Assert.assertTrue(evaluated.getFacts().contains(Atoms.newBasicAtom(Predicates.getPredicate("a", 1), Terms.newConstant(1))));
		Assert.assertTrue(evaluated.getFacts().contains(Atoms.newBasicAtom(Predicates.getPredicate("c", 1), Terms.newConstant(2))));
		Assert.assertTrue(evaluated.getFacts().contains(Atoms.newBasicAtom(Predicates.getPredicate("d", 1), Terms.newConstant(3))));
	}

	private static void verifyAnswerSetsEqualityWithVar(Set<AnswerSet> answerSets) {
		TestUtils.assertAnswerSetsEqual("a(1), a(2), a(3), b(1), c(2), d(3)", answerSets);
	}

}
