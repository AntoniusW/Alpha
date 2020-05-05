package at.ac.tuwien.kr.alpha.grounder.transformation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.common.DisjunctiveHead;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.Program;
import at.ac.tuwien.kr.alpha.common.Rule;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;

/**
 * Intended for use on verifiy programs of test cases - transforms all
 * constraints in the input program to assertion errors according to the
 * specification in the
 * <a href="https://github.com/alpha-asp/Alpha/issues/237">corresponding github
 * issue</a>.
 * Every constraint is transformed to a rule deriving "assertion_error" with an
 * arity that is one greater than the number of variables in the constraint
 * body.
 * Example:
 * 
 * <pre>
 * :- p(X), q(Y), not r(X, Y).
 * gets transformed into:
 * assertion_error(X, Y, ":- p(X), q(Y), not r(X, Y).") :- p(X), q(Y), not r(X, Y).
 * </pre>
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
// FIXME extend refactored version of ProgramTransformation as soon as PR #207 gets merged!
public class ConstraintsToAssertionErrors {

	public static final String ASSERTION_ERROR = "assertion_error";

	public Program apply(Program inputProgram) {
		List<Rule> inputRules = inputProgram.getRules();
		List<Rule> outputRules = new ArrayList<>();
		for (Rule r : inputRules) {
			if (r.isConstraint()) {
				outputRules.add(this.transformConstraint(r));
			} else {
				outputRules.add(r);
			}
		}
		return new Program(outputRules, inputProgram.getFacts(), inputProgram.getInlineDirectives());
	}

	private Rule transformConstraint(Rule constraint) {
		Set<VariableTerm> bodyVariables = new LinkedHashSet<>();
		for (Literal lit : constraint.getBody()) {
			lit.getOccurringVariables().forEach(bodyVariables::add);
		}
		return this.buildAssertionErrorRule(bodyVariables, constraint.toString(), constraint.getBody());
	}

	private Rule buildAssertionErrorRule(Set<VariableTerm> headVars, String constraint, List<Literal> constraintBody) {
		List<Term> headTerms = new ArrayList<>(headVars);
		headTerms.add(ConstantTerm.getInstance(constraint));
		Atom assertionErrorHead = new BasicAtom(Predicate.getInstance(ASSERTION_ERROR, headTerms.size()), headTerms);
		List<Atom> headAtoms = new ArrayList<>();
		headAtoms.add(assertionErrorHead);
		return new Rule(new DisjunctiveHead(headAtoms), constraintBody);
	}

}
