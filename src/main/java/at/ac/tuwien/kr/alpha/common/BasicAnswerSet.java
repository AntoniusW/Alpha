package at.ac.tuwien.kr.alpha.common;

import at.ac.tuwien.kr.alpha.common.atoms.Atom;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySortedSet;

/**
 * Copyright (c) 2016, the Alpha Team.
 */
public class BasicAnswerSet implements AnswerSet {
	public static final BasicAnswerSet EMPTY = new BasicAnswerSet(emptySortedSet(), emptyMap());

	private final SortedSet<Predicate> predicates;
	private final Map<Predicate, SortedSet<Atom>> predicateInstances;

	public BasicAnswerSet(SortedSet<Predicate> predicates, Map<Predicate, SortedSet<Atom>> predicateInstances) {
		this.predicates = predicates;
		this.predicateInstances = predicateInstances;
	}

	@Override
	public SortedSet<Predicate> getPredicates() {
		return predicates;
	}

	@Override
	public SortedSet<Atom> getPredicateInstances(Predicate predicate) {
		return predicateInstances.get(predicate);
	}

	@Override
	public boolean isEmpty() {
		return predicates.isEmpty();
	}

	protected Map<Predicate, SortedSet<Atom>> getPredicateInstances() {
		return predicateInstances;
	}

	@Override
	public String toString() {
		if (predicates.isEmpty()) {
			return "{}";
		}

		final StringBuilder sb = new StringBuilder("{ ");
		for (Iterator<Predicate> iterator = predicates.iterator(); iterator.hasNext();) {
			Predicate predicate = iterator.next();
			Set<Atom> instances = getPredicateInstances(predicate);

			if (instances == null || instances.isEmpty()) {
				sb.append(predicate.getName());
				continue;
			}

			for (Iterator<Atom> instanceIterator = instances.iterator(); instanceIterator.hasNext();) {
				sb.append(instanceIterator.next());
				if (instanceIterator.hasNext()) {
					sb.append(", ");
				}
			}

			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(" }");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BasicAnswerSet)) {
			return false;
		}

		BasicAnswerSet that = (BasicAnswerSet) o;

		if (!predicates.equals(that.predicates)) {
			return false;
		}

		return predicateInstances.equals(that.predicateInstances);
	}

	@Override
	public int hashCode() {
		return  31 * predicates.hashCode() + predicateInstances.hashCode();
	}
}