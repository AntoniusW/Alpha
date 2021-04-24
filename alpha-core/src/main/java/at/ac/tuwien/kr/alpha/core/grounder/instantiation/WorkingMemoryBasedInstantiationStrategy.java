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

import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.core.grounder.WorkingMemory;

/**
 * A very basic implementation of {@link AbstractLiteralInstantiationStrategy} that determines truth of an atom solely based on the atom's
 * presence in a working memory. Atoms that have a corresponding positive instance in the working memory have {@link AssignmentStatus#TRUE},
 * all other atoms have {@link AssignmentStatus#FALSE}. A negated literal lit is true iff
 * <code>getAssignmentStatusForAtom(lit.getAtom()) == AssignmentStatus.FALSE</code>, false otherwise.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public class WorkingMemoryBasedInstantiationStrategy extends AbstractLiteralInstantiationStrategy {

	private final WorkingMemory workingMemory;

	public WorkingMemoryBasedInstantiationStrategy(WorkingMemory workingMemory) {
		this.workingMemory = workingMemory;
	}

	@Override
	protected Iterable<Instance> computeCandidateInstances(Atom partiallyGroundAtom) {
		return this.workingMemory.get(partiallyGroundAtom, true).getInstancesFromPartiallyGroundAtom(partiallyGroundAtom);
	}

	@Override
	protected AssignmentStatus getAssignmentStatusForAtom(Atom atom) {
		return this.workingMemory.get(atom, true).containsInstance(Instance.fromAtom(atom)) ? AssignmentStatus.TRUE : AssignmentStatus.FALSE;
	}

	@Override
	protected AssignmentStatus getAssignmentStatusForNegatedGroundLiteral(Literal negatedGroundLiteral) {
		return this.getAssignmentStatusForAtom(negatedGroundLiteral.getAtom()) == AssignmentStatus.TRUE ? AssignmentStatus.FALSE : AssignmentStatus.TRUE;
	}

	@Override
	protected boolean assignmentStatusAccepted(AssignmentStatus assignmentStatus) {
		return assignmentStatus == AssignmentStatus.TRUE;
	}

}
