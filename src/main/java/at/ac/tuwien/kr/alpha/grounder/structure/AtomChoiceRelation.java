package at.ac.tuwien.kr.alpha.grounder.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static at.ac.tuwien.kr.alpha.Util.arrayGrowthSize;

/**
 * Stores and provides relationships between ordinary atoms and those that represent choice points.
 * More specifically: relations between atoms and choice points (=body-representing atoms) influencing their truth
 * values. Moreover, each body-representing atom is in relation with itself.
 *
 * Copyright (c) 2019, the Alpha Team.
 */
public class AtomChoiceRelation {
	@SuppressWarnings("unchecked")
	private ArrayList<Integer>[] atomToChoiceAtoms = new ArrayList[0];

	public void addRelation(int atom, int bodyRepresentingAtom) {
		if (atomToChoiceAtoms[atom] == null) {
			atomToChoiceAtoms[atom] = new ArrayList<>();
		}
		atomToChoiceAtoms[atom].add(bodyRepresentingAtom);

		// Ensure that each bodyRepresentingAtom is in relation with itself.
		if (atomToChoiceAtoms[bodyRepresentingAtom] == null) {
			atomToChoiceAtoms[bodyRepresentingAtom] = new ArrayList<>();
			atomToChoiceAtoms[bodyRepresentingAtom].add(bodyRepresentingAtom);
		}
	}

	public List<Integer> getRelatedChoiceAtoms(int atom) {
		if (atomToChoiceAtoms[atom] == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(atomToChoiceAtoms[atom]);
	}

	public void growForMaxAtomId(int maxAtomId) {
		if (maxAtomId >= atomToChoiceAtoms.length) {
			int newCapacity = arrayGrowthSize(atomToChoiceAtoms.length);
			if (newCapacity < maxAtomId + 1) {
				newCapacity = maxAtomId + 1;
			}
			atomToChoiceAtoms = Arrays.copyOf(atomToChoiceAtoms, newCapacity);
		}
	}
}
