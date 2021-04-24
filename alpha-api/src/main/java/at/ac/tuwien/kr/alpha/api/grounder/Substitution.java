package at.ac.tuwien.kr.alpha.api.grounder;

import java.util.TreeMap;

import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;

public interface Substitution {

	Term eval(VariableTerm variableTerm);

	TreeMap<VariableTerm, Term> getSubstitution();
	
	<T extends Comparable<T>> Term put(VariableTerm variableTerm, Term groundTerm);

	boolean isVariableSet(VariableTerm var);
	
}
