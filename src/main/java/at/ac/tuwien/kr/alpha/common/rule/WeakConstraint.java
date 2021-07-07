package at.ac.tuwien.kr.alpha.common.rule;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.Term;

import java.util.List;

/**
 * Represents a weak constraint.
 *
 * Copyright (c) 2020, the Alpha Team.
 */
public class WeakConstraint extends BasicRule {

	private final Term weight;
	private final Term level;
	private final List<Term> termList;

	public WeakConstraint(List<Literal> body, Term weight, Term level, List<Term> termList) {
		super(null, body);
		this.weight = weight;
		this.level = level;
		this.termList = termList;
	}

	public Term getWeight() {
		return weight;
	}

	public Term getLevel() {
		return level;
	}

	public List<Term> getTermList() {
		return termList;
	}

	@Override
	public String toString() {
		return Util.join(":~ ", getBody(), ".");
	}
}
