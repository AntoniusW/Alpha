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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;
import at.ac.tuwien.kr.alpha.core.grounder.Grounder;
import at.ac.tuwien.kr.alpha.core.rules.InternalRule;

/**
 * Representation of the result of instantiating, i.e. finding ground instances for a literal, as performed by
 * {@link LiteralInstantiator#instantiateLiteral(Literal, BasicSubstitution)}.
 * 
 * A {@link LiteralInstantiationResult} bundles obtained ground substitutions - or the lack thereof, if none exist for a given literal -
 * together with status information that can be used by a {@link Grounder} to determine how to proceed when grounding an
 * {@link InternalRule}.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public class LiteralInstantiationResult {

	/**
	 * Indicates how a {@link Grounder} should treat this result.
	 */
	private final Type type;

	/**
	 * Ground substitutions together with the {@link AssignmentStatus} of the last literal bound to obtain the substitution. Empty for result
	 * types STOP_BINDING, PUSH_BACK and MAYBE_PUSH_BACK.
	 */
	private final List<ImmutablePair<Substitution, AssignmentStatus>> substitutions;

	private LiteralInstantiationResult(Type type, List<ImmutablePair<Substitution, AssignmentStatus>> substitutions) {
		if (type == Type.CONTINUE && substitutions == null) {
			throw new UnsupportedOperationException("A LiteralInstantiationResult of type CONTINUE must have substitutions!");
		}
		this.type = type;
		this.substitutions = substitutions;
	}

	public static LiteralInstantiationResult continueBinding(List<ImmutablePair<Substitution, AssignmentStatus>> substitutions) {
		return new LiteralInstantiationResult(Type.CONTINUE, substitutions);
	}

	//@formatter:off
	public static LiteralInstantiationResult continueBindingWithTrueSubstitutions(List<Substitution> substitutions) {
		List<ImmutablePair<Substitution, AssignmentStatus>> substitutionsWithAssignment = substitutions.stream()
				.map((substitution) -> new ImmutablePair<>(substitution, AssignmentStatus.TRUE))
				.collect(Collectors.toList());
		return new LiteralInstantiationResult(Type.CONTINUE, substitutionsWithAssignment);
	}
	//@formatter:on

	public static LiteralInstantiationResult continueBinding(Substitution substitution, AssignmentStatus assignmentStatus) {
		return new LiteralInstantiationResult(Type.CONTINUE, Collections.singletonList(new ImmutablePair<>(substitution, assignmentStatus)));
	}

	public static LiteralInstantiationResult stopBinding() {
		return new LiteralInstantiationResult(Type.STOP_BINDING, null);
	}

	public static LiteralInstantiationResult pushBack() {
		return new LiteralInstantiationResult(Type.PUSH_BACK, null);
	}

	public static LiteralInstantiationResult maybePushBack() {
		return new LiteralInstantiationResult(Type.MAYBE_PUSH_BACK, null);
	}

	public Type getType() {
		return this.type;
	}

	public List<ImmutablePair<Substitution, AssignmentStatus>> getSubstitutions() {
		if (this.type != Type.CONTINUE) {
			throw new UnsupportedOperationException("A LiteralInstantiationResult of type " + this.type + " does not have substitutions!");
		}
		return this.substitutions;
	}

	/**
	 * Result type. Used by {@link Grounder}s to determine how to proceed grounding a rule after instantiating the literal that yielded the
	 * respective instantiation result.
	 * 
	 * Copyright (c) 2020, the Alpha Team.
	 */
	public static enum Type {

		/**
		 * Grounder should stop working on the rule, no valid substitutions exist.
		 */
		STOP_BINDING,

		/**
		 * Grounder should continue with the next literal.
		 */
		CONTINUE,

		/**
		 * Literal instantiation yielded no ground instances, but depending on the specific workflow of the grounder, proceeding with another
		 * literal might yet make sense. Grounder should decide whether to stop binding or push the literal back in the overall grounding order.
		 */
		MAYBE_PUSH_BACK,

		/**
		 * Currently no ground instances, but proceeding with another literal might make sense, push the literal back in the overall grounding
		 * order. This is used for {@link at.ac.tuwien.kr.alpha.common.atoms.FixedInterpretationLiteral}s that do not produce bindings, like a
		 * comparision "X < Y" when one variable is not yet bound.
		 */
		PUSH_BACK;
	}
}
