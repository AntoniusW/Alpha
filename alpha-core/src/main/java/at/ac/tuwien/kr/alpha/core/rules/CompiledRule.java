package at.ac.tuwien.kr.alpha.core.rules;

import java.util.List;

import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.rules.NormalHead;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.core.grounder.RuleGroundingInfo;

public interface CompiledRule extends Rule<NormalHead> {
	
	int getRuleId();
	
	List<Predicate> getOccurringPredicates();
	
	RuleGroundingInfo getGroundingInfo();
	
	CompiledRule renameVariables(String str);

	boolean isGround();
}
