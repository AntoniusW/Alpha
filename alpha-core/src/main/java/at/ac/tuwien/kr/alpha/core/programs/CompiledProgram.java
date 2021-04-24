package at.ac.tuwien.kr.alpha.core.programs;

import java.util.LinkedHashSet;
import java.util.Map;

import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.Program;
import at.ac.tuwien.kr.alpha.core.rules.CompiledRule;

public interface CompiledProgram extends Program<CompiledRule> {
	
	Map<Predicate, LinkedHashSet<CompiledRule>> getPredicateDefiningRules();
	
	Map<Predicate, LinkedHashSet<Instance>> getFactsByPredicate();
	
	Map<Integer, CompiledRule> getRulesById();
	

}
