/**
 * Copyright (c) 2019 Siemens AG
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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
package at.ac.tuwien.kr.alpha.api.grounder.heuristics;

/* TODO this should be javadoc but has a problem with the ref to a component from core module:
* Contains configuration parameters for heuristics used by {@link at.ac.tuwien.kr.alpha.grounder.Grounder}s.
*/

/**
 *
 * Both parameters {@link #toleranceConstraints} and {@link #toleranceRules} are interpreted as follows:
 * A rule (or constraint) is grounded if the following conditions are satisfied:
 * <ul>
 * 	<li>All variables in the rule are bound (either by positive body literals that are already
 *      satisfied, or because the whole rule is ground).</li>
 * 	<li>No atom occurring positively in the body is assigned F.</li>
 * 	<li>No atom occurring negatively in the body is true as a fact.
 * 		(Note: The rule is still grounded if an atom occurring negatively in the body is assigned true without
 * 		being a fact, because the alternative would necessitate triggering re-grounding during backtracking.)</li>
 * 	<li>At most {@code N} atoms occurring positively in the body are still unassigned.</li>
 * </ul>
 *
 * The parameter {@link #toleranceConstraints} specifies {@code N} for constraints, while {@link #toleranceRules}
 * specifies {@code N} for all other rules. Infinity is represented by the value {@code -1}.
 * The default value for both parameters is {@code 0}, which means that only those rules and constraints are
 * grounded whose positive body is already satisfied.
 *
 * The additional parameter {@link #accumulatorEnabled} is a switch for the accumulator grounding strategy
 * which disables the removal of instances from the grounder memory in certain cases.
 *
 */
public class GrounderHeuristicsConfiguration {

	public static final String STRICT_STRING = "strict";
	public static final int STRICT_INT = 0;
	public static final String PERMISSIVE_STRING = "permissive";
	public static final int PERMISSIVE_INT = -1;

	private int toleranceConstraints;
	private int toleranceRules;
	private boolean accumulatorEnabled;

	public GrounderHeuristicsConfiguration() {
		super();
		this.toleranceConstraints = STRICT_INT;
		this.toleranceRules = STRICT_INT;
	}

	/**
	 * @param toleranceConstraints
	 * @param toleranceRules
	 */
	private GrounderHeuristicsConfiguration(int toleranceConstraints, int toleranceRules) {
		super();
		this.toleranceConstraints = toleranceConstraints;
		this.toleranceRules = toleranceRules;
	}

	/**
	 * @return the tolerance for constraints
	 */
	public int getToleranceConstraints() {
		return toleranceConstraints;
	}

	/**
	 * @return the tolerance for rules that are not constraints
	 */
	public int getToleranceRules() {
		return toleranceRules;
	}

	/**
	 * @param ruleIsConstraint {@code true} iff the parameter for constraints shall be returned
	 * @return {@link #getToleranceConstraints()} if {@code ruleIsConstraint}, otherwise {@link #getToleranceRules()}
	 */
	public int getTolerance(boolean ruleIsConstraint) {
		return ruleIsConstraint ? getToleranceConstraints() : getToleranceRules();
	}

	/**
	 * @param ruleIsConstraint {@code true} iff the parameter for constraints shall be returned
	 * @return {@code true} iff the tolerance is not 0
	 */
	public boolean isPermissive(boolean ruleIsConstraint) {
		return getTolerance(ruleIsConstraint) != STRICT_INT;
	}

	public boolean isAccumulatorEnabled() {
		return accumulatorEnabled;
	}

	public void setAccumulatorEnabled(boolean accumulatorEnabled) {
		this.accumulatorEnabled = accumulatorEnabled;
	}

	public static GrounderHeuristicsConfiguration strict() {
		return new GrounderHeuristicsConfiguration(STRICT_INT, STRICT_INT);
	}

	public static GrounderHeuristicsConfiguration permissive() {
		return new GrounderHeuristicsConfiguration(PERMISSIVE_INT, PERMISSIVE_INT);
	}

	public static GrounderHeuristicsConfiguration getInstance(int toleranceConstraints, int toleranceRules) {
		return new GrounderHeuristicsConfiguration(toleranceConstraints, toleranceRules);
	}

	public static GrounderHeuristicsConfiguration getInstance(String toleranceConstraints, String toleranceRules) {
		return getInstance(parseTolerance(toleranceConstraints), parseTolerance(toleranceRules));
	}

	private static int parseTolerance(String tolerance) {
		if (STRICT_STRING.equalsIgnoreCase(tolerance)) {
			return STRICT_INT;
		} else if (PERMISSIVE_STRING.equalsIgnoreCase(tolerance)) {
			return PERMISSIVE_INT;
		} else {
			return Integer.parseInt(tolerance);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(toleranceConstraints=" + toleranceConstraints + ",toleranceRules=" + toleranceRules + ",disableInstanceRemoval=" + accumulatorEnabled + ")";
	}

}
