/**
 * Copyright (c) 2016-2019, the Alpha Team.
 * All rights reserved.
 * 
 * Additional changes made by Siemens.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.core.common;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.kr.alpha.api.Util;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.commons.util.IntIdGenerator;
import at.ac.tuwien.kr.alpha.core.atoms.RuleAtom;
import at.ac.tuwien.kr.alpha.core.solver.AtomCounter;

/**
 * This class stores ground atoms and provides the translation from an (integer) atomId to a (structured) predicate instance.
 */
public class AtomStoreImpl implements AtomStore {
	private final List<Atom> atomIdsToInternalBasicAtoms = new ArrayList<>();
	private final Map<Atom, Integer> predicateInstancesToAtomIds = new HashMap<>();
	private final IntIdGenerator atomIdGenerator = new IntIdGenerator(1);
	private final AtomCounter atomCounter = new AtomCounter();

	private final List<Integer> releasedAtomIds = new ArrayList<>();	// contains atomIds ready to be garbage collected if necessary.

	public AtomStoreImpl() {
		// Create atomId for falsum (currently not needed, but it gets atomId 0, which cannot represent a negated literal).
		atomIdsToInternalBasicAtoms.add(null);
	}

	@Override
	public int putIfAbsent(Atom groundAtom) {
		if (!groundAtom.isGround()) {
			throw new IllegalArgumentException("Atom must be ground: " + groundAtom);
		}

		Integer id = predicateInstancesToAtomIds.get(groundAtom);

		if (id == null) {
			id = atomIdGenerator.getNextId();
			predicateInstancesToAtomIds.put(groundAtom, id);
			atomIdsToInternalBasicAtoms.add(id, groundAtom);
			atomCounter.add(groundAtom);
		}

		return id;
	}

	@Override
	public boolean contains(Atom groundAtom) {
		return predicateInstancesToAtomIds.containsKey(groundAtom);
	}

	/**
	 * Removes the given atom from the AtomStoreImpl.
	 * @param atomId
	 */
	public void releaseAtomId(int atomId) {
		releasedAtomIds.add(atomId);
		// HINT: Additionally removing the terms used in the instance might be beneficial in some cases.
	}

	public String printAtomIdTermMapping() {
		StringBuilder ret = new StringBuilder();
		for (Map.Entry<Atom, Integer> entry : predicateInstancesToAtomIds.entrySet()) {
			ret.append(entry.getValue()).append(" <-> ").append(entry.getKey().toString()).append(System.lineSeparator());
		}
		return ret.toString();
	}

	@Override
	public String atomToString(int atomId) {
		return get(atomId).toString();
	}

	@Override
	public boolean isAtomChoicePoint(int atom) {
		return get(atom) instanceof RuleAtom;
	}

	@Override
	public int getMaxAtomId() {
		return atomIdsToInternalBasicAtoms.size() - 1;
	}

	@Override
	public Atom get(int atom) {
		try {
			return atomIdsToInternalBasicAtoms.get(atom);
		} catch (IndexOutOfBoundsException e) {
			throw Util.oops("Unknown atom ID encountered: " + atom, e);
		}
	}

	@Override
	public int get(Atom atom) {
		return predicateInstancesToAtomIds.get(atom);
	}

	@Override
	public AtomCounter getAtomCounter() {
		return atomCounter;
	}
}