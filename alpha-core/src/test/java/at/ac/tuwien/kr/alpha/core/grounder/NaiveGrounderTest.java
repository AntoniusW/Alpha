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
package at.ac.tuwien.kr.alpha.core.grounder;

import static at.ac.tuwien.kr.alpha.TestUtil.atom;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.grounder.heuristics.GrounderHeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.NormalProgram;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.atoms.Literals;
import at.ac.tuwien.kr.alpha.core.common.Assignment;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.grounder.instantiation.BindingResult;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.parser.ProgramPartParser;
import at.ac.tuwien.kr.alpha.core.programs.AnalyzedProgram;
import at.ac.tuwien.kr.alpha.core.programs.CompiledProgram;
import at.ac.tuwien.kr.alpha.core.programs.InternalProgram;
import at.ac.tuwien.kr.alpha.core.programs.transformation.NormalizeProgramTransformation;
import at.ac.tuwien.kr.alpha.core.programs.transformation.StratifiedEvaluation;
import at.ac.tuwien.kr.alpha.core.rules.CompiledRule;
import at.ac.tuwien.kr.alpha.core.rules.InternalRule;
import at.ac.tuwien.kr.alpha.core.solver.ThriceTruth;
import at.ac.tuwien.kr.alpha.core.solver.TrailAssignment;

/**
 * Tests {@link NaiveGrounder}
 * 
 * Some test cases use atoms of the something/1 predicate to trick the grounder
 * into believing that other atoms might become true. This is fragile because future implementations
 * of preprocessing techniques might render this trick useless.
 * If unit tests in this class begin to fail due to such improvements to preprocessing, this issue must be addressed.
 */
public class NaiveGrounderTest {
	private static final ProgramParser PROGRAM_PARSER = new ProgramParserImpl();
	private static final ProgramPartParser PROGRAM_PART_PARSER = new ProgramPartParser();
	private static final NormalizeProgramTransformation NORMALIZE_TRANSFORM = new NormalizeProgramTransformation(false);

	final Literal litP1X = PROGRAM_PART_PARSER.parseLiteral("p1(X)");
	final Literal litP2X = PROGRAM_PART_PARSER.parseLiteral("p2(X)");
	final Literal litQ2Y = PROGRAM_PART_PARSER.parseLiteral("q2(Y)");
	final Literal litQ1Y = PROGRAM_PART_PARSER.parseLiteral("q1(Y)");
	final Literal litAX = PROGRAM_PART_PARSER.parseLiteral("a(X)");
	final Literal litA1 = PROGRAM_PART_PARSER.parseLiteral("a(1)");

	@Before
	public void resetRuleIdGenerator() {
		InternalRule.resetIdGenerator();
	}

	/**
	 * Asserts that a ground rule whose positive body is not satisfied by the empty assignment
	 * is grounded immediately.
	 */
	@Test
	public void groundRuleAlreadyGround() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a :- not b. "
				+ "b :- not a. "
				+ "c :- b.");
		NormalProgram normal = NORMALIZE_TRANSFORM.apply(program);
		CompiledProgram prog = new StratifiedEvaluation().apply(AnalyzedProgram.analyzeNormalProgram(normal));
		
		AtomStore atomStore = new AtomStoreImpl();
		Grounder grounder = GrounderFactory.getInstance("naive", prog, atomStore, true);
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
		ASPCore2Program program = PROGRAM_PARSER.parse("a :- not b. "
				+ "b :- not a. "
				+ "c :- b. "
				+ "d :- b, c. ");
		NormalProgram normal = NORMALIZE_TRANSFORM.apply(program);
		InternalProgram prog = new StratifiedEvaluation().apply(AnalyzedProgram.analyzeNormalProgram(normal));
		
		AtomStore atomStore = new AtomStoreImpl();
		Grounder grounder = GrounderFactory.getInstance("naive", prog, atomStore, true);
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
		ASPCore2Program program = PROGRAM_PARSER.parse("a :- not b. "
				+ "b :- not a. "
				+ ":- b.");
		NormalProgram normal = NORMALIZE_TRANSFORM.apply(program);
		InternalProgram prog = new StratifiedEvaluation().apply(AnalyzedProgram.analyzeNormalProgram(normal));
		
		AtomStore atomStore = new AtomStoreImpl();
		Grounder grounder = GrounderFactory.getInstance("naive", prog, atomStore, true);
		Map<Integer, NoGood> noGoods = grounder.getNoGoods(new TrailAssignment(atomStore));
		int litB = Literals.atomToLiteral(atomStore.get(PROGRAM_PART_PARSER.parseBasicAtom("b")));
		assertTrue(noGoods.containsValue(NoGood.fromConstraint(Collections.singletonList(litB), Collections.emptyList())));
	}

	@Test
	public void avoidDeadEndsWithPermissiveGrounderHeuristicForP1() {
		RuleGroundingOrderImpl groundingOrderP1 = new RuleGroundingOrderImpl(litP1X,
				Arrays.asList(litP2X, litQ2Y, litQ1Y), -1, false);
		testDeadEnd("p1", groundingOrderP1, true);
	}

	@Test
	public void avoidDeadEndsWithPermissiveGrounderHeuristicForQ1() {
		RuleGroundingOrderImpl groundingOrderQ1 = new RuleGroundingOrderImpl(litQ1Y,
				Arrays.asList(litQ2Y, litP2X, litP1X), -1, false);
		testDeadEnd("q1", groundingOrderQ1, true);
	}

	@Test
	public void noDeadEndWithPermissiveGrounderHeuristicForP1() {
		RuleGroundingOrderImpl groundingOrderP1 = new RuleGroundingOrderImpl(litP1X,
				Arrays.asList(litP2X, litQ1Y, litQ2Y), -1, false);
		testDeadEnd("p1", groundingOrderP1, true);
	}

	@Test
	public void noDeadEndWithPermissiveGrounderHeuristicForQ1() {
		RuleGroundingOrderImpl groundingOrderQ1 = new RuleGroundingOrderImpl(litQ1Y,
				Arrays.asList(litQ2Y, litP1X, litP2X), -1, false);
		testDeadEnd("q1", groundingOrderQ1, true);
	}

	/**
	 * Tests the method {@link NaiveGrounder#getGroundInstantiations(InternalRule, RuleGroundingOrder, Substitution, Assignment)} on a predefined program:
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
	private void testDeadEnd(String predicateNameOfStartingLiteral, RuleGroundingOrderImpl groundingOrder, boolean expectNoGoods) {
		String aspStr = "p1(1). q1(1). "
				+ "x :- p1(X), p2(X), q1(Y), q2(Y). "
				+ "p2(X) :- something(X). "
				+ "q2(X) :- something(X). ";
		CompiledProgram program = InternalProgram.fromNormalProgram(
				NORMALIZE_TRANSFORM.apply(
						PROGRAM_PARSER.parse(aspStr)
						)
				);

		AtomStore atomStore = new AtomStoreImpl();
		NaiveGrounder grounder = (NaiveGrounder) GrounderFactory.getInstance("naive", program, atomStore, p -> true, GrounderHeuristicsConfiguration.permissive(), true);

		CompiledRule nonGroundRule = grounder.getNonGroundRule(0);
		String strLiteral = "p1".equals(predicateNameOfStartingLiteral) ? "p1(X)" : "p1(Y)";
		final Literal startingLiteral = PROGRAM_PART_PARSER.parseLiteral(strLiteral);
		((RuleGroundingInfoImpl) nonGroundRule.getGroundingInfo()).groundingOrders.put(startingLiteral, groundingOrder);

		grounder.bootstrap();
		TrailAssignment currentAssignment = new TrailAssignment(atomStore);
		final Substitution subst1 = BasicSubstitution.specializeSubstitution(startingLiteral, new Instance(Terms.newConstant(1)), BasicSubstitution.EMPTY_SUBSTITUTION);
		final BindingResult bindingResult = grounder.getGroundInstantiations(nonGroundRule, groundingOrder, subst1, currentAssignment);

		assertEquals(expectNoGoods, bindingResult.size() > 0);
	}

	@Test
	public void testGroundingOfRuleSwitchedOffByFalsePositiveBody() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X). "
				+ "b(X) :- something(X). ");
		testIfGrounderGroundsRule(program, 0, litAX, 1, ThriceTruth.FALSE, false);
	}

	@Test
	public void testGroundingOfRuleNotSwitchedOffByTruePositiveBody() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X). "
				+ "b(X) :- something(X). ");
		testIfGrounderGroundsRule(program, 0, litAX, 1, ThriceTruth.TRUE, true);
	}

	@Test
	@Ignore("Currently, rule grounding is not switched off by a true negative body atom")
	public void testGroundingOfRuleSwitchedOffByTrueNegativeBody() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), not b(X). "
				+ "b(X) :- something(X). ");
		testIfGrounderGroundsRule(program, 0, litAX, 1, ThriceTruth.TRUE, false);
	}

	@Test
	public void testGroundingOfRuleNotSwitchedOffByFalseNegativeBody() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), not b(X). "
				+ "b(X) :- something(X). ");

		testIfGrounderGroundsRule(program, 0, litAX, 1, ThriceTruth.FALSE, true);
	}

	/**
	 * Tests if {@link NaiveGrounder#getGroundInstantiations(InternalRule, RuleGroundingOrder, Substitution, Assignment)}
	 * produces ground instantiations for the rule with ID {@code ruleID} in {@code program} when {@code startingLiteral}
	 * unified with the numeric instance {@code startingInstance} is used as starting literal and {@code b(1)} is assigned
	 * {@code bTruth}.
	 * It is asserted that ground instantiations are produced if and only if {@code expectNoGoods} is true.
	 */
	private void testIfGrounderGroundsRule(ASPCore2Program program, int ruleID, Literal startingLiteral, int startingInstance, ThriceTruth bTruth, boolean expectNoGoods) {
		CompiledProgram internalPrg = InternalProgram.fromNormalProgram(NORMALIZE_TRANSFORM.apply(program));
		AtomStore atomStore = new AtomStoreImpl();
		TrailAssignment currentAssignment = new TrailAssignment(atomStore);
		NaiveGrounder grounder = (NaiveGrounder) GrounderFactory.getInstance("naive", internalPrg, atomStore, p -> true, GrounderHeuristicsConfiguration.permissive(), true);

		int b = atomStore.putIfAbsent(atom("b", 1));
		currentAssignment.growForMaxAtomId();
		currentAssignment.assign(b, bTruth);

		grounder.bootstrap();
		final CompiledRule nonGroundRule = grounder.getNonGroundRule(ruleID);
		final Substitution substStartingLiteral = BasicSubstitution.specializeSubstitution(startingLiteral, new Instance(Terms.newConstant(startingInstance)), BasicSubstitution.EMPTY_SUBSTITUTION);
		final BindingResult bindingResult = grounder.getGroundInstantiations(nonGroundRule, nonGroundRule.getGroundingInfo().orderStartingFrom(startingLiteral), substStartingLiteral, currentAssignment);
		assertEquals(expectNoGoods, bindingResult.size() > 0);
	}
	
	@Test
	public void testPermissiveGrounderHeuristicTolerance_0_reject() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 0, false, Arrays.asList(1));
	}
	
	@Test
	public void testPermissiveGrounderHeuristicTolerance_1_accept() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 1, true, Arrays.asList(1));
	}
	
	@Test
	public void testPermissiveGrounderHeuristicTolerance_1_reject() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X), b(X+1). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 1, false, Arrays.asList(2));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_2_accept() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X), b(X+1). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 2, true, Arrays.asList(2));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_1_accept_two_substitutions() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X,Y). "
				+ "b(X,Y) :- something(X,Y).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 1, new ThriceTruth[] {ThriceTruth.TRUE, ThriceTruth.TRUE}, 2, true, Arrays.asList(0, 0));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_1_accept_accept_two_substitutions_with_different_remaining_tolerances() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(1), b(X,Y). "
				+ "b(X,Y) :- something(X,Y).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litA1, 1, 1, new ThriceTruth[] {null, null}, 2, true, Arrays.asList(1, 1));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_2_reject() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). "
				+ "c(X) :- a(X), b(X), b(X+1), b(X+2). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 2, false, Arrays.asList(3));
	}

	@Test
	public void testPermissiveGrounderHeuristicTolerance_2_accept_multiple_facts_of_same_variable() {
		ASPCore2Program program = PROGRAM_PARSER.parse("a(1). b(1). "
				+ "c(X) :- a(X), b(X), b(X+1), b(X+2). "
				+ "b(X) :- something(X).");
		testPermissiveGrounderHeuristicTolerance(program, 0, litAX, 1, 2, true, Arrays.asList(2));
	}

	private void testPermissiveGrounderHeuristicTolerance(ASPCore2Program program, int ruleID, Literal startingLiteral, int startingInstance, int tolerance, boolean expectNoGoods, List<Integer> expectedNumbersOfUnassignedPositiveBodyAtoms) {
		testPermissiveGrounderHeuristicTolerance(program, ruleID, startingLiteral, startingInstance, tolerance, new ThriceTruth[]{}, 1, expectNoGoods, expectedNumbersOfUnassignedPositiveBodyAtoms);
	}

	/**
	 * Tests if {@link NaiveGrounder#getGroundInstantiations(InternalRule, RuleGroundingOrder, Substitution, Assignment)}
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
	private void testPermissiveGrounderHeuristicTolerance(ASPCore2Program program, int ruleID, Literal startingLiteral, int startingInstance, int tolerance, ThriceTruth[] truthsOfB, int arityOfB, boolean expectNoGoods, List<Integer> expectedNumbersOfUnassignedPositiveBodyAtoms) {
		CompiledProgram internalPrg = InternalProgram.fromNormalProgram(NORMALIZE_TRANSFORM.apply(program));
		AtomStore atomStore = new AtomStoreImpl();
		TrailAssignment currentAssignment = new TrailAssignment(atomStore);
		GrounderHeuristicsConfiguration heuristicConfiguration = GrounderHeuristicsConfiguration.getInstance(tolerance, tolerance);
		NaiveGrounder grounder = (NaiveGrounder) GrounderFactory.getInstance("naive", internalPrg, atomStore, p -> true, heuristicConfiguration, true);

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
		final CompiledRule nonGroundRule = grounder.getNonGroundRule(ruleID);
		final Substitution substStartingLiteral = BasicSubstitution.specializeSubstitution(startingLiteral, new Instance(Terms.newConstant(startingInstance)), BasicSubstitution.EMPTY_SUBSTITUTION);
		final BindingResult bindingResult = grounder.getGroundInstantiations(nonGroundRule, nonGroundRule.getGroundingInfo().orderStartingFrom(startingLiteral), substStartingLiteral, currentAssignment);
		assertEquals(expectNoGoods, bindingResult.size() > 0);
		if (bindingResult.size() > 0) {
			assertEquals(expectedNumbersOfUnassignedPositiveBodyAtoms, bindingResult.getNumbersOfUnassignedPositiveBodyAtoms());
		} else {
			assertTrue(bindingResult.getNumbersOfUnassignedPositiveBodyAtoms().isEmpty());
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
			temporaryAssignment.assign(b, ThriceTruth.TRUE);
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

}
