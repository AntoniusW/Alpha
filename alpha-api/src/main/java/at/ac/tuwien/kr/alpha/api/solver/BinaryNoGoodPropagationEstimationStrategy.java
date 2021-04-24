package at.ac.tuwien.kr.alpha.api.solver;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Strategies to estimate the amount of influence of a literal.
 */
public enum BinaryNoGoodPropagationEstimationStrategy {
	/**
	 * Counts binary watches involving the literal under consideration
	 */
	CountBinaryWatches,

	/**
	 * Assigns true to the literal under consideration, then does propagation only on binary nogoods
	 * and counts how many other atoms are assigned during this process, then backtracks
	 */
	BinaryNoGoodPropagation;

	/**
	 * @return a comma-separated list of names of known heuristics
	 */
	public static String listAllowedValues() {
		return Arrays.stream(values()).map(BinaryNoGoodPropagationEstimationStrategy::toString).collect(Collectors.joining(", "));
	}
}
