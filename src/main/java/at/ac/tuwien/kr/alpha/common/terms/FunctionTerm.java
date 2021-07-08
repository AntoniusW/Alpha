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

import at.ac.tuwien.kr.alpha.common.Interner;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.*;

import static at.ac.tuwien.kr.alpha.Util.join;

/**
 * Copyright (c) 2016-2018, the Alpha Team.
 */
public class FunctionTerm extends Term {
	private static final Interner<FunctionTerm> INTERNER = new Interner<>();

	private final String symbol;
	private final List<Term> terms;
	private final boolean ground;

	private FunctionTerm(String symbol, List<Term> terms) {
		if (symbol == null) {
			throw new IllegalArgumentException();
		}

		this.symbol = symbol;
		this.terms = Collections.unmodifiableList(terms);

		boolean ground = true;
		for (Term term : terms) {
			if (!term.isGround()) {
				ground = false;
				break;
			}
		}
		this.ground = ground;
	}

	public static FunctionTerm getInstance(String functionSymbol, List<Term> termList) {
		return INTERNER.intern(new FunctionTerm(functionSymbol, termList));
	}

	public static FunctionTerm getInstance(String functionSymbol, Term... terms) {
		return getInstance(functionSymbol, Arrays.asList(terms));
	}

	public List<Term> getTerms() {
		return terms;
	}

	public String getSymbol() {
		return symbol;
	}

	@Override
	public boolean isGround() {
		return ground;
	}

	@Override
	public List<VariableTerm> getOccurringVariables() {
		LinkedList<VariableTerm> vars = new LinkedList<>();
		for (Term term : terms) {
			vars.addAll(term.getOccurringVariables());
		}
		return vars;
	}

	@Override
	public FunctionTerm substitute(Substitution substitution) {
		List<Term> groundTermList = new ArrayList<>(terms.size());
		for (Term term : terms) {
			groundTermList.add(term.substitute(substitution));
		}
		return FunctionTerm.getInstance(symbol, groundTermList);
	}

	/**
	 * Converts a function term back to an atom.
	 * @see Atom#toFunctionTerm()
	 */
	public BasicAtom toAtom() {
		return new BasicAtom(Predicate.getInstance(symbol, terms.size()), terms);
	}

	@Override
	public String toString() {
		if (terms.isEmpty()) {
			return symbol;
		}

		return join(symbol + "(", terms, ")");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		FunctionTerm that = (FunctionTerm) o;

		if (!symbol.equals(that.symbol)) {
			return false;
		}
		return terms.equals(that.terms);
	}

	@Override
	public int hashCode() {
		return 31 * symbol.hashCode() + terms.hashCode();
	}

	@Override
	public int compareTo(Term o) {
		if (this == o) {
			return 0;
		}

		if (!(o instanceof FunctionTerm)) {
			return super.compareTo(o);
		}

		FunctionTerm other = (FunctionTerm)o;

		if (terms.size() != other.terms.size()) {
			return terms.size() - other.terms.size();
		}

		int result = symbol.compareTo(other.symbol);

		if (result != 0) {
			return result;
		}

		for (int i = 0; i < terms.size(); i++) {
			result = terms.get(i).compareTo(other.terms.get(i));
			if (result != 0) {
				return result;
			}
		}

		return 0;
	}

	@Override
	public Term renameVariables(String renamePrefix) {
		ArrayList<Term> renamedTerms = new ArrayList<>(terms.size());
		for (Term term : terms) {
			renamedTerms.add(term.renameVariables(renamePrefix));
		}
		return FunctionTerm.getInstance(symbol, renamedTerms);
	}

	@Override
	public Term normalizeVariables(String renamePrefix, RenameCounter counter) {
		List<Term> normalizedTerms = new ArrayList<>(terms.size());
		for (Term term : terms) {
			normalizedTerms.add(term.normalizeVariables(renamePrefix, counter));
		}
		return FunctionTerm.getInstance(symbol, normalizedTerms);
	}
}
