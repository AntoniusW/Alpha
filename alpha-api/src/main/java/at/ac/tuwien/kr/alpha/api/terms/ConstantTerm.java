package at.ac.tuwien.kr.alpha.api.terms;

public interface ConstantTerm<T extends Comparable<T>> extends Term {
	
	T getObject();
	
}
