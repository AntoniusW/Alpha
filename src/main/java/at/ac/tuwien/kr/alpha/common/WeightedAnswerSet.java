package at.ac.tuwien.kr.alpha.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * Represents a weighted answer set, i.e., it extends a {@link BasicAnswerSet} with weights information.
 *
 * Weights are given as a mapping from levels to the respective weight. Note that negative levels and weights are allowed.
 * The highest level is the most important one and levels not contained in the map have a weight of zero.
 *
 * Copyright (c) 2020-2021, the Alpha Team.
 */
public class WeightedAnswerSet extends BasicAnswerSet {

	private final TreeMap<Integer, Integer> weightPerLevel;

	public WeightedAnswerSet(BasicAnswerSet basicAnswerSet, TreeMap<Integer, Integer> weightPerLevel) {
		super(basicAnswerSet.getPredicates(), basicAnswerSet.getPredicateInstances());
		this.weightPerLevel = weightPerLevel;
	}

	/**
	 * Compares the weights of this {@link WeightedAnswerSet} with the weights of the other one.
	 * @param other the {@link WeightedAnswerSet} to compare with.
	 * @return -1, 0, or 1 depending on whether the weights of this are better, equal, or worse than the other.
	 */
	public int compareWeights(WeightedAnswerSet other) {
		Iterator<Integer> thisDescIterator = this.weightPerLevel.descendingKeySet().iterator();
		Iterator<Integer> otherDescIterator = other.weightPerLevel.descendingKeySet().iterator();
		while (true) {
			// Descend this level down to one with non-zero weight.
			Integer thisLevel = null;
			Integer thisWeight = null;
			while (thisDescIterator.hasNext()) {
				thisLevel = thisDescIterator.next();
				thisWeight = this.weightPerLevel.get(thisLevel);
				if (thisWeight != null && thisWeight != 0) {
					break;
				}
			}
			// Descend other level down to one with non-zero weight.
			Integer otherLevel = null;
			Integer otherWeight = null;
			while (otherDescIterator.hasNext()) {
				otherLevel = otherDescIterator.next();
				otherWeight = other.weightPerLevel.get(otherLevel);
				if (otherWeight != null && otherWeight != 0) {
					break;
				}
			}
			if (thisWeight == null && otherWeight == null) {
				return 0;
			}
			if (thisWeight == null) {
				return otherWeight < 0 ? 1 : -1;
			}
			if (otherWeight == null) {
				return thisWeight < 0 ? -1 : 1;
			}
			if (thisLevel.equals(otherLevel)) {
				if (thisWeight.equals(otherWeight)) {
					continue;
				} else {
					return thisWeight < otherWeight ? -1 : 1;
				}

			} else {
				return thisLevel < otherLevel ? -1 : 1;
			}
		}
	}

	@Override
	public int compareTo(AnswerSet other) {
		if (!(other instanceof WeightedAnswerSet)) {
			return -1;
		}
		int basicComparison = super.compareTo(other);
		if (basicComparison != 0) {
			return basicComparison;
		}
		WeightedAnswerSet otherWeighted = (WeightedAnswerSet) other;
		return compareWeights(otherWeighted);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), weightPerLevel);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof WeightedAnswerSet)) {
			return false;
		}

		WeightedAnswerSet that = (WeightedAnswerSet) o;

		if (!getPredicates().equals(that.getPredicates())) {
			return false;
		}

		if (!getPredicateInstances().equals(that.getPredicateInstances())) {
			return false;
		}

		return weightPerLevel.equals(that.weightPerLevel);
	}

	public String getWeightsAsString() {
		StringJoiner joiner = new StringJoiner(", ", "[", "]");
		for (Map.Entry<Integer, Integer> weightPerLevel : weightPerLevel.entrySet()) {
			joiner.add(weightPerLevel.getValue() + "@" + weightPerLevel.getKey());
		}
		return joiner.toString();
	}

	@Override
	public String toString() {
		return super.toString() + getWeightsAsString();
	}
}
