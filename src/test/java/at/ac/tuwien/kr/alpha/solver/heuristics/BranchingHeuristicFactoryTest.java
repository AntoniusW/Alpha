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

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.solver.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link BranchingHeuristicFactory}
 */
public class BranchingHeuristicFactoryTest {

	private final boolean debugInternalChecks = true;
	private ChoiceManager choiceManager;

	@Before
	public void setUp() {
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		NoGoodStore store = new NoGoodStoreAlphaRoaming(assignment, debugInternalChecks);
		this.choiceManager = new ChoiceManager(assignment, store);
	}

	@Test
	public void testChainedHeuristicWithReplay() {
		HeuristicsConfigurationBuilder builder = new HeuristicsConfigurationBuilder().setRespectDomspecHeuristics(false).setHeuristic(BranchingHeuristicFactory.Heuristic.VSIDS).setReplayChoices(Arrays.asList(1, 2, 3));
		BranchingHeuristic branchingHeuristic = BranchingHeuristicFactory.getInstance(builder.build(), null, choiceManager, null);
		assertEquals(ChainedBranchingHeuristics.class, branchingHeuristic.getClass());
		assertTrue("Unexpected type of branchingHeuristic: " + branchingHeuristic.getClass(), branchingHeuristic instanceof ChainedBranchingHeuristics);
		assertEquals(ChainedBranchingHeuristics.class.getSimpleName() + "[" + ReplayHeuristic.class.getSimpleName() + ", " + VSIDS.class.getSimpleName() + "]", branchingHeuristic.toString());
	}

	@Test
	public void testChainedHeuristicWithoutReplay() {
		HeuristicsConfigurationBuilder builder = new HeuristicsConfigurationBuilder().setRespectDomspecHeuristics(false).setHeuristic(BranchingHeuristicFactory.Heuristic.VSIDS).setReplayChoices(null);
		BranchingHeuristic branchingHeuristic = BranchingHeuristicFactory.getInstance(builder.build(), null, choiceManager, null);
		assertEquals(VSIDS.class, branchingHeuristic.getClass());
	}

}
