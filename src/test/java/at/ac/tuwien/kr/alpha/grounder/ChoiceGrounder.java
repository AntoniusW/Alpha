/*
 * Copyright (c) 2016-2020, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.AnswerSetBuilder;
import at.ac.tuwien.kr.alpha.common.Assignment;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.BasicAnswerSet;
import at.ac.tuwien.kr.alpha.common.IntIterator;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.InternalRule;
import at.ac.tuwien.kr.alpha.common.rule.NormalRule;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.grounder.atoms.ChoiceAtom;
import at.ac.tuwien.kr.alpha.grounder.atoms.RuleAtom;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import static at.ac.tuwien.kr.alpha.Util.entriesToMap;
import static at.ac.tuwien.kr.alpha.Util.entry;
import static at.ac.tuwien.kr.alpha.common.NoGoodCreator.headFirst;
import static at.ac.tuwien.kr.alpha.common.NoGoodTest.fromOldLiterals;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

/**
 * Represents a small ASP program with choices {@code { aa :- not bb.  bb :- not aa. }}.
 */
public class ChoiceGrounder implements Grounder {
	public static final Set<AnswerSet> EXPECTED = new HashSet<>(asList(
		new AnswerSetBuilder()
			.predicate("aa")
			.build(),
		new AnswerSetBuilder()
			.predicate("bb")
			.build()
	));

	private static final int ATOM_AA = 1;
	private static final int ATOM_BB = 2;
	private static final int ATOM_BR1 = 3;
	private static final int ATOM_BR2 = 4;
	private static final int ATOM_EN_BR1 = 5;
	private static final int ATOM_EN_BR2 = 6;
	private static final int ATOM_DIS_BR1 = 7;
	private static final int ATOM_DIS_BR2 = 8;
	private static final int RULE_AA = 11; // { -aa, _br1 }
	private static final int BRULE_AA = 12; // { -_br1, -bb }
	private static final int RULE_BB = 13; // { -bb, _br2 }
	private static final int BRULE_BB = 14; // { -_br2, -aa }
	private static final int CHOICE_EN_BR1 = 15; // { -_en_br1 }
	private static final int CHOICE_EN_BR2 = 16; // { -_en_br2 }
	private static final int CHOICE_DIS_BR1 = 17; // { -_dis_br1,  bb}
	private static final int CHOICE_DIS_BR2 = 18; // { -dis_br2, aa }
	private static final Map<Integer, NoGood> NOGOODS = Stream.of(
		entry(RULE_AA, headFirst(fromOldLiterals(-ATOM_AA, ATOM_BR1))),
		entry(BRULE_AA, headFirst(fromOldLiterals(-ATOM_BR1, -ATOM_BB))),
		entry(RULE_BB, headFirst(fromOldLiterals(-ATOM_BB, ATOM_BR2))),
		entry(BRULE_BB, headFirst(fromOldLiterals(-ATOM_BR2, -ATOM_AA))),
		entry(CHOICE_EN_BR1, headFirst(fromOldLiterals(-ATOM_EN_BR1))),
		entry(CHOICE_EN_BR2, headFirst(fromOldLiterals(-ATOM_EN_BR2))),
		entry(CHOICE_DIS_BR1, headFirst(fromOldLiterals(-ATOM_DIS_BR1, ATOM_BB))),
		entry(CHOICE_DIS_BR2, headFirst(fromOldLiterals(-ATOM_DIS_BR2, ATOM_AA)))
	).collect(entriesToMap());
	private static final Map<Integer, Integer> CHOICE_ENABLE = Stream.of(
		entry(ATOM_BR1, ATOM_EN_BR1),
		entry(ATOM_BR2, ATOM_EN_BR2)
	).collect(entriesToMap());
	private static final Map<Integer, Integer> CHOICE_DISABLE = Stream.of(
		entry(ATOM_BR1, ATOM_DIS_BR1),
		entry(ATOM_BR2, ATOM_DIS_BR2)
	).collect(entriesToMap());
	private static Atom atomAA = new BasicAtom(Predicate.getInstance("aa", 0));
	private static Atom atomBB = new BasicAtom(Predicate.getInstance("bb", 0));
	private static BasicRule ruleAA = new BasicRule(new NormalHead(atomAA), Collections.singletonList(new BasicAtom(Predicate.getInstance("bb", 0)).toLiteral(false)));
	private static BasicRule ruleBB = new BasicRule(new NormalHead(atomBB), Collections.singletonList(new BasicAtom(Predicate.getInstance("aa", 0)).toLiteral(false)));
	private static Atom rule1 = new RuleAtom(InternalRule.fromNormalRule(NormalRule.fromBasicRule(ruleAA)), new Substitution());
	private static Atom rule2 = new RuleAtom(InternalRule.fromNormalRule(NormalRule.fromBasicRule(ruleBB)), new Substitution());
	private static Atom atomEnBR1 = ChoiceAtom.on(1);
	private static Atom atomEnBR2 = ChoiceAtom.on(2);
	private static Atom atomDisBR1 = ChoiceAtom.off(3);
	private static Atom atomDisBR2 = ChoiceAtom.off(4);
	private final AtomStore atomStore;
	private boolean returnedAllNogoods;

	private final java.util.function.Predicate<Predicate> filter;

	public ChoiceGrounder(AtomStore atomStore) {
		this(atomStore, p -> true);
	}

	public ChoiceGrounder(AtomStore atomStore, java.util.function.Predicate<Predicate> filter) {
		this.atomStore = atomStore;
		this.filter = filter;
		Arrays.asList(atomAA, atomBB, rule1, rule2, atomEnBR1, atomEnBR2, atomDisBR1, atomDisBR2).forEach(atomStore::putIfAbsent);
	}

	@Override
	public AnswerSet assignmentToAnswerSet(Iterable<Integer> trueAtoms) {
		SortedSet<Predicate> trueAtomPredicates = new TreeSet<>();
		for (int trueAtom : trueAtoms) {
			Predicate atomPredicate = atomStore.get(trueAtom).getPredicate();
			if (!filter.test(atomPredicate)) {
				continue;
			}
			if (atomPredicate.isInternal()) {
				continue;
			}
			trueAtomPredicates.add(atomPredicate);
		}

		// Add the atom instances
		Map<Predicate, SortedSet<Atom>> predicateInstances = new HashMap<>();
		for (Predicate trueAtomPredicate : trueAtomPredicates) {
			BasicAtom basicAtom = new BasicAtom(trueAtomPredicate);
			predicateInstances.put(trueAtomPredicate, new TreeSet<>(singleton(basicAtom)));
		}

		// Note: This grounder only deals with 0-ary predicates, i.e., every atom is a predicate and there is
		// 	 only one predicate instance representing 0 terms.
		return new BasicAnswerSet(trueAtomPredicates, predicateInstances);
	}

	@Override
	public Map<Integer, NoGood> getNoGoods(Assignment assignment) {
		if (!returnedAllNogoods) {
			returnedAllNogoods = true;
			return NOGOODS;
		} else {
			return new HashMap<>();
		}
	}
	
	private boolean isFirst = true;

	@Override
	public Pair<Map<Integer, Integer>, Map<Integer, Integer>> getChoiceAtoms() {
		if (isFirst) {
			isFirst = false;
			return new ImmutablePair<>(CHOICE_ENABLE, CHOICE_DISABLE);
		} else {
			return new ImmutablePair<>(new HashMap<>(), new HashMap<>());
		}
	}

	@Override
	public Pair<Map<Integer, Integer[]>, Map<Integer, Integer[]>> getHeuristicAtoms() {
		return new ImmutablePair<>(Collections.emptyMap(), Collections.emptyMap());
	}

	@Override
	public Map<Integer, HeuristicDirectiveValues> getHeuristicValues() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Integer, Set<Integer>> getHeadsToBodies() {
		return Collections.emptyMap();
	}

	@Override
	public void updateAssignment(IntIterator it) {
		// This test grounder reports all NoGoods immediately, irrespective of any assignment.
	}

	@Override
	public void forgetAssignment(int[] atomIds) {
	}

	private int solverDerivedNoGoodIdCounter = 20;
	private Map<NoGood, Integer> solverDerivedNoGoods = new HashMap<>();

	@Override
	public int register(NoGood noGood) {
		if (!solverDerivedNoGoods.containsKey(noGood)) {
			solverDerivedNoGoods.put(noGood, solverDerivedNoGoodIdCounter++);
		}
		return solverDerivedNoGoods.get(noGood);
	}

	@Override
	public AtomStore getAtomStore() {
		return null;
	}
}
