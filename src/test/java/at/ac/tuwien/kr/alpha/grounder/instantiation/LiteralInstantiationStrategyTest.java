/*
 * Copyright (c) 2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.grounder.instantiation;

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.Facts;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.NaiveGrounder;
import at.ac.tuwien.kr.alpha.grounder.Substitution;
import at.ac.tuwien.kr.alpha.grounder.WorkingMemory;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import at.ac.tuwien.kr.alpha.solver.TrailAssignment;
import at.ac.tuwien.kr.alpha.solver.WritableAssignment;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.List;

public class LiteralInstantiationStrategyTest {

	@Test
	public void workingMemoryBasedInstantiationAcceptLiteral() {
		Predicate p = Predicate.getInstance("p", 1);
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		workingMemory.addInstance(new BasicAtom(p, ConstantTerm.getSymbolicInstance("a")), true);
		LiteralInstantiationStrategy strategy = new WorkingMemoryBasedInstantiationStrategy(workingMemory);
		Literal positiveAcceptedLiteral = new BasicLiteral(
				new BasicAtom(p, ConstantTerm.getSymbolicInstance("a")), true);
		Assert.assertEquals(AssignmentStatus.TRUE, strategy.getTruthForGroundLiteral(positiveAcceptedLiteral));
		Literal negativeAcceptedLiteral = new BasicLiteral(
				new BasicAtom(p, ConstantTerm.getSymbolicInstance("b")), false);
		Assert.assertEquals(AssignmentStatus.TRUE, strategy.getTruthForGroundLiteral(negativeAcceptedLiteral));
	}

	@Test
	public void workingMemoryBasedInstantiationRejectLiteral() {
		Predicate p = Predicate.getInstance("p", 1);
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		workingMemory.addInstance(new BasicAtom(p, ConstantTerm.getSymbolicInstance("a")), true);
		LiteralInstantiationStrategy strategy = new WorkingMemoryBasedInstantiationStrategy(workingMemory);
		Literal positiveRejectedLiteral = new BasicLiteral(
				new BasicAtom(p, ConstantTerm.getSymbolicInstance("b")), true);
		Assert.assertEquals(AssignmentStatus.FALSE, strategy.getTruthForGroundLiteral(positiveRejectedLiteral));
		Literal negativeRejectedLiteral = new BasicLiteral(
				new BasicAtom(p, ConstantTerm.getSymbolicInstance("a")), false);
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
		Predicate p = Predicate.getInstance("p", 1);
		BasicAtom pOfA = new BasicAtom(p, ConstantTerm.getSymbolicInstance("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, new AtomStoreImpl(),
				new Facts(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(null);
		

		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(new BasicLiteral(pOfA, true));
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
		Predicate q = Predicate.getInstance("q", 2);
		BasicAtom nonGroundAtom = new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), VariableTerm.getInstance("X"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(q);
		workingMemory.addInstance(new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), ConstantTerm.getSymbolicInstance("b")), true);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, new AtomStoreImpl(),
				new Facts(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(null);

		List<ImmutablePair<Substitution, AssignmentStatus>> result = strategy.getAcceptedSubstitutions(new BasicLiteral(nonGroundAtom, true),
				new Substitution());
		Assert.assertEquals(1, result.size());
		ImmutablePair<Substitution, AssignmentStatus> substitutionInfo = result.get(0);
		Substitution substitution = substitutionInfo.left;
		AssignmentStatus assignmentStatus = substitutionInfo.right;
		Assert.assertEquals(AssignmentStatus.TRUE, assignmentStatus);
		Assert.assertTrue(substitution.isVariableSet(VariableTerm.getInstance("X")));
		Assert.assertEquals(ConstantTerm.getSymbolicInstance("b"), substitution.eval(VariableTerm.getInstance("X")));
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
		Predicate p = Predicate.getInstance("p", 1);
		BasicAtom pOfA = new BasicAtom(p, ConstantTerm.getSymbolicInstance("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				new Facts(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(new BasicLiteral(pOfA, true));
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
		Predicate p = Predicate.getInstance("p", 1);
		BasicAtom pOfA = new BasicAtom(p, ConstantTerm.getSymbolicInstance("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		atomStore.putIfAbsent(pOfA);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(pOfA), ThriceTruth.FALSE);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				new Facts(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(new BasicLiteral(pOfA, true));
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
		Predicate p = Predicate.getInstance("p", 1);
		BasicAtom pOfA = new BasicAtom(p, ConstantTerm.getSymbolicInstance("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		atomStore.putIfAbsent(pOfA);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(pOfA), ThriceTruth.TRUE);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				new Facts(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);
		
		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(new BasicLiteral(pOfA, true));
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
		Predicate p = Predicate.getInstance("p", 1);
		BasicAtom pOfA = new BasicAtom(p, ConstantTerm.getSymbolicInstance("a"));
		WorkingMemory workingMemory = new WorkingMemory();
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		atomStore.putIfAbsent(pOfA);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(pOfA), ThriceTruth.MBT);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				new Facts(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		AssignmentStatus assignmentStatus = strategy.getTruthForGroundLiteral(new BasicLiteral(pOfA, true));
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
		Predicate q = Predicate.getInstance("q", 2);
		BasicAtom nonGroundAtom = new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), VariableTerm.getInstance("X"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(q);
		workingMemory.addInstance(new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), ConstantTerm.getSymbolicInstance("b")), true);
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				new Facts(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		List<ImmutablePair<Substitution, AssignmentStatus>> result = strategy.getAcceptedSubstitutions(new BasicLiteral(nonGroundAtom, true),
				new Substitution());
		Assert.assertEquals(1, result.size());
		ImmutablePair<Substitution, AssignmentStatus> substitutionInfo = result.get(0);
		Substitution substitution = substitutionInfo.left;
		AssignmentStatus assignmentStatus = substitutionInfo.right;
		Assert.assertEquals(AssignmentStatus.UNASSIGNED, assignmentStatus);
		Assert.assertTrue(substitution.isVariableSet(VariableTerm.getInstance("X")));
		Assert.assertEquals(ConstantTerm.getSymbolicInstance("b"), substitution.eval(VariableTerm.getInstance("X")));

		Assert.assertEquals(1, staleSet.size());
		Assert.assertTrue(staleSet.contains(new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), ConstantTerm.getSymbolicInstance("b"))));
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
		Predicate q = Predicate.getInstance("q", 2);
		BasicAtom nonGroundAtom = new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), VariableTerm.getInstance("X"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(q);
		workingMemory.addInstance(new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), ConstantTerm.getSymbolicInstance("b")), true);
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		BasicAtom groundAtom = new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), ConstantTerm.getSymbolicInstance("b"));
		atomStore.putIfAbsent(groundAtom);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(groundAtom), ThriceTruth.TRUE);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				new Facts(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		List<ImmutablePair<Substitution, AssignmentStatus>> result = strategy.getAcceptedSubstitutions(new BasicLiteral(nonGroundAtom, true),
				new Substitution());
		Assert.assertEquals(1, result.size());
		ImmutablePair<Substitution, AssignmentStatus> substitutionInfo = result.get(0);
		Substitution substitution = substitutionInfo.left;
		AssignmentStatus assignmentStatus = substitutionInfo.right;
		Assert.assertEquals(AssignmentStatus.TRUE, assignmentStatus);
		Assert.assertTrue(substitution.isVariableSet(VariableTerm.getInstance("X")));
		Assert.assertEquals(ConstantTerm.getSymbolicInstance("b"), substitution.eval(VariableTerm.getInstance("X")));

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
		Predicate q = Predicate.getInstance("q", 2);
		BasicAtom nonGroundAtom = new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), VariableTerm.getInstance("X"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(q);
		workingMemory.addInstance(new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), ConstantTerm.getSymbolicInstance("b")), true);
		AtomStore atomStore = new AtomStoreImpl();
		WritableAssignment assignment = new TrailAssignment(atomStore);
		BasicAtom groundAtom = new BasicAtom(q, ConstantTerm.getSymbolicInstance("a"), ConstantTerm.getSymbolicInstance("b"));
		atomStore.putIfAbsent(groundAtom);
		assignment.growForMaxAtomId();
		assignment.assign(atomStore.get(groundAtom), ThriceTruth.FALSE);
		LinkedHashSet<Atom> staleSet = new LinkedHashSet<>();
		DefaultLazyGroundingInstantiationStrategy strategy = new DefaultLazyGroundingInstantiationStrategy(workingMemory, atomStore,
				new Facts(), false);
		strategy.setStaleWorkingMemoryEntries(staleSet);
		strategy.setCurrentAssignment(assignment);

		List<ImmutablePair<Substitution, AssignmentStatus>> result = strategy.getAcceptedSubstitutions(new BasicLiteral(nonGroundAtom, true),
				new Substitution());
		Assert.assertTrue(result.isEmpty());

		Assert.assertEquals(1, staleSet.size());
		Assert.assertTrue(staleSet.contains(groundAtom));
	}

}
	