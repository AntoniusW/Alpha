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

import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.core.atoms.Literals;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.core.solver.NoGoodStoreAlphaRoaming;
import at.ac.tuwien.kr.alpha.core.solver.TrailAssignment;
import at.ac.tuwien.kr.alpha.core.solver.WritableAssignment;

/**
 * Tests {@link HeapOfActiveAtoms}, including initial heuristic scores computed by {@link MOMs}.
 *
 */
public class HeapOfActiveAtomsTest {

	private static final double DOUBLE_COMPARISON_EPSILON = 0.001;

	private AtomStore atomStore;
	private WritableAssignment assignment;
	private VSIDS vsids;
	private NoGoodStoreAlphaRoaming noGoodStore;

	@Before
	public void setUp() {
		atomStore = new AtomStoreImpl();
		assignment = new TrailAssignment(atomStore);
		noGoodStore = new NoGoodStoreAlphaRoaming(assignment);
		ChoiceManager choiceManager = new PseudoChoiceManager(assignment, noGoodStore);
		this.vsids = new VSIDS(assignment, choiceManager, MOMs.DEFAULT_STRATEGY);
	}

	@Test
	public void testAllAtomsEqualScore() {
		int lit1 = Literals.atomToLiteral(1);
		int lit2 = Literals.atomToLiteral(2);
		int lit3 = Literals.atomToLiteral(3);
		HeuristicTestUtils.addNoGoods(atomStore, assignment, noGoodStore, vsids, new NoGood(lit1, lit2), new NoGood(lit2, lit3),
				new NoGood(lit1, lit3));
		double activity1 = vsids.heapOfActiveAtoms.getActivity(lit1);
		double activity2 = vsids.heapOfActiveAtoms.getActivity(lit2);
		double activity3 = vsids.heapOfActiveAtoms.getActivity(lit3);
		assertEquals(activity1, activity2, DOUBLE_COMPARISON_EPSILON);
		assertEquals(activity2, activity3, DOUBLE_COMPARISON_EPSILON);
	}

	@Test
	public void testOneAtomHigherScore() {
		int lit1 = Literals.atomToLiteral(1);
		int lit2 = Literals.atomToLiteral(2);
		int lit3 = Literals.atomToLiteral(3);
		int lit1Neg = Literals.atomToLiteral(1, false);
		int lit2Neg = Literals.atomToLiteral(2, false);
		int lit3Neg = Literals.atomToLiteral(3, false);
		HeuristicTestUtils.addNoGoods(atomStore, assignment, noGoodStore, vsids, new NoGood(lit1, lit2Neg), new NoGood(lit1Neg, lit2),
				new NoGood(lit2, lit3Neg), new NoGood(lit2Neg, lit3));
		double activity1 = vsids.heapOfActiveAtoms.getActivity(lit1);
		double activity2 = vsids.heapOfActiveAtoms.getActivity(lit2);
		double activity3 = vsids.heapOfActiveAtoms.getActivity(lit3);
		assertLessThan(activity1, activity2);
		assertLessThan(activity3, activity2);
	}

	private static void assertLessThan(double d1, double d2) {
		assertTrue(d1 < d2 + DOUBLE_COMPARISON_EPSILON);
	}

}
