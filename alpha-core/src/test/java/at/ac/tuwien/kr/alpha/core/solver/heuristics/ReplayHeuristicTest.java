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
package at.ac.tuwien.kr.alpha.core.solver.heuristics;

import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomToLiteral;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.core.solver.NoGoodStore;
import at.ac.tuwien.kr.alpha.core.solver.NoGoodStoreAlphaRoaming;
import at.ac.tuwien.kr.alpha.core.solver.TrailAssignment;
import at.ac.tuwien.kr.alpha.core.solver.WritableAssignment;

/**
 * Tests {@link ReplayHeuristic}
 */
public class ReplayHeuristicTest {

	private final boolean debugInternalChecks = true;
	private ChoiceManager choiceManager;

	@Before
	public void setUp() {
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		NoGoodStore store = new NoGoodStoreAlphaRoaming(assignment, debugInternalChecks);
		this.choiceManager = new PseudoChoiceManager(assignment, store);
	}

	@Test
	public void testBasicChoiceSequence() {
		final int literal1 = atomToLiteral(2);
		final int signedAtom1 = 2;

		final int literal2 = atomToLiteral(4, false);
		final int signedAtom2 = -4;

		final int literal3 = atomToLiteral(7);
		final int signedAtom3 = 7;

		final List<Integer> chosenSignedAtoms = Arrays.asList(signedAtom1, signedAtom2, signedAtom3);
		final ReplayHeuristic replayHeuristic = new ReplayHeuristic(chosenSignedAtoms, choiceManager);
		assertEquals(literal1, replayHeuristic.chooseLiteral());
		assertEquals(literal2, replayHeuristic.chooseLiteral());
		assertEquals(literal3, replayHeuristic.chooseLiteral());
	}

}
