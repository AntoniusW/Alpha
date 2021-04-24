package at.ac.tuwien.kr.alpha.core.grounder;

import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;

// TODO should we really expose this?? pretty specific to how our grounder works
public interface RuleGroundingOrder {
	
	Literal getStartingLiteral();
	
	Literal getLiteralAtOrderPosition(int pos);
	
	RuleGroundingOrder pushBack(int pos);
	
	void considerUntilCurrentEnd();
	
	int getPositionFromWhichAllVarsAreBound();

}
