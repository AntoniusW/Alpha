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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;

/**
 * Abstract base implementation of {@link LiteralInstantiationStrategy} that outlines a basic workflow for
 * {@link LiteralInstantiationStrategy#getTruthForGroundLiteral(Literal)} and
 * {@link LiteralInstantiationStrategy#getAcceptedSubstitutions(Literal, BasicSubstitution)} while leaving details of when an atom is true and
 * which {@link AssignmentStatus}es to consider valid for <code>getAcceptedSubstitutions</code> to implementations.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public abstract class AbstractLiteralInstantiationStrategy implements LiteralInstantiationStrategy {

	/**
	 * See {@link LiteralInstantiationStrategy#getTruthForGroundLiteral(Literal).
	 * 
	 * In general - since this code is used in a grounding context where negative literals in rule bodies can be handled in different ways - the
	 * logic for determining the {@link AssignmentStatus} of a negated literal is delegated to the abstract method
	 * {@link AbstractLiteralInstantiationStrategy#getAssignmentStatusForNegatedGroundLiteral(Literal)}. The assignment status for positive
	 * literals is determined using the abstract method {@link AbstractLiteralInstantiationStrategy#getAssignmentStatusForAtom(Atom)}.
	 */
	@Override
	public final AssignmentStatus getTruthForGroundLiteral(Literal groundLiteral) {
		if (groundLiteral.isNegated()) {
			return this.getAssignmentStatusForNegatedGroundLiteral(groundLiteral);
		}
		return this.getAssignmentStatusForAtom(groundLiteral.getAtom());
	}

	/**
	 * See {@link LiteralInstantiationStrategy#getAcceptedSubstitutions(Literal, BasicSubstitution)}.
	 * 
	 * A very general implementation of the basic steps needed to obtain ground substitutions for a positive literal.
	 * Potentially valid ground instances are obtained using {@link AbstractLiteralInstantiationStrategy#computeCandidateInstances(Atom)}, then
	 * checked, such that each candidate instance unifies with the given partial substitution and has a "valid" {@link AssignmentStatus},
	 * where "validity" of an {@link AssignmentStatus} is determined using the abstract method
	 * {@link AbstractLiteralInstantiationStrategy#assignmentStatusAccepted(AssignmentStatus)}.
	 */
	@Override
	public final List<ImmutablePair<Substitution, AssignmentStatus>> getAcceptedSubstitutions(Literal lit, Substitution partialSubstitution) {
		Atom atom = lit.getAtom();
		Iterable<Instance> groundInstances = this.computeCandidateInstances(atom);
		return this.buildSubstitutionsFromInstances(atom, groundInstances, partialSubstitution);
	}

	/**
	 * Computes instances that are potentially valid ground instances of the given partially-ground atom.
	 * 
	 * A candidate instance is a ground instance of the same predicate where all terms that are ground in <code>partiallyGroundAtom</code> have
	 * the same values in the candidate.
	 * 
	 * @param partiallyGroundAtom a partially ground atom for which to find fitting ground instances
	 * @return a list of candidate instances
	 */
	protected abstract Iterable<Instance> computeCandidateInstances(Atom partiallyGroundAtom);

	/**
	 * Based on a list of candidate instances (see {@link AbstractLiteralInstantiationStrategy#computeCandidateInstances(Atom)}), create a list
	 * of substitutions and assignment statuses such that each substitution represents a valid (according to the implementation-specific
	 * definition of this instantiation strategy) ground instance of <code>atomToSubstitute</code>.
	 * 
	 * @param atomToSubstitute
	 * @param candidateInstances
	 * @param partialSubstitution
	 * @return
	 */
	protected final List<ImmutablePair<Substitution, AssignmentStatus>> buildSubstitutionsFromInstances(Atom atomToSubstitute,
			Iterable<Instance> candidateInstances, Substitution partialSubstitution) {
		List<ImmutablePair<Substitution, AssignmentStatus>> retVal = new ArrayList<>();
		// Filter for only instances unifying with partialSubsitution, i.e. "where all joins work out".
		Substitution currentInstanceSubstitution;
		Atom atomForCurrentInstance;
		for (Instance instance : candidateInstances) {
			currentInstanceSubstitution = BasicSubstitution.specializeSubstitution(atomToSubstitute, instance, partialSubstitution);
			if (currentInstanceSubstitution == null) {
				// Instance does not unify with partialSubstitution, move on to the next instance.
				continue;
			}
			// At this point, we know that the substitution works out.
			// Now check whether the resulting Atom has an acceptable AssignmentStatus.
			atomForCurrentInstance = Atoms.newBasicAtom(atomToSubstitute.getPredicate(), atomToSubstitute.getTerms())
					.substitute(currentInstanceSubstitution);
			AssignmentStatus assignmentStatus = this.getAssignmentStatusForAtom(atomForCurrentInstance);
			if (!this.assignmentStatusAccepted(assignmentStatus)) {
				// Atom has an assignment status deemed unacceptable by this instantiation strategy.
				continue;
			}
			retVal.add(new ImmutablePair<>(currentInstanceSubstitution, assignmentStatus));
		}
		return retVal;
	}

	protected abstract AssignmentStatus getAssignmentStatusForAtom(Atom atom);

	protected abstract AssignmentStatus getAssignmentStatusForNegatedGroundLiteral(Literal negatedGroundLiteral);

	protected abstract boolean assignmentStatusAccepted(AssignmentStatus assignmentStatus);

}
