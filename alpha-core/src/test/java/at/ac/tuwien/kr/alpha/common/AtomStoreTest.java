package at.ac.tuwien.kr.alpha.common;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;

/**
 * Copyright (c) 2018, the Alpha Team.
 */
// TODO (what is this? where is this used?)
public class AtomStoreTest {

	public static void fillAtomStore(AtomStore atomStore, int numberOfAtomsToFill) {
		Predicate predA = Predicates.getPredicate("a", 1);
		for (int i = 0; i < numberOfAtomsToFill; i++) {
			atomStore.putIfAbsent(Atoms.newBasicAtom(predA, Terms.newConstant(i)));
		}
	}
}