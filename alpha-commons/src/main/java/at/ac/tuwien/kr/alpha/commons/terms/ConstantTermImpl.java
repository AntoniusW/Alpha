package at.ac.tuwien.kr.alpha.commons.terms;

import java.util.Collections;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.util.Interner;

/**
 * Copyright (c) 2016-2020, the Alpha Team.
 */
class ConstantTermImpl<T extends Comparable<T>> extends AbstractTerm implements ConstantTerm<T> {
	private static final Interner<ConstantTermImpl<?>> INTERNER = new Interner<>();

	private final T object;
	private final boolean symbolic;

	private ConstantTermImpl(T object, boolean symbolic) {
		this.object = object;
		this.symbolic = symbolic;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> ConstantTermImpl<T> getInstance(T symbol) {
		return (ConstantTermImpl<T>) INTERNER.intern(new ConstantTermImpl<>(symbol, false));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> ConstantTermImpl<T> getSymbolicInstance(String symbol) {
		return (ConstantTermImpl<T>) INTERNER.intern(new ConstantTermImpl<>(symbol, true));
	}

	@Override
	public boolean isGround() {
		return true;
	}

	@Override
	public Set<VariableTerm> getOccurringVariables() {
		return Collections.emptySet();
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

		ConstantTermImpl<?> that = (ConstantTermImpl<?>) o;
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
	private static final int priority(final Class<?> clazz, ConstantTermImpl<?> term) {
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

		if (!(o instanceof ConstantTermImpl)) {
			return super.compareTo(o);
		}

		ConstantTermImpl<?> other = (ConstantTermImpl<?>) o;

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
	public AbstractTerm renameVariables(String renamePrefix) {
		// Constant contains no variables, hence stays the same.
		return this;
	}

	@Override
	public AbstractTerm normalizeVariables(String renamePrefix, Term.RenameCounter counter) {
		return this;
	}

	@Override
	public T getObject() {
		return object;
	}
}
