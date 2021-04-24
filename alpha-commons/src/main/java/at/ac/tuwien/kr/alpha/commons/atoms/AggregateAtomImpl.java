/**
 * Copyright (c) 2017-2019, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.commons.atoms;

import static at.ac.tuwien.kr.alpha.api.Util.join;
import static at.ac.tuwien.kr.alpha.api.Util.oops;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.ComparisonOperator;
import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.literals.AggregateLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.comparisons.ComparisonOperators;
import at.ac.tuwien.kr.alpha.commons.literals.Literals;

class AggregateAtomImpl extends AbstractAtom implements AggregateAtom {

	private final ComparisonOperator lowerBoundOperator;
	private final Term lowerBoundTerm;
	private final ComparisonOperator upperBoundOperator;
	private final Term upperBoundTerm;
	private final AggregateFunction aggregatefunction;
	private final List<AggregateElement> aggregateElements;

	AggregateAtomImpl(ComparisonOperator lowerBoundOperator, Term lowerBoundTerm, ComparisonOperator upperBoundOperator, Term upperBoundTerm,
			AggregateFunction aggregatefunction, List<AggregateElement> aggregateElements) {
		this.lowerBoundOperator = lowerBoundOperator;
		this.lowerBoundTerm = lowerBoundTerm;
		this.upperBoundOperator = upperBoundOperator;
		this.upperBoundTerm = upperBoundTerm;
		this.aggregatefunction = aggregatefunction;
		this.aggregateElements = aggregateElements;
		if (upperBoundOperator != null || lowerBoundOperator != ComparisonOperators.LE || lowerBoundTerm == null) {
			throw new UnsupportedOperationException("Aggregate construct not yet supported: " + this);
		}
	}

	@Override
	public boolean isGround() {
		for (AggregateElement aggregateElement : aggregateElements) {
			if (!aggregateElement.isGround()) {
				return false;
			}
		}
		if (lowerBoundTerm != null && !lowerBoundTerm.isGround()
				|| upperBoundTerm != null && !upperBoundTerm.isGround()) {
			return false;
		}
		return true;
	}

	@Override
	public AggregateLiteral toLiteral(boolean positive) {
		return Literals.fromAtom(this, positive);
	}

	@Override
	public List<Term> getTerms() {
		throw oops("Aggregate atom cannot report terms.");
	}

	@Override
	public Atom withTerms(List<Term> terms) {
		throw new UnsupportedOperationException("Editing term list is not supported for aggregate atoms!");
	}

	@Override
	public Predicate getPredicate() {
		throw oops("Aggregate atom cannot report predicate.");
	}

	/**
	 * Returns all variables occurring inside the aggregate, between { ... }.
	 * 
	 * @return each variable occurring in some aggregate element.
	 */
	@Override
	public List<VariableTerm> getAggregateVariables() {
		List<VariableTerm> occurringVariables = new LinkedList<>();
		for (AggregateElement aggregateElement : aggregateElements) {
			occurringVariables.addAll(aggregateElement.getOccurringVariables());
		}
		return occurringVariables;
	}

	@Override
	public AggregateAtomImpl substitute(Substitution substitution) {
		throw new UnsupportedOperationException("Cannot substitute AggregateAtom!");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		AggregateAtomImpl that = (AggregateAtomImpl) o;

		if (lowerBoundOperator != that.lowerBoundOperator) {
			return false;
		}
		if (lowerBoundTerm != null ? !lowerBoundTerm.equals(that.lowerBoundTerm) : that.lowerBoundTerm != null) {
			return false;
		}
		if (upperBoundOperator != that.upperBoundOperator) {
			return false;
		}
		if (upperBoundTerm != null ? !upperBoundTerm.equals(that.upperBoundTerm) : that.upperBoundTerm != null) {
			return false;
		}
		if (aggregateElements != null ? !aggregateElements.equals(that.aggregateElements) : that.aggregateElements != null) {
			return false;
		}
		return aggregatefunction == that.aggregatefunction;
	}

	@Override
	public String toString() {
		String lowerBound = lowerBoundTerm == null ? "" : (lowerBoundTerm.toString() + lowerBoundOperator);
		String upperBound = upperBoundTerm == null ? "" : (upperBoundOperator.toString() + upperBoundTerm);
		return lowerBound + "#" + aggregatefunction + "{ " + join("", aggregateElements, "; ", "") + " }" + upperBound;
	}

	@Override
	public int hashCode() {
		int result = lowerBoundOperator != null ? lowerBoundOperator.hashCode() : 0;
		result = 31 * result + (lowerBoundTerm != null ? lowerBoundTerm.hashCode() : 0);
		result = 31 * result + (upperBoundOperator != null ? upperBoundOperator.hashCode() : 0);
		result = 31 * result + (upperBoundTerm != null ? upperBoundTerm.hashCode() : 0);
		result = 31 * result + (aggregateElements != null ? aggregateElements.hashCode() : 0);
		result = 31 * result + (aggregatefunction != null ? aggregatefunction.hashCode() : 0);
		return result;
	}

	@Override
	public ComparisonOperator getLowerBoundOperator() {
		return lowerBoundOperator;
	}

	@Override
	public Term getLowerBoundTerm() {
		return lowerBoundTerm;
	}

	@Override
	public ComparisonOperator getUpperBoundOperator() {
		return upperBoundOperator;
	}

	@Override
	public Term getUpperBoundTerm() {
		return upperBoundTerm;
	}

	@Override
	public AggregateFunction getAggregateFunction() {
		return this.aggregatefunction;
	}

	@Override
	public List<AggregateElement> getAggregateElements() {
		return Collections.unmodifiableList(aggregateElements);
	}

	static class AggregateElementImpl implements AggregateElement {
		final List<Term> elementTerms;
		final List<Literal> elementLiterals;

		public AggregateElementImpl(List<Term> elementTerms, List<Literal> elementLiterals) {
			this.elementTerms = elementTerms;
			this.elementLiterals = elementLiterals;
		}

		@Override
		public List<Term> getElementTerms() {
			return elementTerms;
		}

		@Override
		public List<Literal> getElementLiterals() {
			return elementLiterals;
		}

		@Override
		public boolean isGround() {
			for (Term elementTerm : elementTerms) {
				if (!elementTerm.isGround()) {
					return false;
				}
			}
			for (Literal elementLiteral : elementLiterals) {
				if (!elementLiteral.isGround()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public List<VariableTerm> getOccurringVariables() {
			List<VariableTerm> occurringVariables = new LinkedList<>();
			for (Term term : elementTerms) {
				if (term instanceof VariableTerm) {
					occurringVariables.add((VariableTerm) term);
				}
			}
			for (Literal literal : elementLiterals) {
				occurringVariables.addAll(literal.getBindingVariables());
				occurringVariables.addAll(literal.getNonBindingVariables());
			}
			return occurringVariables;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			AggregateElementImpl that = (AggregateElementImpl) o;

			if (elementTerms != null ? !elementTerms.equals(that.elementTerms) : that.elementTerms != null) {
				return false;
			}
			return elementLiterals != null ? elementLiterals.equals(that.elementLiterals) : that.elementLiterals == null;
		}

		@Override
		public int hashCode() {
			int result = elementTerms != null ? elementTerms.hashCode() : 0;
			result = 31 * result + (elementLiterals != null ? elementLiterals.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return join("", elementTerms, " : ") + join("", elementLiterals, "");
		}
	}

}
