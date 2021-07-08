/*
 * Copyright (c) 2020-2021 Siemens AG
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
package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.Directive;
import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramPartParser;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.Util.asSet;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests {@link SignSetTransformation}.
 */
public class SignSetTransformationTest {
	private final ProgramParser parser = new ProgramParser();

	@Test
	public void testPositiveAnySignCondition() {
		InputProgram program = parser.parse("a(1)."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic b(N) : FMT a(N). [N@2]");

		final Collection<Directive> directivesBeforeTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(1, directivesBeforeTransformation.size());
		final HeuristicDirective originalHeuristicDirective = (HeuristicDirective) directivesBeforeTransformation.iterator().next();
		program = new SignSetTransformation().apply(program);
		final Collection<Directive> directivesAfterTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(2, directivesAfterTransformation.size());
		final Set<Set<ThriceTruth>> newSignSets = new HashSet<>();
		for (Directive directive : directivesAfterTransformation) {
			final HeuristicDirective heuristicDirective = (HeuristicDirective) directive;
			assertEquals(originalHeuristicDirective.getHead(), heuristicDirective.getHead());
			assertEquals(originalHeuristicDirective.getWeightAtLevel(), heuristicDirective.getWeightAtLevel());
			assertNotEquals(originalHeuristicDirective.getBody(), heuristicDirective.getBody());
			newSignSets.add(heuristicDirective.getBody().getBodyAtomsPositive().iterator().next().getSigns());
		}
		assertEquals(asSet(asSet(MBT, TRUE), asSet(FALSE)), newSignSets);
	}

	@Test
	public void testNegativeAnySignCondition() {
		InputProgram program = parser.parse("a(1)."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic b(N) : a(N), not FTM b(N). [N@2]");

		final Collection<Directive> directivesBeforeTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(1, directivesBeforeTransformation.size());
		final HeuristicDirective originalHeuristicDirective = (HeuristicDirective) directivesBeforeTransformation.iterator().next();
		program = new SignSetTransformation().apply(program);

		final Collection<Directive> directivesAfterTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(1, directivesAfterTransformation.size());
		final HeuristicDirective heuristicDirective = (HeuristicDirective) directivesAfterTransformation.iterator().next();
		assertEquals(originalHeuristicDirective.getHead(), heuristicDirective.getHead());
		assertEquals(originalHeuristicDirective.getWeightAtLevel(), heuristicDirective.getWeightAtLevel());
		assertEquals(originalHeuristicDirective.getBody().getBodyAtomsPositive(), heuristicDirective.getBody().getBodyAtomsPositive());
		assertNotEquals(originalHeuristicDirective.getBody().getBodyAtomsNegative(), heuristicDirective.getBody().getBodyAtomsNegative());

		assertEquals(2, heuristicDirective.getBody().getBodyAtomsNegative().size());
		assertEquals("TM a(N), not TM b(N), not F b(N)", heuristicDirective.getBody().toString());
	}

	@Test
	public void testMultiplePositiveAnySignConditions() {
		InputProgram program = parser.parse("a(1). a(2)."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic b(N) : FMT a(N), FMT a(Nm1), Nm1=N-1. [N@2]");

		final Collection<Directive> directivesBeforeTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(1, directivesBeforeTransformation.size());
		final HeuristicDirective originalHeuristicDirective = (HeuristicDirective) directivesBeforeTransformation.iterator().next();
		program = new SignSetTransformation().apply(program);
		final Collection<Directive> directivesAfterTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(4, directivesAfterTransformation.size());

		final ProgramPartParser programPartParser = new ProgramPartParser();
		final Collection<Directive> expected = new HashSet<>();
		expected.add(programPartParser.parseHeuristicDirective("#heuristic b(N) : MT a(N), MT a(Nm1), Nm1=N-1. [N@2]"));
		expected.add(programPartParser.parseHeuristicDirective("#heuristic b(N) : MT a(N), F  a(Nm1), Nm1=N-1. [N@2]"));
		expected.add(programPartParser.parseHeuristicDirective("#heuristic b(N) : F  a(N), MT a(Nm1), Nm1=N-1. [N@2]"));
		expected.add(programPartParser.parseHeuristicDirective("#heuristic b(N) : F  a(N), F  a(Nm1), Nm1=N-1. [N@2]"));
		assertEquals(expected, new HashSet<>(directivesAfterTransformation));
	}

	@Test
	public void testMultipleNegativeAnySignConditions() {
		InputProgram program = parser.parse("a(1). a(2)."
				+ "{ b(N) } :- a(N)."
				+ "#heuristic b(N) : a(N), not FTM b(N), not FTM b(Nm1), Nm1=N-1. [N@2]");

		final Collection<Directive> directivesBeforeTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(1, directivesBeforeTransformation.size());
		program = new SignSetTransformation().apply(program);

		final Collection<Directive> directivesAfterTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(1, directivesAfterTransformation.size());
		final HeuristicDirective heuristicDirective = (HeuristicDirective) directivesAfterTransformation.iterator().next();
		assertEquals(4, heuristicDirective.getBody().getBodyAtomsNegative().size());
		assertEquals("TM a(N), Nm1 = N - 1, not TM b(N), not F b(N), not TM b(Nm1), not F b(Nm1)", heuristicDirective.getBody().toString());
	}

	/**
	 * Tests Example 7 from our paper "Domain-Specific Heuristics in Answer Set Programming: A Declarative Non-Monotonic Approach"
	 */
	@Test
	public void testExample7() {
		InputProgram program = parser.parse("#heuristic h : FMT a, not FT b.");

		final Collection<Directive> directivesBeforeTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(1, directivesBeforeTransformation.size());
		program = new SignSetTransformation().apply(program);

		final Collection<Directive> directivesAfterTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(2, directivesAfterTransformation.size());

		final ProgramPartParser programPartParser = new ProgramPartParser();
		final Collection<Directive> expected = new HashSet<>();
		expected.add(programPartParser.parseHeuristicDirective("#heuristic h : F a, not F b, not T b."));
		expected.add(programPartParser.parseHeuristicDirective("#heuristic h : MT a, not F b, not T b."));
		assertEquals(expected, new HashSet<>(directivesAfterTransformation));
	}

	/**
	 * Tests Example 8 from our paper "Domain-Specific Heuristics in Answer Set Programming: A Declarative Non-Monotonic Approach"
	 */
	@Test
	public void testExample8() {
		InputProgram program = parser.parse("#heuristic h : M a, not M b.");

		final Collection<Directive> directivesBeforeTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(1, directivesBeforeTransformation.size());
		program = new SignSetTransformation().apply(program);

		final Collection<Directive> directivesAfterTransformation = program.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		assertEquals(3, directivesAfterTransformation.size());

		final ProgramPartParser programPartParser = new ProgramPartParser();
		final Collection<Directive> expected = new HashSet<>();
		expected.add(programPartParser.parseHeuristicDirective("#heuristic h : MT a, F b, not T a."));
		expected.add(programPartParser.parseHeuristicDirective("#heuristic h : MT a, T b, not T a."));
		expected.add(programPartParser.parseHeuristicDirective("#heuristic h : MT a, not T a, not F b, not MT b."));
		assertEquals(expected, new HashSet<>(directivesAfterTransformation));
	}

}
