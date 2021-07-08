/*
 * Copyright (c) 2017-2021, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.Literals;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.NoGoodCreator;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveAtom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import at.ac.tuwien.kr.alpha.common.program.InternalProgram;
import at.ac.tuwien.kr.alpha.grounder.atoms.HeuristicAtom;
import at.ac.tuwien.kr.alpha.grounder.atoms.HeuristicInfluencerAtom;
import at.ac.tuwien.kr.alpha.grounder.atoms.RuleAtom;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.Literals.atomToLiteral;
import static at.ac.tuwien.kr.alpha.common.Literals.negateLiteral;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.NUM_SIGN_SETS;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.getIndex;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.getSignSetByIndex;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.isF;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.isProcessable;
import static at.ac.tuwien.kr.alpha.grounder.atoms.ChoiceAtom.off;
import static at.ac.tuwien.kr.alpha.grounder.atoms.ChoiceAtom.on;
import static at.ac.tuwien.kr.alpha.grounder.transformation.SignSetTransformation.SET_F;
import static java.util.Collections.emptyList;

public class ChoiceRecorder {
	static final IntIdGenerator ID_GENERATOR = new IntIdGenerator();

	private static final int IDX_OFF = 1;
	private static final int IDX_ON = 0;

	private final AtomStore atomStore;
	private Pair<Map<Integer, Integer>, Map<Integer, Integer>> newChoiceAtoms = new ImmutablePair<>(new LinkedHashMap<>(), new LinkedHashMap<>());
	private Pair<Map<Integer, Integer[]>, Map<Integer, Integer[]>> newHeuristicAtoms = new ImmutablePair<>(new LinkedHashMap<>(), new LinkedHashMap<>());
	private Map<Integer, HeuristicDirectiveValues> newHeuristicValues = new LinkedHashMap<>();
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


	/**
	 * @return new heuristic atoms and their enablers and disablers.
	 */
	public Pair<Map<Integer, Integer[]>, Map<Integer, Integer[]>> getAndResetHeuristics() {
		Pair<Map<Integer, Integer[]>, Map<Integer, Integer[]>> currentHeuristicAtoms = newHeuristicAtoms;
		newHeuristicAtoms = new ImmutablePair<>(new LinkedHashMap<>(), new LinkedHashMap<>());
		return currentHeuristicAtoms;
	}

	/**
	 * @return a set of new mappings from heuristic atoms to {@link HeuristicDirectiveValues}.
	 */
	public Map<Integer, HeuristicDirectiveValues> getAndResetHeuristicValues() {
		Map<Integer, HeuristicDirectiveValues> currentHeuristicValues = newHeuristicValues;
		newHeuristicValues = new LinkedHashMap<>();
		return currentHeuristicValues;
	}


	public List<NoGood> generateChoiceNoGoods(final List<Integer> posLiterals, final List<Integer> negLiterals, final int bodyRepresentingAtom) {
		// Obtain an ID for this new choice.
		final int choiceId = ID_GENERATOR.getNextId();
		// Create ChoiceOn and ChoiceOff atoms.
		final int choiceOnAtom = atomStore.putIfAbsent(on(choiceId));
		newChoiceAtoms.getLeft().put(bodyRepresentingAtom, choiceOnAtom);
		final int choiceOffAtom = atomStore.putIfAbsent(off(choiceId));
		newChoiceAtoms.getRight().put(bodyRepresentingAtom, choiceOffAtom);

		final List<NoGood> noGoods = generateNeg(choiceOffAtom, negLiterals);
		noGoods.add(generatePos(choiceOnAtom, posLiterals));

		return noGoods;
	}

	public Collection<NoGood> generateHeuristicNoGoods(HeuristicAtom groundHeuristicAtom, final RuleAtom bodyAtom, final int headId, InternalProgram program) {
		// Obtain an ID for this new heuristic.
		final int heuristicId = ID_GENERATOR.getNextId();
		final Integer[][] influencers = new Integer[2][4]; // dim 1: [on,off], dim 2: [T,TM,M,F]

		final List<NoGood> noGoods = generateHeuristicNoGoodsForPositiveCondition(groundHeuristicAtom, heuristicId, program, influencers);
		if (noGoods == null) {
			return null;  // heuristic can never be applicable
		}
		final List<NoGood> noGoodsForNegativeCondition = generateHeuristicNoGoodsForNegativeCondition(groundHeuristicAtom, heuristicId, program, influencers);
		if (noGoodsForNegativeCondition == null) {
			return null;  // heuristic can never be applicable
		}
		noGoods.addAll(noGoodsForNegativeCondition);

		final int bodyRepresentingAtom = atomStore.putIfAbsent(bodyAtom);
		newHeuristicAtoms.getLeft().put(bodyRepresentingAtom, influencers[IDX_ON]);
		newHeuristicAtoms.getRight().put(bodyRepresentingAtom, influencers[IDX_OFF]);

		if (newHeuristicValues.put(bodyRepresentingAtom, HeuristicDirectiveValues.fromHeuristicAtom(groundHeuristicAtom, headId)) != null) {
			throw oops("Same heuristic body-representing atom used for two heuristic directives");
		}

		return noGoods;
	}

	private List<NoGood> generateHeuristicNoGoodsForPositiveCondition(HeuristicAtom groundHeuristicAtom, int heuristicId, InternalProgram program, Integer[][] influencers) {
		final List<NoGood> noGoods = new ArrayList<>();
		final Map<Integer, Set<Atom>> positiveAtomsBySignSet = new HashMap<>(NUM_SIGN_SETS);
		for (HeuristicDirectiveAtom heuristicDirectiveAtom : groundHeuristicAtom.getOriginalPositiveCondition()) {
			final Atom atom = heuristicDirectiveAtom.getAtom();
			final Set<ThriceTruth> signSet = heuristicDirectiveAtom.getSigns();

			if (program.getFactsByPredicate().isFact(atom)) {
				if (signSet.contains(ThriceTruth.TRUE) || signSet.contains(ThriceTruth.MBT)) {
					continue; // literal is always satisfied
				}
				if (signSet.contains(ThriceTruth.FALSE)) {
					return null; // heuristic is never applicable
				}
			}

			if (!program.existsRuleWithPredicateInHead(atom.getPredicate())) {
				if (signSet.contains(ThriceTruth.TRUE) || signSet.contains(ThriceTruth.MBT)) {
					return null; // heuristic is never applicable
				}
				if (signSet.contains(ThriceTruth.FALSE)) {
					continue; // literal is always satisfied
				}
			}

			final int idxSignSet = getIndex(signSet);
			final Set<Atom> atomsForSignSet = positiveAtomsBySignSet.computeIfAbsent(idxSignSet, k -> new HashSet<>());
			atomsForSignSet.add(atom);
		}
		for (int idxSignSet = 0; idxSignSet < NUM_SIGN_SETS; idxSignSet++) {
			if (positiveAtomsBySignSet.get(idxSignSet) == null) {
				continue;
			}
			final boolean inNegativeBody = false;
			final Set<ThriceTruth> signSet = getSignSetByIndex(idxSignSet);
			createHeuristicInfluencer(heuristicId, signSet, inNegativeBody, influencers);
			noGoods.add(generateHeuristicPos(positiveAtomsBySignSet.get(idxSignSet), signSet, influencers[IDX_ON][idxSignSet]));
		}
		return noGoods;
	}

	private List<NoGood> generateHeuristicNoGoodsForNegativeCondition(HeuristicAtom groundHeuristicAtom, int heuristicId, InternalProgram program, Integer[][] influencers) {
		final List<NoGood> noGoods = new ArrayList<>();
		for (HeuristicDirectiveAtom heuristicDirectiveAtom : groundHeuristicAtom.getOriginalNegativeCondition()) {
			final Atom atom = heuristicDirectiveAtom.getAtom();
			final Set<ThriceTruth> signSet = heuristicDirectiveAtom.getSigns();

			if (program.getFactsByPredicate().isFact(atom)) {
				if (signSet.contains(ThriceTruth.TRUE) || signSet.contains(ThriceTruth.MBT)) {
					return null; // heuristic is never applicable
				}
				if (signSet.contains(ThriceTruth.FALSE)) {
					continue; // literal is always satisfied
				}
			}

			if (!program.existsRuleWithPredicateInHead(atom.getPredicate())) {
				if (signSet.contains(ThriceTruth.TRUE) || signSet.contains(ThriceTruth.MBT)) {
					continue; // literal is always satisfied
				}
				if (signSet.contains(ThriceTruth.FALSE)) {
					return null; // heuristic is never applicable
				}
			}

			final boolean inNegativeBody = true;
			final int idxSignSet = getIndex(signSet);
			createHeuristicInfluencer(heuristicId, signSet, inNegativeBody, influencers);
			noGoods.add(generateHeuristicNeg(atom, signSet, influencers[IDX_OFF][idxSignSet]));
		}
		return noGoods;
	}

	private void createHeuristicInfluencer(int heuristicId, Set<ThriceTruth> signSet, boolean inNegativeBody, Integer[][] influencers) {
		final int idxOnOff = inNegativeBody ? IDX_OFF : IDX_ON;
		final int idxSignSet = getIndex(signSet);
		if (influencers[idxOnOff][idxSignSet] == null) {
			influencers[idxOnOff][idxSignSet] = atomStore.putIfAbsent(HeuristicInfluencerAtom.get(!inNegativeBody, heuristicId, signSet));
		}
	}

	private NoGood generatePos(final int atomOn, List<Integer> posLiterals) {
		final int literalOn = atomToLiteral(atomOn);

		return NoGoodCreator.fromBodyInternal(posLiterals, emptyList(), literalOn);
	}

	private NoGood generateHeuristicPos(Set<Atom> atoms, Set<ThriceTruth> signSet, int heuristicInfluencerAtom) {
		final int literalOn = atomToLiteral(heuristicInfluencerAtom);
		final List<Integer> literals = atoms.stream().map(atomStore::putIfAbsent).map(Literals::atomToLiteral).collect(Collectors.toList());
		if (SET_F.equals(signSet)) {
			return NoGoodCreator.fromBodyInternal(emptyList(), literals, literalOn);
		} else {
			return NoGoodCreator.fromBodyInternal(literals, emptyList(), literalOn);
		}
	}

	private List<NoGood> generateNeg(final int atomOff, List<Integer> negLiterals)  {
		final int negLiteralOff = negateLiteral(atomToLiteral(atomOff));

		final List<NoGood> noGoods = new ArrayList<>(negLiterals.size() + 1);
		for (Integer negLiteral : negLiterals) {
			// Choice is off if any of the negative atoms is assigned true,
			// hence we add one nogood for each such atom.
			noGoods.add(NoGoodCreator.headFirstInternal(negLiteralOff, negLiteral));
		}
		return noGoods;
	}

	private NoGood generateHeuristicNeg(Atom atom, Set<ThriceTruth> signSet, int heuristicInfluencerAtom) {
		if (!isProcessable(signSet)) {
			throw oops("Heuristic sign not processable: " + signSet);
		}
		final int atomID = atomStore.putIfAbsent(atom);
		return NoGoodCreator.headFirstInternal(atomToLiteral(heuristicInfluencerAtom, false), atomToLiteral(atomID, !isF(signSet)));
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
		StringBuilder sb = new StringBuilder("[enablers: ");
		for (Map.Entry<Integer, Integer> enablers : newChoiceAtoms.getLeft().entrySet()) {
			sb.append(enablers.getKey()).append("/").append(enablers.getValue()).append(", ");
		}
		sb.append(" disablers: ");
		for (Map.Entry<Integer, Integer> disablers : newChoiceAtoms.getRight().entrySet()) {
			sb.append(disablers.getKey()).append("/").append(disablers.getValue());
		}
		return sb.append("]").toString();
	}
}
