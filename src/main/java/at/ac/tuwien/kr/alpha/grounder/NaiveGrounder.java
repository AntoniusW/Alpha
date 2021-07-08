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
package at.ac.tuwien.kr.alpha.grounder;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.Assignment;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.BasicAnswerSet;
import at.ac.tuwien.kr.alpha.common.IntIterator;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.NoGoodInterface;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.InternalProgram;
import at.ac.tuwien.kr.alpha.common.rule.InternalRule;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.atoms.ChoiceAtom;
import at.ac.tuwien.kr.alpha.grounder.atoms.RuleAtom;
import at.ac.tuwien.kr.alpha.grounder.bridges.Bridge;
import at.ac.tuwien.kr.alpha.grounder.heuristics.GrounderHeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.grounder.instantiation.AssignmentStatus;
import at.ac.tuwien.kr.alpha.grounder.instantiation.BindingResult;
import at.ac.tuwien.kr.alpha.grounder.instantiation.DefaultLazyGroundingInstantiationStrategy;
import at.ac.tuwien.kr.alpha.grounder.instantiation.LiteralInstantiationResult;
import at.ac.tuwien.kr.alpha.grounder.instantiation.LiteralInstantiator;
import at.ac.tuwien.kr.alpha.grounder.structure.AnalyzeUnjustified;

/**
 * A semi-naive grounder.
 *
 * Copyright (c) 2016-2020, the Alpha Team.
 */
public class NaiveGrounder extends BridgedGrounder implements ProgramAnalyzingGrounder {
	private static final Logger LOGGER = LoggerFactory.getLogger(NaiveGrounder.class);

	private final WorkingMemory workingMemory = new WorkingMemory();
	private final AtomStore atomStore;
	private final NogoodRegistry registry = new NogoodRegistry();
	final NoGoodGenerator noGoodGenerator;
	private final ChoiceRecorder choiceRecorder;
	private final InternalProgram program;
	private final AnalyzeUnjustified analyzeUnjustified;

	private final Map<Predicate, LinkedHashSet<Instance>> factsFromProgram;
	private final Map<IndexedInstanceStorage, ArrayList<FirstBindingAtom>> rulesUsingPredicateWorkingMemory = new HashMap<>();
	private final Map<Integer, InternalRule> knownNonGroundRules;

	private ArrayList<InternalRule> fixedRules = new ArrayList<>();
	private LinkedHashSet<Atom> removeAfterObtainingNewNoGoods = new LinkedHashSet<>();
	private final boolean debugInternalChecks;

	private final GrounderHeuristicsConfiguration heuristicsConfiguration;

	// Handles instantiation of literals, i.e. supplies ground substitutions for literals of non-ground rules
	// according to the rules set by the LiteralInstantiationStrategy used by this grounder.
	private final LiteralInstantiator ruleInstantiator;
	private final DefaultLazyGroundingInstantiationStrategy instantiationStrategy;

	public NaiveGrounder(InternalProgram program, AtomStore atomStore, boolean debugInternalChecks, Bridge... bridges) {
		this(program, atomStore, new GrounderHeuristicsConfiguration(), debugInternalChecks, bridges);
	}

	private NaiveGrounder(InternalProgram program, AtomStore atomStore, GrounderHeuristicsConfiguration heuristicsConfiguration, boolean debugInternalChecks,
			Bridge... bridges) {
		this(program, atomStore, p -> true, heuristicsConfiguration, debugInternalChecks, bridges);
	}

	NaiveGrounder(InternalProgram program, AtomStore atomStore, java.util.function.Predicate<Predicate> filter,
			GrounderHeuristicsConfiguration heuristicsConfiguration, boolean debugInternalChecks, Bridge... bridges) {
		super(filter, bridges);
		this.atomStore = atomStore;
		this.heuristicsConfiguration = heuristicsConfiguration;
		LOGGER.debug("Grounder configuration: {}", heuristicsConfiguration);

		this.program = program;

		this.factsFromProgram = program.getFactsByPredicate();
		this.knownNonGroundRules = program.getRulesById();

		this.analyzeUnjustified = new AnalyzeUnjustified(this.program, this.atomStore, this.factsFromProgram);

		this.initializeFactsAndRules();

		final Set<InternalRule> uniqueGroundRulePerGroundHead = getRulesWithUniqueHead();
		choiceRecorder = new ChoiceRecorder(atomStore);
		noGoodGenerator = new NoGoodGenerator(atomStore, choiceRecorder, factsFromProgram, this.program, uniqueGroundRulePerGroundHead);

		this.debugInternalChecks = debugInternalChecks;

		// Initialize RuleInstantiator and instantiation strategy. Note that the instantiation strategy also
		// needs the current assignment, which is set with every call of getGroundInstantiations.
		this.instantiationStrategy = new DefaultLazyGroundingInstantiationStrategy(this.workingMemory, this.atomStore, this.factsFromProgram,
				this.heuristicsConfiguration.isAccumulatorEnabled());
		this.instantiationStrategy.setStaleWorkingMemoryEntries(this.removeAfterObtainingNewNoGoods);
		this.ruleInstantiator = new LiteralInstantiator(this.instantiationStrategy);
	}

	private void initializeFactsAndRules() {
		// Initialize all facts.
		for (Atom fact : program.getFacts()) {
			final Predicate predicate = fact.getPredicate();

			// Record predicate
			workingMemory.initialize(predicate);
		}

		// Register internal atoms.
		workingMemory.initialize(RuleAtom.PREDICATE);
		workingMemory.initialize(ChoiceAtom.OFF);
		workingMemory.initialize(ChoiceAtom.ON);

		// Initialize rules and constraints in working memory.
		for (InternalRule nonGroundRule : program.getRulesById().values()) {
			// Create working memories for all predicates occurring in the rule.
			for (Predicate predicate : nonGroundRule.getOccurringPredicates()) {
				// FIXME: this also contains interval/builtin predicates that are not needed.
				workingMemory.initialize(predicate);
			}

			// If the rule has fixed ground instantiations, it is not registered but grounded once like facts.
			if (nonGroundRule.getGroundingOrders().fixedInstantiation()) {
				fixedRules.add(nonGroundRule);
				continue;
			}

			// Register each starting literal at the corresponding working memory.
			for (Literal literal : nonGroundRule.getGroundingOrders().getStartingLiterals()) {
				registerLiteralAtWorkingMemory(literal, nonGroundRule);
			}
		}
	}

	private Set<InternalRule> getRulesWithUniqueHead() {
		// FIXME: below optimisation (adding support nogoods if there is only one rule instantiation per unique atom over the interpretation) could
		// be done as a transformation (adding a non-ground constraint corresponding to the nogood that is generated by the grounder).
		// Record all unique rule heads.
		final Set<InternalRule> uniqueGroundRulePerGroundHead = new HashSet<>();

		for (Map.Entry<Predicate, LinkedHashSet<InternalRule>> headDefiningRules : program.getPredicateDefiningRules().entrySet()) {
			if (headDefiningRules.getValue().size() != 1) {
				continue;
			}

			InternalRule nonGroundRule = headDefiningRules.getValue().iterator().next();
			// Check that all variables of the body also occur in the head (otherwise grounding is not unique).
			Atom headAtom = nonGroundRule.getHeadAtom();

			// Rule is not guaranteed unique if there are facts for it.
			HashSet<Instance> potentialFacts = factsFromProgram.get(headAtom.getPredicate());
			if (potentialFacts != null && !potentialFacts.isEmpty()) {
				continue;
			}

			// Collect head and body variables.
			HashSet<VariableTerm> occurringVariablesHead = new HashSet<>(headAtom.toLiteral().getBindingVariables());
			HashSet<VariableTerm> occurringVariablesBody = new HashSet<>();
			for (Literal lit : nonGroundRule.getPositiveBody()) {
				occurringVariablesBody.addAll(lit.getBindingVariables());
			}
			occurringVariablesBody.removeAll(occurringVariablesHead);

			// Check if ever body variables occurs in the head.
			if (occurringVariablesBody.isEmpty()) {
				uniqueGroundRulePerGroundHead.add(nonGroundRule);
			}
		}
		return uniqueGroundRulePerGroundHead;
	}

	/**
	 * Registers a starting literal of a NonGroundRule at its corresponding working memory.
	 * 
	 * @param nonGroundRule the rule in which the literal occurs.
	 */
	private void registerLiteralAtWorkingMemory(Literal literal, InternalRule nonGroundRule) {
		if (literal.isNegated()) {
			throw new RuntimeException("Literal to register is negated. Should not happen.");
		}
		IndexedInstanceStorage workingMemory = this.workingMemory.get(literal.getPredicate(), true);
		rulesUsingPredicateWorkingMemory.putIfAbsent(workingMemory, new ArrayList<>());
		rulesUsingPredicateWorkingMemory.get(workingMemory).add(new FirstBindingAtom(nonGroundRule, literal));
	}

	@Override
	public AnswerSet assignmentToAnswerSet(Iterable<Integer> trueAtoms) {
		Map<Predicate, SortedSet<Atom>> predicateInstances = new LinkedHashMap<>();
		SortedSet<Predicate> knownPredicates = new TreeSet<>();

		// Iterate over all true atomIds, computeNextAnswerSet instances from atomStore and add them if not filtered.
		for (int trueAtom : trueAtoms) {
			final Atom atom = atomStore.get(trueAtom);
			Predicate predicate = atom.getPredicate();

			// Skip atoms over internal predicates.
			if (predicate.isInternal()) {
				continue;
			}

			// Skip filtered predicates.
			if (!filter.test(predicate)) {
				continue;
			}

			knownPredicates.add(predicate);
			predicateInstances.putIfAbsent(predicate, new TreeSet<>());
			Set<Atom> instances = predicateInstances.get(predicate);
			instances.add(atom);
		}

		// Add true atoms from facts.
		for (Map.Entry<Predicate, LinkedHashSet<Instance>> facts : factsFromProgram.entrySet()) {
			Predicate factPredicate = facts.getKey();
			// Skip atoms over internal predicates.
			if (factPredicate.isInternal()) {
				continue;
			}
			// Skip filtered predicates.
			if (!filter.test(factPredicate)) {
				continue;
			}
			// Skip predicates without any instances.
			if (facts.getValue().isEmpty()) {
				continue;
			}
			knownPredicates.add(factPredicate);
			predicateInstances.putIfAbsent(factPredicate, new TreeSet<>());
			for (Instance factInstance : facts.getValue()) {
				SortedSet<Atom> instances = predicateInstances.get(factPredicate);
				instances.add(new BasicAtom(factPredicate, factInstance.terms));
			}
		}

		if (knownPredicates.isEmpty()) {
			return BasicAnswerSet.EMPTY;
		}

		return new BasicAnswerSet(knownPredicates, predicateInstances);
	}

	/**
	 * Prepares facts of the input program for joining and derives all NoGoods representing ground rules. May only be called once.
	 * 
	 * @return
	 */
	protected HashMap<Integer, NoGood> bootstrap() {
		final HashMap<Integer, NoGood> groundNogoods = new LinkedHashMap<>();

		for (Predicate predicate : factsFromProgram.keySet()) {
			// Instead of generating NoGoods, add instance to working memories directly.
			workingMemory.addInstances(predicate, true, factsFromProgram.get(predicate));
		}

		for (InternalRule nonGroundRule : fixedRules) {
			// Generate NoGoods for all rules that have a fixed grounding.
			RuleGroundingOrder groundingOrder = nonGroundRule.getGroundingOrders().getFixedGroundingOrder();
			BindingResult bindingResult = getGroundInstantiations(nonGroundRule, groundingOrder, new Substitution(), null);
			groundAndRegister(nonGroundRule, bindingResult.getGeneratedSubstitutions(), groundNogoods);
		}

		fixedRules = null;

		return groundNogoods;
	}

	@Override
	public Map<Integer, NoGood> getNoGoods(Assignment currentAssignment) {
		// In first call, prepare facts and ground rules.
		final Map<Integer, NoGood> newNoGoods = fixedRules != null ? bootstrap() : new LinkedHashMap<>();

		// Compute new ground rule (evaluate joins with newly changed atoms)
		for (IndexedInstanceStorage modifiedWorkingMemory : workingMemory.modified()) {
			// Skip predicates solely used in the solver which do not occur in rules.
			Predicate workingMemoryPredicate = modifiedWorkingMemory.getPredicate();
			if (workingMemoryPredicate.isSolverInternal()) {
				continue;
			}

			// Iterate over all rules whose body contains the interpretation corresponding to the current workingMemory.
			final ArrayList<FirstBindingAtom> firstBindingAtoms = rulesUsingPredicateWorkingMemory.get(modifiedWorkingMemory);

			// Skip working memories that are not used by any rule.
			if (firstBindingAtoms == null) {
				continue;
			}

			for (FirstBindingAtom firstBindingAtom : firstBindingAtoms) {
				// Use the recently added instances from the modified working memory to construct an initial substitution
				InternalRule nonGroundRule = firstBindingAtom.rule;

				// Generate substitutions from each recent instance.
				for (Instance instance : modifiedWorkingMemory.getRecentlyAddedInstances()) {
					// Check instance if it matches with the atom.

					final Substitution unifier = Substitution.specializeSubstitution(firstBindingAtom.startingLiteral, instance,
							Substitution.EMPTY_SUBSTITUTION);

					if (unifier == null) {
						continue;
					}

					final BindingResult bindingResult = getGroundInstantiations(
							nonGroundRule,
							nonGroundRule.getGroundingOrders().orderStartingFrom(firstBindingAtom.startingLiteral),
							unifier,
							currentAssignment);

					groundAndRegister(nonGroundRule, bindingResult.getGeneratedSubstitutions(), newNoGoods);
				}
			}

			// Mark instances added by updateAssignment as done
			modifiedWorkingMemory.markRecentlyAddedInstancesDone();
		}

		workingMemory.reset();
		for (Atom removeAtom : removeAfterObtainingNewNoGoods) {
			final IndexedInstanceStorage storage = workingMemory.get(removeAtom, true);
			Instance instance = new Instance(removeAtom.getTerms());
			if (storage.containsInstance(instance)) {
				// permissive grounder heuristics may attempt to remove instances that are not yet in the working memory
				storage.removeInstance(instance);
			}
		}

		// Re-Initialize the stale working memory entries set and pass to instantiation strategy.
		removeAfterObtainingNewNoGoods = new LinkedHashSet<>();
		instantiationStrategy.setStaleWorkingMemoryEntries(removeAfterObtainingNewNoGoods);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Grounded NoGoods are:");
			for (Map.Entry<Integer, NoGood> noGoodEntry : newNoGoods.entrySet()) {
				LOGGER.debug("{} == {}", noGoodEntry.getValue(), atomStore.noGoodToString(noGoodEntry.getValue()));
			}
			LOGGER.debug("{}", choiceRecorder);
		}

		if (debugInternalChecks) {
			checkTypesOfNoGoods(newNoGoods.values());
		}

		return newNoGoods;
	}

	/**
	 * Grounds the given {@code nonGroundRule} by applying the given {@code substitutions} and registers the nogoods generated during that
	 * process.
	 *
	 * @param nonGroundRule the rule to be grounded.
	 * @param substitutions the substitutions to be applied.
	 * @param newNoGoods    a set of nogoods to which newly generated nogoods will be added.
	 */
	private void groundAndRegister(final InternalRule nonGroundRule, final List<Substitution> substitutions, final Map<Integer, NoGood> newNoGoods) {
		for (Substitution substitution : substitutions) {
			List<NoGood> generatedNoGoods = noGoodGenerator.generateNoGoodsFromGroundSubstitution(nonGroundRule, substitution);
			registry.register(generatedNoGoods, newNoGoods);
		}
	}

	@Override
	public int register(NoGood noGood) {
		return registry.register(noGood);
	}

	// Ideally, this method should be private. It's only visible because NaiveGrounderTest needs to access it.
	BindingResult getGroundInstantiations(InternalRule rule, RuleGroundingOrder groundingOrder, Substitution partialSubstitution,
			Assignment currentAssignment) {
		int tolerance = heuristicsConfiguration.getTolerance(rule.isConstraint());
		if (tolerance < 0) {
			tolerance = Integer.MAX_VALUE;
		}

		// Update instantiationStrategy with current assignment.
		// Note: Actually the assignment could be an instance variable of the grounder (shared with solver),
		// but this would have a larger impact on grounder/solver communication design as a whole.
		instantiationStrategy.setCurrentAssignment(currentAssignment);
		BindingResult bindingResult = bindNextAtomInRule(groundingOrder, 0, tolerance, tolerance, partialSubstitution);
		if (LOGGER.isDebugEnabled()) {
			for (int i = 0; i < bindingResult.size(); i++) {
				Integer numberOfUnassignedPositiveBodyAtoms = bindingResult.getNumbersOfUnassignedPositiveBodyAtoms().get(i);
				if (numberOfUnassignedPositiveBodyAtoms > 0) {
					LOGGER.debug("Grounded rule in which {} positive atoms are still unassigned: {} (substitution: {})", numberOfUnassignedPositiveBodyAtoms,
							rule, bindingResult.getGeneratedSubstitutions().get(i));
				}
			}
		}
		return bindingResult;
	}

	/**
	 * Helper method used by {@link NaiveGrounder#bindNextAtomInRule(RuleGroundingOrder, int, int, int, Substitution)}.
	 * 
	 * Takes an <code>ImmutablePair</code> of a {@link Substitution} and an accompanying {@link AssignmentStatus} and calls
	 * <code>bindNextAtomInRule</code> for the next literal in the grounding order.
	 * If the assignment status for the last bound literal was {@link AssignmentStatus#UNASSIGNED}, the <code>remainingTolerance</code>
	 * parameter is decreased by 1. If the remaining tolerance drops below zero, this method returns an empty {@link BindingResult}.
	 * 
	 * @param groundingOrder
	 * @param orderPosition
	 * @param originalTolerance
	 * @param remainingTolerance
	 * @param lastLiteralBindingResult
	 * @return the result of calling bindNextAtomInRule on the next literal in the grounding order, or an empty binding result if remaining
	 *         tolerance is less than zero.
	 */
	private BindingResult continueBinding(RuleGroundingOrder groundingOrder, int orderPosition, int originalTolerance, int remainingTolerance,
			ImmutablePair<Substitution, AssignmentStatus> lastLiteralBindingResult) {
		Substitution substitution = lastLiteralBindingResult.left;
		AssignmentStatus lastBoundLiteralAssignmentStatus = lastLiteralBindingResult.right;
		switch (lastBoundLiteralAssignmentStatus) {
			case TRUE:
				return advanceAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingTolerance, substitution);
			case UNASSIGNED:
				// The last literal bound to obtain the current substitution has not been assigned a truth value by the solver yet.
				// If we still have enough tolerance, we can continue grounding nevertheless.
				int toleranceForNextRun = remainingTolerance - 1;
				if (toleranceForNextRun >= 0) {
					return advanceAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, toleranceForNextRun, substitution);
				} else {
					return BindingResult.empty();
				}
			case FALSE:
				throw Util.oops("Got an assignmentStatus FALSE for literal " + groundingOrder.getLiteralAtOrderPosition(orderPosition) + " and substitution "
						+ substitution + " - should not happen!");
			default:
				throw Util.oops("Got unsupported assignmentStatus " + lastBoundLiteralAssignmentStatus);
		}
	}

	private BindingResult advanceAndBindNextAtomInRule(RuleGroundingOrder groundingOrder, int orderPosition, int originalTolerance, int remainingTolerance,
			Substitution partialSubstitution) {
		groundingOrder.considerUntilCurrentEnd();
		return bindNextAtomInRule(groundingOrder, orderPosition + 1, originalTolerance, remainingTolerance, partialSubstitution);
	}

	private BindingResult pushBackAndBindNextAtomInRule(RuleGroundingOrder groundingOrder, int orderPosition, int originalTolerance, int remainingTolerance,
			Substitution partialSubstitution) {
		RuleGroundingOrder modifiedGroundingOrder = groundingOrder.pushBack(orderPosition);
		if (modifiedGroundingOrder == null) {
			return BindingResult.empty();
		}
		return bindNextAtomInRule(modifiedGroundingOrder, orderPosition + 1, originalTolerance, remainingTolerance, partialSubstitution);
	}

	//@formatter:off
	/**
	 * Computes ground substitutions for a literal based on a {@link RuleGroundingOrder} and a {@link Substitution}.
	 * 
	 * Computes ground substitutions for the literal at position <code>orderPosition</code> of <code>groundingOrder</code>
	 * Actual substitutions are computed by this grounder's {@link LiteralInstantiator}. 
	 * 
	 * @param groundingOrder a {@link RuleGroundingOrder} representing the body literals of a rule in the 
	 * 						 sequence in which the should be bound during grounding.
	 * @param orderPosition the current position within <code>groundingOrder</code>, indicates which literal should be bound
	 * @param originalTolerance the original tolerance of the used grounding heuristic
	 * @param remainingTolerance the remaining tolerance, determining if binding continues in the presence of substitutions based on unassigned atoms
	 * @param partialSubstitution a substitution
	 * @return a {@link BindingResult} representing applicable ground substitutions for all literals after orderPosition in groundingOrder
	 */
	//@formatter:on
	private BindingResult bindNextAtomInRule(RuleGroundingOrder groundingOrder, int orderPosition, int originalTolerance, int remainingTolerance,
			Substitution partialSubstitution) {
		Literal currentLiteral = groundingOrder.getLiteralAtOrderPosition(orderPosition);
		if (currentLiteral == null) {
			LOGGER.trace("No more literals found in grounding order, therefore stopping binding!");
			return BindingResult.singleton(partialSubstitution, originalTolerance - remainingTolerance);
		}
		LOGGER.trace("Binding current literal {} with remaining tolerance {} and partial substitution {}.", currentLiteral,
				remainingTolerance, partialSubstitution);
		LiteralInstantiationResult instantiationResult = ruleInstantiator.instantiateLiteral(currentLiteral, partialSubstitution);
		switch (instantiationResult.getType()) {
			case CONTINUE:
				/*
				 * Recursively call bindNextAtomInRule for each generated substitution
				 * and the next literal in the grounding order (i.e. advance), thereby reducing remaining
				 * tolerance by 1 iff a substitution uses an unassigned ground atom.
				 * If remainingTolerance falls below zero, an empty {@link BindingResult} is returned.
				 */
				List<ImmutablePair<Substitution, AssignmentStatus>> substitutionInfos = instantiationResult.getSubstitutions();
				LOGGER.trace("Literal instantiator yielded {} substitutions for literal {}.", substitutionInfos.size(), currentLiteral);
				BindingResult retVal = new BindingResult();
				for (ImmutablePair<Substitution, AssignmentStatus> substitutionInfo : substitutionInfos) {
					retVal.add(this.continueBinding(groundingOrder, orderPosition, originalTolerance, remainingTolerance,
							substitutionInfo));
				}
				return retVal;
			case PUSH_BACK:
				/*
				 * Delegate to pushBackAndBindNextAtomInRule(RuleGroundingOrder, int, int, int, Substitution, Assignment).
				 * Pushes the current literal to the end of the grounding order and calls bindNextAtomInRule with the modified grounding oder.
				 */
				LOGGER.trace("Pushing back literal {} in grounding order.", currentLiteral);
				return pushBackAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingTolerance, partialSubstitution);
			case MAYBE_PUSH_BACK:
				/*
				 * Indicates that the rule instantiator could not find any substitutions for the current literal. If a permissive grounder heuristic is in
				 * use, push the current literal to the end of the grounding order and proceed with the next one, otherwise return an empty BindingResult.
				 */
				if (originalTolerance > 0) {
					LOGGER.trace(
							"No substitutions yielded by literal instantiator for literal {}, but using permissive heuristic, therefore pushing the literal back.",
							currentLiteral);
					// This occurs when the grounder heuristic in use is a "permissive" one,
					// i.e. it is deemed acceptable to have ground rules where a number of body atoms are not yet assigned a truth value by the solver.
					return pushBackAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingTolerance, partialSubstitution);
				} else {
					LOGGER.trace("No substitutions found for literal {}", currentLiteral);
					return BindingResult.empty();
				}
			case STOP_BINDING:
				LOGGER.trace("No substitutions found for literal {}", currentLiteral);
				return BindingResult.empty();
			default:
				throw Util.oops("Unhandled literal instantiation result type: " + instantiationResult.getType());
		}
	}

	@Override
	public Pair<Map<Integer, Integer>, Map<Integer, Integer>> getChoiceAtoms() {
		return choiceRecorder.getAndResetChoices();
	}

	@Override
	public Map<Integer, Set<Integer>> getHeadsToBodies() {
		return choiceRecorder.getAndResetHeadsToBodies();
	}

	@Override
	public void updateAssignment(IntIterator it) {
		while (it.hasNext()) {
			workingMemory.addInstance(atomStore.get(it.next()), true);
		}
	}

	@Override
	public void forgetAssignment(int[] atomIds) {
		throw new UnsupportedOperationException("Forgetting assignments is not implemented");
	}

	@Override
	public InternalRule getNonGroundRule(Integer ruleId) {
		return knownNonGroundRules.get(ruleId);
	}

	@Override
	public boolean isFact(Atom atom) {
		LinkedHashSet<Instance> instances = factsFromProgram.get(atom.getPredicate());
		if (instances == null) {
			return false;
		}
		return instances.contains(new Instance(atom.getTerms()));
	}

	@Override
	public Set<Literal> justifyAtom(int atomToJustify, Assignment currentAssignment) {
		Set<Literal> literals = analyzeUnjustified.analyze(atomToJustify, currentAssignment);
		// Remove facts from justification before handing it over to the solver.
		for (Iterator<Literal> iterator = literals.iterator(); iterator.hasNext();) {
			Literal literal = iterator.next();
			if (literal.isNegated()) {
				continue;
			}
			LinkedHashSet<Instance> factsOverPredicate = factsFromProgram.get(literal.getPredicate());
			if (factsOverPredicate != null && factsOverPredicate.contains(new Instance(literal.getAtom().getTerms()))) {
				iterator.remove();
			}
		}
		return literals;
	}

	/**
	 * Checks that every nogood not marked as {@link NoGoodInterface.Type#INTERNAL} contains only
	 * atoms which are not {@link Predicate#isSolverInternal()} (except {@link RuleAtom}s, which are allowed).
	 * 
	 * @param newNoGoods
	 */
	private void checkTypesOfNoGoods(Collection<NoGood> newNoGoods) {
		for (NoGood noGood : newNoGoods) {
			if (noGood.getType() != NoGoodInterface.Type.INTERNAL) {
				for (int literal : noGood) {
					Atom atom = atomStore.get(atomOf(literal));
					if (atom.getPredicate().isSolverInternal() && !(atom instanceof RuleAtom)) {
						throw oops("NoGood containing atom of internal predicate " + atom + " is " + noGood.getType() + " instead of INTERNAL");
					}
				}
			}
		}
	}

	private static class FirstBindingAtom {
		final InternalRule rule;
		final Literal startingLiteral;

		FirstBindingAtom(InternalRule rule, Literal startingLiteral) {
			this.rule = rule;
			this.startingLiteral = startingLiteral;
		}
	}

}
