package at.ac.tuwien.kr.alpha.commons.util;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.util.AnswerSetFormatter;

public class SimpleAnswerSetFormatter implements AnswerSetFormatter<String> {

	private final String atomSeparator;

	public SimpleAnswerSetFormatter(String atomSeparator) {
		this.atomSeparator = atomSeparator;
	}

	@Override
	public String format(AnswerSet answerSet) {
		List<String> predicateInstanceStrings = new ArrayList<>();
		for (Predicate p : answerSet.getPredicates()) {
			SortedSet<Atom> instances;
			if ((instances = answerSet.getPredicateInstances(p)) == null || instances.isEmpty()) {
				predicateInstanceStrings.add(p.getName());
			} else {
				List<String> atomStrings = instances.stream().map((atom) -> atom.toString()).collect(Collectors.toList());
				predicateInstanceStrings.add(String.join(this.atomSeparator, atomStrings));
			}
		}
		return "{ " + String.join(this.atomSeparator, predicateInstanceStrings) + " }";
	}

}
