/**
 * Copyright (c) 2018-2019, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.core.solver;

import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.FALSE;
import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.MBT;
import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.common.AtomStoreTest;
import at.ac.tuwien.kr.alpha.core.common.Assignment;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.common.IntIterator;

/**
 * Copyright (c) 2018-2020, the Alpha Team.
 */
public class TrailAssignmentTest {
	private final TrailAssignment assignment;

	public TrailAssignmentTest() {
		AtomStore atomStore = new AtomStoreImpl();
		AtomStoreTest.fillAtomStore(atomStore, 20);
		assignment = new TrailAssignment(atomStore);
	}

	@Before
	public void setUp() {
		assignment.clear();
		assignment.growForMaxAtomId();
	}

	@Test(expected = IllegalArgumentException.class)
	public void assign() throws Exception {
		assignment.assign(0, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeAtomThrows() throws Exception {
		assignment.assign(-1, null);
	}

	@Test
	public void alreadyAssignedThrows() throws Exception {
		assertNull(assignment.assign(1, MBT));
		assertNotNull(assignment.assign(1, FALSE));
	}

	@Test
	public void initializeDecisionLevelState() throws Exception {
		assignment.assign(1, MBT);
		assignment.choose(2, MBT);
		assignment.choose(1, TRUE);
	}

	@Test
	public void checkToString() {
		assignment.assign(1, FALSE);
		assertEquals("[F_a(0)@0]", assignment.toString());

		assignment.assign(2, TRUE);
		assertEquals("[F_a(0)@0, T_a(1)@0]", assignment.toString());
	}

	@Test
	public void reassignGracefully() {
		assignment.assign(1, FALSE);
		assignment.assign(1, FALSE);
	}

	@Test
	public void assignAndBacktrack() {
		assignment.assign(1, MBT);
		assignment.assign(2, FALSE);
		assignment.assign(3, TRUE);

		assertEquals(MBT, assignment.getTruth(1));
		assertEquals(FALSE, assignment.getTruth(2));
		assertEquals(TRUE, assignment.getTruth(3));
		assertEquals(assignment.getTrueAssignments(), new HashSet<>(Collections.singletonList(3)));
		assertEquals(1, assignment.getMBTCount());

		assignment.choose(1, TRUE);

		assertEquals(TRUE, assignment.getTruth(1));
		assertEquals(assignment.getTrueAssignments(), new HashSet<>(Arrays.asList(3, 1)));
		assertEquals(0, assignment.getMBTCount());

		assignment.backtrack();

		assertEquals(MBT, assignment.getTruth(1));
		assertEquals(FALSE, assignment.getTruth(2));
		assertEquals(TRUE, assignment.getTruth(3));
		assertEquals(assignment.getTrueAssignments(), new HashSet<>(Collections.singletonList(3)));
		assertEquals(1, assignment.getMBTCount());

		assignment.choose(4, MBT);
		assignment.assign(5, MBT);

		assertEquals(MBT, assignment.getTruth(4));
		assertEquals(MBT, assignment.getTruth(5));

		assignment.backtrack();

		assertFalse(assignment.isAssigned(4));
		assertFalse(assignment.isAssigned(5));

		assignment.choose(4, TRUE);

		assertEquals(TRUE, assignment.getTruth(4));

		assignment.backtrack();

		assertNull(assignment.getTruth(4));
	}

	@Test
	public void assignmentsToProcess() throws Exception {
		assignment.assign(1, MBT);

		Assignment.Pollable queue = assignment.getAssignmentsToProcess();
		assertEquals(1, queue.remove());

		assignment.choose(2, MBT);
		assignment.choose(1, TRUE);

		assertEquals(2, queue.remove());
		assertEquals(1, queue.peek());

		queue = assignment.getAssignmentsToProcess();
		assertEquals(1, queue.remove());
	}

	@Test
	public void newAssignmentsIteratorAndBacktracking() throws Exception {
		IntIterator newAssignmentsIterator;

		assignment.assign(1, MBT);
		assignment.choose(2, MBT);

		newAssignmentsIterator = assignment.getNewPositiveAssignmentsIterator();

		assertEquals(1, (int)newAssignmentsIterator.next());
		assertEquals(2, (int)newAssignmentsIterator.next());
		assertFalse(newAssignmentsIterator.hasNext());


		assignment.choose(1, TRUE);
		assignment.backtrack();
		assignment.assign(3, FALSE);

		newAssignmentsIterator = assignment.getNewPositiveAssignmentsIterator();
		assertEquals(3, (int)newAssignmentsIterator.next());
		assertFalse(newAssignmentsIterator.hasNext());
	}

	@Test
	public void newAssignmentsIteratorLowerDecisionLevelAndBacktracking() throws Exception {
		IntIterator newAssignmentsIterator;

		assignment.choose(1, MBT);
		assignment.choose(2, MBT);
		assignment.assign(3, MBT, null, 1);
		assignment.backtrack();

		newAssignmentsIterator = assignment.getNewPositiveAssignmentsIterator();
		assertEquals(1, (int)newAssignmentsIterator.next());
		assertEquals(3, (int)newAssignmentsIterator.next());
		assertFalse(newAssignmentsIterator.hasNext());
	}

	@Test
	public void iteratorAndBacktracking() throws Exception {
		Assignment.Pollable assignmentsToProcess = assignment.getAssignmentsToProcess();

		assignment.assign(1, MBT);
		assertEquals(1, assignmentsToProcess.remove());

		assignment.choose(2, MBT);
		assertEquals(2, assignmentsToProcess.remove());

		assignment.choose(1, TRUE);
		assertEquals(1, assignmentsToProcess.remove());

		assignment.backtrack();

		assignment.assign(3, FALSE);
		assertEquals(3, assignmentsToProcess.remove());
	}

	@Test
	public void mbtCounterAssignMbtToFalseOnLowerDecisionLevel() {
		assertNull(assignment.choose(1, TRUE));
		assertNull(assignment.choose(2, FALSE));

		assertNull(assignment.assign(3, MBT, null, 2));
		assertEquals(1, assignment.getMBTCount());

		assertNull(assignment.choose(4, TRUE));

		assertNotNull(assignment.assign(3, FALSE, null, 1));

		assignment.backtrack();
		assignment.backtrack();

		assertEquals(0, assignment.getMBTCount());
	}

	@Test
	public void numberOfAssignedAtoms() throws Exception {
		assignment.assign(1, MBT);
		assertEquals(1, assignment.getNumberOfAssignedAtoms());
		assignment.assign(2, FALSE);
		assertEquals(2, assignment.getNumberOfAssignedAtoms());
		assignment.assign(3, MBT);
		assignment.assign(3, TRUE);
		assertEquals(3, assignment.getNumberOfAssignedAtoms());
		assignment.choose(1, TRUE);
		assertEquals(3, assignment.getNumberOfAssignedAtoms());
		assertEquals(1, assignment.getNumberOfAtomsAssignedSinceLastDecision());
		assignment.assign(5, MBT);
		assignment.assign(5, TRUE);
		assertEquals(2, assignment.getNumberOfAtomsAssignedSinceLastDecision());
	}
}