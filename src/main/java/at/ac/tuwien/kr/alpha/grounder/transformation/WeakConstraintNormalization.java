package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.WeakConstraint;
import at.ac.tuwien.kr.alpha.common.rule.head.Head;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.grounder.atoms.WeakConstraintAtom;

import java.util.ArrayList;

/**
 * Rewrites weak constraints into normal rules with special head atom.
 *
 * Copyright (c) 2020, the Alpha Team.
 */
public class WeakConstraintNormalization extends ProgramTransformation<InputProgram, InputProgram> {
	@Override
	public InputProgram apply(InputProgram inputProgram) {
		InputProgram.Builder builder = new InputProgram.Builder();
		builder.addFacts(inputProgram.getFacts());
		builder.addInlineDirectives(inputProgram.getInlineDirectives());

		for (BasicRule rule : inputProgram.getRules()) {
			if (!(rule instanceof WeakConstraint)) {
				builder.addRule(rule);
				continue;
			}
			// Create rule with special head atom.
			WeakConstraint weakConstraint = (WeakConstraint)rule;
			Head weakConstraintHead = new NormalHead(WeakConstraintAtom.getInstance(weakConstraint.getWeight(), weakConstraint.getLevel(), weakConstraint.getTermList()));
			BasicRule rewrittenWeakConstraint = new BasicRule(weakConstraintHead, new ArrayList<>(rule.getBody()));
			builder.addRule(rewrittenWeakConstraint);
		}
		return builder.build();
	}
}
