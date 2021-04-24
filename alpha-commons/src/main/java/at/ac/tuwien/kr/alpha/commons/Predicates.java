package at.ac.tuwien.kr.alpha.commons;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;

public final class Predicates {

	private Predicates() {
		throw new AssertionError("Cannot instantiate utility class!");
	}

	public static Predicate getPredicate(String symbol, int arity) {
		return PredicateImpl.getInstance(symbol, arity, false, false);
	}

	public static Predicate getPredicate(String symbol, int arity, boolean internal) {
		return PredicateImpl.getInstance(symbol, arity, internal, false);
	}

	public static Predicate getPredicate(String symbol, int arity, boolean internal, boolean solverInternal) {
		return PredicateImpl.getInstance(symbol, arity, internal, solverInternal);
	}

}
