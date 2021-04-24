package at.ac.tuwien.kr.alpha.api.programs;

public interface Predicate extends Comparable<Predicate> {
	String getName();

	int getArity();
	
	@Override
	default int compareTo(Predicate other) {
		int result = getName().compareTo(other.getName());

		if (result != 0) {
			return result;
		}

		return Integer.compare(getArity(), other.getArity());
	}
	
	boolean isInternal();
	
	boolean isSolverInternal();
}
