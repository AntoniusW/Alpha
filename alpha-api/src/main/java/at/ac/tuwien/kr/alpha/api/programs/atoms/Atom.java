package at.ac.tuwien.kr.alpha.api.programs.atoms;

import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;

public interface Atom extends Comparable<Atom> {

	Predicate getPredicate();

	List<Term> getTerms();

	/**
	 * Returns whether this atom is ground, i.e., variable-free.
	 *
	 * @return true iff the terms of this atom contain no {@link VariableTerm}.
	 */
	boolean isGround();

	/**
	 * Creates a non-negated literal containing this atom.
	 */
	default Literal toLiteral() {
		return toLiteral(true);
	}

	/**
	 * Creates a literal containing this atom which will be negated if {@code positive} is {@code false}.
	 *
	 * @param positive the polarity of the resulting literal.
	 * @return a literal that is positive iff the given parameter is true.
	 */
	Literal toLiteral(boolean positive);

	Set<VariableTerm> getOccurringVariables();

	Atom substitute(Substitution substitution); // Introduce parameterized interface Substituable<A extends Atom> to get atom types right?

	Atom renameVariables(String newVariablePrefix);

	Atom withTerms(List<Term> terms);

	@Override
	default int compareTo(Atom o) {
		if (o == null) {
			return 1;
		}

		final List<Term> aTerms = this.getTerms();
		final List<Term> bTerms = o.getTerms();

		if (aTerms.size() != bTerms.size()) {
			return Integer.compare(aTerms.size(), bTerms.size());
		}

		int result = this.getPredicate().compareTo(o.getPredicate());

		if (result != 0) {
			return result;
		}

		for (int i = 0; i < aTerms.size(); i++) {
			result = aTerms.get(i).compareTo(o.getTerms().get(i));
			if (result != 0) {
				return result;
			}
		}

		return 0;
	}
}
