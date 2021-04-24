package at.ac.tuwien.kr.alpha.core.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import at.ac.tuwien.kr.alpha.antlr.ASPCore2Lexer;
import at.ac.tuwien.kr.alpha.antlr.ASPCore2Parser;
import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.core.parser.ParseTreeVisitor;

public class AnswerSetsParser {
	private static final ParseTreeVisitor VISITOR = new ParseTreeVisitor(Collections.emptyMap(), false);

	public static Set<AnswerSet> parse(String s) {
		try {
			return parse(CharStreams.fromString(s));
		} catch (IOException e) {
			// In this case we assume that something went fundamentally
			// wrong when using a String as input. The caller probably
			// assumes that I/O on a String should always be fine.
			throw new RuntimeException("Encountered I/O-related exception while parsing a String.", e);
		} catch (RecognitionException | ParseCancellationException e) {
			// If there were issues parsing the given string, we
			// throw something that suggests that the input string
			// is malformed.
			throw new IllegalArgumentException("Could not parse answer sets.", e);
		}
	}

	public static Set<AnswerSet> parse(CharStream stream) throws IOException {
		final ASPCore2Parser parser = new ASPCore2Parser(new CommonTokenStream(new ASPCore2Lexer(stream)));

		// Try SLL parsing mode (faster but may terminate incorrectly).
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		parser.removeErrorListeners();
		parser.setErrorHandler(new BailErrorStrategy());

		return VISITOR.translate(parser.answer_sets());
	}
}
