/*
 * Copyright (c) 2018-2021 Siemens AG
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
package at.ac.tuwien.kr.alpha.solver.heuristics.domspec;

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import at.ac.tuwien.kr.alpha.solver.*;
import at.ac.tuwien.kr.alpha.solver.InfluenceManager.ActivityListener;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Test {@link DefaultDomainSpecificHeuristicsStore}
 */
public class DefaultDomainSpecificHeuristicsStoreTest {
	private static final int INITIAL_GENERATED_ID = 100;

	private final AtomStore atomStore;
	private final WritableAssignment assignment;
	private PseudoChoiceManager choiceManager;
	private DefaultDomainSpecificHeuristicsStore store;
	private AtomicInteger idGenerator = new AtomicInteger(INITIAL_GENERATED_ID);

	public DefaultDomainSpecificHeuristicsStoreTest() {
		atomStore = new AtomStoreImpl();
		assignment = new TrailAssignment(atomStore);
	}

	@Before
	public void setUp() {
		this.store = new DefaultDomainSpecificHeuristicsStore();
		this.choiceManager = new PseudoChoiceManager(assignment, new NaiveNoGoodStore(assignment), store);
	}

	@Test
	public void testInsert_1_atom() {
		int id = idGenerator.getAndIncrement();
		HeuristicDirectiveValues info1 = info(1, 1, 1);
		store.addInfo(id, info1);
		choiceManager.makeAtomsActive(id);
		List<HeuristicDirectiveValues> orderedList = listHeuristicsOrderedByDecreasingPriority();
		assertEquals(Arrays.asList(info1), orderedList);
	}

	@Test
	public void testInsert_2_atoms_sameWeight_sameLevel() {
		int id1 = idGenerator.getAndIncrement();
		int id2 = idGenerator.getAndIncrement();
		HeuristicDirectiveValues info1 = info(1, 2, 3);
		HeuristicDirectiveValues info2 = info(2, 2, 3);
		store.addInfo(id1, info1);
		store.addInfo(id2, info2);
		choiceManager.makeAtomsActive(id1, id2);
		List<HeuristicDirectiveValues> orderedList = listHeuristicsOrderedByDecreasingPriority();
		assertEquals(2, orderedList.size());
		assertEquals(new HashSet<>(Arrays.asList(info1, info2)), new HashSet<>(orderedList));
	}

	@Test
	public void testInsert_3_atoms_sameWeight_differentLevel() {
		int id1 = idGenerator.getAndIncrement();
		int id2 = idGenerator.getAndIncrement();
		int id3 = idGenerator.getAndIncrement();
		HeuristicDirectiveValues info1 = info(1, 2, 3);
		HeuristicDirectiveValues info2 = info(2, 2, 1);
		HeuristicDirectiveValues info3 = info(3, 2, 2);
		store.addInfo(id1, info1);
		store.addInfo(id2, info2);
		store.addInfo(id3, info3);
		choiceManager.makeAtomsActive(id1, id2, id3);
		List<HeuristicDirectiveValues> orderedList = listHeuristicsOrderedByDecreasingPriority();
		assertEquals(Arrays.asList(info1, info3, info2), orderedList);
	}

	@Test
	public void testInsert_3_atoms_differentWeight_sameLevel() {
		int id1 = idGenerator.getAndIncrement();
		int id2 = idGenerator.getAndIncrement();
		int id3 = idGenerator.getAndIncrement();
		HeuristicDirectiveValues info1 = info(1, 4, 1);
		HeuristicDirectiveValues info2 = info(2, 2, 1);
		HeuristicDirectiveValues info3 = info(3, 3, 1);
		store.addInfo(id1, info1);
		store.addInfo(id2, info2);
		store.addInfo(id3, info3);
		choiceManager.makeAtomsActive(id1, id2, id3);
		List<HeuristicDirectiveValues> orderedList = listHeuristicsOrderedByDecreasingPriority();
		assertEquals(Arrays.asList(info1, info3, info2), orderedList);
	}

	private HeuristicDirectiveValues info(int atom, int weight, int level) {
		return new HeuristicDirectiveValues(atom, null, weight, level, true);
	}

	private List<HeuristicDirectiveValues> listHeuristicsOrderedByDecreasingPriority() {
		List<HeuristicDirectiveValues> list = new LinkedList<>();
		HeuristicDirectiveValues currentValues;
		while ((currentValues = store.poll()) != null) {
			list.add(currentValues);
		}
		return list;
	}

	private class PseudoChoiceManager extends ChoiceManager {

		private Collection<ActivityListener> activityListeners;

		public PseudoChoiceManager(WritableAssignment assignment, NoGoodStore store, DomainSpecificHeuristicsStore domainSpecificHeuristicsStore) {
			super(assignment, store, domainSpecificHeuristicsStore);
		}

		@Override
		public void setHeuristicActivityListener(ActivityListener activityListener) {
			super.setHeuristicActivityListener(activityListener);
			if (activityListeners == null) {
				activityListeners = new LinkedList<>();
			}
			this.activityListeners.add(activityListener);
		}

		public void makeAtomsActive(int... atoms) {
			for (int atom : atoms) {
				activityListeners.forEach(al -> al.callbackOnChanged(atom, true));
			}
		}

		@Override
		public boolean isActiveChoiceAtom(int atom) {
			return true;
		}
	}

}
