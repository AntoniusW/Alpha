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

import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs a depth-first search on a given graph.
 * The algorithm follows the approach outlined in "Introduction to Algorithms, 3rd Edition" by Cormen et al.
 */
class DepthFirstSearchAlgorithm {

	private Set<DependencyGraph.Node>                             discoveredNodes;	// The "gray" nodes that have been seen but not fully processed yet.
	private Deque<DependencyGraph.Node>                           finishedNodes;	// The "black" nodes that have been processed.
	private Map<DependencyGraph.Node, List<DependencyGraph.Node>> depthFirstReachable;	// Per root node, all others that are reachable from it.

	private DepthFirstSearchAlgorithm() {
		discoveredNodes = new HashSet<>();
		finishedNodes = new LinkedList<>();
		depthFirstReachable = new HashMap<>();
		depthFirstReachable.put(null, new ArrayList<>());
	}

	public static DfsResult performDfs(Map<DependencyGraph.Node, List<DependencyGraph.Edge>> graph) {
		return performDfs(graph.keySet().iterator(), graph);
	}

	/**
	 * Performs a depth-first search on the given graph. The algorithm follows the approach outlined in 
	 * "Introduction to Algortihms, 3rd Edition" by Cormen et al. 
	 * Discovered nodes are "gray" and finished nodes "black", respectively, in the terminology used in the book.
	 * 
	 * @param nodeVisitIt an Iterator over all nodes of the graph, defining in which sequence nodes should be
	 *                    visited (i.e., nodeVisitIt yields all initially "white" nodes of the graph).
	 * @param graph       an adjacency map defining the dependency graph of an ASP program.
	 * @return a {@link DfsResult} holding all nodes in the sequence they were finished and per root node all its
	 *         depth-first reachable nodes.
	 */
	public static DfsResult performDfs(Iterator<DependencyGraph.Node> nodeVisitIt, Map<DependencyGraph.Node, List<DependencyGraph.Edge>> graph) {
		DepthFirstSearchAlgorithm algorithm = new DepthFirstSearchAlgorithm();
		return algorithm.performDepthFirstSearchAlgorithm(nodeVisitIt, graph);
	}

	private DfsResult performDepthFirstSearchAlgorithm(Iterator<DependencyGraph.Node> nodeVisitIt, Map<DependencyGraph.Node, List<DependencyGraph.Edge>> graph) {
		while (nodeVisitIt.hasNext()) {
			DependencyGraph.Node currentNode = nodeVisitIt.next();
			if (!discoveredNodes.contains(currentNode)) {
				depthFirstReachable.get(null).add(currentNode);
				dfsVisit(currentNode, currentNode, graph);
			}
		}
		DfsResult dfsResult = new DfsResult();
		dfsResult.finishedNodes = finishedNodes;
		dfsResult.depthFirstReachable = depthFirstReachable;
		return dfsResult;
	}

	private void dfsVisit(DependencyGraph.Node currNode, DependencyGraph.Node rootNode, Map<DependencyGraph.Node, List<DependencyGraph.Edge>> graph) {
		discoveredNodes.add(currNode);

		// Record root-reachability of current node.
		if (!depthFirstReachable.containsKey(rootNode)) {
			depthFirstReachable.put(rootNode, new ArrayList<>());
		}
		depthFirstReachable.get(rootNode).add(currNode);

		for (DependencyGraph.Edge edge : graph.get(currNode)) {
			// Proceed with adjacent (i.e., deeper) nodes first.
			DependencyGraph.Node tmpNeighbor = edge.getTarget();
			if (!discoveredNodes.contains(tmpNeighbor)) {
				dfsVisit(tmpNeighbor, rootNode, graph);
			}
		}
		finishedNodes.add(currNode);
	}

	/**
	 * The result of a depth first search:
	 * - a deque containing all nodes in their finishing order and
	 * - per root node all reachable other nodes (including the root itself).
	 */
	static class DfsResult {
		Deque<DependencyGraph.Node>                           finishedNodes;
		Map<DependencyGraph.Node, List<DependencyGraph.Node>> depthFirstReachable;
	}
}
