package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.test.util.TestUtils;
import org.junit.Test;

import java.util.Set;

/**
 * Copyright (c) 2021, the Alpha Team.
 */
public class WeakConstraintsTests extends AbstractSolverTests {

	@Test
	public void simpleWeightsSameLevel() {
		String program = ":~a.[1@0,foo,bar]" +
			":~b.[2@0,baz]" +
			"a :- not b. b:- not a.";
		Set<AnswerSet> actualAnswerSets = collectSet(program);
		TestUtils.assertOptimumAnswerSetEquals("a", "1@0", actualAnswerSets);
	}


	@Test
	public void simpleWeightedAnswerSet() {
		String program = ":~a.[2@2,foo,bar]" +
			":~b.[1@1,baz]" +
			"a :- not b. b:- not a.";
		Set<AnswerSet> actualAnswerSets = collectSet(program);
		TestUtils.assertOptimumAnswerSetEquals("b", "1@1", actualAnswerSets);
	}

	@Test
	public void simpleMultiLevelWeightedAnswerSet() {
		String program = ":~a.[2@2,foo,bar]" +
			":~b.[1@1,baz]" +
			":~b.[3@-4,baz]" +
			"a :- not b. b:- not a.";
		Set<AnswerSet> actualAnswerSets = collectSet(program);
		TestUtils.assertOptimumAnswerSetEquals("b", "3@-4, 1@1", actualAnswerSets);
	}
}
