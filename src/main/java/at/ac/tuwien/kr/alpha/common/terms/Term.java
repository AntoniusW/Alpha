/**
 * Copyright (c) 2016-2018, the Alpha Team.
 * All rights reserved.
 *
 * Additional changes made by Siemens.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.common.terms;

import at.ac.tuwien.kr.alpha.common.Substitutable;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
public abstract class Term implements Comparable<Term>, Substitutable<Term> {
	public abstract boolean isGround();

	public abstract List<VariableTerm> getOccurringVariables();

	/**
	 * Applies a substitution, result may be nonground.
	 *
	 * @param substitution the variable substitution to apply.
	 * @return the non-substitute term where all variable substitutions have been applied.
	 */
	@Override
	public abstract Term substitute(Substitution substitution);

	private static int priority(Term term) {
		final Class<?> clazz = term.getClass();
		if (clazz.equals(ConstantTerm.class)) {
			return 1;
		} else if (clazz.equals(FunctionTerm.class)) {
			return 2;
		} else if (clazz.equals(VariableTerm.class)) {
			return 3;
		}
		throw new UnsupportedOperationException("Can only compare constant term, function terms and variable terms among each other.");
	}

	@Override
	public int compareTo(Term o) {
		return o == null ? 1 : Integer.compare(priority(this), priority(o));
	}

	/**
	 * Rename all variables occurring in this Term by prefixing their name.
	 *
	 * @param renamePrefix the name to prefix all occurring variables.
	 * @return the term with all variables renamed.
	 */
	public abstract Term renameVariables(String renamePrefix);

	public abstract Term normalizeVariables(String renamePrefix, RenameCounter counter);

	public static class RenameCounter {
		int counter;
		final HashMap<VariableTerm, VariableTerm> renamedVariables;

		public RenameCounter(int startingValue) {
			counter = startingValue;
			renamedVariables = new HashMap<>();
		}
	}

	public static List<Term> renameTerms(List<Term> terms, String prefix, int counterStartingValue) {
		List<Term> renamedTerms = new ArrayList<>(terms.size());
		Term.RenameCounter renameCounter = new Term.RenameCounter(counterStartingValue);
		for (Term term : terms) {
			renamedTerms.add(term.normalizeVariables(prefix, renameCounter));
		}
		return renamedTerms;
	}

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

}
