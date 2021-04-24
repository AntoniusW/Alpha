package at.ac.tuwien.kr.alpha.core.rules;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.Util;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.rules.Head;
import at.ac.tuwien.kr.alpha.api.rules.NormalHead;
import at.ac.tuwien.kr.alpha.api.rules.NormalRule;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.core.rules.heads.NormalHeadImpl;

/**
 * A rule that has a normal head, i.e. just one head atom, no disjunction or choice heads allowed.
 * Currently, any constructs such as aggregates, intervals, etc. in the rule body are allowed.
 * 
 * Copyright (c) 2019, the Alpha Team.
 */
public class NormalRuleImpl extends AbstractRule<NormalHead> implements NormalRule {

	public NormalRuleImpl(NormalHead head, List<Literal> body) {
		super(head, body);
	}

	public static NormalRuleImpl fromBasicRule(Rule<Head> rule) {
		Atom headAtom = null;
		if (!rule.isConstraint()) {
			if (!(rule.getHead() instanceof NormalHead)) {
				throw Util.oops("Trying to construct a NormalRule from rule with non-normal head! Head type is: " + rule.getHead().getClass().getSimpleName());
			}
			headAtom = ((NormalHead) rule.getHead()).getAtom();
		}
		return new NormalRuleImpl(headAtom != null ? new NormalHeadImpl(headAtom) : null, new ArrayList<>(rule.getBody()));
	}

	public boolean isGround() {
		if (!isConstraint() && !this.getHead().isGround()) {
			return false;
		}
		for (Literal bodyElement : this.getBody()) {
			if (!bodyElement.isGround()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Atom getHeadAtom() {
		return this.isConstraint() ? null : this.getHead().getAtom();
	}

}
