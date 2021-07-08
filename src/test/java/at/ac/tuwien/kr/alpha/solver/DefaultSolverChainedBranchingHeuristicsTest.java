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
package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.solver.heuristics.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Tests the creation of {@link ChainedBranchingHeuristics} by {@link DefaultSolver}.
 */
public class DefaultSolverChainedBranchingHeuristicsTest extends AbstractSolverTests {

	private static final String LS = System.lineSeparator();
	private static final BasicAtom ATOM_A = new BasicAtom(Predicate.getInstance("a", 0));

	private Solver solver;

	@Before
	public void setUp() {
		final String testProgram = "a :- not b. " + LS +
				"b :- not a. " + LS +
				"c :- not d. " + LS +
				"d :- not c. " + LS +
				"#heuristic a : not b.";
		this.solver = getInstance(testProgram);
	}

	@Test
	public void testChainOfBranchingHeuristics() {
		assumeTrue(solver instanceof DefaultSolver);
		assumeTrue(heuristicsConfiguration.isRespectDomspecHeuristics());
		final DefaultSolver defaultSolver = (DefaultSolver) solver;
		assertTrue(defaultSolver.branchingHeuristic instanceof ChainedBranchingHeuristics);
		final Class heuristicsClass;
		switch (heuristicsConfiguration.getHeuristic()) {
			case NAIVE:
				heuristicsClass = NaiveHeuristic.class; break;
			case BERKMIN:
				heuristicsClass = BerkMin.class; break;
			case BERKMINLITERAL:
				heuristicsClass = BerkMinLiteral.class; break;
			case DD:
			case DD_SUM:
			case DD_AVG:
			case DD_MAX:
			case DD_MIN:
				heuristicsClass = DependencyDrivenHeuristic.class; break;
			case DD_PYRO:
				heuristicsClass = DependencyDrivenPyroHeuristic.class; break;
			case GDD:
			case GDD_MAX:
			case GDD_SUM:
			case GDD_AVG:
			case GDD_MIN:
				heuristicsClass = GeneralizedDependencyDrivenHeuristic.class; break;
			case GDD_PYRO:
				heuristicsClass = GeneralizedDependencyDrivenPyroHeuristic.class; break;
			case ALPHA_ACTIVE_RULE:
				heuristicsClass = AlphaActiveRuleHeuristic.class; break;
			case VSIDS:
				heuristicsClass = VSIDS.class; break;
			case GDD_VSIDS:
				heuristicsClass = DependencyDrivenVSIDS.class; break;
			default:
				throw new IllegalArgumentException("Unknown branching heuristic: " + heuristicsConfiguration.getHeuristic());
		}
		if (heuristicsClass == NaiveHeuristic.class) {
			assertEquals("ChainedBranchingHeuristics[DomainSpecific, " + heuristicsClass.getSimpleName() + "]", defaultSolver.branchingHeuristic.toString());
		} else {
			assertEquals("ChainedBranchingHeuristics[DomainSpecific, " + heuristicsClass.getSimpleName() + ", NaiveHeuristic]", defaultSolver.branchingHeuristic.toString());
		}
	}

}
