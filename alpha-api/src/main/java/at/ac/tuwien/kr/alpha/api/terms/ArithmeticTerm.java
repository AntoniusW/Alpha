package at.ac.tuwien.kr.alpha.api.terms;

public interface ArithmeticTerm extends Term {
	
	ArithmeticOperator getOperator();
	
	Term getLeftOperand();
	
	Term getRightOperand();
	
	Integer evaluateExpression();

}
