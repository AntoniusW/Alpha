package at.ac.tuwien.kr.alpha.api;

import java.util.SortedSet;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;

public interface AnswerSet extends Comparable<AnswerSet> {
	
	SortedSet<Predicate> getPredicates();

	SortedSet<Atom> getPredicateInstances(Predicate predicate);

	boolean isEmpty();
}
