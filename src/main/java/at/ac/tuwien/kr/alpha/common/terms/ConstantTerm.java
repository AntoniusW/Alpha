package at.ac.tuwien.kr.alpha.common.terms;

import at.ac.tuwien.kr.alpha.common.Interner;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2016-2020, the Alpha Team.
 */
public class ConstantTerm<T extends Comparable<T>> extends Term {
	private static final Interner<ConstantTerm<?>> INTERNER = new Interner<>();

	private final T object;
	private final boolean symbolic;

	private ConstantTerm(T object, boolean symbolic) {
		this.object = object;
		this.symbolic = symbolic;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> ConstantTerm<T> getInstance(T symbol) {
		return (ConstantTerm<T>) INTERNER.intern(new ConstantTerm<>(symbol, false));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> ConstantTerm<T> getSymbolicInstance(String symbol) {
		return (ConstantTerm<T>) INTERNER.intern(new ConstantTerm<>(symbol, true));
	}

	@Override
	public boolean isGround() {
		return true;
	}

	@Override
	public List<VariableTerm> getOccurringVariables() {
		return Collections.emptyList();
	}

	@Override
	public Term substitute(Substitution substitution) {
		return this;
	}

	@Override
	public String toString() {
		if (object instanceof String) {
			if (symbolic) {
				return (String) object;
			} else {
				return "\"" + object + "\"";
			}
		}
		return object.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ConstantTerm<?> that = (ConstantTerm<?>) o;
		if (this.symbolic != that.symbolic) {
			return false;
		}

		return object.equals(that.object);
	}

	@Override
	public int hashCode() {
		int result = object.hashCode();
		result = 31 * result + (symbolic ? 1 : 0);
		return result;
	}

	/**
	 * Establishes "priority" for ordering of constant terms depending on the type
	 * of the corresponding object according to ASP-Core-2.03c.
	 */
	private static final int priority(final Class<?> clazz, ConstantTerm<?> term) {
		if (clazz.equals(Integer.class)) {
			return 1;
		} else if (clazz.equals(String.class)) {
			return term.symbolic ? 2 : 3;
		}
		return 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(Term o) {
		if (this == o) {
			return 0;
		}

		if (!(o instanceof ConstantTerm)) {
			return super.compareTo(o);
		}

		ConstantTerm<?> other = (ConstantTerm<?>) o;

		// We will perform an unchecked cast.
		// Because of type erasure, we cannot know the exact type
		// of other.object.
		// However, may assume that other.object actually is of
		// type T.
		// We know that this.object if of type T and implements
		// Comparable<T>. We ensure that the class of other.object
		// equals the class of this.object, which in turn is T.
		// That assumption should be quite safe. It can only be
		// wrong if we have some bug that generates strange
		// ConstantTerms at runtime, bypassing the check for T
		// at compile-time.
		if (other.object.getClass() == this.object.getClass() && other.symbolic == this.symbolic) {
			return this.object.compareTo((T) other.object);
		}

		Class<?> thisType = this.object.getClass();
		Class<?> otherType = other.object.getClass();

		int thisPrio = priority(thisType, this);
		int otherPrio = priority(otherType, other);

		if (thisPrio == 0 || otherPrio == 0) {
			return thisType.getName().compareTo(otherType.getName());
		}

		return Integer.compare(thisPrio, otherPrio);
	}

	@Override
	public Term renameVariables(String renamePrefix) {
		// Constant contains no variables, hence stays the same.
		return this;
	}

	@Override
	public Term normalizeVariables(String renamePrefix, RenameCounter counter) {
		return this;
	}

	public T getObject() {
		return object;
	}

	public boolean isSymbolic() {
		return this.symbolic;
	}
}
