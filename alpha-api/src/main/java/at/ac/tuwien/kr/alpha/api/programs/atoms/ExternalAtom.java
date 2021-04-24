package at.ac.tuwien.kr.alpha.api.programs.atoms;

import java.util.List;

import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.programs.VariableNormalizableAtom;
import at.ac.tuwien.kr.alpha.api.terms.Term;

public interface ExternalAtom extends Atom, VariableNormalizableAtom {

	boolean hasOutput();

	List<Term> getInput();

	List<Term> getOutput();

	PredicateInterpretation getInterpretation();

}
