package at.ac.tuwien.kr.alpha.core.common;

/**
 * Represents truth value that can be converted to a Boolean truth value.
 * Copyright (c) 2016, the Alpha Team.
 */
@FunctionalInterface
public interface Truth {
	boolean toBoolean();
}
