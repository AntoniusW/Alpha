package at.ac.tuwien.kr.alpha.api.programs.atoms;

import at.ac.tuwien.kr.alpha.api.ComparisonOperator;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.VariableNormalizableAtom;

public interface ComparisonAtom extends Atom, VariableNormalizableAtom{
	
	ComparisonOperator getOperator();
	
	@Override
	ComparisonAtom substitute(Substitution subst);
	
}
