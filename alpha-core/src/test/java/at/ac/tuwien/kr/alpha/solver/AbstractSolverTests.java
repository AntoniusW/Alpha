/*
 * Copyright (c) 2017-2020, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.InternalProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.config.SystemConfig;
import at.ac.tuwien.kr.alpha.core.grounder.Grounder;
import at.ac.tuwien.kr.alpha.core.grounder.GrounderFactory;
import at.ac.tuwien.kr.alpha.core.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.solver.heuristics.BranchingHeuristicFactory;
import at.ac.tuwien.kr.alpha.solver.heuristics.BranchingHeuristicFactory.Heuristic;
import at.ac.tuwien.kr.alpha.test.util.TestUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

@RunWith(Parameterized.class)
public abstract class AbstractSolverTests {

	private static final String[] NON_DEPRECATED_HEURISTICS_NAMES;
	static {
		final List<String> nonDeprecatedHeuristicsNames = new ArrayList<>();
		for (Field field : Heuristic.class.getFields()) {
			if (field.getAnnotation(Deprecated.class) == null) {
				nonDeprecatedHeuristicsNames.add(field.getName());
			}
		}
		NON_DEPRECATED_HEURISTICS_NAMES = nonDeprecatedHeuristicsNames.toArray(new String[]{});
	}

	private final ProgramParser parser = new ProgramParser();

	/**
	 * Sets the logging level to TRACE. Useful for debugging; call at beginning of test case.
	 */
	protected static void enableTracing() {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(ch.qos.logback.classic.Level.TRACE);
	}

	protected static void enableDebugLog() {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.DEBUG);
	}

	/**
	 * Calling this method in a test leads to the test being ignored for the naive solver.
	 * Note: use this sparingly and only on tests that require too much run time with the naive solver.
	 */
	void ignoreTestForNaiveSolver() {
		org.junit.Assume.assumeFalse(solverName.equals("naive"));
	}

	/**
	 * Calling this method in a test leads to the test being ignored for non-default domain-independent heuristics.
	 * Note: use this sparingly and only on tests that require too much run time with non-default heuristics
	 * (which are not tuned for good performance as well as VSIDS).
	 */
	void ignoreNonDefaultDomainIndependentHeuristics() {
		org.junit.Assume.assumeTrue(heuristic == BranchingHeuristicFactory.Heuristic.VSIDS);
	}

	private static String[] getProperty(String subKey, String def) {
		return System.getProperty("test." + subKey, def).split(",");
	}

	@Parameters(name = "{0}/{1}/{2}/{3}/seed={4}/checks={5}/gtc={6}/gtr={7}/dir={8}")
	public static Collection<Object[]> parameters() {
		// Check whether we are running in a CI environment.
		boolean ci = Boolean.valueOf(System.getenv("CI"));

		String[] solvers = getProperty("solvers", ci ? "default,naive" : "default");
		String[] grounders = getProperty("grounders", "naive");
		String[] stores = getProperty("stores", ci ? "alpharoaming,naive" : "alpharoaming");
		String[] heuristics = getProperty("heuristics", ci ? "NON_DEPRECATED" : "NAIVE,VSIDS");
		String[] gtcValues = getProperty("grounderToleranceConstraints", "strict,permissive");
		String[] gtrValues = getProperty("grounderToleranceRules", "strict");
		String[] dirValues = getProperty("disableInstanceRemoval", ci ? "false,true" : "false");

		// "ALL" is a magic value that will be expanded to contain all heuristics.
		if ("ALL".equals(heuristics[0])) {
			BranchingHeuristicFactory.Heuristic[] values = BranchingHeuristicFactory.Heuristic.values();
			heuristics = new String[values.length];
			int i = 0;
			for (BranchingHeuristicFactory.Heuristic heuristic : values) {
				heuristics[i++] = heuristic.toString();
			}
		}
		// "NON_DEPRECATED" is a magic value that will be expanded to contain all non-deprecated heuristics.
		if ("NON_DEPRECATED".equals(heuristics[0])) {
			heuristics = NON_DEPRECATED_HEURISTICS_NAMES;
		}

		// NOTE:
		// It is handy to set the seed for reproducing bugs. However, the reverse is also sometimes needed:
		// A test case fails, now what was the seed that "caused" it? To allow this, we need full control over
		// the seed, so we generate one in any case.
		// If your test case fails you can inspect the property called "seed" of AbstractSolverTests and extract
		// its value.
		String seedProperty = System.getProperty("seed", ci ? "0" : "");
		long seed = seedProperty.isEmpty() ? (new Random().nextLong()) : Long.valueOf(seedProperty);

		boolean checks = true;

		Collection<Object[]> factories = new ArrayList<>();

		for (String solver : solvers) {
			for (String grounder : grounders) {
				for (String store : stores) {
					for (String heuristic : heuristics) {
						for (String gtc : gtcValues) {
							for (String gtr : gtrValues) {
								for (String dir : dirValues) {
									factories.add(new Object[]{
											solver, grounder, store, BranchingHeuristicFactory.Heuristic.valueOf(heuristic), seed, checks, gtc, gtr, Boolean.valueOf(dir)
									});
								}
							}
						}
					}
				}
			}
		}

		return factories;
	}

	@Parameter(0)
	public String solverName;

	@Parameter(1)
	public String grounderName;

	@Parameter(2)
	public String storeName;

	@Parameter(3)
	public BranchingHeuristicFactory.Heuristic heuristic;

	@Parameter(4)
	public long seed;

	@Parameter(5)
	public boolean checks;

	@Parameter(6)
	public String grounderToleranceConstraints;

	@Parameter(7)
	public String grounderToleranceRules;

	@Parameter(8)
	public boolean disableInstanceRemoval;

	protected Solver getInstance(AtomStore atomStore, Grounder grounder) {
		return SolverFactory.getInstance(buildSystemConfig(), atomStore, grounder);
	}

	private SystemConfig buildSystemConfig() {
		SystemConfig config = new SystemConfig();
		config.setSolverName(solverName);
		config.setNogoodStoreName(storeName);
		config.setSeed(seed);
		config.setBranchingHeuristic(heuristic);
		config.setDebugInternalChecks(checks);
		config.setDisableJustificationSearch(false);
		return config;
	}

	protected Solver getInstance(InputProgram program) {
		Alpha system = new Alpha();
		AtomStore atomStore = new AtomStoreImpl();
		NormalProgram normalized = system.normalizeProgram(program);
		InternalProgram preprocessed = InternalProgram.fromNormalProgram(normalized);
		return getInstance(atomStore, GrounderFactory.getInstance(grounderName, preprocessed, atomStore, true));
	}

	protected Solver getInstance(String program) {
		return getInstance(CharStreams.fromString(program));
	}

	protected Solver getInstance(CharStream program) {
		try {
			return getInstance(parser.parse(program));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected Set<AnswerSet> collectSet(String program) {
		return getInstance(program).collectSet();
	}

	protected Set<AnswerSet> collectSet(InputProgram program) {
		return getInstance(program).collectSet();
	}

	protected Set<AnswerSet> collectSet(CharStream program) {
		return getInstance(program).collectSet();
	}

	protected void assertAnswerSets(String program, String... answerSets) {
		Set<AnswerSet> actualAnswerSets = collectSet(program);
		TestUtils.assertAnswerSetsEqual(answerSets, actualAnswerSets);
	}

	protected void assertAnswerSet(String program, String answerSet) {
		Set<AnswerSet> actualAnswerSets = collectSet(program);
		TestUtils.assertAnswerSetsEqual(answerSet, actualAnswerSets);
	}

	protected void assertAnswerSetsWithBase(String program, String base, String... answerSets) {
		Set<AnswerSet> actualAnswerSets = collectSet(program);
		TestUtils.assertAnswerSetsEqualWithBase(base, answerSets, actualAnswerSets);
	}

	protected void assertAnswerSets(String program, Set<AnswerSet> answerSets) {
		Set<AnswerSet> actualAnswerSets = collectSet(program);
		TestUtils.assertAnswerSetsEqual(answerSets, actualAnswerSets);
	}
}
