package at.ac.tuwien.kr.alpha.core.common;

import java.util.SortedSet;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;

public interface CoreAnswerSet extends Comparable<CoreAnswerSet> {
	SortedSet<Predicate> getPredicates();

	SortedSet<Atom> getPredicateInstances(Predicate predicate);

	boolean isEmpty();
}
