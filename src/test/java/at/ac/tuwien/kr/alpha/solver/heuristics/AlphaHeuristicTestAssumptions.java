/**
 * Copyright (c) 2017-2019 Siemens AG
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
package at.ac.tuwien.kr.alpha.solver.heuristics;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.InternalProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.grounder.Grounder;
import at.ac.tuwien.kr.alpha.grounder.NaiveGrounder;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.solver.NaiveNoGoodStore;
import at.ac.tuwien.kr.alpha.solver.TestableChoiceManager;
import at.ac.tuwien.kr.alpha.solver.TrailAssignment;
import at.ac.tuwien.kr.alpha.solver.WritableAssignment;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests assumptions made by {@link DependencyDrivenHeuristic} and other domain-independent heuristics.
 * Even if these test cases do not test {@link DependencyDrivenHeuristic} directly,
 * it will break if these test cases break.
 *
 */
public class AlphaHeuristicTestAssumptions {
	private final HeuristicsConfiguration heuristicsConfiguration = new HeuristicsConfigurationBuilder().build();
	private Grounder grounder;
	private WritableAssignment assignment;
	private TestableChoiceManager choiceManager;
	private AtomStore atomStore;

	@Before
	public void setUp() {
		Alpha system = new Alpha();
		String testProgram = ""
				+ "b1."
				+ "b2."
				+ "{b3}."
				+ "{b4}."
				+ "h :- b1, b2, not b3, not b4.";
		InputProgram parsedProgram = new ProgramParser().parse(testProgram);
		NormalProgram normal = system.normalizeProgram(parsedProgram);
		InternalProgram internalProgram = InternalProgram.fromNormalProgram(normal);
		atomStore = new AtomStoreImpl();
		grounder = new NaiveGrounder(internalProgram, atomStore, heuristicsConfiguration, true);
		assignment = new TrailAssignment(atomStore);
		choiceManager = new TestableChoiceManager(assignment, new NaiveNoGoodStore(assignment));
	}

	@Test
	public void testNumbersOfNoGoods_GrounderIsAtomChoicePoint() {
		testNumbersOfNoGoods(atomStore::isAtomChoicePoint);
	}

	@Test
	public void testNumbersOfNoGoods_ChoiceManagerIsAtomChoice() {
		testNumbersOfNoGoods(choiceManager::isAtomChoice);
	}

	private void testNumbersOfNoGoods(Predicate<? super Integer> isRuleBody) {
		int n = 0;
		int bodyNotHead = 0;
		int bodyElementsNotBody = 0;
		int noHead = 0;
		int other = 0;

		Collection<NoGood> noGoods = getNoGoods();
		assignment.growForMaxAtomId();
		choiceManager.growForMaxAtomId(atomStore.getMaxAtomId());
		choiceManager.addChoiceInformation(grounder.getChoiceAtoms(), grounder.getHeadsToBodies());
		for (NoGood noGood : noGoods) {
			n++;
			boolean knownType = false;
			if (DependencyDrivenHeuristic.isBodyNotHead(noGood, isRuleBody)) {
				bodyNotHead++;
				knownType = true;
			}
			if (DependencyDrivenHeuristic.isBodyElementsNotBody(noGood, isRuleBody)) {
				bodyElementsNotBody++;
				knownType = true;
			}
			if (!noGood.hasHead()) {
				noHead++;
				knownType = true;
			}
			if (!knownType) {
				other++;
			}
		}
		
		System.out.println(noGoods.stream().map(atomStore::noGoodToString).collect(Collectors.joining(", ")));

		assertEquals("Unexpected number of bodyNotHead nogoods", 5, bodyNotHead);
		assertEquals("Unexpected number of bodyElementsNotBody nogoods", 5, bodyElementsNotBody);
		assertGreaterThan("Unexpected number of nogoods without head", 4, noHead);

		// there may be other nogoods (e.g. for ChoiceOn, ChoiceOff) which we do not care for here
		System.out.println("Total number of NoGoods: " + n);
		System.out.println("Number of NoGoods of unknown type: " + other);
	}

	@Test
	public void testIsAtomChoice_GrounderIsAtomChoicePoint() {
		testIsAtomChoice(atomStore::isAtomChoicePoint);
	}

	@Test
	public void testIsAtomChoice_ChoiceManagerIsAtomChoice() {
		testIsAtomChoice(choiceManager::isAtomChoice);
	}

	private void testIsAtomChoice(Predicate<? super Integer> isRuleBody) {
		Collection<NoGood> noGoods = getNoGoods();
		assignment.growForMaxAtomId();
		choiceManager.growForMaxAtomId(atomStore.getMaxAtomId());
		choiceManager.addChoiceInformation(grounder.getChoiceAtoms(), grounder.getHeadsToBodies());
		for (NoGood noGood : noGoods) {
			for (Integer literal : noGood) {
				int atom = atomOf(literal);
				String atomToString = atomStore.atomToString(atom);
				if (atomToString.startsWith("_R_")) {
					assertTrue("Atom not choice: " + atomToString, isRuleBody.test(atom));
				}
			}
		}
	}

	private Collection<NoGood> getNoGoods() {
		return grounder.getNoGoods(null).values();
	}

	private void assertGreaterThan(String message, long expected, long actual) {
		assertTrue(message, actual > expected);
	}
}
