/*
 * Copyright (c) 2017-2021, the Alpha Team.
 * All rights reserved.
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
package at.ac.tuwien.kr.alpha.common.program;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.rule.InternalRule;
import at.ac.tuwien.kr.alpha.common.rule.NormalRule;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A program in the internal representation needed for grounder and solver, i.e.: rules must have normal heads, all
 * aggregates must be rewritten, all intervals must be preprocessed (into interval atoms), and equality predicates must
 * be rewritten.
 * 
 * Copyright (c) 2017-2020, the Alpha Team.
 */
public class InternalProgram extends AbstractProgram<InternalRule> {

	private final Map<Predicate, LinkedHashSet<InternalRule>> predicateDefiningRules = new LinkedHashMap<>();
	private final Facts facts = new Facts();
	private final Map<Integer, InternalRule> rulesById = new LinkedHashMap<>();
	private boolean existsHeuristicRule;

	public InternalProgram(List<InternalRule> rules, List<Atom> facts) {
		super(rules, facts, null);
		recordFacts(facts);
		recordRules(rules);
	}

	static ImmutablePair<List<InternalRule>, List<Atom>> internalizeRulesAndFacts(NormalProgram normalProgram) {
		List<InternalRule> internalRules = new ArrayList<>();
		List<Atom> facts = new ArrayList<>(normalProgram.getFacts());
		for (NormalRule r : normalProgram.getRules()) {
			if (r.getBody().isEmpty() && !r.isHeuristicRule()) {
				if (!r.getHead().isGround()) {
					throw new IllegalArgumentException("InternalProgram does not support non-ground rules with empty bodies! (Head = " + r.getHead().toString() + ")");
				}
				facts.add(r.getHeadAtom());
			} else {
				internalRules.add(InternalRule.fromNormalRule(r));
			}
		}
		return new ImmutablePair<>(internalRules, facts);
	}

	public static InternalProgram fromNormalProgram(NormalProgram normalProgram) {
		ImmutablePair<List<InternalRule>, List<Atom>> rulesAndFacts = InternalProgram.internalizeRulesAndFacts(normalProgram);
		return new InternalProgram(rulesAndFacts.left, rulesAndFacts.right);
	}

	private void recordFacts(List<Atom> facts) {
		for (Atom fact : facts) {
			this.facts.add(fact);
		}
	}

	private void recordRules(List<InternalRule> rules) {
		for (InternalRule rule : rules) {
			rulesById.put(rule.getRuleId(), rule);
			if (!rule.isConstraint() && !rule.isHeuristicRule()) {
				recordDefiningRule(rule.getHeadAtom().getPredicate(), rule);
			} else if (rule.isHeuristicRule()) {
				this.existsHeuristicRule = true;
			}
		}
	}

	private void recordDefiningRule(Predicate headPredicate, InternalRule rule) {
		predicateDefiningRules.putIfAbsent(headPredicate, new LinkedHashSet<>());
		predicateDefiningRules.get(headPredicate).add(rule);
	}

	public Map<Predicate, LinkedHashSet<InternalRule>> getPredicateDefiningRules() {
		return Collections.unmodifiableMap(predicateDefiningRules);
	}

	public Facts getFactsByPredicate() {
		return facts;
	}

	public Map<Integer, InternalRule> getRulesById() {
		return Collections.unmodifiableMap(rulesById);
	}

	public boolean existsRuleWithPredicateInHead(final Predicate predicate) {
		final HashSet<InternalRule> definingRules = predicateDefiningRules.get(predicate);
		return definingRules != null && !definingRules.isEmpty();
	}

	public boolean existsHeuristicRule() {
		return existsHeuristicRule;
	}

}
