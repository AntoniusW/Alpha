package at.ac.tuwien.kr.alpha.test.util;

import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;

public class RuleParser {

	public static BasicRule parse(String str) {
		ProgramParser parser = new ProgramParser();
		InputProgram prog = parser.parse(str);
		if (!prog.getFacts().isEmpty()) {
			throw new IllegalArgumentException("Excepted exactly one rule and no facts!");
		}
		if (prog.getRules().size() != 1) {
			throw new IllegalArgumentException("Excepted exactly one rule");
		}
		return prog.getRules().get(0);
	}

}
