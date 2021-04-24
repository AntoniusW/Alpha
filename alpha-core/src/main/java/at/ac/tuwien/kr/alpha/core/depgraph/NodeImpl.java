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

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;

/**
 * A node in a dependency graph. One node references exactly one predicate. This means that all rule heads deriving the
 * same predicate will be condensed into the same graph node. In some cases this results in more "conservative" results
 * in stratification analysis, where some rules will not be evaluated up-front, although that would be possible.
 * 
 * Copyright (c) 2017-2020, the Alpha Team.
 */
public class NodeImpl implements DependencyGraph.Node {

	private final Predicate predicate;

	public NodeImpl(Predicate predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NodeImpl)) {
			return false;
		}
		return predicate.equals(((NodeImpl) o).predicate);
	}

	@Override
	public int hashCode() {
		return predicate.hashCode();
	}

	@Override
	public String toString() {
		return "Node{" + predicate.toString() + "}";
	}

	@Override
	public String getLabel() {
		return predicate.toString();
	}

	@Override
	public Predicate getPredicate() {
		return predicate;
	}
}
