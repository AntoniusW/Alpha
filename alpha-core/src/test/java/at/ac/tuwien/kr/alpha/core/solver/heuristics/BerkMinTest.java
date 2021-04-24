/**
 * Copyright (c) 2016-2018 Siemens AG
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

import static at.ac.tuwien.kr.alpha.common.NoGoodTest.fromOldLiterals;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.common.AtomStoreTest;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.solver.NaiveNoGoodStore;
import at.ac.tuwien.kr.alpha.core.solver.TrailAssignment;
import at.ac.tuwien.kr.alpha.core.solver.WritableAssignment;
import at.ac.tuwien.kr.alpha.core.solver.learning.GroundConflictNoGoodLearner.ConflictAnalysisResult;

/**
 * Tests {@link BerkMin}.
 * 
 * Copyright (c) 2016 Siemens AG
 *
 */
public class BerkMinTest {
	
	/**
	 * The tolerable epsilon for double comparisons
	 */
	private static final double DOUBLE_COMPARISON_EPSILON = 0.000001;
	
	private BerkMin berkmin;
	
	@Before
	public void setUp() {
		AtomStore atomStore = new AtomStoreImpl();
		AtomStoreTest.fillAtomStore(atomStore, 2);
		WritableAssignment assignment = new TrailAssignment(atomStore);
		assignment.growForMaxAtomId();
		this.berkmin = new BerkMin(
			assignment,
			new PseudoChoiceManager(assignment, new NaiveNoGoodStore(assignment)),
			new Random()
		);
	}
	
	@Test
	public void countPositiveLiteralsOnce() {
		NoGood violatedNoGood = new NoGood(fromOldLiterals(1, 2));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(1, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(1, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
	}
	
	@Test
	public void countNegativeLiteralsOnce() {
		NoGood violatedNoGood = new NoGood(fromOldLiterals(-1, -2));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(1, berkmin.getActivity(fromOldLiterals(-1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(1, berkmin.getActivity(fromOldLiterals(-2)), DOUBLE_COMPARISON_EPSILON);
	}
	
	@Test
	public void countPositiveLiteralsTwice() {
		NoGood violatedNoGood = new NoGood(fromOldLiterals(1, 2));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(2, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(2, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
	}
	
	@Test
	public void countNegativeLiteralsTwice() {
		NoGood violatedNoGood = new NoGood(fromOldLiterals(-1, -2));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(2, berkmin.getActivity(fromOldLiterals(-1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(2, berkmin.getActivity(fromOldLiterals(-2)), DOUBLE_COMPARISON_EPSILON);
	}
	
	@Test
	public void countMixedLiteralsTwice() {
		NoGood violatedNoGood = new NoGood(fromOldLiterals(1, -2));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(2, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(2, berkmin.getActivity(fromOldLiterals(-2)), DOUBLE_COMPARISON_EPSILON);
	}
	
	@Test
	public void countPositiveLiteralsThenNegativeLiterals() {
		NoGood violatedNoGood = new NoGood(fromOldLiterals(1, 2));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		violatedNoGood = new NoGood(fromOldLiterals(-1, -2));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(2, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(2, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
	}
	
	@Test
	public void reachDecayPeriodOnce() {
		berkmin.setDecayPeriod(3);
		berkmin.setDecayFactor(1.0 / 3);
		NoGood violatedNoGood = new NoGood(fromOldLiterals(1, 2));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(1, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(1, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(2, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(2, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(1, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(1, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
	}
	
	@Test
	public void reachDecayPeriodTwice() {
		berkmin.setDecayPeriod(2);
		berkmin.setDecayFactor(0.75);
		NoGood violatedNoGood = new NoGood(fromOldLiterals(1, 2));
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(1, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(1, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(1.5, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(1.5, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(2.5, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(2.5, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
		berkmin.violatedNoGood(violatedNoGood);
		berkmin.analyzedConflict(pseudo(violatedNoGood));
		assertEquals(2.625, berkmin.getActivity(fromOldLiterals(1)), DOUBLE_COMPARISON_EPSILON);
		assertEquals(2.625, berkmin.getActivity(fromOldLiterals(2)), DOUBLE_COMPARISON_EPSILON);
	}
	
	@Test
	public void learnNoGood() {
		NoGood learnedNoGood = NoGood.learnt(fromOldLiterals(1, 2));
		int backjumpLevel = 1;
		boolean clearLastChoiceAfterBackjump = true;
		Collection<Integer> resolutionAtoms = Collections.emptySet();
		berkmin.analyzedConflict(new ConflictAnalysisResult(learnedNoGood, backjumpLevel,
			resolutionAtoms));
		assertEquals(learnedNoGood, berkmin.getCurrentTopClause());
	}

	private static ConflictAnalysisResult pseudo(NoGood noGood) {
		return new ConflictAnalysisResult(noGood, 0, Collections.emptySet());
	}
}
