package at.ac.tuwien.kr.alpha.api.solver.heuristics;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The available domain-independent heuristics.
 * Some are deprecated because they perform poorly and have not been improved for some time,
 * however the code is kept for now so that it stays compatible when interfaces are refactored.
 */
public enum Heuristic {
	NAIVE,
	BERKMIN,
	BERKMINLITERAL,
	@Deprecated
	DD,
	@Deprecated
	DD_SUM,
	@Deprecated
	DD_AVG,
	@Deprecated
	DD_MAX,
	@Deprecated
	DD_MIN,
	@Deprecated
	DD_PYRO,
	@Deprecated
	GDD,
	@Deprecated
	GDD_SUM,
	@Deprecated
	GDD_AVG,
	@Deprecated
	GDD_MAX,
	@Deprecated
	GDD_MIN,
	@Deprecated
	GDD_PYRO,
	@Deprecated
	ALPHA_ACTIVE_RULE,
	@Deprecated
	ALPHA_HEAD_MBT,
	VSIDS,
	GDD_VSIDS;

	/**
	 * @return a comma-separated list of names of known heuristics
	 */
	public static String listAllowedValues() {
		return Arrays.stream(values()).map(Heuristic::toString).collect(Collectors.joining(", "));
	}
}
