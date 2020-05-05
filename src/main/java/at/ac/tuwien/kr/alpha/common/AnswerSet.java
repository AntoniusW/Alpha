package at.ac.tuwien.kr.alpha.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.api.answersets.AtomQuery;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;

public interface AnswerSet extends Comparable<AnswerSet> {
	SortedSet<Predicate> getPredicates();

	SortedSet<Atom> getPredicateInstances(Predicate predicate);

	boolean isEmpty();

	@Override
	default int compareTo(AnswerSet other) {
		final SortedSet<Predicate> predicates = this.getPredicates();
		int result = Util.compareSortedSets(predicates, other.getPredicates());

		if (result != 0) {
			return result;
		}

		for (Predicate predicate : predicates) {
			result = Util.compareSortedSets(this.getPredicateInstances(predicate), other.getPredicateInstances(predicate));

			if (result != 0) {
				return result;
			}
		}

		return 0;
	}

	/**
	 * Gives a "flat" version of this answer set by transforming it into a list of
	 * atoms
	 */
	default List<Atom> flatten() {
		List<Atom> flatAnswerSet = new ArrayList<>();
		for (Predicate p : this.getPredicates()) {
			for (Atom a : this.getPredicateInstances(p)) {
				flatAnswerSet.add(a);
			}
		}
		return flatAnswerSet;
	}

	/**
	 * Queries this answer set for predicate instances satisfying a given
	 * {@link AtomQuery}.
	 * 
	 * @param query The {@link AtomQuery} to use
	 * @return a list of {@link Atom}s that satisfy the given query
	 */
	default List<Atom> query(AtomQuery query) {
		if (!this.getPredicates().contains(query.getPredicate())) {
			return Collections.emptyList();
		}
		List<Atom> result = new ArrayList<>();
		for (Atom candidate : this.getPredicateInstances(query.getPredicate())) {
			if (query.test(candidate)) { // candidate satisfies query
				result.add(candidate);
			}
		}
		return result;
	}
}
