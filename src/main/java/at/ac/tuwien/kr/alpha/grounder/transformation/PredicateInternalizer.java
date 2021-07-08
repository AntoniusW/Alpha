package at.ac.tuwien.kr.alpha.grounder.transformation;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.head.Head;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;

/**
 *
 * Rewrites all predicates of a given Program such that they are internal and hence hidden from answer sets.
 *
 * Copyright (c) 2018-2020, the Alpha Team.
 */
public class PredicateInternalizer {


	public static InputProgram makePrefixedPredicatesInternal(InputProgram program, String prefix) {
		InputProgram.Builder prgBuilder = InputProgram.builder();
		for (Atom atom : program.getFacts()) {
			if (atom.getPredicate().getName().startsWith(prefix)) {
				prgBuilder.addFact(PredicateInternalizer.makePredicateInternal(atom));
			} else {
				prgBuilder.addFact(atom);
			}
		}
		for (BasicRule rule : program.getRules()) {
			prgBuilder.addRule(PredicateInternalizer.makePrefixedPredicatesInternal(rule, prefix));
		}
		prgBuilder.addInlineDirectives(program.getInlineDirectives());
		return prgBuilder.build();
	}

	public static BasicRule makePrefixedPredicatesInternal(BasicRule rule, String prefix) {
		Head newHead = null;
		if (rule.getHead() != null) {
			if (!(rule.getHead() instanceof NormalHead)) {
				throw new UnsupportedOperationException("Cannot make predicates in rules internal whose head is not normal.");
			}
			NormalHead head = (NormalHead) rule.getHead();
			if (head.getAtom().getPredicate().getName().startsWith(prefix)) {
				newHead = new NormalHead(makePredicateInternal(head.getAtom()));
			} else {
				newHead = head;
			}
		}
		List<Literal> newBody = new ArrayList<>();
		for (Literal bodyElement : rule.getBody()) {
			// Only rewrite BasicAtoms.
			if (bodyElement instanceof BasicLiteral) {
				if (bodyElement.getAtom().getPredicate().getName().startsWith(prefix)) {
					newBody.add(makePredicateInternal(bodyElement.getAtom()).toLiteral(!bodyElement.isNegated()));
				} else {
					newBody.add(bodyElement);
				}
			} else {
				// Keep other body element as is.
				newBody.add(bodyElement);
			}
		}
		return new BasicRule(newHead, newBody);
	}

	private static Atom makePredicateInternal(Atom atom) {
		Predicate newInternalPredicate = Predicate.getInstance(atom.getPredicate().getName(), atom.getPredicate().getArity(), true);
		return new BasicAtom(newInternalPredicate, atom.getTerms());
	}
}
