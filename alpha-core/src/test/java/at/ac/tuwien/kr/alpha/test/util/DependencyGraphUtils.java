package at.ac.tuwien.kr.alpha.test.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph.Edge;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph.Node;

/**
 * 
 * Copyright (c) 2019-2020, the Alpha Team.
 */
public final class DependencyGraphUtils {

	private DependencyGraphUtils() {

	}

	public static boolean isReachableFrom(Node dest, Node src, DependencyGraph dg) {
		return DependencyGraphUtils.isReachableFrom(dest, src, dg, new HashSet<>());
	}

	private static boolean isReachableFrom(Node dest, Node src, DependencyGraph dg, Set<Node> discovered) {
		List<Edge> outgoingEdges;
		if (src.equals(dest)) {
			return true;
		}
		if ((outgoingEdges = dg.getAdjancencyMap().get(src)) == null) {
			return false;
		}
		discovered.add(src);
		// Checking all edges first before descending deeper.
		for (Edge edge : outgoingEdges) {
			if (edge.getTarget().equals(dest)) {
				return true;
			}
		}
		for (Edge tmp : outgoingEdges) {
			if (discovered.contains(tmp.getTarget())) {
				// Cycle found, do not descend deeper.
				continue;
			}
			if (DependencyGraphUtils.isReachableFrom(dest, tmp.getTarget(), dg, discovered)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the given nodes are strongly connected within the given dependency graph. Strongly connected
	 * means every node in the given list is reachable from every other node in the list and vice versa.
	 * 
	 * @param connectedNodes the nodes to check.
	 * @param dg             the dependency graph in which to check.
	 * @return true if the given nodes are strongly connected, false otherwise.
	 */
	public static boolean areStronglyConnected(List<Node> connectedNodes, DependencyGraph dg) {
		for (Node n1 : connectedNodes) {
			for (Node n2 : connectedNodes) {
				if (!(DependencyGraphUtils.isReachableFrom(n2, n1, dg) && DependencyGraphUtils.isReachableFrom(n1, n2, dg))) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isStronglyConnectedComponent(List<Node> componentNodes, DependencyGraph dg) {
		if (!DependencyGraphUtils.areStronglyConnected(componentNodes, dg)) {
			return false;
		}
		// Check if the given set is maximal.
		List<Node> lst = new ArrayList<>(componentNodes);
		for (Node n : dg.getAdjancencyMap().keySet()) {
			if (lst.contains(n)) {
				continue;
			}
			lst.add(n);
			if (DependencyGraphUtils.areStronglyConnected(lst, dg)) {
				// Not a strongly connected component if there is a bigger set which is strongly connected.
				return false;
			}
		}
		return true;
	}

}
