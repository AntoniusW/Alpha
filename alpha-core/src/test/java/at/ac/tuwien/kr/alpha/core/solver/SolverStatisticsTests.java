/**
 * Copyright (c) 2017-2019 Siemens AG
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.Solver;
import at.ac.tuwien.kr.alpha.api.StatisticsReportingSolver;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.grounder.DummyGrounder;

public class SolverStatisticsTests extends AbstractSolverTests {

	private AtomStore atomStore;

	@Before
	public void setUp() {
		this.atomStore = new AtomStoreImpl();
	}

	@Test
	public void checkStatsStringZeroChoices() {
		Solver solver = getInstance("a.");
		assumeTrue(solver instanceof StatisticsReportingSolver);
		collectAnswerSetsAndCheckStats(solver, 1, 0, 0, 0, 0, 0, 0, 0);
	}

	@Test
	public void checkStatsStringOneChoice() {
		Solver solver = getInstance("a :- not b. b :- not a.");
		assumeTrue(solver instanceof StatisticsReportingSolver);
		collectAnswerSetsAndCheckStats(solver, 2, 1, 1, 1, 1, 0, 0, 0);
	}

	@Test
	public void checkNoGoodCounterStatsByTypeUsingDummyGrounder() {
		Solver solver = getInstance(atomStore, new DummyGrounder(atomStore));
		assumeTrue(solver instanceof StatisticsReportingSolver);
		collectAnswerSetsAndCheckNoGoodCounterStatsByType(solver, 4, 0, 0, 0);
	}

	@Test
	public void checkNoGoodCounterStatsByCardinalityUsingDummyGrounder() {
		Solver solver = getInstance(atomStore, new DummyGrounder(atomStore));
		assumeTrue(solver instanceof StatisticsReportingSolver);
		collectAnswerSetsAndCheckNoGoodCounterStatsByCardinality(solver, 2, 1, 1);
	}

	private void collectAnswerSetsAndCheckStats(Solver solver, int expectedNumberOfAnswerSets, int expectedNumberOfGuesses, int expectedTotalNumberOfBacktracks,
			int expectedNumberOfBacktracksWithinBackjumps, int expectedNumberOfBackjumps, int expectedNumberOfMBTs, int expectedNumberOfConflictsAfterClosing, int expectedNumberOfDeletedNoGoods) {
		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expectedNumberOfAnswerSets, answerSets.size());
		StatisticsReportingSolver solverMaintainingStatistics = (StatisticsReportingSolver) solver;
		assertEquals(
				String.format("g=%d, bt=%d, bj=%d, bt_within_bj=%d, mbt=%d, cac=%d, del_ng=%d", expectedNumberOfGuesses, expectedTotalNumberOfBacktracks, expectedNumberOfBackjumps,
						expectedNumberOfBacktracksWithinBackjumps, expectedNumberOfMBTs, expectedNumberOfConflictsAfterClosing, expectedNumberOfDeletedNoGoods),
				solverMaintainingStatistics.getStatisticsString());
	}

	private void collectAnswerSetsAndCheckNoGoodCounterStatsByType(Solver solver, int expectedNumberOfStaticNoGoods, int expectedNumberOfSupportNoGoods, int expectedNumberOfLearntNoGoods, int expectedNumberOfInternalNoGoods) {
		solver.collectSet();
		// Note: This cast is kinda hacky since it assumes a specific implementation rather than an interface.
		// To be perfectly clean, we need to introduce an interface in core module that extends StatisticsReportingSolver with getNoGoodCounter()
		DefaultSolver solverMaintainingStatistics = (DefaultSolver) solver;
		final NoGoodCounter noGoodCounter =  solverMaintainingStatistics.getNoGoodCounter();
		assertEquals("STATIC: " + expectedNumberOfStaticNoGoods + " SUPPORT: " + expectedNumberOfSupportNoGoods + " LEARNT: " + expectedNumberOfLearntNoGoods + " INTERNAL: " + expectedNumberOfInternalNoGoods, noGoodCounter.getStatsByType());
	}

	private void collectAnswerSetsAndCheckNoGoodCounterStatsByCardinality(Solver solver, int expectedNumberOfUnaryNoGoods, int expectedNumberOfBinaryNoGoods, int expectedNumberOfNAryNoGoods) {
		solver.collectSet();
		// Note: This cast is kinda hacky since it assumes a specific implementation rather than an interface.
		// To be perfectly clean, we need to introduce an interface in core module that extends StatisticsReportingSolver with getNoGoodCounter()
		DefaultSolver solverMaintainingStatistics = (DefaultSolver) solver;
		final NoGoodCounter noGoodCounter =  solverMaintainingStatistics.getNoGoodCounter();
		assertEquals("unary: " + expectedNumberOfUnaryNoGoods + " binary: " + expectedNumberOfBinaryNoGoods + " larger: " + expectedNumberOfNAryNoGoods, noGoodCounter.getStatsByCardinality());
	}

}
