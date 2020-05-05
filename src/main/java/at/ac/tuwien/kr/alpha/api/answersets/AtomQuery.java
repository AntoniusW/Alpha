/**
 * Copyright (c) 2016-2020, the Alpha Team.
 * All rights reserved.
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
package at.ac.tuwien.kr.alpha.api.answersets;

import java.util.HashMap;
import java.util.Map;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;

/**
 * A query for ASP atoms matching a set of filter predicates.
 * An {@link AtomQuery} enables "sql-like" querying of answer sets or streams of
 * atoms.
 * Example - Get instances of p/2 follwoing the pattern "p("bla", _)" from an
 * answer set:
 * 
 * <pre>
 * Predicate p = Predicate.getInstance("p", 2);
 * AnswerSet as = solve(...);
 * List<Atom> ps = as.query(AtomQuery.forPredicate(p).withConstantEquals(0, "bla"));
 * </pre>
 * 
 * Example - Get instances of p/2 follwoing the pattern "p("bla", _)" from a
 * stream of atoms:
 * 
 * <pre>
 * Predicate p = Predicate.getInstance("p", 2);
 * Stream<Atom> stream = ...;
 * Stream<Atom> filteredStream = stream.filter(AtomQuery.forPredicate(p).withConstantEquals(0, "bla"));
 * </pre>
 * 
 * Note that this class is not specifically optimized for querying performance
 * and might not deliver the best possible performance,
 * especially when used together with Java's streaming API.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public final class AtomQuery implements java.util.function.Predicate<Atom> {

	private final Predicate predicate;
	private Map<Integer, java.util.function.Predicate<Term>> filters = new HashMap<>();

	private AtomQuery(Predicate pred) {
		this.predicate = pred;
	}

	/**
	 * Creates a new {@link AtomQuery} for the given {@link Predicate}.
	 * The resulting query will only match atoms with the predicate passed here.
	 * 
	 * @param predicate the predicate this query should match
	 * @return an {@link AtomQuery} matching the given predicate
	 */
	public static AtomQuery forPredicate(Predicate predicate) {
		return new AtomQuery(predicate);
	}

	/**
	 * Adds an additional filter to this {@link AtomQuery}.
	 * The resulting query will apply the given filter predicate to the terms of
	 * it's matching predicate at the given index.
	 * 
	 * @param termIdx the term index on which to apply the new filter
	 * @param filter  the filter to apply on the given term index
	 * @return this atom query, extended by the given filter (builder pattern)
	 */
	public AtomQuery withFilter(int termIdx, java.util.function.Predicate<Term> filter) {
		if (termIdx >= this.predicate.getArity()) {
			throw new IndexOutOfBoundsException(
					"Predicate " + this.predicate.getName() + " has arity " + this.predicate.getArity() + ", term index " + termIdx + " is invalid!");
		}
		if (this.filters.containsKey(termIdx)) {
			java.util.function.Predicate<Term> currFilter = this.filters.get(termIdx);
			this.filters.put(termIdx, currFilter.and(filter));
		} else {
			this.filters.put(termIdx, filter);
		}
		return this;
	}

	/**
	 * Adds an additional filter checking for function terms to this
	 * {@link AtomQuery}.
	 * The resulting query will match atoms of the query predicate where the term at
	 * the specified index is a function term with the given symbol and arity.
	 * 
	 * @param termIdx    the term index on which to apply this filter
	 * @param funcSymbol the function symbol to query for
	 * @param funcArity  the arity the queried function symbol is expected to have
	 * @return this atom query, extended by the given filter (builder pattern)
	 */
	public AtomQuery withFunctionTerm(int termIdx, String funcSymbol, int funcArity) {
		java.util.function.Predicate<Term> isFunction = (term) -> {
			if (!(term instanceof FunctionTerm)) {
				return false;
			}
			FunctionTerm funcTerm = (FunctionTerm) term;
			if (!funcTerm.getSymbol().equals(funcSymbol)) {
				return false;
			}
			if (funcTerm.getTerms().size() != funcArity) {
				return false;
			}
			return true;
		};
		return this.withFilter(termIdx, isFunction);
	}

	/**
	 * Adds an additional filter checking for term equalities to this
	 * {@link AtomQuery}.
	 * The resulting query will only match instances of the query predicate where
	 * the term at the given index is equal to the specified term.
	 * 
	 * @param termIdx   the term index to check
	 * @param otherTerm the term to which instance terms should be equal (Note that
	 *                  only ground terms make sense)
	 * @return this atom query, extended by the given filter (builder pattern)
	 */
	public AtomQuery withTermEquals(int termIdx, Term otherTerm) {
		java.util.function.Predicate<Term> isEqual = (term) -> {
			return term.equals(otherTerm);
		};
		return this.withFilter(termIdx, isEqual);
	}

	/**
	 * Adds an additional filter checking for constant symbols to this
	 * {@link AtomQuery}.
	 * The resulting query will match atoms of the query predicate where the term at
	 * the specified index is a constant symbol (i.e. non-quoted string) equal to
	 * the given string.
	 * Example - The following query will match the atom <code>p(bla)</code>.
	 * 
	 * <pre>
	 * AtomQuery.forPredicate(Predicate.getInstance("p", 1)).withConstantSymbolEquals(0, "bla");
	 * </pre>
	 * 
	 * @param termIdx the index of the term on which to apply the filter
	 * @param smybol  the constant symbol to filter for
	 * @return this atom query, extended by the given filter (builder pattern)
	 */
	public AtomQuery withConstantSymbolEquals(int termIdx, String symbol) {
		return this.withTermEquals(termIdx, ConstantTerm.getSymbolicInstance(symbol));
	}

	/**
	 * Adds an additional filter checking for string constants to this
	 * {@link AtomQuery}.
	 * The resulting query will match atoms of the query predicate where the term at
	 * the specified index is a string constant (i.e. "quoted string") equal to the
	 * given string.
	 * Example - The following query will match the atom <code>p("bla")</code>.
	 * 
	 * <pre>
	 * AtomQuery.forPredicate(Predicate.getInstance("p", 1)).withStringEquals(0, "bla");
	 * </pre>
	 * 
	 * @param termIdx the index of the term on which to apply the filter
	 * @param str     the string to filter for
	 * @return this atom query, extended by the given filter (builder pattern)
	 */
	public AtomQuery withStringEquals(int termIdx, String str) {
		return this.withTermEquals(termIdx, ConstantTerm.getInstance(str));
	}

	/**
	 * Applies this query to an atom. Filters are worked off in
	 * order of ascending term index in a conjunctive fashion, i.e. for an atom
	 * to match the query, all of its terms must satisfy all filters on these
	 * terms
	 * 
	 * @param atom
	 *             the atom to which to apply the query
	 * @return true iff the atom satisfies the query
	 */
	@Override
	public boolean test(Atom atom) {
		java.util.function.Predicate<Term> currTermTester;
		boolean atomMatches;
		if (!atom.getPredicate().equals(this.predicate)) {
			return false;
		}
		atomMatches = true;
		for (int i = 0; i < atom.getTerms().size(); i++) {
			if (this.filters.containsKey(i)) {
				currTermTester = this.filters.get(i);
				atomMatches &= currTermTester.test(atom.getTerms().get(i));
				if (!atomMatches) { // shortcut
					return false;
				}
			}
		}
		return atomMatches;
	}

	public Predicate getPredicate() {
		return this.predicate;
	}

}
