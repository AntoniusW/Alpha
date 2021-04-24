package at.ac.tuwien.kr.alpha.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.externals.AspStandardLibrary;
import at.ac.tuwien.kr.alpha.core.externals.Externals;

public class FixedInterpretationLiteralsTest {

	@at.ac.tuwien.kr.alpha.api.externals.Predicate
	public static boolean isOne(int value) {
		return value == 1;
	}

	/**
	 * Dummy method to test external atoms with more than one output term list.
	 * Dummy input param which is not used exists solely to avoid producing an edge
	 * case where there is output, but no input
	 */
	@at.ac.tuwien.kr.alpha.api.externals.Predicate
	public static Set<List<ConstantTerm<String>>> connection(String dummy) {
		Set<List<ConstantTerm<String>>> retVal = new HashSet<>();
		List<ConstantTerm<String>> l1 = new ArrayList<>();
		List<ConstantTerm<String>> l2 = new ArrayList<>();
		List<ConstantTerm<String>> l3 = new ArrayList<>();
		l1.add(Terms.newConstant("Klagenfurt"));
		l1.add(Terms.newConstant("Villach"));
		l2.add(Terms.newConstant("Klagenfurt"));
		l2.add(Terms.newConstant("Graz"));
		l3.add(Terms.newConstant("Villach"));
		l3.add(Terms.newConstant("Salzburg"));
		retVal.add(l1);
		retVal.add(l2);
		retVal.add(l3);
		return retVal;
	}

	private static final String TEST_PROG = "positive_numeric_comparison :- 2 < 3."
			+ "negative_numeric_comparison :- not 3 < 2."
			+ "positive_unary_external :- &isOne[1]."
			+ "negative_unary_external :- not &isOne[2]."
			+ "positive_external_with_output :- "
			+ "&stdlib_datetime_parse[\"01.01.2121 21:11:12\", \"dd.MM.yyyy HH:mm:ss\"](Y, MO, D, H, MI, S), Y = 2121, MO = 1, D = 1, H = 21, MI = 11, S = 12."
			+ "positive_external_with_output_dontfire :- "
			+ "&stdlib_datetime_parse[\"01.01.2121 21:11:12\", \"dd.MM.yyyy HH:mm:ss\"](Y, MO, D, H, MI, S), Y = 2345, MO = 1, D = 1, H = 21, MI = 11, S = 12."
			+ "negative_external_with_output :- Y = 2121, MO = 1, D = 1, H = 21, MI = 11, S = 12, "
			+ "not &stdlib_datetime_parse[\"22.02.2222 21:11:12\", \"dd.MM.yyyy HH:mm:ss\"](Y, MO, D, H, MI, S)."
			+ "negative_external_with_output_dontfire :- Y = 2222, MO = 2, D = 22, H = 21, MI = 11, S = 12,"
			+ "not &stdlib_datetime_parse[\"22.02.2222 21:11:12\", \"dd.MM.yyyy HH:mm:ss\"](Y, MO, D, H, MI, S)."
			+ "negative_external_multioutput :- START = \"Graz\", END = \"Salzburg\", not &connection[train_network](START, END)."
			+ "negative_external_multioutput_dontfire :- START = \"Klagenfurt\", END = \"Villach\", not &connection[train_network](START, END)."
			+ "positive_external_multioutput :- START = \"Klagenfurt\", END = \"Villach\", &connection[train_network](START, END)."
			+ "positive_external_multioutput_dontfire :- START = \"Graz\", END = \"Salzburg\", &connection[train_network](START, END)."
			+ "positive_external_binding_output(START, END) :- &connection[train_network](START, END).";

	private Alpha alpha;
	private Map<String, PredicateInterpretation> externals;

	public FixedInterpretationLiteralsTest() {
		this.alpha = new AlphaImpl();
		this.externals = new HashMap<>();
		this.externals.putAll(Externals.scan(AspStandardLibrary.class));
		this.externals.putAll(Externals.scan(FixedInterpretationLiteralsTest.class));
	}

	@Test
	public void positiveNumericComparison() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertTrue(answerSet.getPredicates().contains(Predicates.getPredicate("positive_numeric_comparison", 0)));
	}

	@Test
	public void negativeNumericComparison() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertTrue(answerSet.getPredicates().contains(Predicates.getPredicate("negative_numeric_comparison", 0)));
	}

	@Test
	public void positiveUnaryExternal() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertTrue(answerSet.getPredicates().contains(Predicates.getPredicate("positive_unary_external", 0)));
	}

	@Test
	public void negativeUnaryExternal() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertTrue(answerSet.getPredicates().contains(Predicates.getPredicate("negative_unary_external", 0)));
	}

	@Test
	public void positiveExternalWithOutput() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertTrue(answerSet.getPredicates().contains(Predicates.getPredicate("positive_external_with_output", 0)));
	}

	@Test
	public void positiveExternalWithOutputDontfire() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertFalse(answerSet.getPredicates().contains(Predicates.getPredicate("positive_external_with_output_dontfire", 0)));
	}

	@Test
	public void negativeExternalWithOutput() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertTrue(answerSet.getPredicates().contains(Predicates.getPredicate("negative_external_with_output", 0)));
	}

	@Test
	public void negativeExternalWithOutputDontfire() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertFalse(answerSet.getPredicates().contains(Predicates.getPredicate("negative_external_with_output_dontfire", 0)));
	}

	@Test
	public void negativeExternalMultioutput() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertTrue(answerSet.getPredicates().contains(Predicates.getPredicate("negative_external_multioutput", 0)));
	}

	@Test
	public void negativeExternalMultioutputDontfire() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertFalse(answerSet.getPredicates().contains(Predicates.getPredicate("negative_external_multioutput_dontfire", 0)));
	}

	@Test
	public void positiveExternalMultioutput() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertTrue(answerSet.getPredicates().contains(Predicates.getPredicate("positive_external_multioutput", 0)));
	}

	@Test
	public void positiveExternalMultioutputDontfire() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Assert.assertFalse(answerSet.getPredicates().contains(Predicates.getPredicate("positive_external_multioutput_dontfire", 0)));
	}

	@Test
	public void positiveExternalBindingOutput() {
		Optional<AnswerSet> answer = this.alpha.solve(this.alpha.readProgramString(TEST_PROG, this.externals)).findFirst();
		Assert.assertTrue(answer.isPresent());
		AnswerSet answerSet = answer.get();
		Predicate pred = Predicates.getPredicate("positive_external_binding_output", 2);
		Assert.assertTrue(answerSet.getPredicates().contains(pred));
		Set<Atom> instances = answerSet.getPredicateInstances(pred);
		Assert.assertEquals(3, instances.size());
		Assert.assertTrue(instances.contains(Atoms.newBasicAtom(pred, Terms.newConstant("Klagenfurt"), Terms.newConstant("Villach"))));
		Assert.assertTrue(instances.contains(Atoms.newBasicAtom(pred, Terms.newConstant("Klagenfurt"), Terms.newConstant("Graz"))));
		Assert.assertTrue(instances.contains(Atoms.newBasicAtom(pred, Terms.newConstant("Villach"), Terms.newConstant("Salzburg"))));
	}

}
