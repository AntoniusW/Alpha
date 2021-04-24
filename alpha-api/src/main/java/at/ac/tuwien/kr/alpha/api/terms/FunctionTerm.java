package at.ac.tuwien.kr.alpha.api.terms;

import java.util.List;

public interface FunctionTerm extends Term{

	List<Term> getTerms();
	
	String getSymbol();

}
