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
package at.ac.tuwien.kr.alpha.core.solver.heuristics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.common.AtomStoreTest;
import at.ac.tuwien.kr.alpha.core.atoms.Literals;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.solver.NoGoodStoreAlphaRoaming;
import at.ac.tuwien.kr.alpha.core.solver.TrailAssignment;
import at.ac.tuwien.kr.alpha.core.solver.WritableAssignment;
import at.ac.tuwien.kr.alpha.core.solver.learning.GroundConflictNoGoodLearner.ConflictAnalysisResult;

/**
 * Tests {@link VSIDS}.
 *
 */
public class VSIDSTest {

	/**
	 * The tolerable epsilon for double comparisons
	 */
	private static final double DOUBLE_COMPARISON_EPSILON = 0.000001;

	private VSIDS vsids;
	private AtomStore atomStore;
	private WritableAssignment assignment;
	private NoGoodStoreAlphaRoaming noGoodStore;

	private int lit1Neg = Literals.atomToLiteral(1, false);
	private int lit2Neg = Literals.atomToLiteral(2, false);
	private int lit3Neg = Literals.atomToLiteral(3, false);
	private int lit4Neg = Literals.atomToLiteral(4, false);
	private int lit1 = Literals.atomToLiteral(1);
	private int lit2 = Literals.atomToLiteral(2);
	private int lit3 = Literals.atomToLiteral(3);
	private int lit4 = Literals.atomToLiteral(4);

	@Before
	public void setUp() {
		atomStore = new AtomStoreImpl();
		AtomStoreTest.fillAtomStore(atomStore, 4);
		assignment = new TrailAssignment(atomStore);
		assignment.growForMaxAtomId();
		noGoodStore = new NoGoodStoreAlphaRoaming(assignment);
		this.vsids = new VSIDS(
				assignment,
				new PseudoChoiceManager(assignment, noGoodStore),
				null);
	}

	/**
	 * Tests the following artificial situation:
	 * Static nogoods: { {-1, -2}, {-3, -4}}
	 * Learnt nogood: { 2, 3 }
	 * Atoms resolved out during conflict analysis: { 1 }
	 */
	@Test
	public void testConflict() {
		HeuristicTestUtils.addNoGoods(atomStore, assignment, noGoodStore, vsids, new NoGood(lit1Neg, lit2Neg), new NoGood(lit3Neg, lit4Neg));
		vsids.chooseLiteral(); // to make VSIDS ingest buffered nogoods
		assertEquals(vsids.getActivity(lit1), vsids.getActivity(lit2), DOUBLE_COMPARISON_EPSILON);
		assertEquals(vsids.getActivity(lit2), vsids.getActivity(lit3), DOUBLE_COMPARISON_EPSILON);
		assertEquals(vsids.getActivity(lit3), vsids.getActivity(lit4), DOUBLE_COMPARISON_EPSILON);

		NoGood learnedNoGood = new NoGood(lit2, lit3);
		Collection<Integer> resolutionAtoms = Arrays.asList(1);
		ConflictAnalysisResult analysisResult = new ConflictAnalysisResult(learnedNoGood, 1, resolutionAtoms);
		vsids.analyzedConflict(analysisResult);
		assertEquals(vsids.getActivity(lit1), vsids.getActivity(lit2), DOUBLE_COMPARISON_EPSILON);
		assertEquals(vsids.getActivity(lit2), vsids.getActivity(lit3), DOUBLE_COMPARISON_EPSILON);
		assertLessThan(vsids.getActivity(lit4), vsids.getActivity(lit3));

		assertEquals(0, vsids.getSignBalance(1));
		assertEquals(1, vsids.getSignBalance(2));
		assertEquals(1, vsids.getSignBalance(3));
		assertEquals(0, vsids.getSignBalance(4));
	}

	/**
	 * First, calls {@link #testConflict()}.
	 * Then, simulates a second conflict, learning { -1, 4 } with resolved atoms { 2 }.
	 * Due to decay, scores of atoms { 1, 4, 2 } must increase by the increased activity delta of {@link VSIDS#DEFAULT_DECAY_FACTOR}.
	 * 
	 */
	@Test
	public void testTwoConflicts() {
		testConflict();

		double activity1 = vsids.getActivity(lit1);
		double activity2 = vsids.getActivity(lit2);
		double activity3 = vsids.getActivity(lit3);
		double activity4 = vsids.getActivity(lit4);

		NoGood learnedNoGood = new NoGood(lit1Neg, lit4);
		Collection<Integer> resolutionAtoms = Arrays.asList(2);
		ConflictAnalysisResult analysisResult = new ConflictAnalysisResult(learnedNoGood, 1, resolutionAtoms);
		vsids.analyzedConflict(analysisResult);
		assertEquals(activity1 + VSIDS.DEFAULT_DECAY_FACTOR, vsids.getActivity(lit1), DOUBLE_COMPARISON_EPSILON);
		assertEquals(activity2 + VSIDS.DEFAULT_DECAY_FACTOR, vsids.getActivity(lit2), DOUBLE_COMPARISON_EPSILON);
		assertEquals(activity3, vsids.getActivity(lit3), DOUBLE_COMPARISON_EPSILON);
		assertEquals(activity4 + VSIDS.DEFAULT_DECAY_FACTOR, vsids.getActivity(lit4), DOUBLE_COMPARISON_EPSILON);

		assertEquals(-1, vsids.getSignBalance(1));
		assertEquals(1, vsids.getSignBalance(2));
		assertEquals(1, vsids.getSignBalance(3));
		assertEquals(1, vsids.getSignBalance(4));
	}

	private static void assertLessThan(double d1, double d2) {
		assertTrue(d1 < d2 + DOUBLE_COMPARISON_EPSILON);
	}
}
