/**
 * Copyright (c) 2016-2018, the Alpha Team.
 * All rights reserved.
 *
 * Additional changes made by Siemens.
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
package at.ac.tuwien.kr.alpha.core.grounder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.Term;

/**
 * A storage for instances with a certain arity, where each position of the instance can be indexed.
 * This aids in matching and joining instances. An index can be added or removed at any time for a desired position of
 * all instances.
 * Copyright (c) 2016-2020, the Alpha Team.
 */
public class IndexedInstanceStorage {
	private final Predicate predicate;
	private final boolean positive;

	/**
	 * A collection of all instances currently stored in this storage.
	 */
	private final LinkedHashSet<Instance> instances = new LinkedHashSet<>();

	/**
	 * For each position, a mapping of termIds to list of instances with this termId at the corresponding position
	 */
	private final ArrayList<HashMap<Term, ArrayList<Instance>>> indices = new ArrayList<>();

	private final ArrayList<Instance> recentlyAddedInstances = new ArrayList<>();

	public IndexedInstanceStorage(Predicate predicate, boolean positive) {
		this.predicate = predicate;
		this.positive = positive;

		// Create list of mappings, initialize to null.
		while (indices.size() < predicate.getArity()) {
			indices.add(null);
		}
	}

	public Predicate getPredicate() {
		return predicate;
	}

	public void markRecentlyAddedInstancesDone() {
		recentlyAddedInstances.clear();
	}

	public void addIndexPosition(int position) {
		if (position < 0 || position > predicate.getArity() - 1) {
			throw new RuntimeException("Requested to create indices for attribute out of range." +
					"IndexedInstanceStorage: " + this + "  requested indices position: " + position);
		}
		// Add index
		indices.set(position, new LinkedHashMap<>());

		// Initialize index with all instances currently used.
		for (Instance instance : instances) {
			indices.get(position).putIfAbsent(instance.terms.get(position), new ArrayList<>());
			ArrayList<Instance> instancesAtPosition = indices.get(position).get(instance.terms.get(position));
			instancesAtPosition.add(instance);
		}
	}

	public void removeIndexPosition(int position) {
		if (position < 0 || position > predicate.getArity() - 1) {
			throw new RuntimeException("Requested to create indices for attribute out of range." +
					"IndexedInstanceStorage: " + this + "  requested indices position: " + position);
		}
		indices.set(position, null);
	}

	/**
	 * Returns whether an instance is already contained in the storage.
	 * 
	 * @param instance the instance to check for containment.
	 * @return true if the instance is already contained in the storage.
	 */
	public boolean containsInstance(Instance instance) {
		return instances.contains(instance);
	}

	public void addInstance(Instance instance) {
		if (instance.terms.size() != predicate.getArity()) {
			throw new RuntimeException("Instance length does not match arity of IndexedInstanceStorage: " +
					"instance size: " + instance.terms.size()
					+ "IndexedInstanceStorage: " + this);
		}
		instances.add(instance);
		recentlyAddedInstances.add(instance);
		// Add instance to all indices.
		for (int i = 0; i < indices.size(); i++) {
			HashMap<Term, ArrayList<Instance>> posIndex = indices.get(i);
			if (posIndex == null) {
				continue;
			}
			posIndex.putIfAbsent(instance.terms.get(i), new ArrayList<>());
			ArrayList<Instance> matchingInstancesAtPos = posIndex.get(instance.terms.get(i));
			matchingInstancesAtPos.add(instance); // Add instance
		}
	}

	public void removeInstance(Instance instance) {
		if (recentlyAddedInstances.size() != 0) {
			// Hint: exception may be replaced by removing the instance also from the list of recentlyAddedInstances.
			throw new RuntimeException("Instance is removed while there are unprocessed new instances; Result dubious.");
		}
		// Remove from all indices
		for (int i = 0; i < indices.size(); i++) {
			HashMap<Term, ArrayList<Instance>> posIndex = indices.get(i);
			if (posIndex == null) {
				continue;
			}
			ArrayList<Instance> matchingInstancesAtPos = posIndex.get(instance.terms.get(i));
			matchingInstancesAtPos.remove(instance); // Remove instance

			// If there are no more instances having the term at the current position,
			// remove the entry from the hash.
			if (matchingInstancesAtPos.size() == 0) {
				posIndex.remove(instance.terms.get(i));
			}
		}
		instances.remove(instance);
	}

	public List<Instance> getRecentlyAddedInstances() {
		return recentlyAddedInstances;
	}

	/**
	 * Returns a list of all instances having the given term at the given position. Returns null if no such
	 * instances exist.
	 * 
	 * @param term
	 * @param position
	 * @return
	 */
	public List<Instance> getInstancesMatchingAtPosition(Term term, int position) {
		Map<Term, ArrayList<Instance>> indexForPosition = indices.get(position);
		if (indexForPosition == null) {
			throw new RuntimeException("IndexedInstanceStorage queried for position " + position + " which is not indexed.");
		}
		ArrayList<Instance> matchingInstances = indexForPosition.get(term);
		return matchingInstances == null ? Collections.emptyList() : matchingInstances;
	}

	private int getMostSelectiveGroundTermPosition(Atom atom) {
		int smallestNumberOfInstances = Integer.MAX_VALUE;
		int mostSelectiveTermPosition = -1;
		for (int i = 0; i < atom.getTerms().size(); i++) {
			Term testTerm = atom.getTerms().get(i);
			if (testTerm.isGround()) {
				ArrayList<Instance> instancesMatchingTest = indices.get(i).get(testTerm);
				if (instancesMatchingTest == null) {
					// Ground term at i matches zero instances, it is most selective.
					return i;
				}
				int numInstancesTestTerm = instancesMatchingTest.size();
				if (numInstancesTestTerm < smallestNumberOfInstances) {
					smallestNumberOfInstances = numInstancesTestTerm;
					mostSelectiveTermPosition = i;
				}
			}
		}
		return mostSelectiveTermPosition;
	}

	public List<Instance> getInstancesFromPartiallyGroundAtom(Atom substitute) {
		// For selection of the instances, find ground term on which to select.
		int firstGroundTermPosition = getMostSelectiveGroundTermPosition(substitute);
		// Select matching instances, select all if no ground term was found.
		if (firstGroundTermPosition != -1) {
			Term firstGroundTerm = substitute.getTerms().get(firstGroundTermPosition);
			return getInstancesMatchingAtPosition(firstGroundTerm, firstGroundTermPosition);
		} else {
			return new ArrayList<>(getAllInstances());
		}
	}

	public Set<Instance> getAllInstances() {
		return instances;
	}

	@Override
	public String toString() {
		return (positive ? "+" : "-") + predicate;
	}
}