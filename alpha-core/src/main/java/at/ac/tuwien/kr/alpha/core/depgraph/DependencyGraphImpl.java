/**
 * Copyright (c) 2019, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.core.depgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.FixedInterpretationLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.core.rules.CompiledRule;

/**
 * Internal representation of an {@link at.ac.tuwien.kr.alpha.core.programs.InternalProgram}'s dependency graph. The dependency graph tracks dependencies
 * between rules of a program. Each {@link NodeImpl} of the graph represents a {@link Predicate} occurring in the program. A node has an incoming {@link EdgeImpl} for
 * every {@link Literal} in some rule body that depends on it, i.e. the predicate of the literal in question is the same as that of the node. The "sign" flag of
 * an {@link EdgeImpl} indicates whether the dependency is a positive or negative one, i.e. if the atom in question is preceded by a "not".
 * 
 * Note that constraints are represented by one dummy predicate (named "constr_{num}"). Each constraint node has a negative edge to itself to express the
 * notation of a constraint ":- a, b." as "x :- a, b, not x.".
 * 
 * Copyright (c) 2019-2020, the Alpha Team.
 */
public final class DependencyGraphImpl  implements DependencyGraph {

	private static final Logger LOGGER = LoggerFactory.getLogger(DependencyGraphImpl.class);

	/*
	 * Maps nodes to outgoing edges of that node.
	 * Note: using List<EdgeImpl> rather than Map<Integer,Boolean> as one node may have positive and negative edges to
	 * another.
	 */
	private final Map<Node, List<Edge>> adjacencyMap;

	private final Map<Predicate, Node> nodesByPredicate;

	private DependencyGraphImpl(Map<Node, List<Edge>> adjacencyMap, Map<Predicate, Node> nodesByPredicate) {
		this.adjacencyMap = adjacencyMap;
		this.nodesByPredicate = nodesByPredicate;
	}

	public static DependencyGraphImpl buildDependencyGraph(Map<Integer, CompiledRule> nonGroundRules) {
		return new DependencyGraphImpl.Builder(nonGroundRules.values()).build();
	}

	@Override
	public Node getNodeForPredicate(Predicate p) {
		return nodesByPredicate.get(p);
	}

	@Override
	public Map<Node, List<Edge>> getAdjancencyMap() {
		return Collections.unmodifiableMap(adjacencyMap);
	}

	private static class Builder {

		private static final String CONSTRAINT_PREDICATE_FORMAT = "[constr_%d]";

		private int constraintNumber;
		private Collection<CompiledRule> rules;

		private Map<Node, List<Edge>> adjacentNodesMap = new HashMap<>();
		private Map<Predicate, Node>      nodesByPredicate = new HashMap<>();

		private Builder(Collection<CompiledRule> rules) {
			this.rules = rules;
			this.constraintNumber = 0;
		}

		private DependencyGraphImpl build() {
			for (CompiledRule rule : rules) {
				LOGGER.debug("Processing rule: {}", rule);
				Node headNode = handleRuleHead(rule);
				for (Literal literal : rule.getBody()) {
					LOGGER.trace("Processing rule body literal: {}", literal);
					if (literal instanceof FixedInterpretationLiteral) {
						LOGGER.trace("Ignoring FixedInterpretationLiteral {}", literal);
						continue;
					}
					handleRuleBodyLiteral(headNode, literal);
				}
			}
			return new DependencyGraphImpl(adjacentNodesMap, nodesByPredicate);
		}

		private Node handleRuleHead(CompiledRule rule) {
			LOGGER.trace("Processing head of rule: {}", rule);
			Node headNode;
			if (rule.isConstraint()) {
				Predicate pred = generateConstraintDummyPredicate();
				headNode = new NodeImpl(pred);
				List<Edge> dependencies = new ArrayList<>();
				dependencies.add(new EdgeImpl(headNode, false));
				if (adjacentNodesMap.containsKey(headNode)) {
					throw new IllegalStateException("Dependency graph already contains node for constraint " + pred.toString() + "!");
				}
				adjacentNodesMap.put(headNode, dependencies);
				nodesByPredicate.put(pred, headNode);
			} else {
				Atom head = rule.getHeadAtom();
				Predicate pred = head.getPredicate();
				if (!nodesByPredicate.containsKey(pred)) {
					headNode = new NodeImpl(pred);
					adjacentNodesMap.put(headNode, new ArrayList<>());
					nodesByPredicate.put(pred, headNode);
				} else {
					headNode = nodesByPredicate.get(pred);
				}
			}
			return headNode;
		}

		private void handleRuleBodyLiteral(Node headNode, Literal lit) {
			List<Edge> dependants;
			Node bodyNode;
			Predicate bodyPred = lit.getPredicate();
			if (!nodesByPredicate.containsKey(bodyPred)) {
				LOGGER.trace("Creating new node for bodyPred {}", bodyPred);
				dependants = new ArrayList<>();
				bodyNode = new NodeImpl(bodyPred);
				nodesByPredicate.put(bodyPred, bodyNode);
				adjacentNodesMap.put(bodyNode, dependants);
			} else {
				LOGGER.trace("Node for bodyPred {} already exists", bodyPred);
				bodyNode = nodesByPredicate.get(bodyPred);
				dependants = adjacentNodesMap.get(bodyNode);
			}
			Edge tmpEdge = new EdgeImpl(headNode, !lit.isNegated());
			if (!dependants.contains(tmpEdge)) {
				LOGGER.trace("Adding dependency: {} -> {} ({})", bodyNode.getPredicate(), headNode.getPredicate(), tmpEdge.getSign() ? "+" : "-");
				dependants.add(tmpEdge);
			}
			nodesByPredicate.put(bodyPred, bodyNode);
		}

		private Predicate generateConstraintDummyPredicate() {
			return Predicates.getPredicate(String.format(DependencyGraphImpl.Builder.CONSTRAINT_PREDICATE_FORMAT, ++constraintNumber), 0);
		}
	}
}
