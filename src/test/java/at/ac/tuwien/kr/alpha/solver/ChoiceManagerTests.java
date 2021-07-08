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
package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.InternalProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.grounder.Grounder;
import at.ac.tuwien.kr.alpha.grounder.NaiveGrounder;
import at.ac.tuwien.kr.alpha.grounder.atoms.RuleAtom;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfigurationBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;
import static org.junit.Assert.assertTrue;

public class ChoiceManagerTests extends AbstractSolverTests {
	private final HeuristicsConfiguration heuristicsConfiguration = new HeuristicsConfigurationBuilder().build();
	private Grounder grounder;
	private ChoiceManager choiceManager;
	private AtomStore atomStore;

	@Before
	public void setUp() {
		Alpha system = new Alpha();
		String testProgram = "h :- b1, b2, not b3, not b4.";
		InputProgram parsedProgram = new ProgramParser().parse(testProgram);
		NormalProgram normalProgram = system.normalizeProgram(parsedProgram);
		InternalProgram internalProgram = InternalProgram.fromNormalProgram(normalProgram);
		atomStore = new AtomStoreImpl();
		grounder = new NaiveGrounder(internalProgram, atomStore, heuristicsConfiguration, true);
		WritableAssignment assignment = new TrailAssignment(atomStore);
		NoGoodStore store = new NoGoodStoreAlphaRoaming(assignment);
		choiceManager = new ChoiceManager(assignment, store);
	}

	@Test
	public void testIsAtomChoice() {
		Collection<NoGood> noGoods = getNoGoods();
		choiceManager.addChoiceInformation(grounder.getChoiceAtoms(), grounder.getHeadsToBodies());
		for (NoGood noGood : noGoods) {
			for (Integer literal : noGood) {
				int atom = atomOf(literal);
				String atomToString = atomStore.atomToString(atom);
				if (atomToString.startsWith(RuleAtom.PREDICATE.getName())) {
					assertTrue("Atom not choice: " + atomToString, choiceManager.isAtomChoice(atom));
				}
			}
		}
	}

	private Collection<NoGood> getNoGoods() {
		return grounder.getNoGoods(null).values();
	}
}
