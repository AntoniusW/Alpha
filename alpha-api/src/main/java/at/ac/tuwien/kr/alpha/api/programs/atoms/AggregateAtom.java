package at.ac.tuwien.kr.alpha.api.programs.atoms;

import java.util.List;

import at.ac.tuwien.kr.alpha.api.ComparisonOperator;
import at.ac.tuwien.kr.alpha.api.programs.literals.AggregateLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;

public interface AggregateAtom extends Atom{

	enum AggregateFunction {
		COUNT,
		MAX,
		MIN,
		SUM
	}
	
	ComparisonOperator getLowerBoundOperator();

	Term getLowerBoundTerm();

	ComparisonOperator getUpperBoundOperator();

	Term getUpperBoundTerm();
	
	AggregateFunction getAggregateFunction();
	
	List<VariableTerm> getAggregateVariables();
	
	List<AggregateElement> getAggregateElements();
	
	@Override
	AggregateLiteral toLiteral(boolean positive);
	
	interface AggregateElement {
		
		List<Term> getElementTerms();
		
		List<Literal> getElementLiterals();
		
		List<VariableTerm> getOccurringVariables();
		
		boolean isGround();
		
	}
	
}
