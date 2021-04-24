package at.ac.tuwien.kr.alpha.commons;

import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

public class AnswerSetBuilder {
	private boolean firstInstance = true;
	private String predicateSymbol;
	private Predicate predicate;
	private SortedSet<Predicate> predicates = new TreeSet<>();
	private SortedSet<Atom> instances = new TreeSet<>();
	private Map<Predicate, SortedSet<Atom>> predicateInstances = new HashMap<>();

	public AnswerSetBuilder() {
	}

	public AnswerSetBuilder(AnswerSetBuilder copy) {
		this.firstInstance = copy.firstInstance;
		this.predicateSymbol = copy.predicateSymbol;
		this.predicate = copy.predicate;
		this.predicates = new TreeSet<>(copy.predicates);
		this.instances = new TreeSet<>(copy.instances);
		this.predicateInstances = new HashMap<>(copy.predicateInstances);
		this.predicateInstances = copy.predicateInstances.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> new TreeSet<>(e.getValue())));
	}

	private void flush() {
		if (firstInstance) {
			predicate = Predicates.getPredicate(predicateSymbol, 0);
			predicates.add(predicate);
			predicateInstances.put(predicate, new TreeSet<>(singletonList(Atoms.newBasicAtom(predicate))));
		} else {
			SortedSet<Atom> atoms = predicateInstances.get(predicate);
			if (atoms == null) {
				predicateInstances.put(predicate, new TreeSet<>(instances));
			} else {
				atoms.addAll(instances);
			}
		}
		firstInstance = true;
		instances.clear();
		predicate = null;
	}

	public AnswerSetBuilder predicate(String predicateSymbol) {
		if (this.predicateSymbol != null) {
			flush();
		}
		this.predicateSymbol = predicateSymbol;
		return this;
	}

	@SafeVarargs
	public final <T extends Comparable<T>> AnswerSetBuilder instance(final T... terms) {
		if (firstInstance) {
			firstInstance = false;
			predicate = Predicates.getPredicate(predicateSymbol, terms.length);
			predicates.add(predicate);
		}

		// Note that usage of terms does not pollute the heap,
		// since we are only reading, not writing.
		List<Term> termList = Stream
				.of(terms)
				.map(Terms::newConstant)
				.collect(Collectors.toList());

		instances.add(Atoms.newBasicAtom(predicate, termList));
		return this;
	}

	public AnswerSetBuilder symbolicInstance(String... terms) {
		if (firstInstance) {
			firstInstance = false;
			predicate = Predicates.getPredicate(predicateSymbol, terms.length);
			predicates.add(predicate);
		}

		List<Term> termList = Stream.of(terms).map(Terms::newSymbolicConstant).collect(Collectors.toList());
		instances.add(Atoms.newBasicAtom(predicate, termList));
		return this;
	}

	public AnswerSet build() {
		flush();
		return new BasicAnswerSet(predicates, predicateInstances);
	}
}
