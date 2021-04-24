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

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;

/**
 * A {@link LiteralInstantiationStrategy} finds and validates {@link BasicSubstitution}s for {@link Literal}s based on a specific definition of
 * when a {@link BasicSubstitution} is valid, i.e. what makes a literal "true", "false" or "unassigned".
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public interface LiteralInstantiationStrategy {

	/**
	 * Computes the {@link AssignmentStatus} for the given {@link Literal} according to the rules of this {@link LiteralInstantiationStrategy}.
	 * 
	 * @param groundLiteral a ground {@link Literal} for which to compute an {@link AssignmentStatus}
	 * @return the current {@link AssignmentStatus} for the given literal according to the rules of this {@link LiteralInstantiationStrategy}
	 */
	AssignmentStatus getTruthForGroundLiteral(Literal groundLiteral);

	/**
	 * Computes {@link BasicSubstitution}s that yield ground instances for a given literal and starting substitution along with the
	 * {@link AssignmentStatus} of respective ground instances. Note that in all implementations it must hold that an {@link AssignmentStatus}
	 * AS for a {@link BasicSubstitution} S as yielded by this method for a {@link Literal} lit is the same as the result of calling
	 * <code>getTruthForGroundLiteral(lit.substitute(S))</code>, i.e. both methods must yield the same assignment status for the same ground
	 * literal.
	 * 
	 * @param lit                 a non-ground {@link Literal} for which to compute substitutions.
	 * @param partialSubstitution a (possibly empty) substitution to use as a starting point
	 * @return a list of substitutions along with the assignment status of the respective ground atoms
	 */
	List<ImmutablePair<Substitution, AssignmentStatus>> getAcceptedSubstitutions(Literal lit, Substitution partialSubstitution);

}
