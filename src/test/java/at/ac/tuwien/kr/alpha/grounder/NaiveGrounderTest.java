/*
 * Copyright (c) 2018-2020 Siemens AG
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
package at.ac.tuwien.kr.alpha.grounder;

import at.ac.tuwien.kr.alpha.common.Assignment;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.DisjunctiveHead;
import at.ac.tuwien.kr.alpha.common.Literals;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.NoGoodCreator;
import at.ac.tuwien.kr.alpha.common.Program;
import at.ac.tuwien.kr.alpha.common.Rule;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.atoms.HeuristicAtom;
import at.ac.tuwien.kr.alpha.grounder.heuristics.GrounderHeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramPartParser;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import at.ac.tuwien.kr.alpha.solver.TrailAssignment;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfigurationBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static at.ac.tuwien.kr.alpha.TestUtil.atom;
import static at.ac.tuwien.kr.alpha.Util.asSet;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests {@link NaiveGrounder}
 *
 * Some test cases use atoms of the something/1 predicate to trick the grounder
 * into believing that other atoms might become true. This is fragile because future implementations
 * of preprocessing techniques might render this trick useless.
 * If unit tests in this class begin to fail due to such improvements to preprocessing, this issue must be addressed.
 */
public class NaiveGrounderTest {
	private static final ProgramParser PROGRAM_PARSER = new ProgramParser();
	private static final ProgramPartParser PROGRAM_PART_PARSER = new ProgramPartParser();
	private static final VariableTerm N = VariableTerm.getInstance("N");
	private static final ConstantTerm<Integer> ONE = ConstantTerm.getInstance(1);
	private final HeuristicsConfiguration heuristicsConfiguration = new HeuristicsConfigurationBuilder().setRespectDomspecHeuristics(true).build();

	final Literal litP1X = PROGRAM_PART_PARSER.parseLiteral("p1(X)");
	final Literal litP2X = PROGRAM_PART_PARSER.parseLiteral("p2(X)");
	final Literal litQ2Y = PROGRAM_PART_PARSER.parseLiteral("q2(Y)");
	final Literal litQ1Y = PROGRAM_PART_PARSER.parseLiteral("q1(Y)");
	final Literal litAX = PROGRAM_PART_PARSER.parseLiteral("a(X)");
	final Literal litA1 = PROGRAM_PART_PARSER.parseLiteral("a(1)");

	@Before
	public void resetIdGenerator() {
		ChoiceRecorder.ID_GENERATOR.resetGenerator();
	}

	@Before
	public void resetRuleIdGenerator() {
		NonGroundRule.ID_GENERATOR.resetGenerator();
	}

	/**
	 * Asserts that a ground rule whose positive body is not satisfied by the empty assignment
	 * is grounded immediately.
	 */
	@Test
	public void groundRuleAlreadyGround() {
		Program program = PROGRAM_PARSER.parse("a :- not b. "
				+ "b :- not a. "
				+ "c :- b.");

		AtomStore atomStore = new AtomStoreImpl();
		Grounder grounder = GrounderFactory.getInstance("naive", program, atomStore, heuristicsConfiguration, true);
		Map<Integer, NoGood> noGoods = grounder.getNoGoods(new TrailAssignment(atomStore));
		int litCNeg = Literals.atomToLiteral(atomStore.get(PROGRAM_PART_PARSER.parseBasicAtom("c")), false);
		int litB = Literals.atomToLiteral(atomStore.get(PROGRAM_PART_PARSER.parseBasicAtom("b")));
		assertExistsNoGoodContaining(noGoods.values(), litCNeg);
		assertExistsNoGoodContaining(noGoods.values(), litB);
	}

	/**
	 * Asserts that a ground rule whose positive non-unary body is not satisfied by the empty assignment
	 * is grounded immediately.
	 */
	@Test
	public void groundRuleWithLongerBodyAlreadyGround() {
		Program program = PROGRAM_PARSER.parse("a :- not b. "
				+ "b :- not a. "
				+ "c :- b. "
				+ "d :- b, c. ");

		AtomStore atomStore = new AtomStoreImpl();
		Grounder grounder = GrounderFactory.getInstance("naive", program, atomStore, heuristicsConfiguration, true);
		Map<Integer, NoGood> noGoods = grounder.getNoGoods(new TrailAssignment(atomStore));
		int litANeg = Literals.atomToLiteral(atomStore.get(PROGRAM_PART_PARSER.parseBasicAtom("a")), false);
		int litBNeg = Literals.atomToLiteral(atomStore.get(PROGRAM_PART_PARSER.parseBasicAtom("b")), false);
		int litCNeg = Literals.atomToLiteral(atomStore.get(PROGRAM_PART_PARSER.parseBasicAtom("c")), false);
		int litDNeg = Literals.atomToLiteral(atomStore.get(PROGRAM_PART_PARSER.parseBasicAtom("d")), false);
		assertExistsNoGoodContaining(noGoods.values(), litANeg);
		assertExistsNoGoodContaining(noGoods.values(), litBNeg);
		assertExistsNoGoodContaining(noGoods.values(), litCNeg);
		assertExistsNoGoodContaining(noGoods.values(), litDNeg);
	}

	/**
	 * Asserts that a ground constraint whose positive body is not satisfied by the empty assignment
	 * is grounded immediately.
	 */
	@Test
	public void groundConstraintAlreadyGround() {
		Program program = PROGRAM_PARSER.parse("a :- not b. "
				+ "b :- not a. "
				+ ":- b.");

		AtomStore atomStore = new AtomStoreImpl();
		Grounder grounder = GrounderFactory.getInstance("naive", program, atomStore, heuristicsConfiguration, true);
		Map<Integer, NoGood> noGoods = grounder.getNoGoods(new TrailAssignment(atomStore));
		int litB = Literals.atomToLiteral(atomStore.get(PROGRAM_PART_PARSER.parseBasicAtom("b")));
		assertTrue(noGoods.containsValue(NoGoodCreator.fromConstraint(Collections.singletonList(litB), Collections.emptyList())));
	}

	@Test
	public void avoidDeadEndsWithPermissiveGrounderHeuristicForP1() {
		RuleGroundingOrder groundingOrderP1 = new RuleGroundingOrder(litP1X,
				Arrays.asList(litP2X, litQ2Y, litQ1Y), -1, false);
		testDeadEnd("p1", groundingOrderP1, true);
	}

	@Test
	public void avoidDeadEndsWithPermissiveGrounderHeuristicForQ1() {
		RuleGroundingOrder groundingOrderQ1 = new RuleGroundingOrder(litQ1Y,
				Arrays.asList(litQ2Y, litP2X, litP1X), -1, false);
		testDeadEnd("q1", groundingOrderQ1, true);
	}

	@Test
	public void noDeadEndWithPermissiveGrounderHeuristicForP1() {
		RuleGroundingOrder groundingOrderP1 = new RuleGroundingOrder(litP1X,
				Arrays.asList(litP2X, litQ1Y, litQ2Y), -1, false);
		testDeadEnd("p1", groundingOrderP1, true);
	}

	@Test
	public void noDeadEndWithPermissiveGrounderHeuristicForQ1() {
		RuleGroundingOrder groundingOrderQ1 = new RuleGroundingOrder(litQ1Y,
				Arrays.asList(litQ2Y, litP1X, litP2X), -1, false);
		testDeadEnd("q1", groundingOrderQ1, true);
	}

	/**
	 * Tests the method {@link NaiveGrounder#getGroundInstantiations(NonGroundRule, RuleGroundingOrder, Substitution, Assignment)} on a predefined program:
	 * <code>
	 *  p1(1). q1(1). <br/>
	 * 	x :- p1(X), p2(X), q1(Y), q2(Y). <br/>
	 * 	p2(X) :- something(X). <br/>
	 * 	q2(X) :- something(X). <br/>
	 * </code>
	 * Given one grounding order {@code groundingOrder} for the first rule in this program which starts with
	 * the literal whose predicate name is {@code predicateNameOfStartingLiteral} and a substitution substituting
	 * the variable in this literal by 1 it is attempted to ground the rule.
	 * It is then asserted that ground instantiations are produced if and only if {@code expectNoGoods} is true.
	 *
	 * @param predicateNameOfStartingLiteral the predicate name of the starting literal, either "p1" or "q1".
	 * @param groundingOrder a grounding order for the first rule in the predefined program that starts with the literal
	 *                          whose predicate name is {@code predicateNameOfStartingLiteral}.
	 * @param expectNoGoods {@code true} iff ground instantiations are expected to be produced under the conditions
	 *                                     described above.
	 */
	private void testDeadEnd(String predicateNameOfStartingLiteral, RuleGroundingOrder groundingOrder, boolean expectNoGoods) {
		Program program = PROGRAM_PARSER.parse("p1(1). q1(1). "
				+ "x :- p1(X), p2(X), q1(Y), q2(Y). "
				+ "p2(X) :- something(X). "
				+ "q2(X) :- something(X). ");

		AtomStore atomStore = new AtomStoreImpl();
		NaiveGrounder grounder = (NaiveGrounder) GrounderFactory.getInstance("naive", program, atomStore, heuristicsConfiguration, p -> true, GrounderHeuristicsConfiguration.permissive(), true);

		NonGroundRule nonGroundRule = grounder.getNonGroundRule(0);
		String strLiteral = "p1".equals(predicateNameOfStartingLiteral) ? "p1(X)" : "p1(Y)";
		final Literal startingLiteral = PROGRAM_PART_PARSER.parseLiteral(strLiteral);
		nonGroundRule.groundingOrder.groundingOrders.put(startingLiteral, groundingOrder);

		grounder.bootstrap();
		TrailAssignment currentAssignment = new TrailAssignment(atomStore);
		final Substitution subst1 = Substitution.unify(startingLiteral, new Instance(ConstantTerm.getInstance(1)), new Substitution());
		final NaiveGrounder.BindingResult bindingResult = grounder.getGroundInstantiations(nonGroundRule, groundingOrder, subst1, currentAssignment);

		assertEquals(expectNoGoods, bindingResult.size() > 0);
	}

	@Test
	public void testGroundingOfRuleSwitchedOffByFalsePositiveBody() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X). "
				+ "b(X) :- something(X). ");
		testIfGrounderGroundsRule(program, 0, litAX, 1, ThriceTruth.FALSE, false);
	}

	@Test
	public void testGroundingOfRuleNotSwitchedOffByTruePositiveBody() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X). "
				+ "b(X) :- something(X). ");
		testIfGrounderGroundsRule(program, 0, litAX, 1, ThriceTruth.TRUE, true);
	}

	@Test
	@Ignore("Currently, rule grounding is not switched off by a true negative body atom")
	public void testGroundingOfRuleSwitchedOffByTrueNegativeBody() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), not b(X). "
				+ "b(X) :- something(X). ");
		testIfGrounderGroundsRule(program, 0, litAX, 1, ThriceTruth.TRUE, false);
	}

	@Test
	public void testGroundingOfRuleNotSwitchedOffByFalseNegativeBody() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), not b(X). "
				+ "b(X) :- something(X). ");

		testIfGrounderGroundsRule(program, 0, litAX, 1, ThriceTruth.FALSE, true);
	}

	/**
	 * Tests if {@link NaiveGrounder#getGroundInstantiations(NonGroundRule, RuleGroundingOrder, Substitution, Assignment)}
	 * produces ground instantiations for the rule with ID {@code ruleID} in {@code program} when {@code startingLiteral}
	 * unified with the numeric instance {@code startingInstance} is used as starting literal and {@code b(1)} is assigned
	 * {@code bTruth}.
	 * It is asserted that ground instantiations are produced if and only if {@code expectNoGoods} is true.
	 */
	private void testIfGrounderGroundsRule(Program program, int ruleID, Literal startingLiteral, int startingInstance, ThriceTruth bTruth, boolean expectNoGoods) {
		AtomStore atomStore = new AtomStoreImpl();
		TrailAssignment currentAssignment = new TrailAssignment(atomStore);
		NaiveGrounder grounder = (NaiveGrounder) GrounderFactory.getInstance("naive", program, atomStore, heuristicsConfiguration, p -> true, GrounderHeuristicsConfiguration.permissive(), true);

		int b = atomStore.putIfAbsent(atom("b", 1));
		currentAssignment.growForMaxAtomId();
		currentAssignment.assign(b, bTruth);

		grounder.bootstrap();
		final NonGroundRule nonGroundRule = grounder.getNonGroundRule(ruleID);
		final Substitution substStartingLiteral = Substitution.unify(startingLiteral, new Instance(ConstantTerm.getInstance(startingInstance)), new Substitution());
		final NaiveGrounder.BindingResult bindingResult = grounder.getGroundInstantiations(nonGroundRule, nonGroundRule.groundingOrder.groundingOrders.get(startingLiteral), substStartingLiteral, currentAssignment);
		assertEquals(expectNoGoods, bindingResult.size() > 0);
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_0_reject() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 0, false, Arrays.asList(1));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_1_accept() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 1, true, Arrays.asList(1));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_1_reject() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X), b(X+1). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 1, false, Arrays.asList(2));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_2_accept() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X), b(X+1). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 2, true, Arrays.asList(2));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_1_accept_two_substitutions() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X,Y). "
				+ "b(X,Y) :- something(X,Y).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 1, new ThriceTruth[] {TRUE, TRUE}, 2, true, Arrays.asList(0, 0));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_1_accept_accept_two_substitutions_with_different_remaining_tolerances() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(1), b(X,Y). "
				+ "b(X,Y) :- something(X,Y).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litA1, 1, 1, new ThriceTruth[] {null, null}, 2, true, Arrays.asList(1, 1));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_2_reject() {
		Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X), b(X+1), b(X+2). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 2, false, Arrays.asList(3));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_2_accept_multiple_facts_of_same_variable() {
		Program program = PROGRAM_PARSER.parse("a(1). b(1). "
				+ "c(X) :- a(X), b(X), b(X+1), b(X+2). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 2, true, Arrays.asList(2));
	}

	private void testPermissiveGrounderHeuristicTolerance(Program program, int ruleID, Literal startingLiteral, int startingInstance, int tolerance, boolean expectNoGoods, List<Integer> expectedNumbersOfUnassignedPositiveBodyAtoms) {
		testPermissiveGrounderHeuristicTolerance(program, ruleID, startingLiteral, startingInstance, tolerance, new ThriceTruth[]{}, 1, expectNoGoods, expectedNumbersOfUnassignedPositiveBodyAtoms);
	}

	/**
	 * Tests if {@link NaiveGrounder#getGroundInstantiations(NonGroundRule, RuleGroundingOrder, Substitution, Assignment)}
	 * produces ground instantiations for the rule with ID {@code ruleID} in {@code program} when {@code startingLiteral}
	 * unified with the numeric instance {@code startingInstance} is used as starting literal and the following
	 * additional conditions are established:
	 * <ul>
	 *     <li>The atoms {@code b([startingInstance], 1), ..., b([startingInstance], n)} are added to the grounder's
	 *     working memory without changing the assignment, where {@code arityOfB-1} occurences of {@code startingInstance}
	 *     are used instead of {@code [startingInstance]} and {@code n} is the length of the {@code truthsOfB} array.
	 *     For example, if the length of {@code truthsOfB} is 2 and {@code arityOfB} is also 2, these atoms are
	 *     {@code b(1,1), b(1,2)}.
	 *     </li>
	 *     <li>The same atoms are assigned the truth values in the {@code truthsOfB} array.</li>
	 * </ul>
	 * It is asserted that ground instantiations are produced if and only if {@code expectNoGoods} is true.
	 * If ground instantiations are produced, it is also asserted that the numbers of unassigned positive body atoms
	 * determined by {@code getGroundInstantiations} match those given in {@code expectedNumbersOfUnassignedPositiveBodyAtoms}.
	 */
	private void testPermissiveGrounderHeuristicTolerance(Program program, int ruleID, Literal startingLiteral, int startingInstance, int tolerance, ThriceTruth[] truthsOfB, int arityOfB, boolean expectNoGoods, List<Integer> expectedNumbersOfUnassignedPositiveBodyAtoms) {
		AtomStore atomStore = new AtomStoreImpl();
		TrailAssignment currentAssignment = new TrailAssignment(atomStore);
		GrounderHeuristicsConfiguration grounderHeuristicsConfiguration = GrounderHeuristicsConfiguration.getInstance(tolerance, tolerance);
		NaiveGrounder grounder = (NaiveGrounder) GrounderFactory.getInstance("naive", program, atomStore, heuristicsConfiguration, p -> true, grounderHeuristicsConfiguration, true);

		int[] bAtomIDs = new int[truthsOfB.length];
		for (int i = 0; i < truthsOfB.length; i++) {
			int[] bTerms = new int[arityOfB];
			for (int n = 0; n < arityOfB; n++) {
				bTerms[n] = (n == arityOfB - 1) ? i + 1 : startingInstance;
			}
			bAtomIDs[i] = atomStore.putIfAbsent(atom("b", bTerms));
		}
		addAtomsToWorkingMemoryWithoutChangingTheAssignment(atomStore, grounder, bAtomIDs);
		assign(currentAssignment, bAtomIDs, truthsOfB);

		grounder.bootstrap();
		final NonGroundRule nonGroundRule = grounder.getNonGroundRule(ruleID);
		final Substitution substStartingLiteral = Substitution.unify(startingLiteral, new Instance(ConstantTerm.getInstance(startingInstance)), new Substitution());
		final NaiveGrounder.BindingResult bindingResult = grounder.getGroundInstantiations(nonGroundRule, nonGroundRule.groundingOrder.groundingOrders.get(startingLiteral), substStartingLiteral, currentAssignment);
		assertEquals(expectNoGoods, bindingResult.size() > 0);
		if (bindingResult.size() > 0) {
			assertEquals(expectedNumbersOfUnassignedPositiveBodyAtoms, bindingResult.numbersOfUnassignedPositiveBodyAtoms);
		} else {
			assertTrue(bindingResult.numbersOfUnassignedPositiveBodyAtoms.isEmpty());
		}
	}

	/**
	 * Assigns {@code truthValues} to atoms {@code atomIDs} in {@code currentAssignment}.
	 */
	private void assign(TrailAssignment currentAssignment, int[] atomIDs, ThriceTruth[] truthValues) {
		currentAssignment.growForMaxAtomId();
		for (int i = 0; i < truthValues.length; i++) {
			int atomID = atomIDs[i];
			if (truthValues[i] != null) {
				currentAssignment.assign(atomID, truthValues[i]);
			}
		}
	}

	/**
	 * Adds atoms {@code atomIDs} to {@code grounder}'s working memory without changing the assignment.
	 * This is achieved by creating a temporary assignment on {@code atomStore} in which those atoms are assigned true
	 * and using this temporary assignment to update the grounder's working memory.
	 */
	private void addAtomsToWorkingMemoryWithoutChangingTheAssignment(AtomStore atomStore, NaiveGrounder grounder, int[] atomIDs) {
		TrailAssignment temporaryAssignment = new TrailAssignment(atomStore);
		temporaryAssignment.growForMaxAtomId();
		for (int b : atomIDs) {
			temporaryAssignment.assign(b, TRUE);
		}
		grounder.updateAssignment(temporaryAssignment.getNewPositiveAssignmentsIterator());
	}

	private void assertExistsNoGoodContaining(Collection<NoGood> noGoods, int literal) {
		for (NoGood noGood : noGoods) {
			for (int literalInNoGood : noGood) {
				if (literalInNoGood == literal) {
					return;
				}
			}
		}
		fail("No NoGood exists that contains literal " + literal);
	}

	@Test
	public void testGenerateHeuristicNoGoods() {
		final Program program = PROGRAM_PARSER.parse("{ a(0); a(1); a(2); a(3); a(4); a(5); a(6); a(7) }."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic b(1) : T a(0), MT a(1), M a(2), F a(3), not T a(4), not MT a(5), not M a(6), not F a(7). [3@2]");
		final int expectedNumberOfHeuristicRule = 19;	//because there are 18 ground rules except the heuristic rule

		final AtomStore atomStore = new AtomStoreImpl();
		final Grounder grounder = GrounderFactory.getInstance("naive", program, atomStore, heuristicsConfiguration, true);
		final NoGoodGenerator noGoodGenerator = ((NaiveGrounder)grounder).noGoodGenerator;
		final Rule rule = findHeuristicRule(program.getRules());
		assert rule != null;
		final NonGroundRule nonGroundRule = NonGroundRule.constructNonGroundRule(rule);
		final Set<NoGood> generatedNoGoods = new HashSet<>(noGoodGenerator.generateNoGoodsFromGroundSubstitution(nonGroundRule, new Substitution()));
		assertEquals(10, generatedNoGoods.size());
		final Set<String> noGoodsToString = generatedNoGoods.stream().map(atomStore::noGoodToString).collect(Collectors.toSet());
		assertEquals(asSet(
				"*{-(HeuOn(\"0\", \"t\")), +(a(0))}",
				"*{-(HeuOn(\"0\", \"tm\")), +(a(1))}",
				"*{-(HeuOn(\"0\", \"m\")), +(a(2))}",
				"*{-(HeuOn(\"0\", \"f\")), -(a(3))}",
				"*{-(HeuOff(\"0\", \"t\")), +(a(4))}",
				"*{-(HeuOff(\"0\", \"tm\")), +(a(5))}",
				"*{-(HeuOff(\"0\", \"m\")), +(a(6))}",
				"*{-(HeuOff(\"0\", \"f\")), -(a(7))}",
				"*{-(_R_(\"" + expectedNumberOfHeuristicRule + "\",\"{}\")), +(b(1))}",
				"{+(_R_(\"" + expectedNumberOfHeuristicRule + "\",\"{}\")), -(b(1))}"
		), noGoodsToString);
	}

	private Rule findHeuristicRule(List<Rule> rules) {
		for (Rule rule : rules) {
			if (rule.getHead() instanceof DisjunctiveHead && ((DisjunctiveHead)rule.getHead()).disjunctiveAtoms.get(0) instanceof HeuristicAtom) {
				return rule;
			}
		}
		return null;
	}

}
