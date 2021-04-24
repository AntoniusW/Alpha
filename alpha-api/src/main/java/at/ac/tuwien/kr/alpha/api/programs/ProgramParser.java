package at.ac.tuwien.kr.alpha.api.programs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;

public interface ProgramParser {

	default ASPCore2Program parse(String programString) {
		return parse(programString, Collections.emptyMap());
	}

	default ASPCore2Program parse(InputStream programSource) throws IOException {
		return parse(programSource, Collections.emptyMap());
	}

	default ASPCore2Program parse(Path programPath) throws IOException {
		return parse(programPath, Collections.emptyMap());
	}

	default ASPCore2Program parse(Path... programSources) throws IOException {
		return parse(Collections.emptyMap(), programSources);
	}

	default ASPCore2Program parse(Iterable<Path> programSources) throws IOException {
		return parse(programSources, Collections.emptyMap());
	}

	ASPCore2Program parse(String programString, Map<String, PredicateInterpretation> externalPredicateDefinitions);

	ASPCore2Program parse(InputStream programSource, Map<String, PredicateInterpretation> externalPredicateDefinitions) throws IOException;

	ASPCore2Program parse(Path programPath, Map<String, PredicateInterpretation> externalPredicateDefinitions) throws IOException;

	ASPCore2Program parse(Map<String, PredicateInterpretation> externalPredicateDefinitions, Path... programSources) throws IOException;

	ASPCore2Program parse(Iterable<Path> programSources, Map<String, PredicateInterpretation> externalPredicateDefinitions) throws IOException;

}
