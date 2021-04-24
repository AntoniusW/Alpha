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
package at.ac.tuwien.kr.alpha.core.solver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.programs.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.comparisons.ComparisonOperators;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.atoms.ChoiceAtom;
import at.ac.tuwien.kr.alpha.core.atoms.RuleAtom;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.rules.CompiledRule;
import at.ac.tuwien.kr.alpha.core.rules.InternalRule;
import at.ac.tuwien.kr.alpha.core.rules.heads.NormalHeadImpl;

public class AtomCounterTests {

	private AtomStore atomStore;

	@Before
	public void setUp() {
		this.atomStore = new AtomStoreImpl();
	}

	@Test
	public void testGetNumberOfAtoms() throws NoSuchMethodException {
		final AtomCounter atomCounter = atomStore.getAtomCounter();

		expectGetNumberOfAtoms(atomCounter, "BasicAtomImpl", 0);
		expectGetNumberOfAtoms(atomCounter, "AggregateAtomImpl", 0);
		expectGetNumberOfAtoms(atomCounter, "ChoiceAtom", 0);
		expectGetNumberOfAtoms(atomCounter, "RuleAtom", 0);

		createBasicAtom1();
		createBasicAtom2();
		createAggregateAtom();
		createChoiceAtom();
		createRuleAtom();

		expectGetNumberOfAtoms(atomCounter, "BasicAtomImpl", 2);
		expectGetNumberOfAtoms(atomCounter, "AggregateAtomImpl", 1);
		expectGetNumberOfAtoms(atomCounter, "ChoiceAtom", 1);
		expectGetNumberOfAtoms(atomCounter, "RuleAtom", 1);
	}

	@Test
	public void testGetStatsByType() throws NoSuchMethodException {
		final AtomCounter atomCounter = atomStore.getAtomCounter();

		createBasicAtom1();
		createBasicAtom2();
		createAggregateAtom();
		createChoiceAtom();
		createRuleAtom();

		expectGetStatsByType(atomCounter, "BasicAtomImpl", 2);
		expectGetStatsByType(atomCounter, "AggregateAtomImpl", 1);
		expectGetStatsByType(atomCounter, "ChoiceAtom", 1);
		expectGetStatsByType(atomCounter, "RuleAtom", 1);
	}

	private void createBasicAtom1() {
		atomStore.putIfAbsent(Atoms.newBasicAtom(Predicates.getPredicate("p", 0)));
	}

	private void createBasicAtom2() {
		atomStore.putIfAbsent(Atoms.newBasicAtom(Predicates.getPredicate("q", 1), Terms.newConstant(1)));
	}

	private void createAggregateAtom() {
		final ConstantTerm<Integer> c1 = Terms.newConstant(1);
		final ConstantTerm<Integer> c2 = Terms.newConstant(2);
		final ConstantTerm<Integer> c3 = Terms.newConstant(3);
		List<Term> basicTerms = Arrays.asList(c1, c2, c3);
		AggregateAtom.AggregateElement aggregateElement = Atoms.newAggregateElement(basicTerms,
				Collections.singletonList(Atoms.newBasicAtom(Predicates.getPredicate("p", 3), c1, c2, c3).toLiteral()));
		atomStore.putIfAbsent(Atoms.newAggregateAtom(ComparisonOperators.LE, c1, null, null, AggregateAtom.AggregateFunction.COUNT,
				Collections.singletonList(aggregateElement)));
	}

	private void createChoiceAtom() {
		atomStore.putIfAbsent(ChoiceAtom.on(1));
	}

	private void createRuleAtom() {
		Atom atomAA = Atoms.newBasicAtom(Predicates.getPredicate("aa", 0));
		CompiledRule ruleAA = new InternalRule(new NormalHeadImpl(atomAA),
				Collections.singletonList(Atoms.newBasicAtom(Predicates.getPredicate("bb", 0)).toLiteral(false)));
		atomStore.putIfAbsent(new RuleAtom(ruleAA, new BasicSubstitution()));
	}

	private void expectGetNumberOfAtoms(AtomCounter atomCounter, String classOfAtoms, int expectedNumber) {
		assertEquals("Unexpected number of " + classOfAtoms + "s", expectedNumber, atomCounter.getNumberOfAtoms(classOfAtoms));
	}

	private void expectGetStatsByType(AtomCounter atomCounter, String classOfAtoms, int expectedNumber) {
		assertTrue("Expected number of " + classOfAtoms + "s not contained in stats string",
				atomCounter.getStatsByType().contains(classOfAtoms + ": " + expectedNumber));
	}

}
