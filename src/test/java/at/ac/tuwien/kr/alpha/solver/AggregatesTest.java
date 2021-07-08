/**
 * Copyright (c) 2018-2019 Siemens AG
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
package at.ac.tuwien.kr.alpha.solver;

import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.InternalProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.config.SystemConfig;
import at.ac.tuwien.kr.alpha.grounder.GrounderFactory;
import at.ac.tuwien.kr.alpha.grounder.heuristics.GrounderHeuristicsConfiguration;

/**
 * Tests if correct answer sets for programs containing aggregates are computed.
 */
public abstract class AggregatesTest extends AbstractSolverTests {

	private static final String LS = System.lineSeparator();

	@Test
	public void aggregateCountLeGroundPositive() {
		String program = "a." + LS
				+ "b :- 1 <= #count { 1 : a }.";
		assertAnswerSet(program, "a,b");
	}

	@Test
	public void aggregateCountEqSingleElementPositive() {
		String program = "thing(1..3)."
				+ "cnt_things(N) :- N = #count{X : thing(X)}.";
		assertAnswerSet(program, "thing(1), thing(2), thing(3), cnt_things(3)");
	}

	@Test
	public void aggregateCountEqEmptySetPositive() {
		String program = "cnt_things(N) :- N = #count{X : thing(X)}.";
		assertAnswerSet(program, "cnt_things(0)");
	}

	@Test
	public void aggregateCountLeEmptySetPositive() {
		String program = "zero_leq_cnt :- 0 <= #count{X : thing(X)}.";
		assertAnswerSet(program, "zero_leq_cnt");
	}

	@Test
	public void aggregateSumEqEmptySetPositive() {
		String program = "sum_things(S) :- S = #sum{X : thing(X)}.";
		assertAnswerSet(program, "sum_things(0)");
	}

	@Test
	public void aggregateSumLeEmptySetPositive() {
		String program = "zero_leq_sum :- 0 <= #sum{X : thing(X)}.";
		assertAnswerSet(program, "zero_leq_sum");
	}

	@Test
	public void aggregateCountLeWithChoicePositive() {
		String program = "potential_thing(1..4). "
				+ "{ thing(N) : potential_thing(N)}."
				+ "two_things_chosen :- thing(N1), thing(N2), N1 != N2."
				+ "three_things_chosen :- thing(N1), thing(N2), thing(N3), N1 != N2, N1 != N3, N2 != N3."
				+ ":- not two_things_chosen."
				+ ":- three_things_chosen."
				+ "two_leq_cnt :- 2 <= #count{ X : thing(X) }.";
		assertAnswerSetsWithBase(program,
				"potential_thing(1), potential_thing(2), potential_thing(3), potential_thing(4), two_things_chosen, two_leq_cnt",
				"thing(1), thing(2)",
				"thing(1), thing(3)",
				"thing(1), thing(4)",
				"thing(2), thing(3)",
				"thing(2), thing(4)",
				"thing(3), thing(4)");
	}

	@Test
	public void aggregateCountEqWithChoicePositive() {
		String program = "potential_thing(1..4). "
				+ "{ thing(N) : potential_thing(N)}."
				+ "two_things_chosen :- thing(N1), thing(N2), N1 != N2."
				+ "three_things_chosen :- thing(N1), thing(N2), thing(N3), N1 != N2, N1 != N3, N2 != N3."
				+ ":- not two_things_chosen."
				+ ":- three_things_chosen."
				+ "cnt_things(CNT) :- CNT = #count{ X : thing(X) }.";
		assertAnswerSetsWithBase(program,
				"potential_thing(1), potential_thing(2), potential_thing(3), potential_thing(4), two_things_chosen, cnt_things(2)",
				"thing(1), thing(2)",
				"thing(1), thing(3)",
				"thing(1), thing(4)",
				"thing(2), thing(3)",
				"thing(2), thing(4)",
				"thing(3), thing(4)");
	}

	@Test
	public void aggregateSumEqWithChoicePositive() {
		String program = "potential_thing(1..4). "
				+ "{ thing(N) : potential_thing(N)}."
				+ "two_things_chosen :- thing(N1), thing(N2), N1 != N2."
				+ "three_things_chosen :- thing(N1), thing(N2), thing(N3), N1 != N2, N1 != N3, N2 != N3."
				+ ":- not two_things_chosen."
				+ ":- three_things_chosen."
				+ "sum_things(SUM) :- SUM = #sum{ X : thing(X) }.";
		assertAnswerSetsWithBase(program,
				"potential_thing(1), potential_thing(2), potential_thing(3), potential_thing(4), two_things_chosen",
				"thing(1), thing(2), sum_things(3)",
				"thing(1), thing(3), sum_things(4)",
				"thing(1), thing(4), sum_things(5)",
				"thing(2), thing(3), sum_things(5)",
				"thing(2), thing(4), sum_things(6)",
				"thing(3), thing(4), sum_things(7)");
	}

	@Test
	public void aggregateSumBetweenNegative() {
		String program = "potential_thing(1..4). "
				+ "{ thing(N) : potential_thing(N)}."
				+ "two_things_chosen :- thing(N1), thing(N2), N1 != N2."
				+ "three_things_chosen :- thing(N1), thing(N2), thing(N3), N1 != N2, N1 != N3, N2 != N3."
				+ ":- not two_things_chosen."
				+ ":- three_things_chosen."
				+ "sum_not_five :- not 4 < #sum{ X : thing(X) } < 6."
				+ ":- not sum_not_five."
				+ "sum_things(SUM) :- SUM = #sum{ X : thing(X) }.";
		assertAnswerSetsWithBase(program,
				"potential_thing(1), potential_thing(2), potential_thing(3), potential_thing(4), two_things_chosen, sum_not_five",
				"thing(1), thing(2), sum_things(3)",
				"thing(1), thing(3), sum_things(4)",
				"thing(2), thing(4), sum_things(6)",
				"thing(3), thing(4), sum_things(7)");
	}

	@Test
	public void aggregateMaxNegative() {
		String program = "potential_thing(1..4). "
				+ "{ thing(N) : potential_thing(N) }."
				+ "one_chosen :- thing(_)."
				+ ":- not one_chosen."
				+ ":- thing(N1), thing(N2), N1 != N2."
				+ "max_chosen :- thing(X), not X < #max{M : potential_thing(M)}."
				+ ":- not max_chosen.";
		assertAnswerSet(program,
				"potential_thing(1), potential_thing(2), potential_thing(3), potential_thing(4), one_chosen, max_chosen, thing(4)");
	}

	@Test
	public void aggregateCountGroundNegative() {
		String program = "{a}." + LS
				+ "b :- not c." + LS
				+ "c :- 1 <= #count { 1 : a }.";
		assertAnswerSets(program, "a,c", "b");
	}

	@Test
	public void aggregateCountNonGroundPositive() {
		String program = "n(1..3)." + LS
				+ "{x(N)} :- n(N)." + LS
				+ "min(3)." + LS
				+ "ok :- min(M), M <= #count { N : n(N), x(N) }.";
		assertAnswerSetsWithBase(program, "n(1), n(2), n(3), min(3)",
				"", "x(1)", "x(2)", "x(3)", "x(1), x(2)", "x(1), x(3)",
				"x(2), x(3)", "x(1), x(2), x(3), ok");
	}

	@Test
	public void aggregateCountNonGroundLowerAndUpper() {
		String program = "n(1..3)." + LS
				+ "{x(N)} :- n(N)." + LS
				+ "min(2)." + LS
				+ "max(2)." + LS
				+ "ok :- min(M), M <= #count { N : n(N), x(N) }, not exceedsMax." + LS
				+ "exceedsMax :- max(M), M1 = M + 1, M1 <= #count { N : n(N), x(N) }.";
		System.out.println(program);
		assertAnswerSetsWithBase(program, "n(1), n(2), n(3), min(2), max(2)",
				"", "x(1)", "x(2)", "x(3)", "x(1), x(2), ok", "x(1), x(3), ok",
				"x(2), x(3), ok", "x(1), x(2), x(3), exceedsMax");
	}

	@Test
	public void aggregateSumGroundLower() {
		String program = "a." + LS
				+ "b :- 5 <= #sum { 2 : a; 3 }.";
		assertAnswerSet(program, "a,b");
	}

	@Test
	public void aggregateSumNonGroundLowerAndUpper() {
		String program = "n(1..3)." + LS
				+ "{x(N)} :- n(N)." + LS
				+ "min(3)." + LS
				+ "max(4)." + LS
				+ "ok :- min(M), M <= #sum { N : n(N), x(N) }, not exceedsMax." + LS
				+ "exceedsMax :- max(M), M1 = M + 1, M1 <= #sum { N : n(N), x(N) }.";
		System.out.println(program);
		assertAnswerSetsWithBase(program, "n(1), n(2), n(3), min(3), max(4)",
				"", "x(1)", "x(2)", "x(3), ok", "x(1), x(2), ok", "x(1), x(3), ok",
				"x(2), x(3), exceedsMax", "x(1), x(2), x(3), exceedsMax");
	}

	@Test
	public void aggregateSumNonGroundLower() {
		String program = "n(1..3)." + LS
				+ "{x(N)} :- n(N)." + LS
				+ "min(3)." + LS
				+ "ok :- min(M), M <= #sum { N : n(N), x(N) }.";
		System.out.println(program);
		assertAnswerSetsWithBase(program, "n(1), n(2), n(3), min(3)",
				"", "x(1)", "x(2)", "x(3), ok", "x(1), x(2), ok", "x(1), x(3), ok",
				"x(2), x(3), ok", "x(1), x(2), x(3), ok");
	}

	@Test
	public void aggregateSumComputed() {
		ignoreTestForNaiveSolver(); // Do not run this test case with the naive solver.
		String program = "n(1..3)." + LS
				+ "{x(N)} :- n(N)." + LS
				+ "potential_sum(0..6)." + LS
				+ "min(S) :- S <= #sum { N : n(N), x(N) }, potential_sum(S)." + LS
				+ "sum(S) :- min(S), not min(Sp1), Sp1 = S+1.";
		assertAnswerSetsWithBase(program, "n(1), n(2), n(3), potential_sum(0), potential_sum(1), "
				+ "potential_sum(2), potential_sum(3), potential_sum(4), potential_sum(5), potential_sum(6)",
				"min(0), sum(0)",
				"x(1), min(0), min(1), sum(1)",
				"x(2), min(0), min(1), min(2), sum(2)",
				"x(3), min(0), min(1), min(2), min(3), sum(3)",
				"x(1), x(2), min(0), min(1), min(2), min(3), sum(3)",
				"x(1), x(3), min(0), min(1), min(2), min(3), min(4), sum(4)",
				"x(2), x(3), min(0), min(1), min(2), min(3), min(4), min(5), sum(5)",
				"x(1), x(2), x(3), min(0), min(1), min(2), min(3), min(4), min(5), min(6), sum(6)");
	}

	@Test
	public void aggregateCountGlobalVariable() {
		String program = "box(1..2)." + LS
				+ "in(1,1)." + LS
				+ "in(1,2)." + LS
				+ "in(2,2)." + LS
				+ "full(B) :- box(B), 2 <= #count { I : in(I,B) }.";
		assertAnswerSetsWithBase(program, "box(1), box(2), in(1,1), in(1,2), in(2,2)",
				"full(2)");
	}

	@Test
	public void aggregateSumGlobalVariable() {
		String program = "box(1..2)." + LS
				+ "item_size(I,I) :- I=1..2." + LS
				+ "in(1,1)." + LS
				+ "in(1,2)." + LS
				+ "in(2,2)." + LS
				+ "full(B) :- box(B), 3 <= #sum { S : item_size(I,S), in(I,B) }.";
		assertAnswerSetsWithBase(program, "box(1), box(2), item_size(1,1), item_size(2,2), in(1,1), in(1,2), in(2,2)",
				"full(2)");
	}

	@Override
	protected Solver getInstance(InputProgram program) {
		SystemConfig config = this.buildSystemConfig();
		config.setUseNormalizationGrid(useCountingGridNormalization());
		Alpha system = new Alpha(config);
		AtomStore atomStore = new AtomStoreImpl();
		NormalProgram normal = system.normalizeProgram(program);
		InternalProgram preprocessed = InternalProgram.fromNormalProgram(normal);
		return super.getInstance(atomStore,
				GrounderFactory.getInstance(grounderName, preprocessed, atomStore, p -> true, new GrounderHeuristicsConfiguration(), true));
	}

	protected abstract boolean useCountingGridNormalization();

}
