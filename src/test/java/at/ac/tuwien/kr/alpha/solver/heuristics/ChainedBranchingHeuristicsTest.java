/**
 * Copyright (c) 2019 Siemens AG
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

import at.ac.tuwien.kr.alpha.common.*;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.solver.NaiveNoGoodStore;
import at.ac.tuwien.kr.alpha.solver.TrailAssignment;
import at.ac.tuwien.kr.alpha.solver.WritableAssignment;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link ChainedBranchingHeuristics}.
 */
public class ChainedBranchingHeuristicsTest {

	private static final Atom ATOM_A0 = new BasicAtom(Predicate.getInstance("a", 1), ConstantTerm.getInstance(0));

	private ChainedBranchingHeuristics chainedBranchingHeuristics;
	private int idOfA0;

	@Before
	public void setUp() {
		final AtomStore atomStore = new AtomStoreImpl();
		AtomStoreTest.fillAtomStore(atomStore, 1);
		idOfA0 = atomStore.get(ATOM_A0);
		final WritableAssignment assignment = new TrailAssignment(atomStore);
		assignment.growForMaxAtomId();
		final PseudoChoiceManager choiceManager = new PseudoChoiceManager(assignment, new NaiveNoGoodStore(assignment));
		final BerkMin berkMin = new BerkMin(assignment, choiceManager, new Random(0)); // VSIDS does not support choosing from a set of admissible choices yet
		berkMin.newNoGood(new NoGood(Literals.atomToLiteral(idOfA0, false)));
		final NaiveHeuristic naive = new NaiveHeuristic(choiceManager);
		this.chainedBranchingHeuristics = ChainedBranchingHeuristics.chainOf(berkMin, naive);
	}

	@Test
	public void testChooseAtom() {
		final int chosenAtom = chainedBranchingHeuristics.chooseAtom();
		assertEquals(idOfA0, chosenAtom);
		checkNumberOfDecisions(chainedBranchingHeuristics);
	}

	@Test
	public void testChooseAdmissibleAtom() {
		Set<Integer> admissibleChoices = new HashSet<>();
		admissibleChoices.add(idOfA0);
		final int chosenAtom = chainedBranchingHeuristics.chooseAtom(admissibleChoices);
		assertEquals(idOfA0, chosenAtom);
		checkNumberOfDecisions(chainedBranchingHeuristics);
	}

	@Test
	public void testChooseLiteral() {
		final int chosenAtom = Literals.atomOf(chainedBranchingHeuristics.chooseLiteral());
		assertEquals(idOfA0, chosenAtom);
		checkNumberOfDecisions(chainedBranchingHeuristics);
	}

	@Test
	public void testChooseAdmissibleLiteral() {
		Set<Integer> admissibleChoices = new HashSet<>();
		admissibleChoices.add(idOfA0);
		final int chosenAtom = Literals.atomOf(chainedBranchingHeuristics.chooseLiteral(admissibleChoices));
		assertEquals(idOfA0, chosenAtom);
		checkNumberOfDecisions(chainedBranchingHeuristics);
	}

	private void checkNumberOfDecisions(ChainedBranchingHeuristics chainedBranchingHeuristics) {
		for (Map.Entry<BranchingHeuristic, Integer> heuristicToNumberOfDecisions : chainedBranchingHeuristics.getNumberOfDecisions().entrySet()) {
			final int numberOfDecisions = heuristicToNumberOfDecisions.getValue();
			if (heuristicToNumberOfDecisions.getKey() instanceof BerkMin) {
				assertEquals(1, numberOfDecisions);
			} else {
				assertEquals(0, numberOfDecisions);
			}
		}
	}

}
