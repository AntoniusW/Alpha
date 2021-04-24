package at.ac.tuwien.kr.alpha.commons.comparisons;

import at.ac.tuwien.kr.alpha.api.ComparisonOperator;
import at.ac.tuwien.kr.alpha.api.terms.Term;

public class ComparisonOperators {
	
	public static final ComparisonOperator EQ = new AbstractComparisonOperator("=") {

		@Override
		public ComparisonOperator negate() {
			return ComparisonOperators.NE;
		}

		@Override
		public boolean compare(Term t1, Term t2) {
			return t1.compareTo(t2) == 0;
		}
		
	};
	
	public static final ComparisonOperator NE = new AbstractComparisonOperator("!=") {
		
		@Override
		public ComparisonOperator negate() {
			return ComparisonOperators.EQ;
		}
		
		@Override
		public boolean compare(Term t1, Term t2) {
			return t1.compareTo(t2) != 0;
		}
	};

	public static final ComparisonOperator LT = new AbstractComparisonOperator("<") {

		@Override
		public ComparisonOperator negate() {
			return ComparisonOperators.GE;
		}

		@Override
		public boolean compare(Term t1, Term t2) {
			return t1.compareTo(t2) < 0;
		}
		
	};
	
	public static final ComparisonOperator GT = new AbstractComparisonOperator(">") {

		@Override
		public ComparisonOperator negate() {
			return ComparisonOperators.LE;
		}

		@Override
		public boolean compare(Term t1, Term t2) {
			return t1.compareTo(t2) > 0;
		}
		
	};
	
	public static final ComparisonOperator LE = new AbstractComparisonOperator("<=") {

		@Override
		public ComparisonOperator negate() {
			return ComparisonOperators.GT;
		}

		@Override
		public boolean compare(Term t1, Term t2) {
			return t1.compareTo(t2) <= 0;
		}
		
	};
	
	public static final ComparisonOperator GE = new AbstractComparisonOperator(">=") {

		@Override
		public ComparisonOperator negate() {
			return ComparisonOperators.LE;
		}

		@Override
		public boolean compare(Term t1, Term t2) {
			return t1.compareTo(t2) >= 0;
		}
		
	};
	
}
