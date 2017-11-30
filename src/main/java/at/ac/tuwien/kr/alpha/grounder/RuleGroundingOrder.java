package at.ac.tuwien.kr.alpha.grounder;

import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.Variable;
import at.ac.tuwien.kr.alpha.common.terms.Variable;

import java.util.*;

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
 * Copyright (c) 2017, the Alpha Team.
 */
public class RuleGroundingOrder {
	private final NonGroundRule nonGroundRule;
	private HashMap<Literal, Literal[]> groundingOrder;
	private HashMap<Literal, Float> literalSelectivity;
	private LinkedList<Literal> startingLiterals;

	private final boolean fixedGroundingInstantiation;
	private Literal[] fixedGroundingOrder;

	RuleGroundingOrder(NonGroundRule nonGroundRule) {
		this.nonGroundRule = nonGroundRule;
		this.literalSelectivity = new HashMap<>();
		resetLiteralSelectivity();
		this.groundingOrder = new HashMap<>();
		this.fixedGroundingInstantiation = computeStartingLiterals();
	}

	private void resetLiteralSelectivity() {
		// Set selectivity of all literals to 1.0f.
		for (Literal literal : nonGroundRule.getRule().getBody()) {
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
		// Check each literal in the rule body whether it is eligible.
		for (Literal literal : nonGroundRule.getRule().getBody()) {
			// Only literals that need no variables already bound can start grounding.
			if (literal.getNonBindingVariables().size() != 0) {
				continue;
			}

			if (literal instanceof BasicAtom && !literal.isNegated()) {
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

	Literal[] orderStartingFrom(Literal startingLiteral) {
		return groundingOrder.get(startingLiteral);
	}


	Literal[] getFixedGroundingOrder() {
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
		List<Literal> bodyLiterals = nonGroundRule.getRule().getBody();
		HashSet<Variable> boundVariables = new HashSet<>();
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
		while (!remainingLiterals.isEmpty()) {
			Literal nextGroundingLiteral = selectNextGroundingLiteral(remainingLiterals, boundVariables);
			if (nextGroundingLiteral == null) {
				throw new RuntimeException("Could not find a grounding order for rule " + nonGroundRule + " with starting literal: " + startingLiteral + ". Rule is not safe.");
			}
			remainingLiterals.remove(nextGroundingLiteral);
			boundVariables.addAll(nextGroundingLiteral.getBindingVariables());
			literalsOrder.add(nextGroundingLiteral);
		}
		if (fixedGroundingInstantiation) {
			fixedGroundingOrder = literalsOrder.toArray(new Literal[0]);
		}
		groundingOrder.put(startingLiteral, literalsOrder.toArray(new Literal[0]));
	}

	private Literal selectNextGroundingLiteral(LinkedHashSet<Literal> remainingLiterals, Set<Variable> boundVariables) {
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

	private boolean sharesVariables(Collection<Variable> set1, Collection<Variable> set2part1, Collection<Variable> set2part2) {
		return !Collections.disjoint(set1, set2part1) || !Collections.disjoint(set1, set2part2);
	}
}
