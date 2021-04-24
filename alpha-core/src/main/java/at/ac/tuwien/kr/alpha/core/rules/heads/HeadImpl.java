package at.ac.tuwien.kr.alpha.core.rules.heads;

/**
 * Represents the head of a rule, i.e., either a choice or a disjunction of atoms, but not both. For normal rules the disjunction contains only one atom.
 *
 * Copyright (c) 2017-2019, the Alpha Team.
 */
public abstract class HeadImpl {
	
	@Override
	public abstract String toString();
	
	@Override
	public abstract boolean equals(Object o);
	
	@Override
	public abstract int hashCode();

}
