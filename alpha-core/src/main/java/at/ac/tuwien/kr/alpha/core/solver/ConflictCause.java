package at.ac.tuwien.kr.alpha.core.solver;

/**
 * Indicates the presence of a conflict and contains its reason in terms of a violated Antecedent.
 * Throughout the solver the absence of a conflict is indicated by {@code ConflictCause = null}.
 */
public class ConflictCause {
	// Note: Storing the Antecedent is necessary in order to distinguish the cases of no conflict occurring
	// {@code ConflictCause==null} from the case where a choice (with no Antecedent, i.e.,
	// {@code violatedNoGood==null}) is the cause of the conflict.
	// Resolving ConflictCause by Antecedent would require an indicator flag to distinguish these cases.
	private final Antecedent violatedNoGood;

	public ConflictCause(Antecedent violatedNoGood) {
		this.violatedNoGood = violatedNoGood;
	}

	public Antecedent getAntecedent() {
		return violatedNoGood;
	}

	@Override
	public String toString() {
		return String.valueOf(violatedNoGood);
	}
}
