package at.ac.tuwien.kr.alpha.core.grounder;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;

/**
 * Copyright (c) 2016, the Alpha Team.
 */
public abstract class FilteringGrounder implements Grounder {
	protected final java.util.function.Predicate<Predicate> filter;

	protected FilteringGrounder(java.util.function.Predicate<Predicate> filter) {
		this.filter = filter;
	}

	protected FilteringGrounder() {
		this(p -> true);
	}
}
