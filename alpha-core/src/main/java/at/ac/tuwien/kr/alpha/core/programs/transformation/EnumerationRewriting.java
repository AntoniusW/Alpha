package at.ac.tuwien.kr.alpha.core.programs.transformation;

import static at.ac.tuwien.kr.alpha.api.Util.oops;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.BasicLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.rules.Head;
import at.ac.tuwien.kr.alpha.api.rules.NormalHead;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.core.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.core.parser.InlineDirectivesImpl;
import at.ac.tuwien.kr.alpha.core.programs.InputProgram;
import at.ac.tuwien.kr.alpha.core.rules.BasicRule;

/**
 * Rewrites the ordinary atom whose name is given in the input program by the enumeration directive #enum_atom_is into
 * the special EnumerationAtom.
 *
 * Copyright (c) 2017-2020, the Alpha Team.
 */
// TODO this should happen during/after internalization
public class EnumerationRewriting extends ProgramTransformation<ASPCore2Program, ASPCore2Program> {

	@Override
	public ASPCore2Program apply(ASPCore2Program inputProgram) {
		// Read enumeration predicate from directive.
		String enumDirective = inputProgram.getInlineDirectives().getDirectiveValue(InlineDirectivesImpl.DIRECTIVE.enum_predicate_is);
		if (enumDirective == null) {
			// Directive not set, nothing to rewrite.
			return inputProgram;
		}
		Predicate enumPredicate = Predicates.getPredicate(enumDirective, 3);

		InputProgram.Builder programBuilder = InputProgram.builder().addInlineDirectives(inputProgram.getInlineDirectives());

		checkFactsAreEnumerationFree(inputProgram.getFacts(), enumPredicate);
		programBuilder.addFacts(inputProgram.getFacts());

		List<Rule<Head>> srcRules = new ArrayList<>(inputProgram.getRules());
		programBuilder.addRules(rewriteRules(srcRules, enumPredicate));
		return programBuilder.build();
	}

	private void checkFactsAreEnumerationFree(List<Atom> srcFacts, Predicate enumPredicate) {
		for (Atom fact : srcFacts) {
			if (fact.getPredicate().equals(enumPredicate)) {
				throw oops("Atom declared as enumeration atom by directive occurs in a fact: " + fact);
			}
		}
	}

	private List<Rule<Head>> rewriteRules(List<Rule<Head>> srcRules, Predicate enumPredicate) {
		List<Rule<Head>> rewrittenRules = new ArrayList<>();
		for (Rule<Head> rule : srcRules) {
			if (rule.getHead() != null && !(rule.getHead() instanceof NormalHead)) {
				throw oops("Encountered rule whose head is not normal: " + rule);
			}
			if (rule.getHead() != null && ((NormalHead) rule.getHead()).getAtom().getPredicate().equals(enumPredicate)) {
				throw oops("Atom declared as enumeration atom by directive occurs in head of the rule: " + rule);
			}
			List<Literal> modifiedBodyLiterals = new ArrayList<>(rule.getBody());
			Iterator<Literal> rit = modifiedBodyLiterals.iterator();
			LinkedList<Literal> rewrittenLiterals = new LinkedList<>();
			while (rit.hasNext()) {
				Literal literal = rit.next();
				if (!(literal instanceof BasicLiteral)) {
					continue;
				}
				BasicLiteral basicLiteral = (BasicLiteral) literal;
				if (!basicLiteral.getPredicate().equals(enumPredicate)) {
					continue;
				}
				// basicLiteral is an enumeration literal (i.e. predicate is marked as enum using directive)
				rit.remove();
				Term enumIdTerm = basicLiteral.getAtom().getTerms().get(0);
				Term valueTerm = basicLiteral.getAtom().getTerms().get(1);
				VariableTerm indexTerm = (VariableTerm) basicLiteral.getAtom().getTerms().get(2);
				rewrittenLiterals.add(new EnumerationAtom(enumIdTerm, valueTerm, indexTerm).toLiteral());
			}
			modifiedBodyLiterals.addAll(rewrittenLiterals);
			rewrittenRules.add(new BasicRule(rule.getHead(), modifiedBodyLiterals));
		}
		return rewrittenRules;
	}

}
