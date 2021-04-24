package at.ac.tuwien.kr.alpha.core.common.fixedinterpretations;

import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;

public interface PredicateInterpretationImpl extends PredicateInterpretation {
	@Override
	Set<List<ConstantTerm<?>>> evaluate(List<Term> terms);
}
