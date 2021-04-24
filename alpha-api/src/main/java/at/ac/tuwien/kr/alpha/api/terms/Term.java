package at.ac.tuwien.kr.alpha.api.terms;

import java.util.Map;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;

public interface Term extends Comparable<Term> {

	boolean isGround();

	Set<VariableTerm> getOccurringVariables();

	Term substitute(Substitution substitution);

	/**
	 * Rename all variables occurring in this Term by prefixing their name.
	 * 
	 * @param renamePrefix the name to prefix all occurring variables.
	 * @return the term with all variables renamed.
	 */
	Term renameVariables(String renamePrefix);

	Term normalizeVariables(String renamePrefix, RenameCounter counter);

	public static interface RenameCounter {

		Map<VariableTerm, VariableTerm> getRenamedVariables();
		
		int getAndIncrement();
		
	}

}
