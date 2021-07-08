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
package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.AtomStoreTest;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.NoGoodCreator;
import at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static at.ac.tuwien.kr.alpha.common.NoGoodTest.fromOldLiterals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LearnedNoGoodDeletionTest {

	private NoGoodStoreAlphaRoaming store;
	private LearnedNoGoodDeletion learnedNoGoodDeletion;

	public LearnedNoGoodDeletionTest() {
		AtomStore atomStore = new AtomStoreImpl();
		AtomStoreTest.fillAtomStore(atomStore, 200);
		WritableAssignment assignment = new TrailAssignment(atomStore);
		assignment.growForMaxAtomId();
		store = new NoGoodStoreAlphaRoaming(assignment);
		learnedNoGoodDeletion = store.getLearnedNoGoodDeletion();
	}

	@Before
	public void setUp() {
		store.clear();
		store.growForMaxAtomId(fromOldLiterals(200));
		assertNull(store.add(1, new NoGood(fromOldLiterals(1, -2))));
		assertNull(store.add(2, new NoGood(fromOldLiterals(2, -3))));
		assertNull(store.add(3, new NoGood(fromOldLiterals(2, -4, -5))));
		assertNull(store.add(4, new NoGood(fromOldLiterals(3, 4, 5))));
		assertNull(store.propagate());
	}

	@Test
	public void testDeletionRunningAfterConflicts() {
		for (int i = 0; i < LearnedNoGoodDeletion.RUN_AFTER_AT_LEAST; i++) {
			learnedNoGoodDeletion.increaseConflictCounter();
		}
		assertFalse(learnedNoGoodDeletion.needToRunNoGoodDeletion());
		learnedNoGoodDeletion.increaseConflictCounter();
		assertTrue(learnedNoGoodDeletion.needToRunNoGoodDeletion());
		learnedNoGoodDeletion.runNoGoodDeletion();
		assertFalse(learnedNoGoodDeletion.needToRunNoGoodDeletion());
	}

	@Test
	public void testDeletionRemovingWatches() {
		for (int i = 0; i < LearnedNoGoodDeletion.RUN_AFTER_AT_LEAST; i++) {
			learnedNoGoodDeletion.increaseConflictCounter();
		}
		assertFalse(learnedNoGoodDeletion.needToRunNoGoodDeletion());
		learnedNoGoodDeletion.increaseConflictCounter();
		assertTrue(learnedNoGoodDeletion.needToRunNoGoodDeletion());
		assertNull(store.add(4, NoGoodCreator.learnt(fromOldLiterals(10, 11, 12)), 3));
		assertNull(store.add(5, NoGoodCreator.learnt(fromOldLiterals(10, -13, -14)), 4));
		List<WatchedNoGood> watchedNoGoods = learnedNoGoodDeletion.inspectLearnedNoGoods();
		assertTrue(watchedNoGoods.size() >= 2);
		WatchedNoGood watchedNoGood = watchedNoGoods.get(0);
		for (int i = 0; i < 10; i++) {
			watchedNoGood.bumpActivity();
		}
		learnedNoGoodDeletion.runNoGoodDeletion();
		watchedNoGoods = learnedNoGoodDeletion.inspectLearnedNoGoods();
		assertTrue(watchedNoGoods.size() < 2);
	}

	@Test
	public void testDeletionIncreasesDeletionCounter() {
		for (int i = 0; i < LearnedNoGoodDeletion.RUN_AFTER_AT_LEAST; i++) {
			learnedNoGoodDeletion.increaseConflictCounter();
		}
		assertFalse(learnedNoGoodDeletion.needToRunNoGoodDeletion());
		learnedNoGoodDeletion.increaseConflictCounter();
		assertTrue(learnedNoGoodDeletion.needToRunNoGoodDeletion());
		assertNull(store.add(4, NoGoodCreator.learnt(fromOldLiterals(10, 11, 12)), 3));
		assertNull(store.add(5, NoGoodCreator.learnt(fromOldLiterals(10, -13, -14)), 4));
		assertEquals(0, learnedNoGoodDeletion.getNumberOfDeletedNoGoods());
		learnedNoGoodDeletion.runNoGoodDeletion();
		assertTrue(learnedNoGoodDeletion.getNumberOfDeletedNoGoods() > 0);
	}

	@Test
	public void testDeletionReducesNumberOfLearntNoGoods() {
		for (int i = 0; i < LearnedNoGoodDeletion.RUN_AFTER_AT_LEAST; i++) {
			learnedNoGoodDeletion.increaseConflictCounter();
		}
		assertFalse(learnedNoGoodDeletion.needToRunNoGoodDeletion());
		learnedNoGoodDeletion.increaseConflictCounter();
		assertTrue(learnedNoGoodDeletion.needToRunNoGoodDeletion());
		assertNull(store.add(4, NoGoodCreator.learnt(fromOldLiterals(10, 11, 12)), 3));
		assertNull(store.add(5, NoGoodCreator.learnt(fromOldLiterals(10, -13, -14)), 4));

		final Map<Type, Integer> countersBeforeDeletion = countNoGoodsByType(store);
		learnedNoGoodDeletion.runNoGoodDeletion();
		final Map<Type, Integer> countersAfterDeletion = countNoGoodsByType(store);

		for (Type type : Type.values()) {
			if (type == Type.LEARNT) {
				assertTrue("Count of LEARNT nogoods did not decrease during deletion", countersBeforeDeletion.get(type) > countersAfterDeletion.get(type));
			} else {
				assertEquals("Unexpected count of " + type + " nogoods", countersBeforeDeletion.get(type), countersAfterDeletion.get(type));
			}
		}

	}

	private Map<Type, Integer> countNoGoodsByType(NoGoodStore store) {
		final Map<Type, Integer> counters = new HashMap<>();
		final NoGoodCounter noGoodCounter = store.getNoGoodCounter();
		for (Type type : Type.values()) {
			counters.put(type, noGoodCounter.getNumberOfNoGoods(type));
		}
		return counters;
	}
}