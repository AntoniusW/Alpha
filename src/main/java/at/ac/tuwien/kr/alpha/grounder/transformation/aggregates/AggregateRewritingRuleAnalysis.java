package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import org.apache.commons.collections4.SetUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyses a rule and records occurring aggregates and for each aggregate its global variables and its dependencies on
 * other literals.
 *
 * Copyright (c) 2021, the Alpha Team.
 */ // Should be private, but needs to be visible to tests.
class AggregateRewritingRuleAnalysis {

	private final BasicRule rule;
	final Map<AggregateLiteral, Set<VariableTerm>> globalVariablesPerAggregate = new LinkedHashMap<>();
	final Map<AggregateLiteral, Set<Literal>> dependenciesPerAggregate = new LinkedHashMap<>();
	final List<AggregateLiteral> aggregatesInRule = new ArrayList<>();

	private AggregateRewritingRuleAnalysis(BasicRule rule) {
		this.rule = rule;
	}

	/**
	 * Analyze the given rule and record its global variables and dependencies.
	 * @param rule
	 * @return
	 */
	static AggregateRewritingRuleAnalysis analyzeRuleDependencies(BasicRule rule) {
		AggregateRewritingRuleAnalysis ruleAnalysis = new AggregateRewritingRuleAnalysis(rule);
		ruleAnalysis.findGlobalVariablesPerAggregate();
		ruleAnalysis.analyzeRuleDependencies();
		return ruleAnalysis;
	}

	private void analyzeRuleDependencies() {
		for (AggregateLiteral lit : globalVariablesPerAggregate.keySet()) {
			Set<VariableTerm> nonBindingVars = new HashSet<>(globalVariablesPerAggregate.get(lit));
			Term leftHandTerm = lit.getAtom().getLowerBoundTerm();
			if (lit.getBindingVariables().isEmpty() && leftHandTerm instanceof VariableTerm) {
				/*
				 * If the "left-hand" term LT of the literal is a variable and not binding, it has to be non-binding,
				 * i.e. the aggregate literal depends on the literals binding LT.
				 */
				nonBindingVars.add((VariableTerm) leftHandTerm);
			}
			Set<Literal> dependencies = new HashSet<>();
			Set<Literal> bodyWithoutLit = SetUtils.difference(rule.getBody(), Collections.singleton(lit));
			findBindingLiterals(nonBindingVars, new HashSet<>(), dependencies, bodyWithoutLit, globalVariablesPerAggregate);
			dependenciesPerAggregate.put(lit, dependencies);
		}
	}


	/**
	 * Recursively looks for literals in <code>searchScope</code> that bind the variables in the set
	 * <code>varsToBind</code>, i.e. any literal lit that has any variable var in question in its
	 * <code>bindingVariables</code> (i.e. lit assigns a value to var). Found binding literals are added to the set
	 * <code>boundSoFar</code>. If a literal has any of the desired variables as a binding variable, but also has other
	 * non-binding variables, the literals binding these are added to the set of desired variables for the next recursive
	 * call. Since {@link AggregateLiteral}s cannot report their non-binding variables by themselves, this method also needs
	 * a map of all aggregate literals and their global variables within the search scope.
	 */
	// Note: This algorithm has potentially exponential time complexity. Tuning potential definitely exists, but
	// performance optimization seems non-trivial.
	private static void findBindingLiterals(Set<VariableTerm> varsToBind, Set<VariableTerm> varsBoundSoFar, Set<Literal> foundSoFar,
						Set<Literal> searchScope,
						Map<AggregateLiteral, Set<VariableTerm>> aggregatesWithGlobalVars) {
		int newlyBoundVars = 0;
		Set<VariableTerm> furtherVarsToBind = new HashSet<>();
		for (VariableTerm varToBind : varsToBind) {
			for (Literal lit : searchScope) {
				Set<VariableTerm> bindingVars = lit.getBindingVariables();
				Set<VariableTerm> nonBindingVars = (lit instanceof AggregateLiteral) ? aggregatesWithGlobalVars.get((AggregateLiteral) lit)
					: lit.getNonBindingVariables();
				if (bindingVars.contains(varToBind)) {
					varsBoundSoFar.add(varToBind);
					foundSoFar.add(lit);
					newlyBoundVars++;
					for (VariableTerm nonBindingVar : nonBindingVars) {
						if (!varsBoundSoFar.contains(nonBindingVar)) {
							furtherVarsToBind.add(nonBindingVar);
						}
					}
				}
			}
		}
		if (newlyBoundVars == 0 && !varsToBind.isEmpty()) {
			// Sanity check to prevent endless recursions: If because of weird cyclic dependencies we end up with unbound variables,
			// but couldn't find any binding literals in the last run, it seems we're running in circles. Better to give up
			// screaming than producing a stack overflow ;-)
			throw new IllegalStateException("Couldn't find any literals binding variables: " + varsToBind + " in search scope " + searchScope);
		}
		if (!furtherVarsToBind.isEmpty()) {
			// As long as we find variables we still need to bind, repeat with the new set of variables to bind.
			findBindingLiterals(furtherVarsToBind, varsBoundSoFar, foundSoFar, searchScope, aggregatesWithGlobalVars);
		}
	}

	private void findGlobalVariablesPerAggregate() {
		// First, compute all global variables, that is all variables occurring in a rule except those occurring
		// inside aggregate elements.
		Set<VariableTerm> globalVariables = new HashSet<>();
		if (!rule.isConstraint()) {
			NormalHead head = (NormalHead)rule.getHead();	// Head must be normal at this point.
			globalVariables.addAll(head.getAtom().getOccurringVariables());
		}
		for (Literal literal : rule.getBody()) {
			if (literal instanceof AggregateLiteral) {
				aggregatesInRule.add((AggregateLiteral) literal);
				AggregateAtom aggregateAtom = (AggregateAtom) literal.getAtom();
				// All variables in the bounds of an aggregate are also global variables.
				// Note that at this point, only lower bounds appear in aggregates.
				globalVariables.addAll(aggregateAtom.getLowerBoundTerm().getOccurringVariables());
			} else {
				globalVariables.addAll(literal.getOccurringVariables());
			}
		}
		// Second, compute for each aggregate those of its variables that are global.
		for (AggregateLiteral aggregateLiteral : aggregatesInRule) {
			Set<VariableTerm> globalVariablesInAggregate = new HashSet<>();
			for (VariableTerm aggregateVariable : aggregateLiteral.getAtom().getAggregateVariables()) {
				if (globalVariables.contains(aggregateVariable)) {
					globalVariablesInAggregate.add(aggregateVariable);
				}
			}
			globalVariablesPerAggregate.put(aggregateLiteral, globalVariablesInAggregate);
		}

	}

}
