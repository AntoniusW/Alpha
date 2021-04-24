/**
 * Copyright (c) 2016-2018, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.core.grounder;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.rules.Head;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.literals.Literals;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.atoms.RuleAtom;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.rules.CompiledRule;
import at.ac.tuwien.kr.alpha.core.rules.InternalRule;
import at.ac.tuwien.kr.alpha.core.rules.NormalRuleImpl;
import at.ac.tuwien.kr.alpha.core.util.Substitutions;
import at.ac.tuwien.kr.alpha.test.util.SubstitutionTestUtil;

public class SubstitutionTest {
	private static final ProgramParser PARSER = new ProgramParserImpl();

	private static final ConstantTerm<String> A = Terms.newSymbolicConstant("a");
	private static final ConstantTerm<String> B = Terms.newSymbolicConstant("b");
	private static final ConstantTerm<String> C = Terms.newSymbolicConstant("c");

	private static final VariableTerm X = Terms.newVariable("X");
	private static final VariableTerm Y = Terms.newVariable("Y");
	private static final BasicAtom PX = Atoms.newBasicAtom(Predicates.getPredicate("p", 1), X);
	private static final BasicAtom PY = Atoms.newBasicAtom(Predicates.getPredicate("p", 1), Y);
	private static final Instance PA = new Instance(A);
	private static final Instance PB = new Instance(B);

	@Test
	public void putSimpleBinding() {
		Substitution substitution = new BasicSubstitution();
		substitution.put(Y, A);
		assertEquals(A, substitution.eval(Y));
	}

	@Test
	public void specializeTermsSimpleBinding() {
		Substitution substitution = BasicSubstitution.specializeSubstitution(PY, PA, BasicSubstitution.EMPTY_SUBSTITUTION);
		assertEquals(A, substitution.eval(Y));
	}

	@Test
	public void specializeTermsFunctionTermBinding() {
		Substitution substitution = new BasicSubstitution();
		substitution.put(Y, A);

		FunctionTerm groundFunctionTerm = Terms.newFunctionTerm("f", B, C);
		Instance qfBC = new Instance(groundFunctionTerm);
		Term nongroundFunctionTerm = Terms.newFunctionTerm("f", B, X);
		BasicAtom qfBX = Atoms.newBasicAtom(Predicates.getPredicate("q", 2), nongroundFunctionTerm);

		Substitution substitution1 = BasicSubstitution.specializeSubstitution(qfBX, qfBC, substitution);

		assertEquals(C, substitution1.eval(X));
		assertEquals(A, substitution1.eval(Y));
	}

	@Test
	public void substitutePositiveBasicAtom() {
		substituteBasicAtomLiteral(false);
	}

	@Test
	public void substituteNegativeBasicAtom() {
		substituteBasicAtomLiteral(true);
	}

	@Test
	public void groundAndPrintRule() {
		Rule<Head> rule = PARSER.parse("x :- p(X,Y), not q(X,Y).").getRules().get(0);
		CompiledRule nonGroundRule = InternalRule.fromNormalRule(NormalRuleImpl.fromBasicRule(rule));
		Substitution substitution1 = BasicSubstitution.specializeSubstitution(PX, PA, BasicSubstitution.EMPTY_SUBSTITUTION);
		Substitution substitution2 = BasicSubstitution.specializeSubstitution(PY, PB, substitution1);
		String printedString = SubstitutionTestUtil.groundAndPrintRule(nonGroundRule, substitution2);
		assertEquals("x :- p(a, b), not q(a, b).", printedString);
	}

	@Test
	public void specializeBasicAtom() {
		Predicate p = Predicates.getPredicate("p", 2);
		BasicAtom atom = Atoms.newBasicAtom(p, Arrays.asList(X, Y));
		Instance instance = new Instance(A, B);
		Substitution substitution = BasicSubstitution.specializeSubstitution(atom, instance, BasicSubstitution.EMPTY_SUBSTITUTION);
		Atom substituted = atom.substitute(substitution);
		assertEquals(p, substituted.getPredicate());
		assertEquals(A, substituted.getTerms().get(0));
		assertEquals(B, substituted.getTerms().get(1));
	}

	private void substituteBasicAtomLiteral(boolean negated) {
		Predicate p = Predicates.getPredicate("p", 2);
		BasicAtom atom = Atoms.newBasicAtom(p, Arrays.asList(X, Y));
		Literal literal = Literals.fromAtom(atom, !negated);
		Substitution substitution = new BasicSubstitution();
		substitution.put(X, A);
		substitution.put(Y, B);
		literal = literal.substitute(substitution);
		assertEquals(p, literal.getPredicate());
		assertEquals(A, literal.getTerms().get(0));
		assertEquals(B, literal.getTerms().get(1));
		assertEquals(negated, literal.isNegated());
	}

	@Test
	public void groundLiteralToString_PositiveBasicAtom() {
		groundLiteralToString(false);
	}

	@Test
	public void groundLiteralToString_NegativeBasicAtom() {
		groundLiteralToString(true);
	}

	private void groundLiteralToString(boolean negated) {
		Predicate p = Predicates.getPredicate("p", 2);
		BasicAtom atom = Atoms.newBasicAtom(p, Arrays.asList(X, Y));
		Substitution substitution1 = BasicSubstitution.specializeSubstitution(PX, PA, BasicSubstitution.EMPTY_SUBSTITUTION);
		Substitution substitution = BasicSubstitution.specializeSubstitution(PY, PB, substitution1);
		String printedString = SubstitutionTestUtil.groundLiteralToString(atom.toLiteral(!negated), substitution, true);
		assertEquals((negated ? "not " : "") + "p(a, b)", printedString);
	}

	@Test
	public void substitutionFromString() {
		Rule<Head> rule = PARSER.parse("x :- p(X,Y), not q(X,Y).").getRules().get(0);
		CompiledRule nonGroundRule = InternalRule.fromNormalRule(NormalRuleImpl.fromBasicRule(rule));
		Substitution substitution1 = BasicSubstitution.specializeSubstitution(PX, PA, BasicSubstitution.EMPTY_SUBSTITUTION);
		Substitution substitution = BasicSubstitution.specializeSubstitution(PY, PB, substitution1);
		RuleAtom ruleAtom = new RuleAtom(nonGroundRule, substitution);
		String substitutionString = (String) ((ConstantTerm<?>) ruleAtom.getTerms().get(1)).getObject();
		Substitution fromString = Substitutions.fromString(substitutionString);
		assertEquals(substitution, fromString);
	}
}
