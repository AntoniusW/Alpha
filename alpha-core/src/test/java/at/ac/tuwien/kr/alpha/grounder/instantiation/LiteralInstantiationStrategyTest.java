package at.ac.tuwien.kr.alpha.grounder.instantiation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.literals.Literals;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.core.grounder.NaiveGrounder;
import at.ac.tuwien.kr.alpha.core.grounder.WorkingMemory;
import at.ac.tuwien.kr.alpha.core.grounder.instantiation.AssignmentStatus;
import at.ac.tuwien.kr.alpha.core.grounder.instantiation.DefaultLazyGroundingInstantiationStrategy;
import at.ac.tuwien.kr.alpha.core.grounder.instantiation.LiteralInstantiationStrategy;
import at.ac.tuwien.kr.alpha.core.grounder.instantiation.WorkingMemoryBasedInstantiationStrategy;
import at.ac.tuwien.kr.alpha.core.solver.ThriceTruth;
import at.ac.tuwien.kr.alpha.core.solver.TrailAssignment;
import at.ac.tuwien.kr.alpha.core.solver.WritableAssignment;

public class LiteralInstantiationStrategyTest {

	@Test
	public void workingMemoryBasedInstantiationAcceptLiteral() {
		Predicate p = Predicates.getPredicate("p", 1);
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		workingMemory.addInstance(Atoms.newBasicAtom(p, Terms.newSymbolicConstant("a")), true);
		LiteralInstantiationStrategy strategy = new WorkingMemoryBasedInstantiationStrategy(workingMemory);
		Literal positiveAcceptedLiteral = Literals.fromAtom(
				Atoms.newBasicAtom(p, Terms.newSymbolicConstant("a")), true);
		Assert.assertEquals(AssignmentStatus.TRUE, strategy.getTruthForGroundLiteral(positiveAcceptedLiteral));
		Literal negativeAcceptedLiteral = Literals.fromAtom(
				Atoms.newBasicAtom(p, Terms.newSymbolicConstant("b")), false);
		Assert.assertEquals(AssignmentStatus.TRUE, strategy.getTruthForGroundLiteral(negativeAcceptedLiteral));
	}

	@Test
	public void workingMemoryBasedInstantiationRejectLiteral() {
		Predicate p = Predicates.getPredicate("p", 1);
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		workingMemory.addInstance(Atoms.newBasicAtom(p, Terms.newSymbolicConstant("a")), true);
		LiteralInstantiationStrategy strategy = new WorkingMemoryBasedInstantiationStrategy(workingMemory);
		Literal positiveRejectedLiteral = Literals.fromAtom(
				Atoms.newBasicAtom(p, Terms.newSymbolicConstant("b")), true);
		Assert.assertEquals(AssignmentStatus.FALSE, strategy.getTruthForGroundLiteral(positiveRejectedLiteral));
		Literal negativeRejectedLiteral = Literals.fromAtom(
				Atoms.newBasicAtom(p, Terms.newSymbolicConstant("a")), false);
		Assert.assertEquals(AssignmentStatus.FALSE, strategy.getTruthForGroundLiteral(negativeRejectedLiteral));
	}

	/**
	 * Uses {@link DefaultLazyGroundingInstantiationStrategy} to check the truth
	 * (i.e. {@link AssignmentStatus}) of the positive ground literal "p(a)".
	 * 
	 * In this case, the instantiation strategy does not have an assignment set (as
	 * is the case when {@link NaiveGrounder} is in bootstrap),
	 * so we expect the instantiation strategy to determine that p(a) is TRUE.
	 * Furthermore, the stale atom set (used by {@link NaiveGrounder} to clean up
	 * atoms that should be deleted from working memory) must stay empty.
	 */
	@Test
	public void defaultLazyGroundingNoAssignmentGroundLiteral() {
		Predicate p = Predicates.getPredicate("p", 1);
		BasicAtom pOfA = Atoms.newBasicAtom(p, Terms.newSymbolicConstant("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, new AtomStoreImpl(),
				Collections.emptyMap(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(null);
		

		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(Literals.fromAtom(pOfA, true));
		Assert.assertEquals(AssignmentStatus.TRUE, assignmentStatus);
		Assert.assertTrue(staleSet.isEmpty());
	}

	/**
	 * Uses {@link DefaultLazyGroundingInstantiationStrategy} to find ground
	 * instances for the partially ground positive literal "q(a, X)".
	 * 
	 * In this case, the instantiation strategy does not have an assignment set (as
	 * is the case when {@link NaiveGrounder} is in bootstrap), so we expect the
	 * assignment status (i.e. assignment status of the found ground instance)
	 * passed back with the substitution to be TRUE. Furthermore, the stale atom set
	 * (used by {@link NaiveGrounder} to clean up atoms that should be deleted from
	 * working memory) must stay empty.
	 */
	@Test
	public void defaultLazyGroundingNoAssignmentSubstituteNonGroundLiteral() {
		Predicate q = Predicates.getPredicate("q", 2);
		BasicAtom nonGroundAtom = Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newVariable("X"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(q);
		workingMemory.addInstance(Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newSymbolicConstant("b")), true);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, new AtomStoreImpl(),
				Collections.emptyMap(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(null);

		List<ImmutablePair<Substitution, AssignmentStatus>> result = strategy.getAcceptedSubstitutions(Literals.fromAtom(nonGroundAtom, true),
				new BasicSubstitution());
		Assert.assertEquals(1, result.size());
		ImmutablePair<Substitution, AssignmentStatus> substitutionInfo = result.get(0);
		Substitution substitution = substitutionInfo.left;
		AssignmentStatus assignmentStatus = substitutionInfo.right;
		Assert.assertEquals(AssignmentStatus.TRUE, assignmentStatus);
		Assert.assertTrue(substitution.isVariableSet(Terms.newVariable("X")));
		Assert.assertEquals(Terms.newSymbolicConstant("b"), substitution.eval(Terms.newVariable("X")));
		Assert.assertTrue(staleSet.isEmpty());
	}

	/**
	 * Uses {@link DefaultLazyGroundingInstantiationStrategy} to check the truth
	 * (i.e. {@link AssignmentStatus}) of the positive ground literal "p(a)".
	 * 
	 * In this case, the instantiation strategy has an empty assignment,
	 * so we expect the instantiation strategy to determine that p(a) is UNASSIGNED.
	 * Since UNASSIGNED and FALSE atoms are (potentially) stale in working memory,
	 * we expect the atom "p(a)" to be added to the stale set by the instantiation
	 * strategy.
	 */
	@Test
	public void defaultLazyGroundingCheckUnassignedGroundLiteral() {
		Predicate p = Predicates.getPredicate("p", 1);
		BasicAtom pOfA = Atoms.newBasicAtom(p, Terms.newSymbolicConstant("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				Collections.emptyMap(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(Literals.fromAtom(pOfA, true));
		Assert.assertEquals(AssignmentStatus.UNASSIGNED, assignmentStatus);

		Assert.assertEquals(1, staleSet.size());
		Assert.assertTrue(staleSet.contains(pOfA));
	}

	/**
	 * Uses {@link DefaultLazyGroundingInstantiationStrategy} to check the truth
	 * (i.e. {@link AssignmentStatus}) of the positive ground literal "p(a)".
	 * 
	 * In this case, the instantiation strategy has an assignment where the atom
	 * "p(a)" is assigned ThriceTruth.FALSE, so we expect the instantiation strategy
	 * to determine that p(a) is FALSE. Since UNASSIGNED and FALSE atoms are
	 * (potentially) stale in working memory, we expect the atom "p(a)" to be added
	 * to the stale set by the instantiation strategy.
	 */
	@Test
	public void defaultLazyGroundingCheckFalseGroundLiteral() {
		Predicate p = Predicates.getPredicate("p", 1);
		BasicAtom pOfA = Atoms.newBasicAtom(p, Terms.newSymbolicConstant("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		atomStore.putIfAbsent(pOfA);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(pOfA), ThriceTruth.FALSE);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				Collections.emptyMap(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(Literals.fromAtom(pOfA, true));
		Assert.assertEquals(AssignmentStatus.FALSE, assignmentStatus);

		Assert.assertEquals(1, staleSet.size());
		Assert.assertTrue(staleSet.contains(pOfA));
	}

	/**
	 * Uses {@link DefaultLazyGroundingInstantiationStrategy} to check the truth
	 * (i.e. {@link AssignmentStatus}) of the positive ground literal "p(a)".
	 * 
	 * In this case, the instantiation strategy has an assignment where the atom
	 * "p(a)" is assigned ThriceTruth.TRUE, so we expect the instantiation strategy
	 * to determine that p(a) is TRUE. Furthermore, the stale atom set
	 * (used by {@link NaiveGrounder} to clean up atoms that should be deleted from
	 * working memory) must stay empty.
	 */
	@Test
	public void defaultLazyGroundingCheckTrueGroundLiteral() {
		Predicate p = Predicates.getPredicate("p", 1);
		BasicAtom pOfA = Atoms.newBasicAtom(p, Terms.newSymbolicConstant("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		atomStore.putIfAbsent(pOfA);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(pOfA), ThriceTruth.TRUE);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				Collections.emptyMap(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);
		
		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(Literals.fromAtom(pOfA, true));
		Assert.assertEquals(AssignmentStatus.TRUE, assignmentStatus);

		Assert.assertTrue(staleSet.isEmpty());
	}

	/**
	 * Uses {@link DefaultLazyGroundingInstantiationStrategy} to check the truth
	 * (i.e. {@link AssignmentStatus}) of the positive ground literal "p(a)".
	 * 
	 * In this case, the instantiation strategy has an assignment where the atom
	 * "p(a)" is assigned ThriceTruth.MBT, so we expect the instantiation strategy
	 * to determine that p(a) is TRUE. Furthermore, the stale atom set
	 * (used by {@link NaiveGrounder} to clean up atoms that should be deleted from
	 * working memory) must stay empty.
	 */
	@Test
	public void defaultLazyGroundingCheckMustBeTrueGroundLiteral() {
		Predicate p = Predicates.getPredicate("p", 1);
		BasicAtom pOfA = Atoms.newBasicAtom(p, Terms.newSymbolicConstant("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		atomStore.putIfAbsent(pOfA);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(pOfA), ThriceTruth.MBT);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				Collections.emptyMap(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(Literals.fromAtom(pOfA, true));
		Assert.assertEquals(AssignmentStatus.TRUE, assignmentStatus);

		Assert.assertTrue(staleSet.isEmpty());
	}

	/**
	 * Uses {@link DefaultLazyGroundingInstantiationStrategy} to find the ground
	 * instance "q(a, b)" for the partially ground positive literal "q(a, X)".
	 * 
	 * In this case, the instantiation strategy has an empty assignment, so we
	 * expect the assignment status (i.e. assignment status of the found ground
	 * instance) passed back with the substitution to be UNASSIGNED. Since
	 * UNASSIGNED and FALSE atoms are (potentially) stale in working memory, we
	 * expect the atom "q(a, b)" to be added to the stale set by the instantiation
	 * strategy.
	 */
	@Test
	public void defaultLazyGroundingSubstituteNonGroundLiteralWithUnassignedInstance() {
		Predicate q = Predicates.getPredicate("q", 2);
		BasicAtom nonGroundAtom = Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newVariable("X"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(q);
		workingMemory.addInstance(Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newSymbolicConstant("b")), true);
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				Collections.emptyMap(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		List<ImmutablePair<Substitution, AssignmentStatus>> result = strategy.getAcceptedSubstitutions(Literals.fromAtom(nonGroundAtom, true),
				new BasicSubstitution());
		Assert.assertEquals(1, result.size());
		ImmutablePair<Substitution, AssignmentStatus> substitutionInfo = result.get(0);
		Substitution substitution = substitutionInfo.left;
		AssignmentStatus assignmentStatus = substitutionInfo.right;
		Assert.assertEquals(AssignmentStatus.UNASSIGNED, assignmentStatus);
		Assert.assertTrue(substitution.isVariableSet(Terms.newVariable("X")));
		Assert.assertEquals(Terms.newSymbolicConstant("b"), substitution.eval(Terms.newVariable("X")));

		Assert.assertEquals(1, staleSet.size());
		Assert.assertTrue(staleSet.contains(Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newSymbolicConstant("b"))));
	}

	/**
	 * Uses {@link DefaultLazyGroundingInstantiationStrategy} to find the ground
	 * instance "q(a, b)" for the partially ground positive literal "q(a, X)".
	 * 
	 * In this case, the instantiation strategy has an assignment where q(a, b) is
	 * assigned ThriceTruth.TRUE, so we expect the assignment status (i.e.
	 * assignment status of the found ground instance) passed back with the
	 * substitution to be TRUE. Furthermore, we expect the stale atom set to stay
	 * empty.
	 */
	@Test
	public void defaultLazyGroundingSubstituteNonGroundLiteralWithTrueInstance() {
		Predicate q = Predicates.getPredicate("q", 2);
		BasicAtom nonGroundAtom = Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newVariable("X"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(q);
		workingMemory.addInstance(Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newSymbolicConstant("b")), true);
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		BasicAtom groundAtom = Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newSymbolicConstant("b"));
		atomStore.putIfAbsent(groundAtom);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(groundAtom), ThriceTruth.TRUE);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				Collections.emptyMap(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		List<ImmutablePair<Substitution, AssignmentStatus>> result = strategy.getAcceptedSubstitutions(Literals.fromAtom(nonGroundAtom, true),
				new BasicSubstitution());
		Assert.assertEquals(1, result.size());
		ImmutablePair<Substitution, AssignmentStatus> substitutionInfo = result.get(0);
		Substitution substitution = substitutionInfo.left;
		AssignmentStatus assignmentStatus = substitutionInfo.right;
		Assert.assertEquals(AssignmentStatus.TRUE, assignmentStatus);
		Assert.assertTrue(substitution.isVariableSet(Terms.newVariable("X")));
		Assert.assertEquals(Terms.newSymbolicConstant("b"), substitution.eval(Terms.newVariable("X")));

		Assert.assertTrue(staleSet.isEmpty());
	}

	/**
	 * Uses {@link DefaultLazyGroundingInstantiationStrategy} to find the ground
	 * instance "q(a, b)" for the partially ground positive literal "q(a, X)".
	 * 
	 * In this case, the instantiation strategy has an assignment where q(a, b) is
	 * assigned ThriceTruth.FALSE, so we expect an empty list from
	 * {@link LiteralInstantiationStrategy#getAcceptedSubstitutions(Literal, Substitution)}.
	 * Furthermore, we expect the atom q(a, b) to be added to the stale atom set.
	 */
	@Test
	public void defaultLazyGroundingSubstituteNonGroundLiteralWithFalseInstance() {
		Predicate q = Predicates.getPredicate("q", 2);
		BasicAtom nonGroundAtom = Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newVariable("X"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(q);
		workingMemory.addInstance(Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newSymbolicConstant("b")), true);
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		BasicAtom groundAtom = Atoms.newBasicAtom(q, Terms.newSymbolicConstant("a"), Terms.newSymbolicConstant("b"));
		atomStore.putIfAbsent(groundAtom);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(groundAtom), ThriceTruth.FALSE);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				Collections.emptyMap(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		List<ImmutablePair<Substitution, AssignmentStatus>> result = strategy.getAcceptedSubstitutions(Literals.fromAtom(nonGroundAtom, true),
				new BasicSubstitution());
		Assert.assertTrue(result.isEmpty());

		Assert.assertEquals(1, staleSet.size());
		Assert.assertTrue(staleSet.contains(groundAtom));
	}

}
	