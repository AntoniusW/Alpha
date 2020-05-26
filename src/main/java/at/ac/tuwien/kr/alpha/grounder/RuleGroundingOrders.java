/**
 * Copyright (c) 2016-2019, the Alpha Team.
 * All rights reserved.
 *
 * Additional changes made by Siemens.
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
package at.ac.tuwien.kr.alpha.grounder;

import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Provides the grounder with information on the order to ground the literals in the body of a rule.
 * Grounding starts with some starting literal (i.e., one that does not require any variables to be bound already) and
 * then may join this with any other literal that requires no other variables to be bound other than those already bound
 * by the first literal. This class is prepared to take join-selectivities into account for finding a good grounding
 * order.
 *
 * Since the grounder must yield all ground instantiations of rules whose positive body is true in the current assignment,
 * a starting literals is a positive BasicAtom and the grounder can wait until after some instance in the working memory
 * of the corresponding predicate arrives and only then start grounding.
 *
 * There is also the case that a rule has no ordinary positive literals (i.e., no positive BasicAtom) but is still safe.
 * Such a rule has no starting literal but those rules have a fixed number of ground instantiations and they can be
 * computed similar to facts at the beginning of the computation.
 *
 * Note that rules with self-joins (rules with p(X,Y), p(A,B) in their body) make it necessary that every positive
 * literal (whose interpretation is not fixed) is a starting literal, at least for the current grounding procedure.
 */
public class RuleGroundingOrders {
	private final NonGroundRule nonGroundRule;
	HashMap<Literal, RuleGroundingOrder> groundingOrders;
	private HashMap<Literal, Float> literalSelectivity;
	private List<Literal> startingLiterals;

	private final boolean fixedGroundingInstantiation;
	private RuleGroundingOrder fixedGroundingOrder;

	RuleGroundingOrders(NonGroundRule nonGroundRule) {
		this.nonGroundRule = nonGroundRule;
		this.literalSelectivity = new HashMap<>();
		resetLiteralSelectivity();
		this.groundingOrders = new HashMap<>();
		this.fixedGroundingInstantiation = computeStartingLiterals();
	}

	private void resetLiteralSelectivity() {
		// Set selectivity of all literals to 1.0f.
		for (Literal literal : nonGroundRule.getBodyLiterals()) {
			literalSelectivity.put(literal, 1.0f);
		}
	}

	/**
	 * Computes starting literals and indicates whether there is a fixed ground instantiation for this rule.
	 * @return true iff the rule has a fixed ground instantiation.
	 */
	private boolean computeStartingLiterals() {
		LinkedHashSet<Literal> fixedStartingLiterals = new LinkedHashSet<>();
		LinkedHashSet<Literal> ordinaryStartingLiterals = new LinkedHashSet<>();
		
		// If the rule is ground, every body literal is a starting literal and the ground instantiation is fixed.
		if (nonGroundRule.getRule().isGround()) {
			startingLiterals = new LinkedList<>(nonGroundRule.getBodyLiterals());
			return true;
		}
		
		// Check each literal in the rule body whether it is eligible.
		for (Literal literal : nonGroundRule.getBodyLiterals()) {
			// Only literals that need no variables already bound can start grounding.
			if (literal.getNonBindingVariables().size() != 0) {
				continue;
			}

			if (literal.getAtom() instanceof BasicAtom && !literal.isNegated()) {
				// Positive BasicAtom is the main/ordinary case.
				ordinaryStartingLiterals.add(literal);
			} else {
				// If literal is no positive BasicAtom but requires no bound variables,
				// it can be the starting literal for some (fixed) instantiation.
				fixedStartingLiterals.add(literal);
			}
		}
		// If there are no positive BasicAtoms, the rule only contains fixed ground
		// instantiation literals and those are starting for the one-time grounding.
		if (!ordinaryStartingLiterals.isEmpty()) {
			startingLiterals = new LinkedList<>(ordinaryStartingLiterals);
			return false;
		} else if (!fixedStartingLiterals.isEmpty()) {
			startingLiterals = new LinkedList<>(fixedStartingLiterals);
			return true;
		} else {
			throw new RuntimeException("Unsafe rule encountered: " + nonGroundRule.getRule());
		}
	}

	Collection<Literal> getStartingLiterals() {
		return Collections.unmodifiableList(startingLiterals);
	}

	public void updateLiteralSelectivity(Literal literal, int numGivenTuples, int numObtainedTuples) {
		// TODO: add old selectivity (with a decay factor) and new selectivity.
	}

	RuleGroundingOrder orderStartingFrom(Literal startingLiteral) {
		return groundingOrders.get(startingLiteral);
	}


	RuleGroundingOrder getFixedGroundingOrder() {
		return fixedGroundingOrder;
	}

	/**
	 * States whether the rule is without positive ordinary atoms, as for example in: p(Y) :- X = 1..3, not q(X), Y = X + 2, &ext[X,Y]().
	 * @return true if the rule has a (limited number of) fixed grounding instantiation(s).
	 */
	boolean fixedInstantiation() {
		return fixedGroundingInstantiation;
	}

	void computeGroundingOrders() {
		if (fixedGroundingInstantiation) {
			// Fixed grounding is only evaluated once and not depending on a starting variable, just use the first.
			computeGroundingOrder(startingLiterals.get(0));
			return;
		}
		// Compute grounding orders for all positive BasicAtoms.
		for (Literal literal : startingLiterals) {
			computeGroundingOrder(literal);
		}
	}

	private void computeGroundingOrder(Literal startingLiteral) {
		List<Literal> bodyLiterals = nonGroundRule.getBodyLiterals();
		HashSet<VariableTerm> boundVariables = new HashSet<>();
		boundVariables.addAll(startingLiteral.getBindingVariables());
		LinkedHashSet<Literal> remainingLiterals = new LinkedHashSet<>(bodyLiterals);
		remainingLiterals.remove(startingLiteral);
		ArrayList<Literal> literalsOrder;
		if (fixedGroundingInstantiation) {
			literalsOrder = new ArrayList<>(bodyLiterals.size());
			literalsOrder.add(startingLiteral);
		} else {
			literalsOrder = new ArrayList<>(bodyLiterals.size() - 1);
		}
		
		int position = 0;
		int positionLastVarBound = -1;
		while (!remainingLiterals.isEmpty()) {
			Literal nextGroundingLiteral = selectNextGroundingLiteral(remainingLiterals, boundVariables);
			if (nextGroundingLiteral == null) {
				throw new RuntimeException("Could not find a grounding order for rule " + nonGroundRule + " with starting literal: " + startingLiteral + ". Rule is not safe.");
			}
			remainingLiterals.remove(nextGroundingLiteral);
			boolean boundNewVars = boundVariables.addAll(nextGroundingLiteral.getBindingVariables());
			if (boundNewVars) {
				positionLastVarBound = position;
			}
			literalsOrder.add(nextGroundingLiteral);
			position++;
		}
		if (fixedGroundingInstantiation) {
			fixedGroundingOrder = new RuleGroundingOrder(null, literalsOrder, positionLastVarBound, nonGroundRule.getRule().isGround());
		}
		groundingOrders.put(startingLiteral, new RuleGroundingOrder(startingLiteral, literalsOrder, positionLastVarBound, nonGroundRule.getRule().isGround()));
	}

	private Literal selectNextGroundingLiteral(LinkedHashSet<Literal> remainingLiterals, Set<VariableTerm> boundVariables) {
		Float bestSelectivity = Float.MAX_VALUE;
		Literal bestLiteral = null;
		boolean bestLiteralSharesVariables = false;
		// Find the best literal whose nonbinding variables are already bound and whose selectivity is highest.
		// To avoid cross products, select those first that have some of their variables already bound.
		for (Literal literal : remainingLiterals) {
			if (!boundVariables.containsAll(literal.getNonBindingVariables())) {
				// Only consider literals whose nonbinding variables are already bound.
				continue;
			}
			Float selectivity = literalSelectivity.get(literal);
			boolean sharesVariables = sharesVariables(boundVariables, literal.getBindingVariables(), literal.getNonBindingVariables());
			if (bestLiteral == null
				|| sharesVariables && selectivity < bestSelectivity
				|| sharesVariables && !bestLiteralSharesVariables) {
				bestLiteral = literal;
				bestSelectivity = selectivity;
				bestLiteralSharesVariables = sharesVariables;
			}
		}
		return bestLiteral;
	}

	private boolean sharesVariables(Collection<VariableTerm> set1, Collection<VariableTerm> set2part1, Collection<VariableTerm> set2part2) {
		return !Collections.disjoint(set1, set2part1) || !Collections.disjoint(set1, set2part2);
	}
}
