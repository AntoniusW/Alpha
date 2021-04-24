package at.ac.tuwien.kr.alpha.api.programs;

import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;

/**
 * Interface for atom whose variables can be normalized, i.e., enumerated from
 * left to right.
 * Copyright (c) 2018 the Alpha Team.
 */
public interface VariableNormalizableAtom {

	/**
	 * Returns an Atom whose variables are enumerated as Vi, .. Vn.
	 * 
	 * @param prefix               the variable prefix V in front of the counter.
	 * @param counterStartingValue the initial value i of the counter.
	 * @return the Atom where all variables are renamed and enumerated
	 *         left-to-right.
	 */
	Atom normalizeVariables(String prefix, int counterStartingValue);
	
}
