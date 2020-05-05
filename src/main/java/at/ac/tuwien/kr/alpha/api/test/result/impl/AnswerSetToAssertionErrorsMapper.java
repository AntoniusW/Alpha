package at.ac.tuwien.kr.alpha.api.test.result.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.mapper.AnswerSetToObjectMapper;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.transformation.ConstraintsToAssertionErrors;
import at.ac.tuwien.kr.alpha.lang.test.AssertionError;

public class AnswerSetToAssertionErrorsMapper implements AnswerSetToObjectMapper<List<AssertionError>> {

	@Override
	public List<AssertionError> mapFromAnswerSet(AnswerSet answerSet) {
		Map<String, Set<List<Term>>> failedAssertions = new HashMap<>();
		answerSet.getPredicates().forEach((predicate) -> {
			if (predicate.getName().equals(ConstraintsToAssertionErrors.ASSERTION_ERROR)) {
				answerSet.getPredicateInstances(predicate).forEach((atom) -> {
					List<Term> terms = atom.getTerms();
					Term lastTerm = terms.get(terms.size() - 1);
					if (!(lastTerm instanceof ConstantTerm<?>)) {
						throw new IllegalArgumentException(
								"Expected last term of assertion error to be violated constraint body! (atom: " + atom.toString() + ")");
					}
					ConstantTerm<?> constraintTerm = (ConstantTerm<?>) lastTerm;
					if (!(constraintTerm.getObject() instanceof String)) {
						throw new IllegalArgumentException(
								"Expected last term of assertion error to be violated constraint body! (atom: " + atom.toString() + ")");
					}
					String violatedConstraint = (String) constraintTerm.getObject();
					failedAssertions.putIfAbsent(violatedConstraint, new HashSet<>());
					Set<List<Term>> violatingInstances = failedAssertions.get(violatedConstraint);
					List<Term> instance = new ArrayList<>();
					for (int i = 0; i < terms.size() - 1; i++) {
						instance.add(terms.get(i));
					}
					violatingInstances.add(instance);
				});
			}
		});
		List<AssertionError> retVal = new ArrayList<>();
		failedAssertions.forEach((key, value) -> {
			retVal.add(new AssertionError(key, value));
		});
		return retVal;
	}

}
