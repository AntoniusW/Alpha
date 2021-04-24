package at.ac.tuwien.kr.alpha.api.programs.analysis;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;

import java.util.List;
import java.util.Map;

public interface DependencyGraph {

	Node getNodeForPredicate(Predicate p);

	Map<Node, List<Edge>> getAdjancencyMap();

	interface Edge {

		Node getTarget();

		boolean getSign();

	}

	interface Node {

		Predicate getPredicate();

		String getLabel();

	}
}
