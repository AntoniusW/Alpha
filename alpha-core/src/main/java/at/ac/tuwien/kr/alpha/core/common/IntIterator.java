package at.ac.tuwien.kr.alpha.core.common;

/**
 * An iterator returning raw int integers instead of Integer objects (for efficiency).
 *
 * Copyright (c) 2020, the Alpha Team.
 */
public interface IntIterator {

	/**
	 * @return true if the iterator has more elements.
	 */
	boolean hasNext();

	/**
	 * @return the next int in the iteration.
	 */
	int next();

}
