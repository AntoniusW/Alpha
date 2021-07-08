/*
 * Copyright (c) 2021 Siemens AG
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

import at.ac.tuwien.kr.alpha.common.WeightAtLevel;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.common.rule.NormalRule;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.atoms.HeuristicAtom;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramPartParser;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfigurationBuilder;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.Util.asSet;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link VariableEqualityRemoval}.
 */
public class VariableEqualityRemovalTest {
	private final ProgramParser parser = new ProgramParser();
	private final ProgramPartParser programPartParser = new ProgramPartParser();
	private final HeuristicsConfiguration heuristicsConfiguration = new HeuristicsConfigurationBuilder().setRespectDomspecHeuristics(true).build();
	
	@Test
	public void testRemoveEqualityOfVariableInHeuristicRules() {
		InputProgram inputProgram = parser.parse("a(1)."
			+ "b(X) :- a(X), not n_b(X)."
			+ "n_b(X) :- a(X), not b(X)."
			+ "#heuristic b(X) : a(X), Y=X. [Y]"
			+ "#heuristic b(X) : a(X), Y=X+1. [Y]");
		inputProgram = new HeuristicDirectiveToRule(heuristicsConfiguration).apply(inputProgram);
		NormalProgram program = NormalProgram.fromInputProgram(inputProgram);
		program = new VariableEqualityRemoval().apply(program);

		final NormalRule expectedRule1 = changeToHeuristicRule(NormalRule.fromBasicRule(programPartParser.parseBasicRule("h(Y, 0, true, b(Y), condpos(tm(a(Y))), condneg()) :- a(Y).")));
		final NormalRule expectedRule2 = changeToHeuristicRule(NormalRule.fromBasicRule(programPartParser.parseBasicRule("h(Y, 0, true, b(X), condpos(tm(a(X))), condneg()) :- a(X), Y=X+1.")));
		final Set<NormalRule> expectedHeuristicRules = asSet(expectedRule1, expectedRule2);

		final Set<NormalRule> actualHeuristicRules = new HashSet<>();
		for (NormalRule rule : program.getRules()) {
			if (rule.isHeuristicRule()) {
				actualHeuristicRules.add(rule);
			}
		}

		assertEquals(expectedHeuristicRules, actualHeuristicRules);
	}

	@Test
	public void testFunctionTermGeneratedFromAffectedAtom() {
		final InputProgram inputProgram = parser.parse("a(1)."
				+ "h(Y) :- a(X), Y=X.");
		NormalProgram program = NormalProgram.fromInputProgram(inputProgram);

		final Set<Literal> body = program.getRules().get(0).getBody();
		Atom atomA = null;
		for (Literal bodyLiteral : body) {
			if (bodyLiteral instanceof BasicLiteral) {
				atomA = bodyLiteral.getAtom();
			}
		}

		final FunctionTerm functionTerm = atomA.toFunctionTerm();
		final String strFunctionTermBeforeTransformation = functionTerm.toString();

		new VariableEqualityRemoval().apply(program);
		final String strFunctionTermAfterTransformation = functionTerm.toString();

		// atom a(X) has changed to a(Y):
		assertEquals("a(Y)", atomA.toString());
		// but the function term must not change, because it may be used in an unrelated rule:
		assertEquals(strFunctionTermBeforeTransformation, strFunctionTermAfterTransformation);
	}

	/**
	 * Changes the given rule to use {@link HeuristicAtom#PREDICATE} as head predicate,
	 * because rules with heads of this internal predicate cannot be parsed.
	 */
	private NormalRule changeToHeuristicRule(NormalRule rule) {
		final Atom head = rule.getHeadAtom();
		final List<Term> terms = head.getTerms();
		final Term weight = terms.get(0);
		final Term level = terms.get(1);
		@SuppressWarnings("unchecked") final ConstantTerm<Boolean> sign = (ConstantTerm<Boolean>) terms.get(2);
		final FunctionTerm headAtom = (FunctionTerm) terms.get(3);
		final FunctionTerm positiveCondition = (FunctionTerm) terms.get(4);
		final FunctionTerm negativeCondition = (FunctionTerm) terms.get(5);
		final NormalHead modifiedHead = new NormalHead(new HeuristicAtom(new WeightAtLevel(weight, level), ThriceTruth.valueOf(sign.getObject()), headAtom, positiveCondition, negativeCondition));
		return new NormalRule(modifiedHead, rule.getBody());
	}

}
