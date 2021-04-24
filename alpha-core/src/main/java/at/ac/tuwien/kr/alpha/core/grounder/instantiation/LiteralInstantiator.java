/**
 * Copyright (c) 2020, the Alpha Team.
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.literals.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.ExternalLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.FixedInterpretationLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.commons.substitutions.BasicSubstitution;
import at.ac.tuwien.kr.alpha.core.atoms.EnumerationLiteral;
import at.ac.tuwien.kr.alpha.core.atoms.IntervalLiteral;

/**
 * Provides ground instantiations for literals.
 *
 * This class is intended to be used for grounding and other use cases where ground instantiations of literals need to be computed and
 * serves as an abstraction layer to decouple the knowledge of how to ground literals from the overall (rule-)grounding workflow. The task of
 * actually finding fitting ground substitutions is mostly delegated to a {@link LiteralInstantiationStrategy}.
 *
 * Copyright (c) 2020, the Alpha Team.
 */
public class LiteralInstantiator {

	private static final Logger LOGGER = LoggerFactory.getLogger(LiteralInstantiator.class);

	private final LiteralInstantiationStrategy instantiationStrategy;

	/**
	 * Creates a new {@link LiteralInstantiator} with the given {@link LiteralInstantiationStrategy}.
	 *
	 * @param instantiationStrategy the instantiation strategy to use for this instantiator
	 */
	public LiteralInstantiator(LiteralInstantiationStrategy instantiationStrategy) {
		this.instantiationStrategy = instantiationStrategy;
	}

	/**
	 * Instantiates a literal using an existing {@link BasicSubstitution} as starting point.
	 *
	 * This method is intended to be called as part of a larger rule instantiation (i.e. grounding) workflow in order to find ground
	 * instantiations of literals, i.e. extensions of the given partial substitution that yield useable ground instances for the given literal.
	 * These substitutions (if any exist) are wrapped together with some additional status information in a {@link LiteralInstantiationResult}.
	 *
	 * @param lit                 the literal for which to find substitutions that yield ground instances
	 * @param partialSubstitution a substitution that serves as a starting point. May be empty.
	 * @return a {@link LiteralInstantiationResult} containing ground substitutions - if any exist - along with some metadata for the grounder
	 */
	public LiteralInstantiationResult instantiateLiteral(Literal lit, Substitution partialSubstitution) {
		LOGGER.trace("Instantiating literal: {}", lit);
		if (lit instanceof FixedInterpretationLiteral) {
			return this.instantiateFixedInterpretationLiteral((FixedInterpretationLiteral) lit, partialSubstitution);
		} else if (lit instanceof EnumerationLiteral) {
			return this.instantiateEnumerationLiteral((EnumerationLiteral) lit, partialSubstitution);
		} else {
			// Note: At this point we just assume lit to be a basic literal, actual type
			// check is not performed since the assumption is that any literal that is no
			// FixedInterpretationLiteral or EnumerationLiteral follows the semantics of a
			// BasicLiteral even if it has another (currently not existing) type.
			return this.instantiateBasicLiteral(lit, partialSubstitution);
		}
	}

	/**
	 * Calculates satisfying substitutions for a given {@link FixedInterpretationLiteral} based on a partial substitution. This method assumes
	 * that the partial substitution has <emph>not</emph> been applied to the passed literal.
	 *
	 * @param lit                 the (fixed interpretation) literal for which to calculate substitutions
	 * @param partialSubstitution
	 * @return a LiteralInstantiationResult representing the result of the search for substitutions
	 */
	private LiteralInstantiationResult instantiateFixedInterpretationLiteral(FixedInterpretationLiteral lit, Substitution partialSubstitution) {
		LOGGER.trace("Instantiating FixedInterpretationLiteral: {}", lit);
		List<Substitution> substitutions;
		FixedInterpretationLiteral substitutedLiteral = (FixedInterpretationLiteral) lit.substitute(partialSubstitution);
		if (this.shouldPushBackFixedInterpretationLiteral(substitutedLiteral)) {
			return LiteralInstantiationResult.pushBack();
		} else {
			substitutions = substitutedLiteral.getSatisfyingSubstitutions(partialSubstitution);
			return substitutions.isEmpty() ? LiteralInstantiationResult.stopBinding()
				: LiteralInstantiationResult.continueBindingWithTrueSubstitutions(substitutions);
		}
	}

	/**
	 * Calculates a substitution that adds an enumeration index (see {@link EnumerationLiteral#addEnumerationIndexToSubstitution(BasicSubstitution)})
	 * to the given partial substitution. Due to the special nature of enumeration literals, this method will always return
	 * {@link LiteralInstantiationResult.Type#CONTINUE} as its result type. This method assumes that the partial substitution has
	 * <emph>not</emph> been applied to the passed literal.
	 *
	 * @param lit                 an enumeration literal
	 * @param partialSubstitution
	 */
	private LiteralInstantiationResult instantiateEnumerationLiteral(EnumerationLiteral lit, Substitution partialSubstitution) {
		LOGGER.trace("Instantiating EnumerationLiteral: {}", lit);
		return LiteralInstantiationResult.continueBinding(lit.addEnumerationIndexToSubstitution(partialSubstitution), AssignmentStatus.TRUE);
	}

	/**
	 * Calculates substitutions for a given literal that is not a {@link FixedInterpretationLiteral} or {@link EnumerationLiteral}.
	 * If applying the given partial substitution to the literal already grounds the literal, the resulting ground literal is verified based on
	 * this instantiators {@link LiteralInstantiationStrategy}. If the literal is only partially ground after applying the partial substitution,
	 * ground substitutions are looked up using the instantiators {@link LiteralInstantiationStrategy}. This method assumes that the partial
	 * substitution has <emph>not</emph> been applied to the passed literal.
	 *
	 * @param lit
	 * @param partialSubstitution
	 */
	private LiteralInstantiationResult instantiateBasicLiteral(Literal lit, Substitution partialSubstitution) {
		LOGGER.trace("Instantiating basic literal: {}", lit);
		List<ImmutablePair<Substitution, AssignmentStatus>> substitutions;
		Literal substitutedLiteral = lit.substitute(partialSubstitution);
		LOGGER.trace("Substituted literal is {}", substitutedLiteral);
		if (substitutedLiteral.isGround()) {
			LOGGER.trace("Literal {} is already ground, checking truth", substitutedLiteral);
			// Lit seems to be a basic literal, so its satisfiability w.r.t.
			// partialSubstitution is decided based on knownInstances by the
			// instantiationStrategy.
			AssignmentStatus truthForLiteral = this.instantiationStrategy.getTruthForGroundLiteral(substitutedLiteral);
			if (truthForLiteral == AssignmentStatus.FALSE) {
				return LiteralInstantiationResult.stopBinding();
			} else {
				return LiteralInstantiationResult.continueBinding(partialSubstitution, truthForLiteral);
			}
		} else {
			LOGGER.trace("Handling non-ground literal {}", substitutedLiteral);
			if (substitutedLiteral.isNegated()) {
				return LiteralInstantiationResult.maybePushBack();
			}
			// Query instantiationStrategy for acceptable substitutions.
			// Note: getAcceptedSubstitutions will only give substitutions where the
			// resulting ground atom is true or unassigned, false atoms are internally
			// discarded.
			substitutions = this.instantiationStrategy.getAcceptedSubstitutions(substitutedLiteral, partialSubstitution);
			LOGGER.trace("Got {} substitutions from instantiation strategy for {}", substitutions.size(), substitutedLiteral);
			return substitutions.isEmpty() ? LiteralInstantiationResult.maybePushBack() : LiteralInstantiationResult.continueBinding(substitutions);
		}
	}

	/**
	 * Helper method for <code>instantiateLiteral</code> to determine whether a {@link FixedInterpretationLiteral} may have substitutions later
	 * on and should therefore be pushed back in the grounding order.
	 *
	 * Any {@link FixedInterpretationLiteral} that does <emph>not</emph> fulfil any of the following conditions is "pushed back" in the
	 * grounding order because it cannot be used to generate substitutions now but maybe later:
	 * <ul>
	 * <li>the literal is ground</li>
	 * <li>the literal is a {@link ComparisonLiteral} that is left-assigning or right-assigning</li>
	 * <li>the literal is an {@link IntervalLiteral} representing a ground interval term</li>
	 * <li>the literal is an {@link ExternalLiteral}.</li>
	 * </ul>
	 *
	 * @param lit a {@link FixedInterpretationLiteral} that is substituted with the partial substitution passed into
	 *            <code>instantiateLiteral</code>
	 */
	private boolean shouldPushBackFixedInterpretationLiteral(FixedInterpretationLiteral lit) {
		return !(lit.isGround() ||
			(lit instanceof ComparisonLiteral && ((ComparisonLiteral) lit).isLeftOrRightAssigning()) ||
			(lit instanceof IntervalLiteral && lit.getTerms().get(0).isGround()) ||
			(lit instanceof ExternalLiteral));

	}
}
