package at.ac.tuwien.kr.alpha.test.util;

import at.ac.tuwien.kr.alpha.AnswerSetsParser;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.BasicAnswerSet;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.WeightedAnswerSet;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.program.AbstractProgram;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;

import static java.util.Collections.emptySet;

public class TestUtils {

	public static void assertAnswerSetsEqual(Set<AnswerSet> expected, Set<AnswerSet> actual) {
		if (expected == null) {
			if (actual != null) {
				throw new AssertionError("Expected answer sets are null, but actual are not!");
			}
		}
		try {
			Assert.assertEquals(expected, actual);
		} catch (AssertionError e) {
			Set<AnswerSet> expectedMinusActual = new LinkedHashSet<>(expected);
			expectedMinusActual.removeAll(actual);
			Set<AnswerSet> actualMinusExpected = new LinkedHashSet<>(actual);
			actualMinusExpected.removeAll(expected);
			String setDiffs = "Expected and actual answer sets do not agree, differences are:\nExpected \\ Actual:\n" + expectedMinusActual
					+ "\nActual \\ Expected:\n" + actualMinusExpected;
			throw new AssertionError(setDiffs + e.getMessage(), e);
		}
	}

	public static void assertAnswerSetsEqual(String[] expected, Set<AnswerSet> actual) {
		if (expected.length == 0) {
			TestUtils.assertAnswerSetsEqual(emptySet(), actual);
			return;
		}
		StringJoiner joiner = new StringJoiner("} {", "{", "}");
		Arrays.stream(expected).forEach(joiner::add);
		TestUtils.assertAnswerSetsEqual(AnswerSetsParser.parse(joiner.toString()), actual);
	}

	public static void assertAnswerSetsEqual(String expectedAnswerSet, Set<AnswerSet> actual) {
		TestUtils.assertAnswerSetsEqual(AnswerSetsParser.parse("{ " + expectedAnswerSet + " }"), actual);
	}

	public static void assertAnswerSetsEqualWithBase(String base, String[] expectedAnswerSets, Set<AnswerSet> actual) {
		base = base.trim();
		if (!base.endsWith(",")) {
			base += ", ";
		}

		for (int i = 0; i < expectedAnswerSets.length; i++) {
			expectedAnswerSets[i] = base + expectedAnswerSets[i];
			// Remove trailing ",".
			expectedAnswerSets[i] = expectedAnswerSets[i].trim();
			if (expectedAnswerSets[i].endsWith(",")) {
				expectedAnswerSets[i] = expectedAnswerSets[i].substring(0, expectedAnswerSets[i].length() - 1);
			}
		}
		TestUtils.assertAnswerSetsEqual(expectedAnswerSets, actual);
	}

	public static void assertFactsContainedInProgram(AbstractProgram<?> prog, Atom... facts) {
		for (Atom fact : facts) {
			Assert.assertTrue(prog.getFacts().contains(fact));
		}
	}

	public static Atom basicAtomWithStringTerms(String predicate, String... terms) {
		Predicate pred = Predicate.getInstance(predicate, terms.length);
		List<Term> trms = new ArrayList<>();
		for (String str : terms) {
			trms.add(ConstantTerm.getInstance(str));
		}
		return new BasicAtom(pred, trms);
	}

	public static Atom basicAtomWithSymbolicTerms(String predicate, String... constantSymbols) {
		Predicate pred = Predicate.getInstance(predicate, constantSymbols.length);
		List<Term> trms = new ArrayList<>();
		for (String str : constantSymbols) {
			trms.add(ConstantTerm.getSymbolicInstance(str));
		}
		return new BasicAtom(pred, trms);
	}

	public static WeightedAnswerSet weightedAnswerSetFromStrings(String basicAnswerSetAsString, String weightAtLevelsAsString) {
		BasicAnswerSet basicAnswerSet = (BasicAnswerSet) AnswerSetsParser.parse("{ " + basicAnswerSetAsString + " }").iterator().next();
		// Extract weights at levels from given string.
		String[] weightsAtLevels = weightAtLevelsAsString.split(", ");
		TreeMap<Integer, Integer> weightAtLevelsTreeMap = new TreeMap<>();
		for (String weightsAtLevel : weightsAtLevels) {
			String[] wAtL = weightsAtLevel.split("@");
			weightAtLevelsTreeMap.put(Integer.parseInt(wAtL[1]), Integer.parseInt(wAtL[0]));
		}
		return new WeightedAnswerSet(basicAnswerSet, weightAtLevelsTreeMap);
	}

	public static void assertOptimumAnswerSetEquals(String expectedOptimumAnswerSet, String expectedWeightsAtLevels, Set<AnswerSet> actual) {
		WeightedAnswerSet optimumAnswerSet = weightedAnswerSetFromStrings(expectedOptimumAnswerSet, expectedWeightsAtLevels);

		// Check the optimum is contained in the set of actual answer sets.
		if (!actual.contains(optimumAnswerSet)) {
			throw new AssertionError("Expected optimum answer set is not contained in actual.\n" +
				"Expected optimum answer set: " + optimumAnswerSet + "\n" +
				"Actual answer sets: " + actual);
		}
		// Ensure that there is no better answer set contained in the actual answer sets.
		for (AnswerSet actualAnswerSet : actual) {
			if (actualAnswerSet.equals(optimumAnswerSet)) {
				// Skip optimum itself.
				continue;
			}
			if (!(actualAnswerSet instanceof WeightedAnswerSet)) {
				throw new AssertionError("Expecting weighted answer sets but obtained answer set is not: " + actualAnswerSet);
			}
			WeightedAnswerSet actualWeightedAnswerSet = (WeightedAnswerSet) actualAnswerSet;
			if (optimumAnswerSet.compareWeights(actualWeightedAnswerSet) >= 0) {
				throw new AssertionError("Actual answer set is better than expected one.\n" +
					"Expected: " + optimumAnswerSet + "\n" +
					"Actual: " + actualWeightedAnswerSet);
			}
		}
	}
}
