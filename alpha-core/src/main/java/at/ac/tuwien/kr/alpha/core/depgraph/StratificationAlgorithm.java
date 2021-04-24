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

import at.ac.tuwien.kr.alpha.api.programs.analysis.ComponentGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Algorithm for finding a stratification of a given {@link ComponentGraphImpl}.
 * 
 * Copyright (c) 2019-2020, the Alpha Team.
 */
public class StratificationAlgorithm {

	private static final Logger LOGGER = LoggerFactory.getLogger(StratificationAlgorithm.class);

	private ComponentGraph componentGraph;

	private final boolean[]                    visitedComponents;		// Marks visited components.
	private final boolean[]                    isUnstratifiableComponent;	// Marks those known to be not stratifiable (may be due to parent not being stratifiable).
	private final ComponentGraph.SCComponent[] componentEvaluationSequence;	// Store the topological order of all components.

	private int doneComponents;
	private int numComponents;

	private StratificationAlgorithm(ComponentGraph cg) {
		componentGraph = cg;
		doneComponents = 0;
		numComponents = cg.getComponents().size();
		visitedComponents = new boolean[numComponents];
		isUnstratifiableComponent = new boolean[numComponents];
		componentEvaluationSequence = new ComponentGraph.SCComponent[numComponents];
	}

	/**
	 * Calculates a stratification covering as much as possible (the maximal stratifiable part) of the given
	 * component graph. It assigns each strongly-connected component its own stratum and returns a sequence of
	 * all stratifiable components in topological order, i.e., if component A depends on component B then B occurs
	 * earlier in the returned sequence.
	 *
	 * @param cg the graph of strongly-connected-components (that must be acyclic).
	 * @return a list of all stratifiable components in the order that they can be evaluated.
	 */
	public static List<ComponentGraph.SCComponent> calculateStratification(ComponentGraph cg) {
		return new StratificationAlgorithm(cg).runStratification();
	}


	private List<ComponentGraph.SCComponent> runStratification() {
		LOGGER.debug("Initial call to stratify with entry points!");

		// Compute topological order and mark unstratifiable components.
		for (ComponentGraph.SCComponent component : componentGraph.getEntryPoints()) {
			topoSort(component, false);
		}

		// Extract list of stratifiable components.
		List<ComponentGraph.SCComponent> stratifiableComponentsSequence = new ArrayList<>();
		for (ComponentGraph.SCComponent component : componentEvaluationSequence) {
			if (!isUnstratifiableComponent[component.getId()]) {
				stratifiableComponentsSequence.add(component);
			}
		}

		return stratifiableComponentsSequence;
	}

	private void topoSort(ComponentGraph.SCComponent component, boolean hasUnstratifiableParent) {
		// We compute a topological ordering using a depth-first search where for any node its position in the
		// topological ordering is the reverse of its finishing time (i.e., last-finishing node comes first).

		int componentId = component.getId();

		// If this component is marked unstratifiable, it was processed already, immediately return.
		if (isUnstratifiableComponent[componentId]) {
			return;
		}

		boolean isUnstratifiable = hasUnstratifiableParent || component.hasNegativeCycle();
		isUnstratifiableComponent[componentId] = isUnstratifiable;

		// Note: the input graph is a component graph, hence it is a DAG and has no cycles, thus we do not have
		// to mark nodes as visited before descending to deeper nodes.

		for (Integer dependentComponentId : component.getDependentIds()) {
			// Descend if the lower node has not been visited yet or we need to propagate unstratifiable.
			if (isUnstratifiable || !visitedComponents[dependentComponentId]) {
				topoSort(componentGraph.getComponents().get(dependentComponentId), isUnstratifiable);
			}
		}

		// Add current component in front of all others (if we are visiting for the first time).
		if (!visitedComponents[componentId]) {
			visitedComponents[componentId] = true;
			doneComponents++;
			componentEvaluationSequence[numComponents - doneComponents] = component;
		}
	}
}
