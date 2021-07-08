package at.ac.tuwien.kr.alpha.common;

import java.util.List;
import java.util.SortedSet;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.api.query.AnswerSetQuery;
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
	 * Applies a given {@link AnswerSetQuery} to this AnswerSet.
	 * 
	 * @param query the query to apply
	 * @return all atoms that are instances of the predicate specified by the query and meet the filters of the query
	 */
	default List<Atom> query(AnswerSetQuery query) {
		return query.applyTo(this);
	}
}
