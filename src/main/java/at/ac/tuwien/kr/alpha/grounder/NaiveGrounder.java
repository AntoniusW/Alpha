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

import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.Assignment;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.BasicAnswerSet;
import at.ac.tuwien.kr.alpha.common.IntIterator;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.NoGoodInterface;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.Program;
import at.ac.tuwien.kr.alpha.common.Rule;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.ExternalLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.FixedInterpretationLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.atoms.ChoiceAtom;
import at.ac.tuwien.kr.alpha.grounder.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.grounder.atoms.IntervalLiteral;
import at.ac.tuwien.kr.alpha.grounder.atoms.RuleAtom;
import at.ac.tuwien.kr.alpha.grounder.bridges.Bridge;
import at.ac.tuwien.kr.alpha.grounder.heuristics.GrounderHeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.grounder.structure.AnalyzeUnjustified;
import at.ac.tuwien.kr.alpha.grounder.structure.AtomChoiceRelation;
import at.ac.tuwien.kr.alpha.grounder.structure.ProgramAnalysis;
import at.ac.tuwien.kr.alpha.grounder.transformation.CardinalityNormalization;
import at.ac.tuwien.kr.alpha.grounder.transformation.ChoiceHeadToNormal;
import at.ac.tuwien.kr.alpha.grounder.transformation.EnumerationRewriting;
import at.ac.tuwien.kr.alpha.grounder.transformation.IntervalTermToIntervalAtom;
import at.ac.tuwien.kr.alpha.grounder.transformation.SumNormalization;
import at.ac.tuwien.kr.alpha.grounder.transformation.VariableEqualityRemoval;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
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

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;
import static java.util.Collections.singletonList;

/**
 * A semi-naive grounder.
 * Copyright (c) 2016-2020, the Alpha Team.
 */
public class NaiveGrounder extends BridgedGrounder implements ProgramAnalyzingGrounder {
	private static final Logger LOGGER = LoggerFactory.getLogger(NaiveGrounder.class);

	private final static int NEITHER_TERMINATE_BINDING_NOR_DECREMENT_TOLERANCE = 0;
	private final static int TERMINATE_BINDING = 1;
	private final static int DECREMENT_TOLERANCE = 2;

	private final WorkingMemory workingMemory = new WorkingMemory();
	private final AtomStore atomStore;
	private final NogoodRegistry registry = new NogoodRegistry();
	final NoGoodGenerator noGoodGenerator;
	private final ChoiceRecorder choiceRecorder;
	private final ProgramAnalysis programAnalysis;
	private final AnalyzeUnjustified analyzeUnjustified;

	private final Map<Predicate, LinkedHashSet<Instance>> factsFromProgram = new LinkedHashMap<>();
	private final Map<IndexedInstanceStorage, ArrayList<FirstBindingAtom>> rulesUsingPredicateWorkingMemory = new HashMap<>();
	private final Map<NonGroundRule, HashSet<Substitution>> knownGroundingSubstitutions = new HashMap<>();
	private final Map<Integer, NonGroundRule> knownNonGroundRules = new HashMap<>();
	private final AtomChoiceRelation atomChoiceRelation = new AtomChoiceRelation();

	private ArrayList<NonGroundRule> fixedRules = new ArrayList<>();
	private LinkedHashSet<Atom> removeAfterObtainingNewNoGoods = new LinkedHashSet<>();
	private final boolean useCountingGridNormalization;
	private final boolean debugInternalChecks;

	private final GrounderHeuristicsConfiguration heuristicsConfiguration;

	public NaiveGrounder(Program program, AtomStore atomStore, boolean debugInternalChecks, Bridge... bridges) {
		this(program, atomStore, new GrounderHeuristicsConfiguration(), debugInternalChecks, bridges);
	}

	private NaiveGrounder(Program program, AtomStore atomStore, GrounderHeuristicsConfiguration heuristicsConfiguration, boolean debugInternalChecks, Bridge... bridges) {
		this(program, atomStore, p -> true, heuristicsConfiguration, false, debugInternalChecks, bridges);
	}

	NaiveGrounder(Program program, AtomStore atomStore, java.util.function.Predicate<Predicate> filter, GrounderHeuristicsConfiguration heuristicsConfiguration, boolean useCountingGrid, boolean debugInternalChecks, Bridge... bridges) {
		super(filter, bridges);
		this.atomStore = atomStore;
		this.heuristicsConfiguration = heuristicsConfiguration;
		LOGGER.debug("Grounder configuration: {}", heuristicsConfiguration);

		programAnalysis = new ProgramAnalysis(program);
		analyzeUnjustified = new AnalyzeUnjustified(programAnalysis, atomStore, factsFromProgram);

		// Apply program transformations/rewritings.
		useCountingGridNormalization = useCountingGrid;
		applyProgramTransformations(program);
		LOGGER.debug("Transformed input program is:\n{}", program);

		initializeFactsAndRules(program);

		final Set<NonGroundRule> uniqueGroundRulePerGroundHead = getRulesWithUniqueHead();
		choiceRecorder = new ChoiceRecorder(atomStore);
		noGoodGenerator = new NoGoodGenerator(atomStore, choiceRecorder, factsFromProgram, programAnalysis, uniqueGroundRulePerGroundHead, atomChoiceRelation);
		this.debugInternalChecks = debugInternalChecks;
	}

	@Override
	public AtomChoiceRelation getAtomChoiceRelation() {
		return atomChoiceRelation;
	}

	private void initializeFactsAndRules(Program program) {
		// initialize all facts
		for (Atom fact : program.getFacts()) {
			final Predicate predicate = fact.getPredicate();

			// Record predicate
			workingMemory.initialize(predicate);

			// Construct fact instance(s).
			List<Instance> instances = FactIntervalEvaluator.constructFactInstances(fact);

			// Add instances to corresponding list of facts.
			factsFromProgram.putIfAbsent(predicate, new LinkedHashSet<>());
			HashSet<Instance> internalPredicateInstances = factsFromProgram.get(predicate);
			internalPredicateInstances.addAll(instances);
		}

		// Register internal atoms.
		workingMemory.initialize(RuleAtom.PREDICATE);
		workingMemory.initialize(ChoiceAtom.OFF);
		workingMemory.initialize(ChoiceAtom.ON);

		// Initialize rules and constraints.
		for (Rule rule : program.getRules()) {
			// Record the rule for later use
			NonGroundRule nonGroundRule = NonGroundRule.constructNonGroundRule(rule);
			knownNonGroundRules.put(nonGroundRule.getRuleId(), nonGroundRule);
			LOGGER.debug("NonGroundRule #{}: {}", nonGroundRule.getRuleId(), nonGroundRule);

			// Record defining rules for each predicate.
			Atom headAtom = nonGroundRule.getHeadAtom();
			if (headAtom != null) {
				Predicate headPredicate = headAtom.getPredicate();
				programAnalysis.recordDefiningRule(headPredicate, nonGroundRule);
			}

			// Create working memories for all predicates occurring in the rule
			for (Predicate predicate : nonGroundRule.getOccurringPredicates()) {
				// FIXME: this also contains interval/builtin predicates that are not needed.
				workingMemory.initialize(predicate);
			}

			// If the rule has fixed ground instantiations, it is not registered but grounded once like facts.
			if (nonGroundRule.groundingOrder.fixedInstantiation()) {
				fixedRules.add(nonGroundRule);
				continue;
			}

			// Register each starting literal at the corresponding working memory.
			for (Literal literal : nonGroundRule.groundingOrder.getStartingLiterals()) {
				registerLiteralAtWorkingMemory(literal, nonGroundRule);
			}
		}
	}

	private Set<NonGroundRule> getRulesWithUniqueHead() {
		// FIXME: below optimisation (adding support nogoods if there is only one rule instantiation per unique atom over the interpretation) could be done as a transformation (adding a non-ground constraint corresponding to the nogood that is generated by the grounder).
		// Record all unique rule heads.
		final Set<NonGroundRule> uniqueGroundRulePerGroundHead = new HashSet<>();

		for (Map.Entry<Predicate, HashSet<NonGroundRule>> headDefiningRules : programAnalysis.getPredicateDefiningRules().entrySet()) {
			if (headDefiningRules.getValue().size() != 1) {
				continue;
			}

			NonGroundRule nonGroundRule = headDefiningRules.getValue().iterator().next();
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
			for (Atom atom : nonGroundRule.getBodyAtomsPositive()) {
				occurringVariablesBody.addAll(atom.toLiteral().getBindingVariables());
			}
			occurringVariablesBody.removeAll(occurringVariablesHead);

			// Check if ever body variables occurs in the head.
			if (occurringVariablesBody.isEmpty()) {
				uniqueGroundRulePerGroundHead.add(nonGroundRule);
			}
		}
		return uniqueGroundRulePerGroundHead;
	}

	private void applyProgramTransformations(Program program) {
		// Transform choice rules.
		new ChoiceHeadToNormal().transform(program);
		// Transform cardinality aggregates.
		new CardinalityNormalization(!useCountingGridNormalization).transform(program);
		// Transform sum aggregates.
		new SumNormalization().transform(program);
		// Transform intervals.
		new IntervalTermToIntervalAtom().transform(program);
		// Remove variable equalities.
		new VariableEqualityRemoval().transform(program);
		// Transform enumeration atoms.
		new EnumerationRewriting().transform(program);
		EnumerationAtom.resetEnumerations();
	}

	/**
	 * Registers a starting literal of a NonGroundRule at its corresponding working memory.
	 * @param nonGroundRule   the rule in which the literal occurs.
	 */
	private void registerLiteralAtWorkingMemory(Literal literal, NonGroundRule nonGroundRule) {
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
	 * @return
	 */
	HashMap<Integer, NoGood> bootstrap() {
		final HashMap<Integer, NoGood> groundNogoods = new LinkedHashMap<>();

		for (Predicate predicate : factsFromProgram.keySet()) {
			// Instead of generating NoGoods, add instance to working memories directly.
			workingMemory.addInstances(predicate, true, factsFromProgram.get(predicate));
		}

		for (NonGroundRule nonGroundRule : fixedRules) {
			// Generate NoGoods for all rules that have a fixed grounding.
			RuleGroundingOrder groundingOrder = nonGroundRule.groundingOrder.getFixedGroundingOrder();
			BindingResult bindingResult = getGroundInstantiations(nonGroundRule, groundingOrder, new Substitution(), null);
			groundAndRegister(nonGroundRule, bindingResult.generatedSubstitutions, groundNogoods);
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
				NonGroundRule nonGroundRule = firstBindingAtom.rule;

				// Generate substitutions from each recent instance.
				for (Instance instance : modifiedWorkingMemory.getRecentlyAddedInstances()) {
					// Check instance if it matches with the atom.

					final Substitution unifier = Substitution.unify(firstBindingAtom.startingLiteral, instance, new Substitution());

					if (unifier == null) {
						continue;
					}

					final BindingResult bindingResult = getGroundInstantiations(
						nonGroundRule,
						nonGroundRule.groundingOrder.orderStartingFrom(firstBindingAtom.startingLiteral),
						unifier,
						currentAssignment
					);

					groundAndRegister(nonGroundRule, bindingResult.generatedSubstitutions, newNoGoods);
				}
			}

			// Mark instances added by updateAssignment as done
			modifiedWorkingMemory.markRecentlyAddedInstancesDone();
		}

		workingMemory.reset();
		for (Atom removeAtom : removeAfterObtainingNewNoGoods) {
			final IndexedInstanceStorage storage = this.workingMemory.get(removeAtom, true);
			Instance instance = new Instance(removeAtom.getTerms());
			if (storage.containsInstance(instance)) {
				// permissive grounder heuristics may attempt to remove instances that are not yet in the working memory
				storage.removeInstance(instance);
			}
		}

		removeAfterObtainingNewNoGoods = new LinkedHashSet<>();
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
	 * Grounds the given {@code nonGroundRule} by applying the given {@code substitutions} and registers the nogoods generated during that process.
	 *
	 * @param nonGroundRule
	 *          the rule to be grounded
	 * @param substitutions
	 *          the substitutions to be applied
	 * @param newNoGoods
	 *          a set of nogoods to which newly generated nogoods will be added
	 */
	private void groundAndRegister(final NonGroundRule nonGroundRule, final List<Substitution> substitutions, final Map<Integer, NoGood> newNoGoods) {
		for (Substitution substitution : substitutions) {
			List<NoGood> generatedNoGoods = noGoodGenerator.generateNoGoodsFromGroundSubstitution(nonGroundRule, substitution);
			registry.register(generatedNoGoods, newNoGoods);
		}
	}

	@Override
	public int register(NoGood noGood) {
		return registry.register(noGood);
	}

	BindingResult getGroundInstantiations(NonGroundRule rule, RuleGroundingOrder groundingOrder, Substitution partialSubstitution, Assignment currentAssignment) {
		int tolerance = heuristicsConfiguration.getTolerance(rule.isConstraint());
		if (tolerance < 0) {
			tolerance = Integer.MAX_VALUE;
		}
		BindingResult bindingResult = bindNextAtomInRule(groundingOrder, 0, tolerance, tolerance, partialSubstitution, currentAssignment);
		if (LOGGER.isDebugEnabled()) {
			for (int i = 0; i < bindingResult.size(); i++) {
				Integer numberOfUnassignedPositiveBodyAtoms = bindingResult.numbersOfUnassignedPositiveBodyAtoms.get(i);
				if (numberOfUnassignedPositiveBodyAtoms > 0) {
					LOGGER.debug("Grounded rule in which {} positive atoms are still unassigned: {} (substitution: {})", numberOfUnassignedPositiveBodyAtoms, rule, bindingResult.generatedSubstitutions.get(i));
				}
			}
		}
		return bindingResult;
	}

	private BindingResult advanceAndBindNextAtomInRule(RuleGroundingOrder groundingOrder, int orderPosition, int originalTolerance, int remainingTolerance, Substitution partialSubstitution, Assignment currentAssignment) {
		groundingOrder.considerUntilCurrentEnd();
		return bindNextAtomInRule(groundingOrder, orderPosition + 1, originalTolerance, remainingTolerance, partialSubstitution, currentAssignment);
	}

	private BindingResult pushBackAndBindNextAtomInRule(RuleGroundingOrder groundingOrder, int orderPosition, int originalTolerance, int remainingTolerance, Substitution partialSubstitution, Assignment currentAssignment) {
		RuleGroundingOrder modifiedGroundingOrder = groundingOrder.pushBack(orderPosition);
		if (modifiedGroundingOrder == null) {
			return BindingResult.empty();
		}
		return bindNextAtomInRule(modifiedGroundingOrder, orderPosition + 1, originalTolerance, remainingTolerance, partialSubstitution, currentAssignment);
	}

	private BindingResult bindNextAtomInRule(RuleGroundingOrder groundingOrder, int orderPosition, int originalTolerance, int remainingTolerance, Substitution partialSubstitution, Assignment currentAssignment) {
		boolean permissiveGrounderHeuristic = originalTolerance > 0;

		Literal currentLiteral = groundingOrder.getLiteralAtOrderPosition(orderPosition);
		if (currentLiteral == null) {
			return BindingResult.singleton(partialSubstitution, originalTolerance - remainingTolerance);
		}

		Atom currentAtom = currentLiteral.getAtom();
		if (currentLiteral instanceof FixedInterpretationLiteral) {
			// Generate all substitutions for the builtin/external/interval atom.
			FixedInterpretationLiteral substitutedLiteral = (FixedInterpretationLiteral)currentLiteral.substitute(partialSubstitution);
			if (shallPushBackFixedInterpretationLiteral(substitutedLiteral)) {
				return pushBackAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingTolerance, partialSubstitution, currentAssignment);
			}
			final List<Substitution> substitutions = substitutedLiteral.getSatisfyingSubstitutions(partialSubstitution);

			if (substitutions.isEmpty()) {
				// if FixedInterpretationLiteral cannot be satisfied now, it will never be
				return BindingResult.empty();
			}

			final BindingResult bindingResult = new BindingResult();
			for (Substitution substitution : substitutions) {
				// Continue grounding with each of the generated values.
				bindingResult.add(advanceAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingTolerance, substitution, currentAssignment));
			}
			return bindingResult;
		}
		if (currentAtom instanceof EnumerationAtom) {
			// Get the enumeration value and add it to the current partialSubstitution.
			((EnumerationAtom) currentAtom).addEnumerationToSubstitution(partialSubstitution);
			return advanceAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingTolerance, partialSubstitution, currentAssignment);
		}

		Collection<Instance> instances = null;

		// check if partialVariableSubstitution already yields a ground atom
		final Atom substitute = currentAtom.substitute(partialSubstitution);
		if (substitute.isGround()) {
			// Substituted atom is ground, in case it is positive, only ground if it also holds true
			if (currentLiteral.isNegated()) {
				// Atom occurs negated in the rule: continue grounding
				return advanceAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingTolerance, partialSubstitution, currentAssignment);
			}

			if (!groundingOrder.isGround() && remainingTolerance <= 0
					&& !workingMemory.get(currentAtom.getPredicate(), true).containsInstance(new Instance(substitute.getTerms()))) {
				// Generate no variable substitution.
				return BindingResult.empty();
			}

			instances = singletonList(new Instance(substitute.getTerms()));
		}

		// substituted atom contains variables
		if (currentLiteral.isNegated()) {
			if (permissiveGrounderHeuristic) {
				return pushBackAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingTolerance, partialSubstitution, currentAssignment);
			} else {
				throw oops("Current atom should be positive at this point but is not");
			}
		}

		if (instances == null) {
			instances = getInstancesForSubstitute(substitute, partialSubstitution);
		}

		if (permissiveGrounderHeuristic && instances.isEmpty()) {
			// we have reached a point where we have to terminate binding,
			// but it might be possible that a different grounding order would allow us to continue binding
			// under the presence of a permissive grounder heuristic
			return pushBackAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingTolerance, partialSubstitution, currentAssignment);
		}

		return createBindings(groundingOrder, orderPosition, originalTolerance, remainingTolerance, partialSubstitution, currentAssignment, instances, substitute);
	}

	/**
	 * Any {@link FixedInterpretationLiteral} that does <emph>not</emph> fulfil any of the following conditions is
	 * "pushed back" in the grounding order because it cannot be used to generate substitutions now but maybe later:
	 * <ul>
	 * 	<li>the literal is ground</li>
	 * 	<li>the literal is a {@link ComparisonLiteral} that is left-assigning or right-assigning</li>
	 * 	<li>the literal is an {@link IntervalLiteral} representing a ground interval term</li>
	 * 	<li>the literal is an {@link ExternalLiteral}.</li>
	 * </ul>
	 * @param substitutedLiteral
	 * @return
	 */
	private boolean shallPushBackFixedInterpretationLiteral(FixedInterpretationLiteral substitutedLiteral) {
		return !(substitutedLiteral.isGround() ||
				(substitutedLiteral instanceof ComparisonLiteral && ((ComparisonLiteral)substitutedLiteral).isLeftOrRightAssigning()) ||
				(substitutedLiteral instanceof IntervalLiteral && substitutedLiteral.getTerms().get(0).isGround()) ||
				(substitutedLiteral instanceof ExternalLiteral));
	}

	private Collection<Instance> getInstancesForSubstitute(Atom substitute, Substitution partialSubstitution) {
		Collection<Instance> instances;
		IndexedInstanceStorage storage = workingMemory.get(substitute.getPredicate(), true);
		if (partialSubstitution.isEmpty()) {
			// No variables are bound, but first atom in the body became recently true, consider all instances now.
			instances = storage.getAllInstances();
		} else {
			instances = storage.getInstancesFromPartiallyGroundAtom(substitute);
		}
		return instances;
	}

	/**
	 * Uses {@code instances} to create ground instantiations for {@code substitute} and then proceeds in the {@groundingOrder}.
	 * @param groundingOrder the grounding order to follow while grounding the corresponding rule.
	 * @param orderPosition the current position in the {@code groundingOrder}.
	 * @param originalTolerance the original number of tolerated unassigned positive body literals.
	 * @param remainingTolerance the remaining number of tolerated unassigned positive body literals.
	 * @param partialSubstitution the partial substitution created so far while grounding the current rule.
	 * @param currentAssignment the current assignment.
	 * @param instances instances used to ground the current atom.
	 * @param substitute the current atom, to which {@code partialSubstitution} has already been applied.
	 * @return
	 */
	private BindingResult createBindings(RuleGroundingOrder groundingOrder, int orderPosition, int originalTolerance, int remainingTolerance, Substitution partialSubstitution, Assignment currentAssignment, Collection<Instance> instances, Atom substitute) {
		BindingResult bindingResult = new BindingResult();
		for (Instance instance : instances) {
			int remainingToleranceForThisInstance = remainingTolerance;
			// Check each instance if it matches with the atom.
			Substitution unified = Substitution.unify(substitute, instance, new Substitution(partialSubstitution));
			if (unified == null) {
				continue;
			}

			// Check if atom is also assigned true.
			Atom substituteClone = new BasicAtom(substitute.getPredicate(), substitute.getTerms());
			Atom substitutedAtom = substituteClone.substitute(unified);
			if (!substitutedAtom.isGround()) {
				throw oops("Grounded atom should be ground but is not");
			}

			if (factsFromProgram.get(substitutedAtom.getPredicate()) == null || !factsFromProgram.get(substitutedAtom.getPredicate()).contains(new Instance(substitutedAtom.getTerms()))) {
				final int terminateOrDecrement = storeAtomAndTerminateIfAtomDoesNotHold(substitutedAtom, currentAssignment, remainingToleranceForThisInstance);
				if (terminateOrDecrement == TERMINATE_BINDING) {
					continue;
				}
				if (terminateOrDecrement == DECREMENT_TOLERANCE) {
					remainingToleranceForThisInstance--;
				}
			}
			bindingResult.add(advanceAndBindNextAtomInRule(groundingOrder, orderPosition, originalTolerance, remainingToleranceForThisInstance, unified, currentAssignment));
		}

		return bindingResult;
	}

	/**
	 * Does nothing if {@code currentAssignment == null}, which means we are in bootstrapping.
	 * Otherwise, stores {@code substitute} in the atom store if it is not yet stored.
	 * Afterwards, the truth value currently assigned to this atom and the observation whether the atom is in the
	 * grounder's working memory or not are used to determine whether binding the current rule shall be terminated or not,
	 * and whether or not the remaining tolerance (for permissive grounding) shall be decremented.
	 * <p/>
	 * Binding shall not be terminated if accumulator is enabled and the atom is in the working memory.
	 * Otherwise, binding shall be terminated if either the atom is not assigned and tolerance is exhausted,
	 * or if the atom is assigned false.
	 * <p/>
	 * Tolerance shall be decremented if the atom is unassigned (and also not in the working memory, if accumulator
	 * is enabled) and binding is not terminated.
	 * <p/>
	 * If the atom is assigned false and accumulator is not enabled, the atom is also added to {@link #removeAfterObtainingNewNoGoods}
	 * to trigger lazy update of the working memory.
	 *
	 * @param substitute         the atom to store.
	 * @param currentAssignment  the current assignment.
	 * @param remainingTolerance the remaining number of positive body atoms tolerated not to be assigned.
	 * @return {@link #TERMINATE_BINDING} if binding shall be terminated; {@link #DECREMENT_TOLERANCE} if remaining
	 * tolerance shall be decremented; {@link #NEITHER_TERMINATE_BINDING_NOR_DECREMENT_TOLERANCE} if neither shall be done.
	 */
	private int storeAtomAndTerminateIfAtomDoesNotHold(final Atom substitute, final Assignment currentAssignment, final int remainingTolerance) {
		if (currentAssignment == null) { // if we are in bootstrapping
			return NEITHER_TERMINATE_BINDING_NOR_DECREMENT_TOLERANCE;
		}

		int decrementedTolerance = remainingTolerance;
		final int atomId = atomStore.putIfAbsent(substitute);
		currentAssignment.growForMaxAtomId();
		ThriceTruth truth = currentAssignment.isAssigned(atomId) ? currentAssignment.getTruth(atomId) : null;

		if (heuristicsConfiguration.isAccumulatorEnabled()) {
			// special handling for the accumulator variants of lazy-grounding strategies
			final Instance instance = new Instance(substitute.getTerms());
			boolean isInWorkingMemory = workingMemory.get(substitute, true).containsInstance(instance);
			if (isInWorkingMemory) {
				// the atom is in the working memory, so we need neither terminate nor decrement tolerance
				return NEITHER_TERMINATE_BINDING_NOR_DECREMENT_TOLERANCE;
			}
		} else if (truth == null || !truth.toBoolean()) {
			// no accumulator and the atom currently does not hold, so the working memory needs to be updated
			removeAfterObtainingNewNoGoods.add(substitute);
		}

		if (truth == null && --decrementedTolerance < 0) {
			// terminate if more positive atoms are unsatisfied as tolerated by the heuristic
			return TERMINATE_BINDING;
		}
		// terminate if positive body atom is assigned false
		if (truth != null && !truth.toBoolean()) {
			return TERMINATE_BINDING;
		} else if (decrementedTolerance < remainingTolerance) {
			return DECREMENT_TOLERANCE;
		}
		return NEITHER_TERMINATE_BINDING_NOR_DECREMENT_TOLERANCE;
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
	public NonGroundRule getNonGroundRule(Integer ruleId) {
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
		final NonGroundRule rule;
		final Literal startingLiteral;

		FirstBindingAtom(NonGroundRule rule, Literal startingLiteral) {
			this.rule = rule;
			this.startingLiteral = startingLiteral;
		}
	}

	/**
	 * Contains substitions produced for generating ground substitutions of a rule,
	 * and for every substitution the number of positive body atoms still unassigned in the respective ground rule.
	 */
	static class BindingResult {
		final List<Substitution> generatedSubstitutions = new ArrayList<>();
		final List<Integer> numbersOfUnassignedPositiveBodyAtoms = new ArrayList<>();

		void add(Substitution generatedSubstitution, int numberOfUnassignedPositiveBodyAtoms) {
			this.generatedSubstitutions.add(generatedSubstitution);
			this.numbersOfUnassignedPositiveBodyAtoms.add(numberOfUnassignedPositiveBodyAtoms);
		}

		void add(BindingResult otherBindingResult) {
			this.generatedSubstitutions.addAll(otherBindingResult.generatedSubstitutions);
			this.numbersOfUnassignedPositiveBodyAtoms.addAll(otherBindingResult.numbersOfUnassignedPositiveBodyAtoms);
		}

		int size() {
			return generatedSubstitutions.size();
		}

		static BindingResult empty() {
			return new BindingResult();
		}

		static BindingResult singleton(Substitution generatedSubstitution, int numberOfUnassignedPositiveBodyAtoms) {
			BindingResult bindingResult = new BindingResult();
			bindingResult.add(generatedSubstitution, numberOfUnassignedPositiveBodyAtoms);
			return bindingResult;
		}

	}
}
