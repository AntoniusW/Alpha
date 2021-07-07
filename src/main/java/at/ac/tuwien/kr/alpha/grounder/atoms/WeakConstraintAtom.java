package at.ac.tuwien.kr.alpha.grounder.atoms;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.Util.oops;

/**
 * Represents the head of a weak constraint (i.e., a special internal atom indicating that a rule really is a weak
 * constraint).
 *
 * Copyright (c) 2020-2021, the Alpha Team.
 */
public class WeakConstraintAtom extends Atom {
	private static final Predicate PREDICATE = Predicate.getInstance("_weakconstraint_", 3, true, true);
	private static final String TERMLISTSYMBOL = "_tuple";

	private final Term weight;
	private final Term level;
	private final FunctionTerm termList;

	private WeakConstraintAtom(Term weight, Term level, FunctionTerm termList) {
		if (!isIntegerOrVariable(weight)) {
			throw new IllegalArgumentException("WeakConstraint with non-integer weight encountered: " + weight);
		}
		if (isNegativeInteger(weight)) {
			throw new IllegalArgumentException("WeakConstraint with negative weight encountered: " + weight + "@" + level + "\n"
			+ "Negative weights in weak constraints are not supported. Please rewrite your input program (turn negative weight if condition is met into positive weight if inverse condition is met).");
		}
		if (!isIntegerOrVariable(level)) {
			throw new IllegalArgumentException("WeakConstraint with non-integer level encountered: " + level);
		}
		this.weight = weight;
		this.level = level;
		this.termList = termList;
	}

	public static WeakConstraintAtom getInstance(Term weight, Term level, List<Term> termList) {
		Term actualLevel = level != null ? level : ConstantTerm.getInstance(0);
		List<Term> actualTermlist = termList != null ? termList : Collections.emptyList();
		return new WeakConstraintAtom(weight, actualLevel, FunctionTerm.getInstance(TERMLISTSYMBOL, actualTermlist));
	}

	public Integer getWeight() {
		return (Integer) ((ConstantTerm)weight).getObject();
	}

	public Integer getLevel() {
		return (Integer) ((ConstantTerm)level).getObject();
	}

	private static boolean isIntegerOrVariable(Term term) {
		if (term instanceof VariableTerm) {
			return true;
		}
		if (term instanceof ConstantTerm) {
			Comparable constant = ((ConstantTerm) term).getObject();
			return constant instanceof Integer;
		}
		return false;
	}

	private static boolean isNegativeInteger(Term term) {
		if (term instanceof ConstantTerm && ((ConstantTerm) term).getObject() instanceof Integer) {
			return ((Integer)((ConstantTerm) term).getObject()) < 0;
		}
		return false;
	}

	@Override
	public Predicate getPredicate() {
		return PREDICATE;
	}

	@Override
	public List<Term> getTerms() {
		List<Term> ret = new ArrayList<>(3);
		ret.add(weight);
		ret.add(level);
		ret.add(termList);
		return ret;
	}

	@Override
	public Atom withTerms(List<Term> terms) {
		if (terms.size() != 3) {
			throw oops("Trying to create WeakConstraintAtom with other than 3 terms.");
		}
		if (!(terms.get(2) instanceof FunctionTerm) || !((FunctionTerm) terms.get(2)).getSymbol().equals(TERMLISTSYMBOL)) {
			throw oops("Trying to create WeakConstraintAtom with 3rd term not being the correct function term.");
		}
		return new WeakConstraintAtom(terms.get(0), terms.get(1), (FunctionTerm) terms.get(2));
	}

	@Override
	public boolean isGround() {
		return weight.isGround() && level.isGround() && termList.isGround();
	}

	@Override
	public Atom substitute(Substitution substitution) {
		if (isGround()) {
			return this;
		}
		return new WeakConstraintAtom(weight.substitute(substitution), level.substitute(substitution), termList.substitute(substitution));
	}

	@Override
	public Set<VariableTerm> getOccurringVariables() {
		Set<VariableTerm> occurringVariables = new HashSet<>();
		occurringVariables.addAll(weight.getOccurringVariables());
		occurringVariables.addAll(level.getOccurringVariables());
		occurringVariables.addAll(termList.getOccurringVariables());
		return occurringVariables;
	}

	@Override
	public String toString() {
		return PREDICATE.getName() + "(" + weight + "@" + level + ", " + termList + ")";
	}

	@Override
	public Literal toLiteral(boolean positive) {
		throw new UnsupportedOperationException("WeakConstraintAtom cannot be literalized.");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WeakConstraintAtom that = (WeakConstraintAtom) o;
		return weight.equals(that.weight) &&
			level.equals(that.level) &&
			termList.equals(that.termList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(weight, level, termList);
	}
}
