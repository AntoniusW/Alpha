package at.ac.tuwien.kr.alpha.api;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.terms.Term;

public interface ComparisonOperator {
	
	String getSymbol();
	
	Predicate toPredicate();
	
	ComparisonOperator negate();
	
	boolean compare(Term t1, Term t2);

}
