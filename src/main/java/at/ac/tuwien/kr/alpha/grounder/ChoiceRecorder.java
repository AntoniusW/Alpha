/**
 * Copyright (c) 2017-2019, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.grounder;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.grounder.atoms.RuleAtom;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static at.ac.tuwien.kr.alpha.common.Literals.*;
import static at.ac.tuwien.kr.alpha.grounder.atoms.ChoiceAtom.off;
import static at.ac.tuwien.kr.alpha.grounder.atoms.ChoiceAtom.on;
import static java.util.Collections.emptyList;

public class ChoiceRecorder {
	private static final IntIdGenerator ID_GENERATOR = new IntIdGenerator();

	private final AtomStore atomStore;
	private Pair<Map<Integer, Integer>, Map<Integer, Integer>> newChoiceAtoms = new ImmutablePair<>(new LinkedHashMap<>(), new LinkedHashMap<>());
	private Map<Integer, Set<Integer>> newHeadsToBodies = new LinkedHashMap<>();

	public ChoiceRecorder(AtomStore atomStore) {
		this.atomStore = atomStore;
	}

	/**
	 * @return new choice points and their enablers and disablers.
	 */
	public Pair<Map<Integer, Integer>, Map<Integer, Integer>> getAndResetChoices() {
		Pair<Map<Integer, Integer>, Map<Integer, Integer>> currentChoiceAtoms = newChoiceAtoms;
		newChoiceAtoms = new ImmutablePair<>(new LinkedHashMap<>(), new LinkedHashMap<>());
		return currentChoiceAtoms;
	}

	/**
	 * @return a set of new mappings from head atoms to {@link RuleAtom}s deriving it.
	 */
	public Map<Integer, Set<Integer>> getAndResetHeadsToBodies() {
		Map<Integer, Set<Integer>> currentHeadsToBodies = newHeadsToBodies;
		newHeadsToBodies = new LinkedHashMap<>();
		return currentHeadsToBodies;
	}

	
	public List<NoGood> generateChoiceNoGoods(final List<Integer> posLiterals, final List<Integer> negLiterals, final int bodyRepresentingLiteral) {
		// Obtain an ID for this new choice.
		final int choiceId = ID_GENERATOR.getNextId();
		final int bodyRepresentingAtom = atomOf(bodyRepresentingLiteral);
		// Create ChoiceOn and ChoiceOff atoms.
		final int choiceOnAtom = atomStore.putIfAbsent(on(choiceId));
		newChoiceAtoms.getLeft().put(bodyRepresentingAtom, choiceOnAtom);
		final int choiceOffAtom = atomStore.putIfAbsent(off(choiceId));
		newChoiceAtoms.getRight().put(bodyRepresentingAtom, choiceOffAtom);

		final List<NoGood> noGoods = generateNeg(choiceOffAtom, negLiterals);
		noGoods.add(generatePos(choiceOnAtom, posLiterals));

		return noGoods;
	}
	
	private NoGood generatePos(final int atomOn, List<Integer> posLiterals) {
		final int literalOn = atomToLiteral(atomOn);

		return NoGood.fromBodyInternal(posLiterals, emptyList(), literalOn);
	}

	private List<NoGood> generateNeg(final int atomOff, List<Integer> negLiterals)  {
		final int negLiteralOff = negateLiteral(atomToLiteral(atomOff));

		final List<NoGood> noGoods = new ArrayList<>(negLiterals.size() + 1);
		for (Integer negLiteral : negLiterals) {
			// Choice is off if any of the negative atoms is assigned true,
			// hence we add one nogood for each such atom.
			noGoods.add(NoGood.headFirstInternal(negLiteralOff, negLiteral));
		}
		return noGoods;
	}

	public void addHeadToBody(int headId, int bodyId) {
		Set<Integer> existingBodies = newHeadsToBodies.get(headId);
		if (existingBodies == null) {
			existingBodies = new HashSet<>();
			newHeadsToBodies.put(headId, existingBodies);
		}
		existingBodies.add(bodyId);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Util.join("[enablers: ", newChoiceAtoms.getLeft().entrySet(), (entry) -> entry.getKey() + "/" + entry.getValue(), ", ", ""));
		sb.append(Util.join(" disablers: ", newChoiceAtoms.getRight().entrySet(), (entry) -> entry.getKey() + "/" + entry.getValue(), ", ", "]"));
		return sb.toString();
	}
}
