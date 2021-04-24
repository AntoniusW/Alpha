package at.ac.tuwien.kr.alpha.core.solver;

/**
 * Marks classes that implement some "checking" logic that can perform
 * exhaustive validation of the internal state of an object. This is
 * mainly used in debugging and should not be done for production
 * purposes.
 */
@FunctionalInterface
public interface Checkable {
	void setChecksEnabled(boolean checksEnabled);
}
