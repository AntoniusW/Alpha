package at.ac.tuwien.kr.alpha.core.grounder.bridges;

import at.ac.tuwien.kr.alpha.commons.util.IntIdGenerator;
import at.ac.tuwien.kr.alpha.core.common.Assignment;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.rules.InternalRule;

import java.util.Collection;

public interface Bridge {
	Collection<InternalRule> getRules(Assignment assignment, AtomStore atomStore, IntIdGenerator intIdGenerator);
}
