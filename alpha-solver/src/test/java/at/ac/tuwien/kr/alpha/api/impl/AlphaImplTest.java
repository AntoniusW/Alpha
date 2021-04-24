/**
 * Copyright (c) 2017-2018, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.api.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.config.InputConfig;
import at.ac.tuwien.kr.alpha.api.config.SystemConfig;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.NormalProgram;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.solver.heuristics.Heuristic;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.commons.AnswerSetBuilder;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.literals.Literals;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.common.fixedinterpretations.MethodPredicateInterpretation;
import at.ac.tuwien.kr.alpha.core.externals.AspStandardLibrary;
import at.ac.tuwien.kr.alpha.core.externals.Externals;
import at.ac.tuwien.kr.alpha.core.parser.InlineDirectivesImpl;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.programs.CompiledProgram;
import at.ac.tuwien.kr.alpha.core.programs.InputProgram;
import at.ac.tuwien.kr.alpha.core.rules.BasicRule;
import at.ac.tuwien.kr.alpha.core.rules.heads.NormalHeadImpl;
import at.ac.tuwien.kr.alpha.core.util.AnswerSetsParser;
import at.ac.tuwien.kr.alpha.test.util.TestUtils;

public class AlphaImplTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AspStandardLibrary.class);
	
	//@formatter:off
	private static final String STRINGSTUFF_ASP =
			"string(\"bla\")."
			+ "string(\"blubb\")."
			+ "string(\"foo\")."
			+ "string(\"bar\")."
			+ "{ strcat(S1, S2) } :- string(S1), string(S2)."
			+ "resultstring(SCAT) :- strcat(S1, S2), &stdlib_string_concat[S1, S2](SCAT)."
			+ ":- resultstring(S), &stdlib_string_length[S](LEN), LEN != 6."
			+ "containsFoo(S) :- resultstring(S), &stdlib_string_matches_regex[S, \".*foo.*\"]."
			+ ":- resultstring(S), not containsFoo(S)."
			+ "has_resultstring :- resultstring(_)."
			+ ":- not has_resultstring.";
	
	// same as stringstuff asp, but without the "containsFoo" intermediate predicate
	private static final String NEGATED_EXTERNAL_ASP =
			"string(\"bla\")."
			+ "string(\"blubb\")."
			+ "string(\"foo\")."
			+ "string(\"bar\")."
			+ "{ strcat(S1, S2) } :- string(S1), string(S2)."
			+ "resultstring(SCAT) :- strcat(S1, S2), &stdlib_string_concat[S1, S2](SCAT)."
			+ ":- resultstring(S), &stdlib_string_length[S](LEN), LEN != 6."
			+ ":- resultstring(S), not &stdlib_string_matches_regex[S, \".*foo.*\"]."
			+ "has_resultstring :- resultstring(_)."
			+ ":- not has_resultstring.";
	//@formatter:on

	private static int invocations;
	
	@at.ac.tuwien.kr.alpha.api.externals.Predicate
	public static boolean isOne(int term) {
		invocations++;
		return term == 1;
	}

	@at.ac.tuwien.kr.alpha.api.externals.Predicate
	public static boolean isFoo(Integer a) {
		return a == 0xF00;
	}

	@at.ac.tuwien.kr.alpha.api.externals.Predicate
	public static boolean thinger(Thingy thingy) {
		return true;
	}

	@Test
	public void withExternal() throws Exception {
		Alpha alpha = new AlphaImpl();
		InputConfig inputCfg = InputConfig.forString("a :- &isOne[1].");
		inputCfg.addPredicateMethod("isOne", Externals.processPredicateMethod(this.getClass().getMethod("isOne", int.class)));
		ASPCore2Program program = alpha.readProgram(inputCfg);
		Set<AnswerSet> actual = alpha.solve(program).collect(Collectors.toSet());
		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("a").build()));
		assertEquals(expected, actual);
	}

	@Test
	public void addsFacts() throws Exception {
		Alpha system = new AlphaImpl();
		Thingy a = new Thingy();
		Thingy b = new Thingy();
		List<Thingy> things = asList(a, b);
		InputProgram program = InputProgram.builder().addFacts(Externals.asFacts(Thingy.class, things)).build();
		Set<AnswerSet> actual = system.solve(program).collect(Collectors.toSet());
		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("thingy").instance(a).instance(b).build()));
		assertEquals(expected, actual);
	}

	@Test(expected = IllegalArgumentException.class)
	public void withExternalTypeConflict() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig inputCfg = InputConfig.forString("a :- &isFoo[\"adsfnfdsf\"].");
		inputCfg.addPredicateMethod("isFoo", Externals.processPredicateMethod(this.getClass().getMethod("isFoo", Integer.class)));
		Set<AnswerSet> actual = system.solve(system.readProgram(inputCfg)).collect(Collectors.toSet());
		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("a").build()));
		assertEquals(expected, actual);
	}

	@Test
	public void smallGraph() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig inputCfg = InputConfig.forString("node(1). node(2). node(3). a :- &connected[1,2].");
		inputCfg.addPredicateMethod("connected", Externals.processPredicate((Integer a, Integer b) -> (a == 1 && b == 2) || (b == 2 || b == 3)));
		ASPCore2Program program = system.readProgram(inputCfg);

		Set<AnswerSet> actual = system.solve(program).collect(Collectors.toSet());
		Set<AnswerSet> expected = AnswerSetsParser.parse("{ a, node(1), node(2), node(3) }");
		assertEquals(expected, actual);
	}

	@Test
	public void filterOutput() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig inputCfg = InputConfig.forString("node(1). node(2). outgoing13(X) :- node(X), &getLargeGraphEdges(13,X).");
		inputCfg.addPredicateMethod("getLargeGraphEdges",
				Externals.processPredicate(() -> new HashSet<>(asList(asList(Terms.newConstant(1), Terms.newConstant(2)),
						asList(Terms.newConstant(2), Terms.newConstant(1)), asList(Terms.newConstant(13), Terms.newConstant(1))))));
		ASPCore2Program program = system.readProgram(inputCfg);
		Set<AnswerSet> actual = system.solve(program).collect(Collectors.toSet());
		Set<AnswerSet> expected = AnswerSetsParser.parse("{ node(1), node(2), outgoing13(1) }");
		assertEquals(expected, actual);
	}

	@Test
	public void supplier() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("node(1). a :- &bestNode(X), node(X).");
		cfg.addPredicateMethod("bestNode", Externals.processPredicate(() -> singleton(singletonList(Terms.newConstant(1)))));
		ASPCore2Program prog = system.readProgram(cfg);

		Set<AnswerSet> expected = AnswerSetsParser.parse("{ node(1), a }");
		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		assertEquals(expected, actual);
	}

	@at.ac.tuwien.kr.alpha.api.externals.Predicate
	public static Set<List<ConstantTerm<?>>> bestNode() {
		return singleton(singletonList(Terms.newConstant(1)));
	}

	@Test
	public void noInput() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("node(1). a :- &bestNode(X), node(X).");
		cfg.addPredicateMethod("bestNode", Externals.processPredicateMethod(this.getClass().getMethod("bestNode")));
		ASPCore2Program prog = system.readProgram(cfg);

		Set<AnswerSet> expected = AnswerSetsParser.parse("{ node(1), a }");
		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		assertEquals(expected, actual);
	}

	@Test(expected = IllegalArgumentException.class)
	public void smallGraphWithWrongType() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("a :- &connected[\"hello\",2].");
		cfg.addPredicateMethod("connected", Externals.processPredicate((Integer a, Integer b) -> (a == 1 && b == 2) || (b == 2 || b == 3)));
		ASPCore2Program prog = system.readProgram(cfg);

		system.solve(prog).collect(Collectors.toSet());
	}

	public static Set<List<ConstantTerm<Integer>>> neighbors(int node) {
		if (node == 1) {
			return new HashSet<>(asList(singletonList(Terms.newConstant(2)), singletonList(Terms.newConstant(3))));
		}
		return emptySet();
	}

	public static Set<List<ConstantTerm<Integer>>> coolNode(int node) {
		if (node == 1) {
			return singleton(emptyList());
		}
		return emptySet();
	}

	@Test
	@Ignore("Test program is not safe (external lacking output variables). This should throw some exception.")
	public void smallGraphNoNeighbors() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("noNeighbors(2) :- not &neighbors[2].");
		cfg.addPredicateMethod("neighbors", Externals.processPredicateMethod(this.getClass().getMethod("neighbors", int.class)));
		ASPCore2Program prog = system.readProgram(cfg);

		Set<AnswerSet> expected = AnswerSetsParser.parse("{ noNeighbors(2) }");
		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		assertEquals(expected, actual);
	}

	@Test
	public void smallGraphCoolNode() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("node(1..2). in(X) :- node(X), &coolNode[X].");
		cfg.addPredicateMethod("coolNode", Externals.processPredicateMethod(this.getClass().getMethod("coolNode", int.class)));
		ASPCore2Program prog = system.readProgram(cfg);

		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		Set<AnswerSet> expected = AnswerSetsParser.parse("{ in(1), node(1), node(2) }");
		assertEquals(expected, actual);
	}

	@Test
	public void smallGraphSingleNeighbor() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("node(1..3). in(1,X) :- &neighbors[1](X), node(X).");
		cfg.addPredicateMethod("neighbors", Externals.processPredicateMethod(this.getClass().getMethod("neighbors", int.class)));
		ASPCore2Program prog = system.readProgram(cfg);

		Set<AnswerSet> expected = AnswerSetsParser.parse("{ in(1,2), in(1,3), node(1), node(2), node(3) }");
		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		assertEquals(expected, actual);
	}

	@Test
	@Ignore("Test program is not safe (external lacking output variables). This should throw some exception.")
	public void smallGraphSingleNeighborNoTerm() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("success :- &neighbors[1], not &neighbors[2].");
		cfg.addPredicateMethod("neighbors", Externals.processPredicateMethod(this.getClass().getMethod("neighbors", int.class)));
		ASPCore2Program prog = system.readProgram(cfg);

		Set<AnswerSet> expected = AnswerSetsParser.parse("{ success }");
		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		assertEquals(expected, actual);
	}

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

	private static class SubThingy extends Thingy {
	}

	@Test
	public void withExternalSubtype() throws Exception {
		SubThingy thingy = new SubThingy();

		BasicRule rule = new BasicRule(
				new NormalHeadImpl(Atoms.newBasicAtom(Predicates.getPredicate("p", 1), Terms.newConstant("x"))),
				singletonList(Literals.fromAtom(Atoms.newExternalAtom(Predicates.getPredicate("thinger", 1),
						new MethodPredicateInterpretation(this.getClass().getMethod("thinger", Thingy.class)), singletonList(Terms.newConstant(thingy)),
						emptyList()), true)));

		Alpha system = new AlphaImpl();

		InputProgram prog = new InputProgram(singletonList(rule), emptyList(), new InlineDirectivesImpl());

		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("p").instance("x").build()));
		assertEquals(expected, actual);
	}

	@Test
	public void withExternalViaAnnotation() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("a :- &isOne[1].");
		cfg.addPredicateMethods(Externals.scan(this.getClass()));
		ASPCore2Program prog = system.readProgram(cfg);

		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("a").build()));
		assertEquals(expected, actual);
	}

	/**
	 * Externals may only be scanned once per implementation.
	 * If at the time of a scan, the name of an external is already registered,
	 * an exception is thrown.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void errorDuplicateExternal() throws Exception {
		InputConfig cfg = InputConfig.forString("someString.");
		cfg.addPredicateMethods(Externals.scan(this.getClass()));
		cfg.addPredicateMethods(Externals.scan(this.getClass()));
	}

	@Test
	public void withNativeExternal() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("a :- &isTwo[2].");
		cfg.addPredicateMethod("isTwo", Externals.processPredicate((Integer t) -> t == 2));
		ASPCore2Program prog = system.readProgram(cfg);

		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("a").build()));
		assertEquals(expected, actual);
	}

	@Test
	@Ignore("External atom has state, which is not allowed. Caching of calls makes the number of invocations wrong.")
	public void withExternalInvocationCounted1() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("a :- &isOne[1], &isOne[1].");
		cfg.addPredicateMethod("isOne", Externals.processPredicateMethod(this.getClass().getMethod("isOne", int.class)));
		ASPCore2Program prog = system.readProgram(cfg);

		int before = invocations;
		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		int after = invocations;

		assertEquals(2, after - before);

		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("a").build()));
		assertEquals(expected, actual);
	}

	@Test
	@Ignore("External atom has state, which is not allowed. Caching of calls makes the number of invocations wrong.")
	public void withExternalInvocationCounted2() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("a. b :- &isOne[1], &isOne[2].");
		cfg.addPredicateMethod("isOne", Externals.processPredicateMethod(this.getClass().getMethod("isOne", int.class)));
		ASPCore2Program prog = system.readProgram(cfg);

		int before = invocations;
		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		int after = invocations;

		assertEquals(2, after - before);

		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("a").build()));
		assertEquals(expected, actual);
	}

	@Test
	@Ignore("External atom has state, which is not allowed. Caching of calls makes the number of invocations wrong.")
	public void withExternalInvocationCounted3() throws Exception {
		Alpha system = new AlphaImpl();
		InputConfig cfg = InputConfig.forString("a :- &isOne[1], not &isOne[2].");
		cfg.addPredicateMethod("isOne", Externals.processPredicateMethod(this.getClass().getMethod("isOne", int.class)));
		ASPCore2Program prog = system.readProgram(cfg);

		int before = invocations;
		Set<AnswerSet> actual = system.solve(prog).collect(Collectors.toSet());
		int after = invocations;

		assertEquals(1, after - before);

		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("a").build()));
		assertEquals(expected, actual);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void programWithExternalStringStuff() throws IOException {
		Alpha alpha = new AlphaImpl();
		ASPCore2Program prog = alpha.readProgram(InputConfig.forString(STRINGSTUFF_ASP));
		Set<AnswerSet> answerSets = alpha.solve(prog).collect(Collectors.toSet());
		// Verify every result string has length 6 and contains "foo"
		for (AnswerSet as : answerSets) {
			for (Atom atom : as.getPredicateInstances(Predicates.getPredicate("resultstring", 1))) {
				String resultstring = ((ConstantTerm<String>) atom.getTerms().get(0)).getObject();
				Assert.assertEquals(6, resultstring.length());
				Assert.assertTrue(resultstring.contains("foo"));
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void withNegatedExternal() throws IOException {
		Alpha alpha = new AlphaImpl();
		ASPCore2Program prog = alpha.readProgram(InputConfig.forString(NEGATED_EXTERNAL_ASP));
		Set<AnswerSet> answerSets = alpha.solve(prog).collect(Collectors.toSet());
		Assert.assertEquals(31, answerSets.size());
		// Verify every result string has length 6 and contains "foo"
		for (AnswerSet as : answerSets) {
			for (Atom atom : as.getPredicateInstances(Predicates.getPredicate("resultstring", 1))) {
				String resultstring = ((ConstantTerm<String>) atom.getTerms().get(0)).getObject();
				LOGGER.debug("ResultString is {}", resultstring);
				Assert.assertEquals(6, resultstring.length());
				Assert.assertTrue(resultstring.contains("foo"));
			}
		}
	}

	@Test
	public void basicUsage() throws Exception {
		Alpha system = new AlphaImpl();
		Set<AnswerSet> actual = system.solve(system.readProgram(InputConfig.forString("p(a)."))).collect(Collectors.toSet());
		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("p").symbolicInstance("a").build()));
		assertEquals(expected, actual);
	}

	@Test
	public void basicUsageWithString() throws Exception {
		Alpha system = new AlphaImpl();
		Set<AnswerSet> actual = system.solve(system.readProgram(InputConfig.forString("p(\"a\")."))).collect(Collectors.toSet());
		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("p").instance("a").build()));
		assertEquals(expected, actual);
	}

	/**
	 * Verifies that filters are handled correctly (regression test case introduced when fixing issue #189).
	 */
	@Test
	public void filterTest() {
		String progstr = "a. b. c. d :- c. e(a, b) :- d.";
		Alpha system = new AlphaImpl();
		ASPCore2Program prog = system.readProgramString(progstr);
		Set<AnswerSet> actual = system.solve(prog, (p) -> p.equals(Predicates.getPredicate("a", 0)) || p.equals(Predicates.getPredicate("e", 2)))
				.collect(Collectors.toSet());
		Set<AnswerSet> expected = new HashSet<>(singletonList(new AnswerSetBuilder().predicate("a").predicate("e").symbolicInstance("a", "b").build()));
		assertEquals(expected, actual);
	}

	/**
	 * Verifies that no stratified evaluation is performed up-front when disabled in config.
	 */
	@Test
	public void disableStratifiedEvalTest() {
		// Note: This might be cleaner if the test used the debugSolve method from the interface
		String progstr = "p(a). q(X) :- p(X).";
		SystemConfig cfg = new SystemConfig();
		cfg.setEvaluateStratifiedPart(false);
		AlphaImpl system = new AlphaImpl(cfg);
		ASPCore2Program input = system.readProgramString(progstr);
		NormalProgram normal = system.normalizeProgram(input);
		CompiledProgram preprocessed = system.performProgramPreprocessing(normal);
		Assert.assertFalse("Preprocessed program contains fact derived from stratifiable rule, but should not!",
				preprocessed.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("q", "a")));
	}

	/**
	 * Verifies that stratified evaluation is performed up-front if not otherwise configured.
	 */
	@Test
	public void enableStratifiedEvalTest() {
		// Note: This might be cleaner if the test used the debugSolve method from the interface
		String progstr = "p(a). q(X) :- p(X).";
		SystemConfig cfg = new SystemConfig();
		AlphaImpl system = new AlphaImpl(cfg);
		ASPCore2Program input = system.readProgramString(progstr);
		NormalProgram normal = system.normalizeProgram(input);
		CompiledProgram preprocessed = system.performProgramPreprocessing(normal);
		Assert.assertTrue("Preprocessed program does not contain fact derived from stratifiable rule, but should!",
				preprocessed.getFacts().contains(TestUtils.basicAtomWithSymbolicTerms("q", "a")));
	}

	/**
	 * Runs a test that formerly caused some sort of exception.
	 */
	@Test
	public void problematicRun_3col_1119654162577372() throws IOException {
		/*
		 * NOTE: This was constructed from the following commandline invocation: -DebugEnableInternalChecks -q -g naive -s
		 * default -e 1119654162577372 -n 200 -i
		 * 3col-20-38.txt
		 */
		problematicRun("3col-20-38.txt", 1119654162577372L, 200);
	}

	/**
	 * Runs a test that formerly caused some sort of exception.
	 */
	@Test
	public void problematicRun_3col_1119718541727902() throws IOException {
		/*
		 * NOTE: This was constructed from the following commandline invocation:
		 * -DebugEnableInternalChecks -q -g naive -s default -e 1119718541727902 -n 200 -i
		 * 3col-20-38.txt
		 */
		problematicRun("3col-20-38.txt", 1119718541727902L, 200);
	}

	/**
	 * Runs a test that formerly caused some sort of exception.
	 */
	@Test
	public void problematicRun_vehicle_97598271567626() throws IOException {
		/*
		 * NOTE: This was constructed from the following commandline invocation:
		 * -DebugEnableInternalChecks -q -g naive -s default -e 97598271567626 -n 2 -i vehicle_normal_small.asp
		 */
		problematicRun("vehicle_normal_small.asp", 1119718541727902L, 2);
	}

	/**
	 * Runs a test that formerly caused some sort of exception.
	 */
	@Test
	public void problematicRun_3col_1119718541727902_sorted_400() throws IOException {
		/*
		 * NOTE: This was constructed from the following commandline invocation:
		 * -DebugEnableInternalChecks -q -g naive -s default -sort -n 400 -i 3col-20-38.txt
		 */
		SystemConfig cfg = new SystemConfig();
		cfg.setGrounderName("naive");
		cfg.setSolverName("default");
		cfg.setNogoodStoreName("alpharoaming");
		cfg.setDebugInternalChecks(true);
		cfg.setSeed(1119718541727902L);
		final Alpha system = new AlphaImpl(cfg);

		final Path path = Paths.get("src", "test", "resources", "PreviouslyProblematic").resolve("3col-20-38.txt");
		InputConfig inputCfg = new InputConfig();
		List<String> files = new ArrayList<>();
		files.add(path.toString());
		inputCfg.setFiles(files);
		ASPCore2Program prog = system.readProgram(inputCfg);

		assertFalse(system.solve(prog).sorted().limit(400).collect(Collectors.toList()).isEmpty());
	}

	private void problematicRun(String program, long seed, int limit) throws IOException {
		final Path base = Paths.get("src", "test", "resources", "PreviouslyProblematic");
		SystemConfig cfg = new SystemConfig();
		cfg.setGrounderName("naive");
		cfg.setSolverName("default");
		cfg.setNogoodStoreName("alpharoaming");
		cfg.setDebugInternalChecks(true);
		cfg.setSeed(seed);
		final Alpha system = new AlphaImpl(cfg);
		InputConfig inputCfg = new InputConfig();
		List<String> files = new ArrayList<>();
		files.add(base.resolve(program).toString());
		inputCfg.setFiles(files);
		ASPCore2Program prog = system.readProgram(inputCfg);
		assertFalse(system.solve(prog).limit(limit).collect(Collectors.toList()).isEmpty());
	}
	
	@Test
	public void testLearnedUnaryNoGoodCausingOutOfOrderLiteralsConflict() throws IOException {
		final ProgramParser parser = new ProgramParserImpl();
		InputProgram.Builder bld = InputProgram.builder();
		bld.accumulate(parser.parse(Paths.get("src", "test", "resources", "HanoiTower_Alpha.asp")));
		bld.accumulate(parser.parse(Paths.get("src", "test", "resources", "HanoiTower_instances", "simple.asp")));
		InputProgram parsedProgram = bld.build();
		
		SystemConfig config = new SystemConfig();
		config.setSolverName("default");
		config.setNogoodStoreName("alpharoaming");
		config.setSeed(0);
		config.setBranchingHeuristic(Heuristic.valueOf("VSIDS"));
		config.setDebugInternalChecks(true);
		config.setDisableJustificationSearch(false);
		config.setEvaluateStratifiedPart(false);
		config.setReplayChoices(Arrays.asList(21, 26, 36, 56, 91, 96, 285, 166, 101, 290, 106, 451, 445, 439, 448,
			433, 427, 442, 421, 415, 436, 409, 430, 397, 391, 424, 385, 379,
			418, 373, 412, 406, 394, 388, 382, 245, 232, 208
		));
		Alpha alpha = new AlphaImpl(config);
		Optional<AnswerSet> answerSet = alpha.solve(parsedProgram).findFirst();
		assertTrue(answerSet.isPresent());
	}	
	
}
