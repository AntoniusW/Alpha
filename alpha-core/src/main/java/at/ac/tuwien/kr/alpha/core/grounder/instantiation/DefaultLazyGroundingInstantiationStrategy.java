/**
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
package at.ac.tuwien.kr.alpha.core.grounder.instantiation;

import java.util.LinkedHashSet;
import java.util.Map;

import at.ac.tuwien.kr.alpha.api.Solver;
import at.ac.tuwien.kr.alpha.api.Util;
import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.core.common.Assignment;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.grounder.IndexedInstanceStorage;
import at.ac.tuwien.kr.alpha.core.grounder.NaiveGrounder;
import at.ac.tuwien.kr.alpha.core.grounder.WorkingMemory;
import at.ac.tuwien.kr.alpha.core.solver.ThriceTruth;

/**
 * Implementation of {@link AbstractLiteralInstantiationStrategy} designed for use in {@link NaiveGrounder}.
 * 
 * The instantiation strategy shares a {@link WorkingMemory}, an {@link AtomStore}, an {@link Assignment}, a {@link Map} of atoms that were
 * facts of the currently grounded program, as well as a list of {@link AbstractAtom}s that should be lazily deleted from the working memory,
 * with
 * the grounder.
 * 
 * The working memory and the facts map are maintained by the grounder and are being read by
 * {@link DefaultLazyGroundingInstantiationStrategy} in order to determine {@link AssignmentStatus}es for atoms. The {@link AtomStore} is
 * maintained by {@link DefaultLazyGroundingInstantiationStrategy} in the sense that atoms created from newly encountered ground instances
 * are added by the instantiation strategy. The {@link Assignment} reflects the {@link Solver}s "current view of the world". It is used by
 * {@link DefaultLazyGroundingInstantiationStrategy} to determine {@link AssignmentStatus}es for atoms.
 * 
 * A specialty of this implementation is that - since deletion of obsolete {@link AbstractAtom}s from {@link NaiveGrounder}s
 * {@link WorkingMemory}
 * happens lazily (i.e. at the end of each run of {@link NaiveGrounder#getNoGoods(Assignment)}) - it maintains a set of "stale" atoms that
 * is shared with the grounder. Specifically, whenever {@link DefaultLazyGroundingInstantiationStrategy#getAssignmentStatusForAtom(Atom)}
 * determines that an {@link AbstractAtom} is {@link AssignmentStatus#UNASSIGNED} or {@link AssignmentStatus#FALSE}, that {@link AbstractAtom} is
 * added to
 * the stale atom set, which in turn is processed by the grounder, which then deletes the respective atoms from the working memory.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public class DefaultLazyGroundingInstantiationStrategy extends AbstractLiteralInstantiationStrategy {

	private WorkingMemory workingMemory;
	private AtomStore atomStore;
	private Assignment currentAssignment;
	private LinkedHashSet<Atom> staleWorkingMemoryEntries;
	private Map<Predicate, LinkedHashSet<Instance>> facts;
	private boolean accumulatorEnabled;

	public DefaultLazyGroundingInstantiationStrategy(WorkingMemory workingMemory, AtomStore atomStore,
			Map<Predicate, LinkedHashSet<Instance>> facts, boolean accumulator) {
		this.workingMemory = workingMemory;
		this.atomStore = atomStore;
		this.facts = facts;
		this.accumulatorEnabled = accumulator;
	}

	@Override
	protected Iterable<Instance> computeCandidateInstances(Atom partiallyGroundAtom) {
		IndexedInstanceStorage instanceStorage = this.workingMemory.get(partiallyGroundAtom, true);
		return instanceStorage.getInstancesFromPartiallyGroundAtom(partiallyGroundAtom);
	}

	//@formatter:off
	/**
	 * Computes the {@link AssignmentStatus} for a given {@link AbstractAtom} a.
	 * 
	 * The atom a is {@link AssignmentStatus#TRUE} iff
	 * <ul>
	 * 	<li>The instantiation strategy's <pre>currentAssignment</pre> is null (i.e. the call originated from {@link NaiveGrounder#bootstrap}).</li>
	 *  <li>a is contained in the working memory and the accumulator flag is enabled.</li>
	 * 	<li>a is a fact.</li>
	 *  <li>a is assigned {@link ThriceTruth#TRUE} or {@link ThriceTruth#MBT} in the current assignment by the {@link Solver}.
	 * </ul>
	 * 
	 * An atom is {@link AssignmentStatus#UNASSIGNED} iff it has no {@link ThriceTruth} assigned to it in the current assignment.
	 * An atom is {@link AssignmentStatus#FALSE} iff it is assigned {@link ThriceTruth#FALSE} in the current assignment by the {@link Solver}.
	 * 
	 * Whenever an {@link AbstractAtom} is found to be UNASSIGNED or FALSE,
	 * that {@link AbstractAtom} is added to the stale atom set for later deletion from working memory by the grounder.
	 */
	//@formatter:on
	@Override
	protected AssignmentStatus getAssignmentStatusForAtom(Atom atom) {
		if (this.currentAssignment == null || this.isFact(atom)) {
			// currentAssignment == null is a legitimate case, grounder may be in bootstrap
			// and will call bindNextAtom with null assignment in that case.
			// Assumption: since the atom came from working memory and we must be in
			// bootstrap here, we can assume for the atom to be true (or atom is a fact
			// anyway, in which case it's also true).
			return AssignmentStatus.TRUE;
		}
		if (this.accumulatorEnabled) {
			if (this.workingMemory.get(atom.getPredicate(), true).containsInstance(Instance.fromAtom(atom))) {
				// Accumulator is enabled. In this case, if the Atom is in the working memory, it is considered true.
				return AssignmentStatus.TRUE;
			}
		}

		// We're neither in bootstrap nor running in accumulator mode, normal workflow starts from here.
		AssignmentStatus retVal;
		// First, make sure that the Atom in question exists in the AtomStore.
		if (atomStore.contains(atom)) {
			int atomId = this.atomStore.get(atom);
			this.currentAssignment.growForMaxAtomId();
			if (currentAssignment.isAssigned(atomId)) {
				retVal = currentAssignment.getTruth(atomId).toBoolean() ? AssignmentStatus.TRUE : AssignmentStatus.FALSE;
			} else {
				retVal = AssignmentStatus.UNASSIGNED;
			}
		} else {
			retVal = AssignmentStatus.UNASSIGNED;
		}
		if (retVal == AssignmentStatus.FALSE || retVal == AssignmentStatus.UNASSIGNED) {
			this.staleWorkingMemoryEntries.add(atom);
		}
		return retVal;
	}

	private boolean isFact(Atom atom) {
		if (this.facts.get(atom.getPredicate()) == null) {
			return false;
		} else {
			return this.facts.get(atom.getPredicate()).contains(Instance.fromAtom(atom));
		}
	}

	@Override
	protected AssignmentStatus getAssignmentStatusForNegatedGroundLiteral(Literal negatedGroundLiteral) {
		return AssignmentStatus.TRUE;
	}

	/**
	 * Checks whether a given {@link AssignmentStatus} is "acceptable" in the sense
	 * that an atom with that assignment status represents a valid ground
	 * substitution for a non-ground atom. This instantiation strategy accepts
	 * {@link AssignmentStatus#TRUE} and {@link AssignmentStatus#UNASSIGNED}.
	 */
	@Override
	protected boolean assignmentStatusAccepted(AssignmentStatus assignmentStatus) {
		switch (assignmentStatus) {
			case TRUE:
			case UNASSIGNED:
				return true;
			case FALSE:
				return false;
			default:
				throw Util.oops("Unsupported AssignmentStatus: " + assignmentStatus);
		}
	}

	public void setCurrentAssignment(Assignment currentAssignment) {
		this.currentAssignment = currentAssignment;
	}

	public void setStaleWorkingMemoryEntries(LinkedHashSet<Atom> staleWorkingMemoryEntries) {
		this.staleWorkingMemoryEntries = staleWorkingMemoryEntries;
	}

}
