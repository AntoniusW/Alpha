package at.ac.tuwien.kr.alpha.core.programs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.antlr.v4.runtime.CharStreams;

import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;

public class Programs {

	private Programs() {
		throw new AssertionError("This is a pure utility class and should therefore not be instantiated!");
	}

	public static ASPCore2Program fromInputStream(InputStream is, Map<String, PredicateInterpretation> externals) throws IOException {
		ProgramParserImpl parser = new ProgramParserImpl();
		return parser.parse(CharStreams.fromStream(is), externals);
	}

}
