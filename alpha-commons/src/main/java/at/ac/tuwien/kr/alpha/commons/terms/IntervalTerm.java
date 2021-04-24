package at.ac.tuwien.kr.alpha.commons.terms;

import java.util.LinkedHashSet;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.Util;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.util.Interner;

/**
 * An IntervalTerm represents the shorthand notation for a set of rules where all elements in this interval occur once, e.g., fact(2..5).
 * An IntervalTerm is a meta-term and the grounder must replace it with its corresponding set of facts or rules.
 * Copyright (c) 2017, the Alpha Team.
 */
public class IntervalTerm extends AbstractTerm {
	private static final Interner<IntervalTerm> INTERNER = new Interner<>();
	private final Term lowerBoundTerm;
	private final Term upperBoundTerm;

	private final int lowerBound;
	private final int upperBound;

	private final boolean ground;

	private IntervalTerm(Term lowerBound, Term upperBound) {
		if (lowerBound == null || upperBound == null) {
			throw new IllegalArgumentException();
		}

		this.ground = !((lowerBound instanceof VariableTerm) || (upperBound instanceof VariableTerm));

		this.lowerBoundTerm = lowerBound;
		this.upperBoundTerm = upperBound;

		if (this.ground) {
			this.upperBound = Integer.parseInt(upperBoundTerm.toString());
			this.lowerBound = Integer.parseInt(lowerBoundTerm.toString());
		} else {
			this.upperBound = -1;
			this.lowerBound = -1;
		}
	}

	public static IntervalTerm getInstance(Term lowerBound, Term upperBound) {
		return INTERNER.intern(new IntervalTerm(lowerBound, upperBound));
	}

	@Override
	public boolean isGround() {
		return this.ground;
	}

	public int getLowerBound() {
		if (!isGround()) {
			throw Util.oops("Cannot get the lower bound of non-ground interval");
		}
		return this.lowerBound;
	}

	public int getUpperBound() {
		if (!isGround()) {
			throw Util.oops("Cannot get the upper bound of non-ground interval");
		}
		return this.upperBound;
	}

	public Term getLowerBoundTerm() {
		return lowerBoundTerm;
	}

	public Term getUpperBoundTerm() {
		return upperBoundTerm;
	}

	@Override
	public Set<VariableTerm> getOccurringVariables() {
		Set<VariableTerm> variables = new LinkedHashSet<>();
		if (lowerBoundTerm instanceof VariableTerm) {
			variables.add((VariableTermImpl) lowerBoundTerm);
		}
		if (upperBoundTerm instanceof VariableTerm) {
			variables.add((VariableTermImpl) upperBoundTerm);
		}
		return variables;
	}

	@Override
	public IntervalTerm substitute(Substitution substitution) {
		if (isGround()) {
			return this;
		}
		return new IntervalTerm(lowerBoundTerm.substitute(substitution), upperBoundTerm.substitute(substitution));
	}

	@Override
	public String toString() {
		return lowerBoundTerm + ".." + upperBoundTerm;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IntervalTerm that = (IntervalTerm) o;

		if (!lowerBoundTerm.equals(that.lowerBoundTerm)) {
			return false;
		}
		return upperBoundTerm.equals(that.upperBoundTerm);

	}

	@Override
	public int hashCode() {
		return 31 * lowerBoundTerm.hashCode() + upperBoundTerm.hashCode();
	}

	@Override
	public int compareTo(Term o) {
		throw new UnsupportedOperationException("Intervals cannot be compared.");
	}

	@Override
	public Term renameVariables(String renamePrefix) {
		return new IntervalTerm(lowerBoundTerm.renameVariables(renamePrefix), upperBoundTerm.renameVariables(renamePrefix));
	}

	@Override
	public Term normalizeVariables(String renamePrefix, Term.RenameCounter counter) {
		return IntervalTerm.getInstance(
			lowerBoundTerm.normalizeVariables(renamePrefix, counter),
			upperBoundTerm.normalizeVariables(renamePrefix, counter));
	}

	/**
	 * Returns true if the term contains (or is) some IntervalTerm.
	 * @param term the term to test
	 * @return true iff an IntervalTerm occurs in term.
	 */
	public static boolean termContainsIntervalTerm(Term term) {
		if (term instanceof IntervalTerm) {
			return true;
		} else if (term instanceof FunctionTermImpl) {
			return functionTermContainsIntervals((FunctionTermImpl) term);
		} else {
			return false;
		}
	}

	public static boolean functionTermContainsIntervals(FunctionTerm functionTerm) {
		// Test whether a function term contains an interval term (recursively).
		for (Term term : functionTerm.getTerms()) {
			if (term instanceof IntervalTerm) {
				return true;
			}
			if (term instanceof FunctionTermImpl && functionTermContainsIntervals((FunctionTerm) term)) {
				return true;
			}
		}
		return false;
	}
}