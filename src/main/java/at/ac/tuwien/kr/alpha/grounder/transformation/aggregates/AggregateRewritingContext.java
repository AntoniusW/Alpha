package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import static at.ac.tuwien.kr.alpha.Util.oops;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;

/**
 * Holds all information about aggregate literals that need to be rewritten within a program.
 * Specifically, a rewriting context holds a list of rules with aggregates, as well as detailed analysis results like
 * global variables and dependencies per aggregate (see AggregateInfo).
 * 
 * Copyright (c) 2021, the Alpha Team.
 */
public final class AggregateRewritingContext {

	private static final ST AGGREGATE_RESULT_TEMPLATE = new ST("<id>_result");
	private static final ST AGGREGATE_ARGS_FUNCTION_SYMBOL = new ST("<id>_args");
	private static final ST AGGREGATE_ARGS_NOARGS_CONST = new ST("<id>_no_args");

	private int idCounter;
	private Map<AggregateLiteral, AggregateInfo> aggregateInfos = new HashMap<>(); // Maps aggregate literals to their respective AggregateInfo.
	private Map<ImmutablePair<AggregateFunctionSymbol, ComparisonOperator>, Set<AggregateInfo>> aggregateFunctionsToRewrite = new LinkedHashMap<>();
	private List<BasicRule> rulesWithAggregates = new ArrayList<>();

	public AggregateRewritingContext() {
		idCounter = 0;
	}

	/**
	 * Registers a rule that potentially contains one or more {@link AggregateLiteral}s with this context.
	 * In case aggregates are found in the rule, global variables and dependencies of the aggregate are calculated and the
	 * aggregate literal is stored in the rewriting context along with a reference to the rule it occurs in
	 * 
	 * @param rule
	 * @return true if the given rule contains one or more aggregate literals, false otherwise
	 */
	public boolean registerRule(BasicRule rule) {
		AggregateRewritingRuleAnalysis ruleAnalysis = AggregateRewritingRuleAnalysis.analyzeRuleDependencies(rule);
		if (ruleAnalysis.aggregatesInRule.isEmpty()) {
			// Rule has no aggregates.
			return false;
		}
		// Do initial registration of each aggregate literal and keep the ids.
		for (Map.Entry<AggregateLiteral, Set<VariableTerm>> entry : ruleAnalysis.globalVariablesPerAggregate.entrySet()) {
			registerAggregateLiteral(entry.getKey(), entry.getValue());
		}
		// Now go through dependencies and replace the actual aggregate literals with their rewritten versions
		for (Map.Entry<AggregateLiteral, Set<Literal>> entry : ruleAnalysis.dependenciesPerAggregate.entrySet()) {
			AggregateInfo aggregateInfo = getAggregateInfo(entry.getKey());
			for (Literal dependency : entry.getValue()) {
				if (dependency instanceof AggregateLiteral) {
					AggregateInfo dependencyInfo = getAggregateInfo((AggregateLiteral) dependency);
					aggregateInfo.addDependency(dependencyInfo.getOutputAtom().toLiteral(!dependency.isNegated()));
				} else {
					aggregateInfo.addDependency(dependency);
				}
			}
		}
		rulesWithAggregates.add(rule);
		return true;
	}

	private void registerAggregateLiteral(AggregateLiteral lit, Set<VariableTerm> globalVariables) {
		AggregateAtom atom = lit.getAtom();
		String id = atom.getAggregatefunction().toString().toLowerCase() + "_" + (++idCounter);
		AggregateInfo info = new AggregateInfo(id, lit, globalVariables);
		if (aggregateInfos.containsKey(lit)) {
			throw oops("AggregateInfo for AggregateLiteral already existing.");
		}
		aggregateInfos.put(lit, info);
		aggregateFunctionsToRewrite.putIfAbsent(new ImmutablePair<>(atom.getAggregatefunction(), atom.getLowerBoundOperator()), new LinkedHashSet<>());
		aggregateFunctionsToRewrite.get(new ImmutablePair<>(atom.getAggregatefunction(), atom.getLowerBoundOperator())).add(info);
	}

	AggregateInfo getAggregateInfo(AggregateLiteral aggregateLiteral) {
		return aggregateInfos.get(aggregateLiteral);
	}

	List<BasicRule> getRulesWithAggregates() {
		return rulesWithAggregates;
	}

	public Map<ImmutablePair<AggregateFunctionSymbol, ComparisonOperator>, Set<AggregateInfo>> getAggregateFunctionsToRewrite() {
		return Collections.unmodifiableMap(aggregateFunctionsToRewrite);
	}

	public static class AggregateInfo {
		private final String id;
		private final AggregateLiteral literal;
		private final BasicAtom outputAtom;
		private final Set<VariableTerm> globalVariables;
		private final Term aggregateArguments;
		private final Set<Literal> dependencies = new LinkedHashSet<>();

		AggregateInfo(String id, AggregateLiteral literal, Set<VariableTerm> globalVariables) {
			this.id = id;
			this.literal = literal;
			this.globalVariables = globalVariables;
			this.aggregateArguments = buildArguments();
			this.outputAtom = buildOutputAtom();
		}

		/**
		 * Builds a term representing the global variables of the {@link AggregateLiteral} represented by this
		 * {@link AggregateInfo}. For a literal with global variables "A, B, C", the argument term is "$aggregate_id_$args(A, B,
		 * C)". For a
		 * literal without global variables, the constant "$aggregate_id$_no_args" is returned.
		 */
		private Term buildArguments() {
			Term argumentTerm;
			if (globalVariables.isEmpty()) {
				argumentTerm = ConstantTerm.getSymbolicInstance(new ST(AGGREGATE_ARGS_NOARGS_CONST).add("id", id).render());
			} else {
				argumentTerm = FunctionTerm.getInstance(new ST(AGGREGATE_ARGS_FUNCTION_SYMBOL).add("id", id).render(),
						new ArrayList<>(globalVariables));
			}
			return argumentTerm;
		}

		/**
		 * Builds an {@link Atom} which is inserted into rule bodies instead of the respective {@link AggregateLiteral}.
		 * The atom is of structure "$aggregate_id$_result($argTerm$, $resultTerm$)", where aggregate_id is the unique id of the
		 * aggregate literal as generated by {@link AggregateRewritingContext}, arg_term contains the global variables of the
		 * aggregate literal in question (or a constant "$aggregate_id$_no_args" for a literal without global variables), and
		 * resultTerm is the
		 * "lowerBoundTerm" of the aggregate literal.
		 * 
		 * @return
		 */
		private BasicAtom buildOutputAtom() {
			String outputPredicateName = new ST(AGGREGATE_RESULT_TEMPLATE).add("id", id).render();
			Term argumentTerm = aggregateArguments;
			Term resultTerm = literal.getAtom().getLowerBoundTerm();
			return new BasicAtom(Predicate.getInstance(outputPredicateName, 2, true), argumentTerm, resultTerm);
		}

		private void addDependency(Literal dependency) {
			dependencies.add(dependency);
		}

		public String getId() {
			return id;
		}

		public AggregateLiteral getLiteral() {
			return literal;
		}

		public BasicAtom getOutputAtom() {
			return outputAtom;
		}

		public Set<VariableTerm> getGlobalVariables() {
			return globalVariables;
		}

		public Term getAggregateArguments() {
			return aggregateArguments;
		}

		public Set<Literal> getDependencies() {
			return dependencies;
		}

	}

}
