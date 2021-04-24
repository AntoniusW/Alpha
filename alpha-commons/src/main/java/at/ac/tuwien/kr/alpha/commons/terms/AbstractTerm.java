package at.ac.tuwien.kr.alpha.commons.terms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;

//@formatter:off
/**
 * Common representation of Terms. Terms are constructed such that each term is represented by a unique object, hence
 * term equality can be checked by object reference comparison. Each concrete subclass of a Term must implement a
 * factory-like method to obtain instances.
 *
 * We use {@link Comparable} to establish the following ordering among terms:
 * 	<ol>
 * 		<li>Constant terms according to their corresponding object and its type
 * 			<ol>
 * 				<li>{@link ConstantTerm<Integer>} ordered by value of the integers</li>
 * 				<li>{@link ConstantTerm<String>} and symbolic, lexicographically ordered on the symbol</li>
 * 				<li>{@link ConstantTerm<String>} lexicographically
 * 				<li>{@link ConstantTerm} for all other types, where {@link Comparable#compareTo(Object)} is used as ordering whenever
 * 				possible (i.e. two terms' objects have the same type). For two terms with objects of different type, 
 *	 			the result is the lexicographic ordering of the type names.</li>
 * 			</ol>
 * 		</li>
 * 		<li>Function terms (ordered by arity, functor name, and then on their argument terms).</li>
 * 		<li>Variable terms (lexicographically ordered on their variable names)</li>
 * </ol>
 *
 * Copyright (c) 2016-2020, the Alpha Team.
 */
//@formatter:on
abstract class AbstractTerm implements Term {

	@Override
	public abstract Set<VariableTerm> getOccurringVariables();

	/**
	 * Applies a substitution, result may be nonground.
	 * 
	 * @param substitution the variable substitution to apply.
	 * @return the non-substitute term where all variable substitutions have been applied.
	 */
	@Override
	public abstract Term substitute(Substitution substitution);

	// TODO change this to work on interfaces and break out of this class
	private static int priority(Term term) {
		if (term instanceof ConstantTerm) {
			return 1;
		} else if (term instanceof FunctionTerm) {
			return 2;
		} else if (term instanceof VariableTerm) {
			return 3;
		}
		throw new UnsupportedOperationException("Can only compare constant term, function terms and variable terms among each other.");
	}

	@Override
	public int compareTo(Term o) {
		return o == null ? 1 : Integer.compare(priority(this), priority(o));
	}

	@Override
	public abstract boolean isGround();

	/**
	 * Rename all variables occurring in this Term by prefixing their name.
	 * 
	 * @param renamePrefix the name to prefix all occurring variables.
	 * @return the term with all variables renamed.
	 */
	@Override
	public abstract Term renameVariables(String renamePrefix);

	@Override
	public abstract Term normalizeVariables(String renamePrefix, Term.RenameCounter counter);

	public static class RenameCounterImpl implements Term.RenameCounter {
		int counter;
		final HashMap<VariableTerm, VariableTerm> renamedVariables;

		public RenameCounterImpl(int startingValue) {
			counter = startingValue;
			renamedVariables = new HashMap<>();
		}
		
		@Override
		public Map<VariableTerm, VariableTerm> getRenamedVariables() {
			return renamedVariables;
		}
		
		@Override
		public int getAndIncrement() {
			int retVal = counter;
			counter++;
			return retVal;
		}
		
	}

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

}
