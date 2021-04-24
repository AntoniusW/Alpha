package at.ac.tuwien.kr.alpha.core.solver;

import static at.ac.tuwien.kr.alpha.common.NoGoodTest.fromOldLiterals;
import static at.ac.tuwien.kr.alpha.core.common.NoGood.fact;
import static at.ac.tuwien.kr.alpha.core.common.NoGood.headFirst;
import static at.ac.tuwien.kr.alpha.core.solver.AntecedentTest.antecedentsEquals;
import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.FALSE;
import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.MBT;
import static at.ac.tuwien.kr.alpha.core.solver.ThriceTruth.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.common.AtomStoreTest;
import at.ac.tuwien.kr.alpha.core.common.Assignment;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.common.NoGood;

/**
 * Copyright (c) 2017, the Alpha Team.
 */
public class NaiveNoGoodStoreTest {
	private final AtomStore atomStore;
	private final TrailAssignment assignment;
	private final NaiveNoGoodStore store;

	public NaiveNoGoodStoreTest() {
		atomStore = new AtomStoreImpl();
		assignment = new TrailAssignment(atomStore);
		store = new NaiveNoGoodStore(assignment);
	}

	@Before
	public void setUp() {
		store.clear();
		AtomStoreTest.fillAtomStore(atomStore, 200);
		assignment.growForMaxAtomId();
	}

	@Test
	public void singleFact() {
		store.add(1, fact(fromOldLiterals(-1)));
		store.propagate();

		assertEquals(TRUE, assignment.getTruth(1));
	}

	@Test
	public void single() {
		store.add(1, new NoGood(fromOldLiterals(-1)));
		store.propagate();

		assertEquals(MBT, assignment.getTruth(1));
	}

	@Test
	public void constraintWithAssignment() {
		assignment.assign(123, MBT);
		assignment.assign(23, TRUE);
		store.add(3, new NoGood(fromOldLiterals(-123, 22, 23)));
	}

	@Test
	public void assignment() {
		store.add(3, headFirst(fromOldLiterals(-7, -4, -2)));
		assignment.assign(4, TRUE);
		assignment.assign(2, FALSE);
		assignment.assign(7, FALSE);
		assertNull(store.propagate());
	}


	@Test
	public void addNotCausingAssignment() {
		assignment.assign(1, TRUE);
		store.add(3, headFirst(fromOldLiterals(-3, -2, 1)));

		assertEquals(null, assignment.getTruth(3));
	}

	@Test
	public void addNotCausingAssignmentUnassigned() {
		assignment.assign(1, TRUE);
		store.add(3, headFirst(fromOldLiterals(-5, 4, 1)));

		assertEquals(null, assignment.getTruth(5));
	}

	@Test
	@Ignore("Checks for conflict detection in add.")
	public void addNotCausingAssignmentFalse() {
		assignment.assign(1, FALSE);
		assignment.assign(4, TRUE);
		store.add(3, headFirst(fromOldLiterals(-5, 4, -1)));

		assertEquals(TRUE, assignment.getTruth(5));
	}

	@Test
	public void addNotCausingAssignmentTrue() {
		assignment.assign(1, TRUE);
		assignment.assign(2, TRUE);
		store.add(1, headFirst(fromOldLiterals(-3, 2, -1)));

		assertEquals(null, assignment.getTruth(3));
	}

	@Test
	public void propBinary() {
		assignment.assign(2, FALSE);

		store.add(1, headFirst(fromOldLiterals(-1, 2)));
		store.propagate();

		assertEquals(null, assignment.getTruth(1));
	}

	@Test
	public void propagateBinaryFirstTrue() {
		assignment.assign(2, TRUE);

		store.add(1, headFirst(fromOldLiterals(-1, 2)));
		store.propagate();
		assertEquals(TRUE, assignment.getTruth(1));
	}

	@Test
	public void propagateBinaryMBT() {
		assignment.assign(2, MBT);

		store.add(1, headFirst(fromOldLiterals(-1, 2)));
		store.propagate();

		assertEquals(MBT, assignment.getTruth(1));
	}

	@Test
	public void propagateBinaryTrue() {
		assignment.assign(2, TRUE);

		store.add(1, headFirst(fromOldLiterals(-1, 2)));
		store.propagate();

		assertEquals(TRUE, assignment.getTruth(1));
	}

	@Test
	public void propagateBinaryMBTAfterAssignment() {
		store.add(1, headFirst(fromOldLiterals(-1, 2)));
		store.propagate();

		assignment.assign(2, MBT);
		store.propagate();

		assertEquals(MBT, assignment.getTruth(1));
	}

	@Test
	public void propagateBinaryTrueAfterAssignment() {
		store.add(1, headFirst(fromOldLiterals(-1, 2)));
		assignment.assign(2, TRUE);
		store.propagate();

		assertEquals(TRUE, assignment.getTruth(1));
	}

	@Test
	public void propagateBinaryMBTTwice() {
		assignment.assign(2, MBT);

		store.add(1, new NoGood(fromOldLiterals(-1, 2)));
		store.add(2, new NoGood(fromOldLiterals(-3, 1)));

		store.propagate();

		assertEquals(MBT, assignment.getTruth(1));
		assertEquals(MBT, assignment.getTruth(3));
	}


	@Test
	public void propagateBinaryMBTTwiceOutofSync() {
		store.add(1, new NoGood(fromOldLiterals(-1, 2)));
		store.add(2, new NoGood(fromOldLiterals(-3, 1)));

		assignment.assign(2, MBT);

		store.propagate();

		assertEquals(MBT, assignment.getTruth(1));
		assertEquals(MBT, assignment.getTruth(3));
	}

	@Test
	public void propagateNaryTrue() {
		assignment.assign(2, TRUE);
		assignment.assign(3, TRUE);

		assertNull(store.add(1, headFirst(fromOldLiterals(-1, 2, 3))));

		store.propagate();

		assertEquals(TRUE, assignment.getTruth(1));
	}

	@Test
	public void propagateNaryFalse() {
		assignment.assign(2, FALSE);
		assignment.assign(3, FALSE);

		store.add(1, NoGood.headFirst(fromOldLiterals(-1, -3, -2)));
		store.propagate();

		assertEquals(TRUE, assignment.getTruth(1));
	}

	@Test
	public void addFullyAssignedBinary() {
		assignment.assign(2, TRUE);
		assignment.assign(3, TRUE);

		store.add(1, headFirst(fromOldLiterals(-2, 3)));
		store.propagate();

		assertEquals(TRUE, assignment.getTruth(2));
		assertEquals(TRUE, assignment.getTruth(3));
	}

	@Test
	public void addFullyAssignedNary() {
		assignment.assign(2, TRUE);
		assignment.assign(3, TRUE);
		assignment.assign(4, TRUE);

		store.add(1, headFirst(fromOldLiterals(-2, 3, 4)));
		store.propagate();

		assertEquals(TRUE, assignment.getTruth(2));
		assertEquals(TRUE, assignment.getTruth(3));
		assertEquals(TRUE, assignment.getTruth(4));
	}

	@Test
	public void propagateNaryMBT() {
		final NoGood noGood = headFirst(fromOldLiterals(-1, 2, 3));

		assignment.assign(2, MBT);
		assignment.assign(3, MBT);

		store.add(1, noGood);
		store.propagate();

		assertEquals(MBT, assignment.getTruth(1));
	}

	@Test
	@Ignore("Checks for propagation in add.")
	public void propagateNaryMBTTwice() {
		assignment.assign(4, FALSE);
		assignment.assign(3, MBT);
		assignment.assign(2, MBT);

		store.add(1, headFirst(fromOldLiterals(-1, 2, 3)));
		assertEquals(MBT, assignment.getTruth(1));

		store.add(2, headFirst(fromOldLiterals(-5, -4, 1)));
		store.propagate();

		assertEquals(MBT, assignment.getTruth(1));
		assertEquals(MBT, assignment.getTruth(5));
	}

	@Test
	public void propagateNaryFactsMultiple() {
		NoGood[] noGoods = new NoGood[]{
			headFirst(fromOldLiterals(-1, 2, 3)), // 1 <- 2, 3.
			headFirst(fromOldLiterals(-5, 4, 1)), // 5 <- 4, 1.
			fact(fromOldLiterals(-4)),            // 4.
			fact(fromOldLiterals(-3)),            // 3.
			fact(fromOldLiterals(-2))             // 2.
		};
		for (int i = 0; i < noGoods.length; i++) {
			assertNull(store.add(i + 1, noGoods[i]));
		}

		// First deduce 1 from 2 and 3, then deduce
		// 5 from -4 and 1.
		store.propagate();
		assertTrue(store.didPropagate());

		assertEquals(TRUE, assignment.getTruth(1));
		assertEquals(TRUE, assignment.getTruth(5));
	}

	@Test
	public void moveThirdPointer() {
		// 1 <- 2, 3.
		store.add(1, headFirst(fromOldLiterals(-1, 2, 3)));
		assertNull(store.propagate());
		assertFalse(store.didPropagate());

		// 2.
		store.add(2, fact(fromOldLiterals(-2)));
		assertNull(store.propagate());
		assertTrue(store.didPropagate());
		assertEquals(TRUE, assignment.getTruth(2));
		assertNull(assignment.getTruth(1));

		// 3.
		store.add(3, fact(fromOldLiterals(-3)));
		assertNull(store.propagate());
		assertTrue(store.didPropagate());
		assertEquals(TRUE, assignment.getTruth(3));

		assertEquals(TRUE, assignment.getTruth(1));
	}

	@Test
	public void propagateNaryMBTTwiceReordered() {
		// From 2 and 3 follows 1.
		store.add(1, headFirst(fromOldLiterals(-1, 2, 3)));
		// From -4 and 1 follows 5.
		store.add(2, headFirst(fromOldLiterals(-5, -4, 1)));

		// Assign 4 to false (first premise for 5).
		assignment.assign(4, FALSE);

		// Assign 3 and 2 to MBT (premises for 1).
		assignment.assign(3, MBT);
		assignment.assign(2, MBT);

		// Now 1 must follow from 2 and 3,
		// and 5 must follow from -4 and 1.
		store.propagate();

		assertEquals(MBT, assignment.getTruth(1));
		assertEquals(MBT, assignment.getTruth(5));
	}

	@Test
	public void conflictingFact() {
		final NoGood noGood = fact(fromOldLiterals(-1));
		assignment.assign(1, FALSE);
		ConflictCause conflictCause = store.add(1, noGood);
		assertNotNull(conflictCause);
		assertTrue(antecedentsEquals(noGood.asAntecedent(), conflictCause.getAntecedent()));
	}

	@Test
	@Ignore("Checks for conflict detection in add.")
	public void conflictingBinary() {
		final NoGood noGood = new NoGood(fromOldLiterals(1, 2));
		assignment.assign(1, TRUE);
		assignment.assign(2, TRUE);
		ConflictCause conflictCause = store.add(1, noGood);
		assertTrue(antecedentsEquals(noGood.asAntecedent(), conflictCause.getAntecedent()));
	}

	@Test
	@Ignore("Checks for conflict detection in add.")
	public void conflictingNary() {
		final NoGood noGood = new NoGood(fromOldLiterals(1, 2, 3));
		assignment.assign(1, TRUE);
		assignment.assign(2, TRUE);
		assignment.assign(3, TRUE);
		ConflictCause conflictCause = store.add(1, noGood);
		assertTrue(antecedentsEquals(noGood.asAntecedent(), conflictCause.getAntecedent()));
	}

	@Test
	public void propagateViolatedConstraint() {
		NoGood noGood = headFirst(fromOldLiterals(-3, -2, -1));
		assertNull(store.add(1, noGood));
		assertNull(assignment.assign(1, FALSE));
		assertNull(assignment.assign(2, FALSE));
		assertNull(assignment.assign(3, FALSE));
		ConflictCause conflictCause = store.propagate();
		assertNotNull(conflictCause);
		assertFalse(store.didPropagate());
		assertTrue(antecedentsEquals(noGood.asAntecedent(), conflictCause.getAntecedent()));
	}

	@Test
	public void noViolation() {
		assertNull(store.add(1, headFirst(fromOldLiterals(-7, -4, -2))));
		assertNull(assignment.assign(4, TRUE));
		assertNull(assignment.assign(2, FALSE));
		assertNull(assignment.assign(7, FALSE));
		assertNull(store.propagate());
		assertNull(store.propagate());
	}

	@Test
	public void propagateViolatedConstraintHeadless() {
		NoGood noGood = new NoGood(fromOldLiterals(3, 11, 19));
		assertNull(store.add(24, noGood));
		assertNull(assignment.assign(3, TRUE));
		assertNull(assignment.assign(11, TRUE));
		assertNull(assignment.assign(19, TRUE));
		ConflictCause conflictCause = store.propagate();
		assertNotNull(conflictCause);
		assertFalse(store.didPropagate());
		assertTrue(antecedentsEquals(noGood.asAntecedent(), conflictCause.getAntecedent()));
	}

	@Test
	public void propagateViolatedConstraintHeadlessMbt() {
		NoGood noGood = new NoGood(fromOldLiterals(3, 11, 19));
		assertNull(store.add(24, noGood));
		assertNull(assignment.assign(3, MBT));
		assertNull(assignment.assign(11, MBT));
		assertNull(assignment.assign(19, MBT));
		ConflictCause conflictCause = store.propagate();
		assertNotNull(conflictCause);
		assertFalse(store.didPropagate());
		assertTrue(antecedentsEquals(noGood.asAntecedent(), conflictCause.getAntecedent()));
	}

	@Test
	public void neverViolatedNoGood() {
		NoGood noGood = new NoGood(fromOldLiterals(-44, 10, 13, 44));
		assertNull(store.add(80, noGood));
		assertNull(assignment.assign(10, TRUE));
		assertNull(assignment.assign(13, TRUE));
		assertNull(assignment.assign(44, FALSE));
		assertNull(store.propagate());
	}

	@Test
	public void naryNoGoodViolatedAfterAddition() {
		NoGood noGood = new NoGood(fromOldLiterals(1, 2, 3));
		assertNull(store.add(11, noGood));
		assertNull(assignment.assign(1, MBT));
		assertNull(assignment.assign(2, MBT));
		assertNull(assignment.assign(3, MBT));
		assertNotNull(store.propagate());
	}

	@Test
	@Ignore("Checks for conflict detection in add.")
	public void naryNoGoodViolatedDuringAdditionAllTrue() {
		NoGood noGood = new NoGood(fromOldLiterals(1, 2, 3));
		assertNull(assignment.assign(1, TRUE));
		assertNull(assignment.assign(2, TRUE));
		assertNull(assignment.assign(3, TRUE));
		ConflictCause conflictCause = store.add(11, noGood);
		assertNotNull(conflictCause);
		assertNotNull(conflictCause.getAntecedent());
	}

	@Test
	@Ignore("Checks for conflict detection in add.")
	public void naryNoGoodViolatedDuringAdditionAllMbt() {
		NoGood noGood = new NoGood(fromOldLiterals(1, 2, 3));
		assertNull(assignment.assign(1, MBT));
		assertNull(assignment.assign(2, MBT));
		assertNull(assignment.assign(3, MBT));
		ConflictCause conflictCause = store.add(11, noGood);
		assertNotNull(conflictCause);
		assertNotNull(conflictCause.getAntecedent());
	}

	@Test
	public void binaryNoGoodViolatedAfterAddition() {
		NoGood noGood = new NoGood(fromOldLiterals(1, 2));
		assertNull(store.add(1, noGood));
		assertNull(assignment.assign(1, MBT));
		assertNull(assignment.assign(2, MBT));
		assertNotNull(store.propagate());
	}

	@Test
	@Ignore("Checks for conflict detection in add.")
	public void binaryNoGoodViolatedDuringAdditionAllTrue() {
		NoGood noGood = new NoGood(fromOldLiterals(1, 2));
		assertNull(assignment.assign(1, TRUE));
		assertNull(assignment.assign(2, TRUE));
		ConflictCause conflictCause = store.add(11, noGood);
		assertNotNull(conflictCause);
		assertNotNull(conflictCause.getAntecedent());
	}

	@Test
	@Ignore("Checks for conflict detection in add.")
	public void binaryNoGoodViolatedDuringAdditionAllMbt() {
		NoGood noGood = new NoGood(fromOldLiterals(1, 2));
		assertNull(assignment.assign(1, MBT));
		assertNull(assignment.assign(2, MBT));
		ConflictCause conflictCause = store.add(11, noGood);
		assertNotNull(conflictCause);
		assertNotNull(conflictCause.getAntecedent());
	}

	@Test
	public void addedViolatedBinaryNoGoodPropagatesAfterBacktracking() {
		NoGood noGood = new NoGood(fromOldLiterals(70, 195));
		assertNull(assignment.choose(70, MBT));
		assertNull(assignment.choose(195, MBT));
		assertNotNull(store.add(3, noGood));
		assignment.backtrack();
		assertNull(store.add(3, noGood));
		store.propagate();
		assertEquals(FALSE, assignment.getTruth(195));
	}

	@Test
	public void addedViolatedNaryNoGoodPropagatesAfterBacktracking() {
		NoGood noGood = new NoGood(fromOldLiterals(70, 195, 36));
		assertNull(assignment.choose(70, MBT));
		assertNull(assignment.choose(195, MBT));
		assertNull(assignment.choose(36, MBT));
		assertNotNull(store.add(3, noGood));
		assignment.backtrack();
		assertNull(store.add(3, noGood));
		store.propagate();
		assertEquals(FALSE, assignment.getTruth(36));
	}

	@Test
	public void binaryNoGoodPropagatesTrueFromFalse() {
		NoGood noGood = headFirst(fromOldLiterals(-11, -12));
		assertNull(store.add(5, noGood));
		assertNull(assignment.choose(12, FALSE));
		store.propagate();
		assertEquals(TRUE, assignment.getTruth(11));
	}

	@Test
	public void binaryNoGoodPropagatesTrueFromTrue() {
		NoGood noGood = headFirst(fromOldLiterals(-11, 12));
		assertNull(store.add(5, noGood));
		assertNull(assignment.choose(12, TRUE));
		store.propagate();
		assertEquals(TRUE, assignment.getTruth(11));
	}


	@Test
	@Ignore("Checks for propagation in add.")
	public void addedBinaryNoGoodPropagatesTrueFromFalse() {
		NoGood noGood = headFirst(fromOldLiterals(-11, -12));
		assertNull(assignment.choose(12, FALSE));
		assertNull(store.add(5, noGood));
		assertEquals(TRUE, assignment.getTruth(11));
	}

	@Test
	public void addedBinaryNoGoodPropagatesTrueFromTrue() {
		NoGood noGood = headFirst(fromOldLiterals(-11, 12));
		assertNull(assignment.choose(12, TRUE));
		assertNull(store.add(5, noGood));
		store.propagate();
		assertEquals(TRUE, assignment.getTruth(11));
	}

	@Test
	public void naryNoGoodPropagatesTrueFromFalse() {
		NoGood noGood = headFirst(fromOldLiterals(-1, 2, -3));
		assertNull(store.add(10, noGood));
		assertNull(assignment.assign(2, TRUE));
		store.propagate();
		assertNull(assignment.assign(3, FALSE));
		store.propagate();
		assertEquals(TRUE, assignment.getTruth(1));
	}

	@Test
	public void naryNoGoodPropagatesTrueFromTrue() {
		NoGood noGood = headFirst(fromOldLiterals(-1, 2, -3));
		assertNull(store.add(10, noGood));
		assertNull(assignment.assign(3, FALSE));
		store.propagate();
		assertNull(assignment.assign(2, TRUE));
		store.propagate();
		assertEquals(TRUE, assignment.getTruth(1));
	}

	@Test
	@Ignore("Not decision-level aware.")
	public void propagationAtLowerDecisionLevel() {
		NoGood noGood = headFirst(fromOldLiterals(-1, 2, -3));
		assertNull(assignment.choose(3, FALSE));
		assertNull(assignment.choose(2, TRUE));
		assertNull(assignment.choose(4, TRUE));
		assertNull(store.add(10, noGood));
		assertNull(store.propagate());
		assertEquals(TRUE, assignment.getTruth(1));
		Assignment.Entry entry = assignment.get(1);
		assertEquals(TRUE, entry.getTruth());
		assertEquals(2, entry.getDecisionLevel());
	}
}