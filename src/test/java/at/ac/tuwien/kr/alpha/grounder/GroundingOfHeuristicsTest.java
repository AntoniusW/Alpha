/*
 * Copyright (c) 2018-2021, the Alpha Team.
 * All rights reserved.
 *
 * Additional changes made by Siemens.
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

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.InternalProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.common.rule.InternalRule;
import at.ac.tuwien.kr.alpha.grounder.atoms.HeuristicAtom;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfigurationBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static at.ac.tuwien.kr.alpha.Util.asSet;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link NaiveGrounder} as it grounds heuristic rules.
 */
public class GroundingOfHeuristicsTest {
	private static final ProgramParser PROGRAM_PARSER = new ProgramParser();
	private final HeuristicsConfiguration heuristicsConfiguration = new HeuristicsConfigurationBuilder().setRespectDomspecHeuristics(true).build();

	@Before
	public void resetIdGenerator() {
		ChoiceRecorder.ID_GENERATOR.resetGenerator();
	}

	@Before
	public void resetRuleIdGenerator() {
		InternalRule.resetIdGenerator();
	}

	@Test
	public void testGenerateHeuristicNoGoods_GeneralCase() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("{ a(0); a(1); a(2); a(3); a(4); a(5) }."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic b(1) : T a(0), MT a(1), F a(2), not T a(3), not MT a(4), not F a(5). [3@2]");
		final Set<String> expectedNoGoodsToString = asSet(
				"*{-(HeuOn(\"0\", \"t\")), +(a(0))}",
				"*{-(HeuOn(\"0\", \"tm\")), +(a(1))}",
				"*{-(HeuOn(\"0\", \"f\")), -(a(2))}",
				"*{-(HeuOff(\"0\", \"t\")), +(a(3))}",
				"*{-(HeuOff(\"0\", \"tm\")), +(a(4))}",
				"*{-(HeuOff(\"0\", \"f\")), -(a(5))}"
		);
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfFacts_SkipLiteralPositiveT() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), T fact(1).");
		final Set<String> expectedNoGoodsToString = asSet(
				"*{-(HeuOn(\"0\", \"tm\")), +(guess(1))}"
		);
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfFacts_SkipLiteralPositiveM() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), T fact(1).");
		final Set<String> expectedNoGoodsToString = asSet(
				"*{-(HeuOn(\"0\", \"tm\")), +(guess(1))}"
		);
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfFacts_SkipLiteralPositiveMT() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), MT fact(1).");
		final Set<String> expectedNoGoodsToString = asSet(
				"*{-(HeuOn(\"0\", \"tm\")), +(guess(1))}"
		);
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfFacts_SkipRulePositiveF() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), F fact(1).");
		final Set<String> expectedNoGoodsToString = Collections.emptySet();
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfFacts_SkipRuleNegativeT() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), not T fact(1).");
		final Set<String> expectedNoGoodsToString = Collections.emptySet();
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfFacts_SkipRuleNegativeM() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), not T fact(1).");
		final Set<String> expectedNoGoodsToString = Collections.emptySet();
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfFacts_SkipRuleNegativeTM() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), not TM fact(1).");
		final Set<String> expectedNoGoodsToString = Collections.emptySet();
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfFacts_SkipLiteralNegativeF() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), not F fact(1).");
		final Set<String> expectedNoGoodsToString = asSet(
				"*{-(HeuOn(\"0\", \"tm\")), +(guess(1))}"
		);
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfUnderivableAtoms_SkipRulePositiveT() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), T fact(0).");
		final Set<String> expectedNoGoodsToString = Collections.emptySet();
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfUnderivableAtoms_SkipRulePositiveM() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), M fact(0).");
		final Set<String> expectedNoGoodsToString = Collections.emptySet();
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfUnderivableAtoms_SkipRulePositiveTM() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), TM fact(0).");
		final Set<String> expectedNoGoodsToString = Collections.emptySet();
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfUnderivableAtoms_SkipLiteralPositiveF() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), F fact(0).");
		final Set<String> expectedNoGoodsToString = asSet(
				"*{-(HeuOn(\"0\", \"tm\")), +(guess(1))}"
		);
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfUnderivableAtoms_SkipLiteralNegativeT() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), not T fact(0).");
		final Set<String> expectedNoGoodsToString = asSet(
				"*{-(HeuOn(\"0\", \"tm\")), +(guess(1))}"
		);
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfUnderivableAtoms_SkipLiteralNegativeM() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), not T fact(0).");
		final Set<String> expectedNoGoodsToString = asSet(
				"*{-(HeuOn(\"0\", \"tm\")), +(guess(1))}"
		);
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfUnderivableAtoms_SkipLiteralNegativeTM() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), not TM fact(0).");
		final Set<String> expectedNoGoodsToString = asSet(
				"*{-(HeuOn(\"0\", \"tm\")), +(guess(1))}"
		);
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	@Test
	public void testGenerateHeuristicNoGoods_HandlingOfUnderivableAtoms_SkipRuleNegativeF() {
		final InputProgram inputProgram = PROGRAM_PARSER.parse("fact(1). fact(2)."
				+ "{ guess(N) } :- fact(N)."
				+ "#heuristic guess(2) : guess(1), not F fact(0).");
		final Set<String> expectedNoGoodsToString = Collections.emptySet();
		testGenerateHeuristicNoGoods(inputProgram, expectedNoGoodsToString);
	}

	private void testGenerateHeuristicNoGoods(InputProgram inputProgram, Set<String> expectedNoGoodsToString) {
		final Alpha system = new Alpha();
		final NormalProgram normalProgram = system.normalizeProgram(inputProgram);
		final InternalProgram internalProgram = InternalProgram.fromNormalProgram(normalProgram);

		final AtomStore atomStore = new AtomStoreImpl();
		final Grounder grounder = GrounderFactory.getInstance("naive", internalProgram, atomStore, heuristicsConfiguration, true);
		final NoGoodGenerator noGoodGenerator = ((NaiveGrounder)grounder).noGoodGenerator;
		final InternalRule rule = findHeuristicRule(internalProgram.getRules());
		assert rule != null;
		final Set<NoGood> generatedNoGoods = new HashSet<>(noGoodGenerator.generateNoGoodsFromGroundSubstitution(rule, new Substitution()));
		assertEquals(expectedNoGoodsToString.size(), generatedNoGoods.size());
		final Set<String> noGoodsToString = generatedNoGoods.stream().map(atomStore::noGoodToString).collect(Collectors.toSet());
		assertEquals(expectedNoGoodsToString, noGoodsToString);
	}

	private InternalRule findHeuristicRule(List<InternalRule> rules) {
		for (InternalRule rule : rules) {
			if (rule.getHead().getAtom() instanceof HeuristicAtom) {
				return rule;
			}
		}
		return null;
	}

}
