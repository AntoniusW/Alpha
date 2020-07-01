package at.ac.tuwien.kr.alpha.grounder.instantiation;

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.ExternalLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.FixedInterpretationLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.grounder.Substitution;
import at.ac.tuwien.kr.alpha.grounder.atoms.EnumerationLiteral;
import at.ac.tuwien.kr.alpha.grounder.atoms.IntervalLiteral;

/**
 * Provides ground instantiations for literals.
 * 
 * RuleInstantiator is intended to be used for grounding and other use cases where ground instantiations 
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public class LiteralInstantiator {

	private static final Logger LOGGER = LoggerFactory.getLogger(LiteralInstantiator.class);

	private final LiteralInstantiationStrategy instantiationStrategy;
	private final boolean pushBackNonGroundNegatedLiterals;

	public LiteralInstantiator(LiteralInstantiationStrategy instantiationStrategy, boolean pushBackNonGroundNegatedLiterals) {
		this.instantiationStrategy = instantiationStrategy;
		this.pushBackNonGroundNegatedLiterals = pushBackNonGroundNegatedLiterals;
	}

	// FIXME we should find a global contract for when to call
	// "substitute(substitution)" on a literal

	// TODO think about method name
	// Method contract: literal is NOT substituted with given partialSubstitution
	// Method contract: knownInstances is an instance storage containing all
	// instances to consider when grounding given literal,
	// supposed to be null for FixedInterpretationLiteral and EnumerationLiteral
	public LiteralInstantiationResult instantiateLiteral(Literal lit, Substitution partialSubstitution) {
		LOGGER.trace("Instantiating literal: {}", lit);
		LiteralInstantiationResult retVal;
		if (lit instanceof FixedInterpretationLiteral) {
			retVal = this.instantiateFixedInterpretationLiteral((FixedInterpretationLiteral) lit, partialSubstitution);
		} else if (lit instanceof EnumerationLiteral) {
			retVal = this.instantiateEnumerationLiteral((EnumerationLiteral) lit, partialSubstitution);
		} else {
			// Note: At this point we just assume lit to be a basic literal, actual type
			// check is not performed since the assumption is that any literal that is no
			// FixedInterpretationLiteral or EnumerationLiteral follows the semantics of a
			// BasicLiteral even if it has another (currently not existing) type.
			retVal = this.instantiateBasicLiteral(lit, partialSubstitution);
		}
		return retVal;
	}

	/**
	 * Calculates satisfying substitutions for a given
	 * {@link FixedInterpretationLiteral} based on a partial substitution.
	 * This method assumes that the partial substitution has <emph>not</emph> been
	 * applied to the passed literal.
	 * 
	 * @param lit                 the (fixed interpretation) literal for which to
	 *                            calculate substitutions
	 * @param partialSubstitution
	 * @return a LiteralInstantiationResult representing the result of the search
	 *         for substitutions
	 */
	private LiteralInstantiationResult instantiateFixedInterpretationLiteral(FixedInterpretationLiteral lit, Substitution partialSubstitution) {
		LOGGER.trace("Instantiating FixedInterpretationLiteral: {}", lit);
		LiteralInstantiationResult retVal;
		List<Substitution> substitutions;
		FixedInterpretationLiteral substitutedLiteral = (FixedInterpretationLiteral) lit.substitute(partialSubstitution);
		if (this.shouldPushBackFixedInterpretationLiteral(substitutedLiteral)) {
			retVal = LiteralInstantiationResult.pushBack();
		} else {
			substitutions = substitutedLiteral.getSatisfyingSubstitutions(partialSubstitution);
			retVal = substitutions.isEmpty() ? LiteralInstantiationResult.stopBinding()
					: LiteralInstantiationResult.continueBindingWithTrueSubstitutions(substitutions);
		}
		return retVal;
	}

	/**
	 * Calculates a substitution that adds an enumeration index (see
	 * {@link EnumerationLiteral#addEnumerationIndexToSubstitution(Substitution)})
	 * to the given partial substitution. Due to the special nature of enumeration
	 * literals, this method will always return
	 * {@link LiteralInstantiationResult.Type#CONTINUE} as its result type.
	 * This method assumes that the partial substitution has <emph>not</emph> been
	 * applied to the passed literal.
	 * 
	 * @param lit                 an enumeration literal
	 * @param partialSubstitution
	 */
	private LiteralInstantiationResult instantiateEnumerationLiteral(EnumerationLiteral lit, Substitution partialSubstitution) {
		LOGGER.trace("Instantiating EnumerationLiteral: {}", lit);
		return LiteralInstantiationResult.continueBinding(lit.addEnumerationIndexToSubstitution(partialSubstitution), AssignmentStatus.TRUE);
	}

	/**
	 * Calculates substitutions for a given literal that is not a
	 * {@link FixedInterpretationLiteral} or {@link EnumerationLiteral}.
	 * If applying the given partial substitution to the literal already grounds the
	 * literal, the resulting ground literal is verified based on this instantiators
	 * {@link LiteralInstantiationStrategy}.
	 * If the literal is only partially ground after applying the partial
	 * substitution, ground substitutions are looked up using the instantiators
	 * {@link LiteralInstantiationStrategy}.
	 * This method assumes that the partial substitution has <emph>not</emph> been
	 * applied to the passed literal.
	 * 
	 * @param lit
	 * @param partialSubstitution
	 * @param storageView
	 */
	private LiteralInstantiationResult instantiateBasicLiteral(Literal lit, Substitution partialSubstitution) {
		LOGGER.trace("Instantiating basic literal: {}", lit);
		LiteralInstantiationResult retVal;
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
				retVal = LiteralInstantiationResult.stopBinding();
			} else {
				retVal = LiteralInstantiationResult.continueBinding(partialSubstitution, truthForLiteral);
			}
		} else {
			LOGGER.trace("Handling non-ground literal {}", substitutedLiteral);
			if (substitutedLiteral.isNegated()) {
				if (this.pushBackNonGroundNegatedLiterals) {
					return LiteralInstantiationResult.pushBack();
				} else {
					throw Util.oops("We got a non-ground literal which is negated, but are not allowed to push it back - should not happen!");
				}
			}
			// Query instantiationStrategy for acceptable substitutions.
			// Note: getAcceptedSubstitutions will only give substitutions where the
			// resulting ground atom is true or unassigned, false atoms are internally
			// discarded
			substitutions = this.instantiationStrategy.getAcceptedSubstitutions(substitutedLiteral, partialSubstitution);
			LOGGER.trace("Got {} substitutions from instantiation strategy for {}", substitutions.size(), substitutedLiteral);
			retVal = substitutions.isEmpty() ? LiteralInstantiationResult.stopBinding() : LiteralInstantiationResult.continueBinding(substitutions);
		}
		return retVal;
	}

	/**
	 * Helper method for <code>instantiateLiteral</code> to determine whether a
	 * {@link FixedInterpretationLiteral} may have substitutions later on and should
	 * therefore be pushed back in the grounding order.
	 * 
	 * Any {@link FixedInterpretationLiteral} that does <emph>not</emph> fulfil any
	 * of the following conditions is "pushed back" in the grounding order because
	 * it cannot be used to generate substitutions now but maybe later:
	 * <ul>
	 * <li>the literal is ground</li>
	 * <li>the literal is a {@link ComparisonLiteral} that is left-assigning or
	 * right-assigning</li>
	 * <li>the literal is an {@link IntervalLiteral} representing a ground interval
	 * term</li>
	 * <li>the literal is an {@link ExternalLiteral}.</li>
	 * </ul>
	 * 
	 * @param lit a {@link FixedInterpretationLiteral} that is substituted with the
	 *            partial substitution passed into <code>instantiateLiteral</code>
	 */
	private boolean shouldPushBackFixedInterpretationLiteral(FixedInterpretationLiteral lit) {
		return !(lit.isGround() ||
				(lit instanceof ComparisonLiteral && ((ComparisonLiteral) lit).isLeftOrRightAssigning()) ||
				(lit instanceof IntervalLiteral && lit.getTerms().get(0).isGround()) ||
				(lit instanceof ExternalLiteral));

	}
}