package at.ac.tuwien.kr.alpha.commons;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.commons.util.Interner;

/**
 * A predicate as used by the Alpha solver internally.
 * 
 * Copyright (c) 2016-2020, the Alpha Team.
 */
class PredicateImpl implements Predicate {
	
	private static final Interner<PredicateImpl> INTERNER = new Interner<>();

	private final String name;
	private final int arity;
	private final boolean internal;
	private final boolean solverInternal;

	PredicateImpl(String name, int arity, boolean internal, boolean solverInternal) {
		this.name = name;
		this.arity = arity;
		this.internal = internal;
		this.solverInternal = solverInternal;
	}

	static PredicateImpl getInstance(String symbol, int arity) {
		return getInstance(symbol, arity, false, false);
	}

	static PredicateImpl getInstance(String symbol, int arity, boolean internal) {
		return getInstance(symbol, arity, internal, false);
	}

	static PredicateImpl getInstance(String symbol, int arity, boolean internal, boolean solverInternal) {
		return INTERNER.intern(new PredicateImpl(symbol, arity, internal, solverInternal));
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + arity;
		result = 31 * result + (internal ? 1 : 0);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof PredicateImpl)) {
			return false;
		}

		PredicateImpl predicate = (PredicateImpl) o;

		if (arity != predicate.arity) {
			return false;
		}

		if (internal != predicate.internal) {
			return false;
		}

		return name != null ? name.equals(predicate.name) : predicate.name == null;
	}

	/**
	 * Marks internal predicates that should not be shown/printed in answer sets.
	 * @return true iff this Predicate should be omitted from answer sets.
	 */
	@Override
	public boolean isInternal() {
		return internal;
	}

	/**
	 * Marks predicates that are used purely for encoding rules by NoGoods in the solver component. Solver internal
	 * predicates are guaranteed to not occur in any rule bodies and hence are ignored by the grounder.
	 * @return true iff this Predicate is internal to the solver component.
	 */
	@Override
	public boolean isSolverInternal() {
		return solverInternal;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getArity() {
		return arity;
	}

	@Override
	public String toString() {
		return name + "/" + arity;
	}
}
