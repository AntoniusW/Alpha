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

import at.ac.tuwien.kr.alpha.core.grounder.Grounder;
import at.ac.tuwien.kr.alpha.core.solver.ThriceTruth;

/**
 * Helper type to represent truth values as understood by a {@link Grounder} and {@link LiteralInstantiator}.
 * 
 * Note that this enum is not related in any way to {@link ThriceTruth} and mainly serves to have a clear mechanism to indicate that the
 * truth value of an atom is not known at a given point in time (UNASSIGNED)
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public enum AssignmentStatus {

	/**
	 * True
	 */
	TRUE,

	/**
	 * False
	 */
	FALSE,

	/**
	 * Unassigned - indicates that at a given point in time, a {@link LiteralInstantiationStrategy} can not determine whether a literal is true
	 * or false. This is needed because some grounding strategies consider UNASSIGNED atoms to be valid ground instances in order to be able to
	 * ground larger parts of a program earlier on in the ground/solve cycle
	 */
	UNASSIGNED;
}
