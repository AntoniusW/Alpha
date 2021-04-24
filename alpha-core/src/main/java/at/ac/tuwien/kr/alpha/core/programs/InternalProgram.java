package at.ac.tuwien.kr.alpha.core.programs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.programs.NormalProgram;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.rules.NormalHead;
import at.ac.tuwien.kr.alpha.api.rules.NormalRule;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.core.grounder.FactIntervalEvaluator;
import at.ac.tuwien.kr.alpha.core.rules.CompiledRule;
import at.ac.tuwien.kr.alpha.core.rules.InternalRule;
import at.ac.tuwien.kr.alpha.core.rules.NormalRuleImpl;

/**
 * A program in the internal representation needed for grounder and solver, i.e.: rules must have normal heads, all
 * aggregates must be rewritten, all intervals must be preprocessed (into interval atoms), and equality predicates must
 * be rewritten.
 * <p>
 * Copyright (c) 2017-2021, the Alpha Team.
 */
public class InternalProgram extends AbstractProgram<CompiledRule> implements CompiledProgram {

	private final Map<Predicate, LinkedHashSet<CompiledRule>> predicateDefiningRules = new LinkedHashMap<>();
	private final Map<Predicate, LinkedHashSet<Instance>> factsByPredicate = new LinkedHashMap<>();
	private final Map<Integer, CompiledRule> rulesById = new LinkedHashMap<>();

	// TODO constructor param internal rule
	public InternalProgram(List<CompiledRule> rules, List<Atom> facts) {
		super(rules, facts, null);
		recordFacts(facts);
		recordRules(rules);
	}

	static ImmutablePair<List<CompiledRule>, List<Atom>> internalizeRulesAndFacts(NormalProgram normalProgram) {
		List<CompiledRule> internalRules = new ArrayList<>();
		List<Atom> facts = new ArrayList<>(normalProgram.getFacts());
		for (Rule<NormalHead> r : normalProgram.getRules()) {
			if (r.getBody().isEmpty()) {
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
		ImmutablePair<List<CompiledRule>, List<Atom>> rulesAndFacts = InternalProgram.internalizeRulesAndFacts(normalProgram);
		return new InternalProgram(rulesAndFacts.left, rulesAndFacts.right);
	}

	private void recordFacts(List<Atom> facts) {
		for (Atom fact : facts) {
			List<Instance> tmpInstances = FactIntervalEvaluator.constructFactInstances(fact);
			Predicate tmpPredicate = fact.getPredicate();
			factsByPredicate.putIfAbsent(tmpPredicate, new LinkedHashSet<>());
			factsByPredicate.get(tmpPredicate).addAll(tmpInstances);
		}
	}

	private void recordRules(List<CompiledRule> rules) {
		for (CompiledRule rule : rules) {
			rulesById.put(rule.getRuleId(), rule);
			if (!rule.isConstraint()) {
				recordDefiningRule(rule.getHeadAtom().getPredicate(), rule);
			}
		}
	}

	private void recordDefiningRule(Predicate headPredicate, CompiledRule rule) {
		predicateDefiningRules.putIfAbsent(headPredicate, new LinkedHashSet<>());
		predicateDefiningRules.get(headPredicate).add(rule);
	}

	@Override
	public Map<Predicate, LinkedHashSet<CompiledRule>> getPredicateDefiningRules() {
		return Collections.unmodifiableMap(predicateDefiningRules);
	}

	@Override
	public Map<Predicate, LinkedHashSet<Instance>> getFactsByPredicate() {
		return Collections.unmodifiableMap(factsByPredicate);
	}

	@Override
	public Map<Integer, CompiledRule> getRulesById() {
		return Collections.unmodifiableMap(rulesById);
	}

	public NormalProgram toNormalProgram() {
		List<NormalRule> normalRules = new ArrayList<>();
		for (CompiledRule rule : getRules()) {
			normalRules.add(new NormalRuleImpl(rule.getHead(), new ArrayList<>(rule.getBody())));
		}
		return new NormalProgramImpl(normalRules, getFacts(), getInlineDirectives());
	}

}
