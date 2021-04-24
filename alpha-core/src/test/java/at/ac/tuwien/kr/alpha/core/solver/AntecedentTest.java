package at.ac.tuwien.kr.alpha.core.solver;

import java.util.HashSet;

public class AntecedentTest {

	/**
	 * Tests whether two Antecedent objects have the same reason literals (irrespective of their order).
	 * Note that both Antecedents are assumed to contain no duplicate literals.
	 * @param l left Antecedent.
	 * @param r right Antecedent
	 * @return true iff both Antecedents contain the same literals.
	 */
	public static boolean antecedentsEquals(Antecedent l, Antecedent r) {
		if (l == r) {
			return true;
		}
		if (l != null && r != null && l.getReasonLiterals().length == r.getReasonLiterals().length) {
			HashSet<Integer> lSet = new HashSet<>();
			for (int literal : l.getReasonLiterals()) {
				lSet.add(literal);
			}
			for (int literal : r.getReasonLiterals()) {
				if (!lSet.contains(literal)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}