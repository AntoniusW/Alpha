package at.ac.tuwien.kr.alpha.common.depgraph.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.kr.alpha.common.depgraph.DependencyGraph;
import at.ac.tuwien.kr.alpha.common.depgraph.Edge;
import at.ac.tuwien.kr.alpha.common.depgraph.Node;

public class DependencyGraphWriter {

	private static final String DEFAULT_GRAPH_HEADING = "digraph dependencyGraph";

	private static final String DEFAULT_NODE_FORMAT = "n%d [label = \"%s\"]\n";
	private static final String DEFAULT_EDGE_FORMAT = "n%d -> n%d [xlabel=\"%s\" labeldistance=0.1]\n";

	public void writeAsDotfile(DependencyGraph graph, String path) throws IOException {
		this.writeAsDot(graph, new FileOutputStream(path));
	}

	public void writeAsDot(DependencyGraph graph, OutputStream out) throws IOException {
		PrintStream ps = new PrintStream(out);
		Map<Node, List<Edge>> graphData = graph.getNodes();

		this.startGraph(ps);

		Set<Map.Entry<Node, List<Edge>>> graphDataEntries = graphData.entrySet();
		// first write all nodes
		int nodeCnt = 0;
		Map<Node, Integer> nodesToNumbers = new HashMap<>();
		for (Map.Entry<Node, List<Edge>> entry : graphDataEntries) {
			ps.printf(DependencyGraphWriter.DEFAULT_NODE_FORMAT, nodeCnt, entry.getKey().getLabel());
			nodesToNumbers.put(entry.getKey(), nodeCnt);
			nodeCnt++;
		}

		// now, write edges
		int fromNodeNum = -1;
		int toNodeNum = -1;
		for (Map.Entry<Node, List<Edge>> entry : graphDataEntries) {
			fromNodeNum = nodesToNumbers.get(entry.getKey());
			for (Edge edge : entry.getValue()) {
				toNodeNum = nodesToNumbers.get(edge.getTarget());
				ps.printf(DependencyGraphWriter.DEFAULT_EDGE_FORMAT, fromNodeNum, toNodeNum, edge.getSign() ? "+" : "-");
			}
		}

		this.finishGraph(ps);
		ps.close();
	}

	private void startGraph(PrintStream ps) {
		ps.println(DependencyGraphWriter.DEFAULT_GRAPH_HEADING);
		ps.println("{");
		ps.println("splines=false;");
		ps.println("ranksep=4.0;");
	}

	private void finishGraph(PrintStream ps) {
		ps.println("}");
	}

}