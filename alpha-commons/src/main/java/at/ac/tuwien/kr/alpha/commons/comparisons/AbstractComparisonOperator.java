package at.ac.tuwien.kr.alpha.commons.comparisons;

import at.ac.tuwien.kr.alpha.api.ComparisonOperator;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.Predicates;

abstract class AbstractComparisonOperator implements ComparisonOperator {

	private final String symbol;

	AbstractComparisonOperator(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public String getSymbol() {
		return this.symbol;
	}

	@Override
	public String toString() {
		return this.getSymbol();
	}

	@Override
	public Predicate toPredicate() {
		return Predicates.getPredicate(this.symbol, 2);
	}

	@Override
	public abstract ComparisonOperator negate();

	@Override
	public abstract boolean compare(Term t1, Term t2);

}
