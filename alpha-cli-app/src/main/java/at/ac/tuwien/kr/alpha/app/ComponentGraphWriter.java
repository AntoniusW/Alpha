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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map.Entry;

import at.ac.tuwien.kr.alpha.api.programs.analysis.ComponentGraph;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;

public class ComponentGraphWriter {

	private static final String GRAPH_HEADING = "digraph componentGraph";

	private static final String NODE_FMT = "n%d [label = C%d]%n";
	private static final String EDGE_FMT = "n%d -> n%d [xlabel=\"%s\" labeldistance=0.1]%n";

	public void writeAsDot(ComponentGraph graph, OutputStream out) {
		PrintStream ps = new PrintStream(out);
		startGraph(ps);
		writeComponentsTable(ps, graph);
		ps.println();
		writeGraph(ps, graph);
		finishGraph(ps);
		ps.close();
	}

	private void startGraph(PrintStream ps) {
		ps.println(ComponentGraphWriter.GRAPH_HEADING);
		ps.println("{");
		ps.println("splines=false;");
		ps.println("ranksep=4.0;");
	}

	private void writeComponentsTable(PrintStream ps, ComponentGraph cg) {
		ps.println("label = <");
		ps.println("\t<table border = '1' cellborder = '0'>");
		StringBuilder headerBuilder = new StringBuilder("<tr>");
		headerBuilder.append("<td>Component Id</td>");
		for (int i = 0; i < cg.getComponents().size(); i++) {
			headerBuilder.append("<td>").append(i).append("</td>");
		}
		headerBuilder.append("</tr>");
		ps.println("\t\t" + headerBuilder.toString());
		StringBuilder contentBuilder = new StringBuilder("<tr>");
		contentBuilder.append("<td>Predicates</td>");
		for (int i = 0; i < cg.getComponents().size(); i++) {
			contentBuilder.append("<td>");
			for (DependencyGraph.Node n : cg.getComponents().get(i).getNodes()) {
				contentBuilder.append(n.getLabel()).append("<br/>");
			}
			contentBuilder.append("</td>");
		}
		contentBuilder.append("</tr>");
		ps.println("\t\t" + contentBuilder.toString());
		ps.println("\t</table>");
		ps.println(">");
	}

	private void writeGraph(PrintStream ps, ComponentGraph cg) {
		List<ComponentGraph.SCComponent> components = cg.getComponents();
		// Write the node descriptors for the components.
		for (int componentId = 0; componentId < components.size(); componentId++) {
			ps.printf(NODE_FMT, componentId, componentId);
			for (Entry<Integer, Boolean> dependency : components.get(componentId).getDependencyIds().entrySet()) {
				ps.printf(EDGE_FMT, dependency.getKey(), componentId, dependency.getValue().equals(true) ? "+" : "-");
			}
		}
	}

	private void finishGraph(PrintStream ps) {
		ps.println("}");
	}

}
