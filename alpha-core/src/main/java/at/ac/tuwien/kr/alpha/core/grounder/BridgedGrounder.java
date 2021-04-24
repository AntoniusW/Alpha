package at.ac.tuwien.kr.alpha.core.grounder;

import java.util.HashSet;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.commons.util.IntIdGenerator;
import at.ac.tuwien.kr.alpha.core.common.Assignment;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.grounder.bridges.Bridge;
import at.ac.tuwien.kr.alpha.core.rules.InternalRule;

public abstract class BridgedGrounder extends AbstractGrounder {
	protected final Bridge[] bridges;

	protected BridgedGrounder(java.util.function.Predicate<Predicate> filter, Bridge... bridges) {
		super(filter);
		this.bridges = bridges;
	}

	protected BridgedGrounder(Bridge... bridges) {
		super();
		this.bridges = bridges;
	}

	protected Set<InternalRule> collectExternalRules(Assignment assignment, AtomStore atomStore, IntIdGenerator intIdGenerator) {
		Set<InternalRule> collectedRules = new HashSet<>();

		for (Bridge bridge : bridges) {
			collectedRules.addAll(bridge.getRules(assignment, atomStore, intIdGenerator));
		}

		return collectedRules;
	}
}
