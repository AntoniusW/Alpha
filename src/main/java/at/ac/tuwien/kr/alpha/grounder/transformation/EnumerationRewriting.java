package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.EnumerationDirective;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.grounder.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static at.ac.tuwien.kr.alpha.Util.oops;

/**
 * Rewrites the ordinary atom whose name is given in the input program by the enumeration directive #enum_atom_is into
 * the special EnumerationAtom.
 *
 * Copyright (c) 2017-2020, the Alpha Team.
 */
public class EnumerationRewriting extends ProgramTransformation<InputProgram, InputProgram> {

	@Override
	public InputProgram apply(InputProgram inputProgram) {
		// Read enumeration predicate from directive.
		EnumerationDirective enumDirective = (EnumerationDirective)inputProgram.getInlineDirectives().getDirectiveValue(InlineDirectives.DIRECTIVE.enum_predicate_is);
		if (enumDirective == null) {
			// Directive not set, nothing to rewrite.
			return inputProgram;
		}
		Predicate enumPredicate = Predicate.getInstance(enumDirective.getValue(), 3);

		InputProgram.Builder programBuilder = InputProgram.builder().addInlineDirectives(inputProgram.getInlineDirectives());

		checkFactsAreEnumerationFree(inputProgram.getFacts(), enumPredicate);
		programBuilder.addFacts(inputProgram.getFacts());

		List<BasicRule> srcRules = new ArrayList<>(inputProgram.getRules());
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

	private List<BasicRule> rewriteRules(List<BasicRule> srcRules, Predicate enumPredicate) {
		List<BasicRule> rewrittenRules = new ArrayList<>();
		for (BasicRule rule : srcRules) {
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
				rit.remove();
				rewrittenLiterals.add(new EnumerationAtom(basicLiteral.getAtom().getTerms()).toLiteral());
			}
			modifiedBodyLiterals.addAll(rewrittenLiterals);
			rewrittenRules.add(new BasicRule(rule.getHead(), modifiedBodyLiterals));
		}
		return rewrittenRules;
	}

}
