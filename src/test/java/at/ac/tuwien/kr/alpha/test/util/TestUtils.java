package at.ac.tuwien.kr.alpha.test.util;

import static java.util.Collections.emptySet;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.program.AbstractProgram;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;

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

	public static Atom atom(String predicateName, String... termStrings) {
		Term[] terms = new Term[termStrings.length];
		for (int i = 0; i < termStrings.length; i++) {
			String termString = termStrings[i];
			if (StringUtils.isAllUpperCase(termString.substring(0, 1))) {
				terms[i] = VariableTerm.getInstance(termString);
			} else {
				terms[i] = ConstantTerm.getInstance(termString);
			}
		}
		return new BasicAtom(Predicate.getInstance(predicateName, terms.length), terms);
	}

	public static Atom atom(String predicateName, int... termInts) {
		Term[] terms = new Term[termInts.length];
		for (int i = 0; i < termInts.length; i++) {
			terms[i] = ConstantTerm.getInstance(termInts[i]);
		}
		return new BasicAtom(Predicate.getInstance(predicateName, terms.length), terms);
	}

	public static void printNoGoods(AtomStore atomStore, Collection<NoGood> noGoods) {
		System.out.println(noGoods.stream().map(atomStore::noGoodToString).collect(Collectors.toSet()));
	}

	public static void assertProgramContainsRule(InputProgram prog, BasicRule containedRule) {
		for (BasicRule rule : prog.getRules()) {
			if (rule.equals(containedRule)) {
				return;
			}
		}
		Assert.fail("Program should contain rule, but does not! (rule = " + containedRule + ")");
	}

}
