package at.ac.tuwien.kr.alpha.common;

import at.ac.tuwien.kr.alpha.test.util.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Copyright (c) 2021, the Alpha Team.
 */
public class WeightedAnswerSetTest {

	@Test
	public void compareWeightedAnswerSets() {
		String was1Atoms = "a, p(b)";
		WeightedAnswerSet was1 = TestUtils.weightedAnswerSetFromStrings(was1Atoms, "1@1");
		WeightedAnswerSet was2 = TestUtils.weightedAnswerSetFromStrings("a", "1@1, 2@3");
		WeightedAnswerSet was1Same = TestUtils.weightedAnswerSetFromStrings(was1Atoms, "1@1");

		assertFalse(was1.getPredicates().isEmpty());
		assertFalse(was2.getPredicates().isEmpty());
		assertNotEquals(was1, was2);
		assertEquals(was1Same, was1);
		assertEquals(was1.getPredicateInstances(), was1Same.getPredicateInstances());
	}

	@Test
	public void compareAnswerSetsSameAtoms() {
		String was3Atoms = "q(0,1), b, foo(a)";
		WeightedAnswerSet was3 = TestUtils.weightedAnswerSetFromStrings(was3Atoms, "1@1, 2@3");
		WeightedAnswerSet was3AtomsNotWeights = TestUtils.weightedAnswerSetFromStrings(was3Atoms, "2@1, 2@3");

		assertFalse(was3.getPredicates().isEmpty());
		assertNotEquals(was3, was3AtomsNotWeights);
		assertEquals(was3.getPredicateInstances(), was3AtomsNotWeights.getPredicateInstances());
	}


	@Test
	public void compareAnswerSetsSameWeights() {
		String was4Weights = "1@1, 4@3";
		WeightedAnswerSet was4 = TestUtils.weightedAnswerSetFromStrings("b, c", was4Weights);
		WeightedAnswerSet was5 = TestUtils.weightedAnswerSetFromStrings("b, d", was4Weights);

		assertFalse(was4.getPredicates().isEmpty());
		assertFalse(was5.getPredicates().isEmpty());
		assertEquals(0, was4.compareWeights(was5));
		assertNotEquals(was4, was5);
		assertNotEquals(was5, was4);
	}

	@Test
	public void compareWeights() {
		WeightedAnswerSet was6 = TestUtils.weightedAnswerSetFromStrings("a", "2@3");
		WeightedAnswerSet was7 = TestUtils.weightedAnswerSetFromStrings("a", "1@1, 1@3");
		WeightedAnswerSet was8 = TestUtils.weightedAnswerSetFromStrings("a", "1@-3, 1@1, 1@3");
		assertEquals(-1, was7.compareWeights(was6));
		assertEquals(1, was6.compareWeights(was7));

		assertEquals(-1, was7.compareWeights(was8));
		assertEquals(1, was8.compareWeights(was7));

		assertEquals(-1, was8.compareWeights(was6));
		assertEquals(1, was6.compareWeights(was8));
	}

}