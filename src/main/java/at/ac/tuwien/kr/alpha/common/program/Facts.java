/*
 * Copyright (c) 2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.common.program;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.grounder.FactIntervalEvaluator;
import at.ac.tuwien.kr.alpha.grounder.Instance;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Facts {

	private final Map<Predicate, LinkedHashSet<Instance>> factsByPredicate = new LinkedHashMap<>();

	void add(Atom fact) {
		List<Instance> tmpInstances = FactIntervalEvaluator.constructFactInstances(fact);
		Predicate tmpPredicate = fact.getPredicate();
		factsByPredicate.putIfAbsent(tmpPredicate, new LinkedHashSet<>());
		factsByPredicate.get(tmpPredicate).addAll(tmpInstances);
	}

	public Set<Map.Entry<Predicate, ? extends Set<Instance>>> entrySet() {
		return Collections.unmodifiableSet(factsByPredicate.entrySet());
	}

	public Iterable<? extends Predicate> getPredicates() {
		return Collections.unmodifiableSet(factsByPredicate.keySet());
	}

	public Set<Instance> get(Predicate predicate) {
		final Set<Instance> instances = factsByPredicate.get(predicate);
		return instances != null ? Collections.unmodifiableSet(instances) : Collections.emptySet();
	}

	public boolean isFact(Atom groundAtom) {
		final Set<Instance> factInstances = factsByPredicate.get(groundAtom.getPredicate());
		return factInstances != null && factInstances.contains(new Instance(groundAtom.getTerms()));
	}

	public boolean isEmpty() {
		return factsByPredicate.isEmpty();
	}
}
