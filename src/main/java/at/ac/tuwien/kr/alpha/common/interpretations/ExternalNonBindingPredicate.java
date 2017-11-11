package at.ac.tuwien.kr.alpha.common.interpretations;

import at.ac.tuwien.kr.alpha.common.terms.Constant;
import at.ac.tuwien.kr.alpha.common.terms.Term;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class ExternalNonBindingPredicate extends FixedInterpretation {
	private final int arity;

	public ExternalNonBindingPredicate(int arity) {
		this.arity = arity;
	}

	public ExternalNonBindingPredicate() {
		this(1);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<List<Constant>> evaluate(List<Term> terms) {
		if (terms.size() != arity) {
			throw new IllegalArgumentException("Exactly " + arity + " term(s) required.");
		}

		final List<Constant> constants = new ArrayList<>(terms.size());
		for (int i = 0; i < terms.size(); i++) {
			if (!(terms.get(i) instanceof Constant)) {
				throw new IllegalArgumentException(
					"Expected only constants as input, but got " +
						"something else at position " + i + "."
				);
			}

			constants.add((Constant) terms.get(i));
		}

		try {
			return test(constants) ? TRUE : FALSE;
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Argument types do not match.", e);
		}
	}

	protected abstract boolean test(List<Constant> terms);
}
