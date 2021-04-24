/**
 * Copyright (c) 2019-2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.app;


import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class DependencyGraphWriter {

	private static final String DEFAULT_GRAPH_HEADING = "digraph dependencyGraph";

	private static final String DEFAULT_NODE_FORMAT = "n%d [label = \"%s\"]%n";
	private static final String DEFAULT_EDGE_FORMAT = "n%d -> n%d [xlabel=\"%s\" labeldistance=0.1]%n";

	public void writeAsDot(DependencyGraph graph, OutputStream out) {
		writeAsDot(graph.getAdjancencyMap(), out);
	}

	private void writeAsDot(Map<DependencyGraph.Node, List<DependencyGraph.Edge>> graph, OutputStream out) {
		BiFunction<DependencyGraph.Node, Integer, String> nodeFormatter = this::buildNodeString;
		writeAsDot(graph, out, nodeFormatter);
	}

	private void writeAsDot(Map<DependencyGraph.Node, List<DependencyGraph.Edge>> graph, OutputStream out, BiFunction<DependencyGraph.Node, Integer, String> nodeFormatter) {
		PrintStream ps = new PrintStream(out);
		startGraph(ps);

		Set<Map.Entry<DependencyGraph.Node, List<DependencyGraph.Edge>>> graphDataEntries = graph.entrySet();
		// First, write all nodes.
		int nodeCnt = 0;
		Map<DependencyGraph.Node, Integer> nodesToNumbers = new HashMap<>();
		for (Map.Entry<DependencyGraph.Node, List<DependencyGraph.Edge>> entry : graphDataEntries) {
			ps.print(nodeFormatter.apply(entry.getKey(), nodeCnt));
			nodesToNumbers.put(entry.getKey(), nodeCnt);
			nodeCnt++;
		}

		// Second, write all edges.
		for (Map.Entry<DependencyGraph.Node, List<DependencyGraph.Edge>> entry : graphDataEntries) {
			int fromNodeNum = nodesToNumbers.get(entry.getKey());
			for (DependencyGraph.Edge edge : entry.getValue()) {
				int toNodeNum = nodesToNumbers.get(edge.getTarget());
				ps.printf(DependencyGraphWriter.DEFAULT_EDGE_FORMAT, fromNodeNum, toNodeNum, edge.getSign() ? "+" : "-");
			}
		}

		finishGraph(ps);
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

	private String buildNodeString(DependencyGraph.Node n, int nodeNum) {
		return String.format(DependencyGraphWriter.DEFAULT_NODE_FORMAT, nodeNum, n.getLabel());
	}

}
