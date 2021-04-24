package at.ac.tuwien.kr.alpha.commons;

import java.util.Map;
import java.util.SortedSet;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;

public final class AnswerSets {

	public static final AnswerSet EMPTY_SET = BasicAnswerSet.EMPTY;
	
	private AnswerSets() {
		throw new AssertionError("Cannot instantiate utility class");
	}

	public static AnswerSet newAnswerSet(SortedSet<Predicate> predicates, Map<Predicate, SortedSet<Atom>> predicateInstances) {
		return new BasicAnswerSet(predicates, predicateInstances);
	}

}
