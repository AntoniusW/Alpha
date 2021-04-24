package at.ac.tuwien.kr.alpha.core.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import at.ac.tuwien.kr.alpha.antlr.ASPCore2Lexer;
import at.ac.tuwien.kr.alpha.antlr.ASPCore2Parser;
import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.core.externals.Externals;
import at.ac.tuwien.kr.alpha.core.programs.InputProgram;

public class ProgramParserImpl implements ProgramParser {

	private final Map<String, PredicateInterpretation> preloadedExternals = new HashMap<>();

	public ProgramParserImpl() {
		this.preloadedExternals.putAll(Externals.getStandardLibraryExternals());
	}

	public ProgramParserImpl(Map<String, PredicateInterpretation> externals) {
		this();
		this.preloadedExternals.putAll(externals);
	}
	
	@Override
	public ASPCore2Program parse(String s) {
		return parse(s, Collections.emptyMap());
	}

	@Override
	public ASPCore2Program parse(String s, Map<String, PredicateInterpretation> externals) {
		try {
			return parse(CharStreams.fromString(s), externals);
		} catch (IOException e) {
			// In this case we assume that something went fundamentally
			// wrong when using a String as input. The caller probably
			// assumes that I/O on a String should always be fine.
			throw new RuntimeException("Encountered I/O-related exception while parsing a String.", e);
		} catch (RecognitionException | ParseCancellationException e) {
			// If there were issues parsing the given string, we
			// throw something that suggests that the input string
			// is malformed.
			throw new IllegalArgumentException("Could not parse input program.", e);
		}
	}

	public ASPCore2Program parse(CharStream stream) throws IOException {
		return parse(stream, Collections.emptyMap());
	}

	public ASPCore2Program parse(CharStream stream, Map<String, PredicateInterpretation> externals) throws IOException {
		//@formatter:off
		/*
		 * // In order to require less memory: use unbuffered streams and avoid constructing a full parse tree. 
		 * ASPCore2Lexer lexer = new ASPCore2Lexer(new UnbufferedCharStream(is)); 
		 * lexer.setTokenFactory(new CommonTokenFactory(true)); 
		 * final ASPCore2Parser parser = new ASPCore2Parser(new UnbufferedTokenStream<>(lexer)); 
		 * parser.setBuildParseTree(false);
		 */
		//@formatter:on
		CommonTokenStream tokens = new CommonTokenStream(new ASPCore2Lexer(stream));
		final ASPCore2Parser parser = new ASPCore2Parser(tokens);

		// Try SLL parsing mode (faster but may terminate incorrectly).
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		parser.removeErrorListeners();
		parser.setErrorHandler(new BailErrorStrategy());

		final CustomErrorListener errorListener = new CustomErrorListener(stream.getSourceName());

		ASPCore2Parser.ProgramContext programContext;
		try {
			// Parse program
			programContext = parser.program();
		} catch (ParseCancellationException e) {
			// Recognition exception may be caused simply by SLL parsing failing,
			// retry with LL parser and DefaultErrorStrategy printing errors to console.
			if (e.getCause() instanceof RecognitionException) {
				tokens.seek(0);
				parser.addErrorListener(errorListener);
				parser.setErrorHandler(new DefaultErrorStrategy());
				parser.getInterpreter().setPredictionMode(PredictionMode.LL);
				// Re-run parse.
				programContext = parser.program();
			} else {
				throw e;
			}
		}

		// If the our SwallowingErrorListener has handled some exception during parsing
		// just re-throw that exception.
		// At this time, error messages will be already printed out to standard error
		// because ANTLR by default adds an org.antlr.v4.runtime.ConsoleErrorListener
		// to every parser.
		// That ConsoleErrorListener will print useful messages, but not report back to
		// our code.
		// org.antlr.v4.runtime.BailErrorStrategy cannot be used here, because it would
		// abruptly stop parsing as soon as the first error is reached (i.e. no recovery
		// is attempted) and the user will only see the first error encountered.
		if (errorListener.getRecognitionException() != null) {
			throw errorListener.getRecognitionException();
		}

		// Abort parsing if there were some (recoverable) syntax errors.
		if (parser.getNumberOfSyntaxErrors() != 0) {
			throw new ParseCancellationException();
		}

		// The union of this parser's preloaded externals and the (program-specific) externals passed to the parse method
		Map<String, PredicateInterpretation> knownExternals;
		if (externals != null && !externals.isEmpty()) {
			knownExternals = new HashMap<>(preloadedExternals);
			knownExternals.putAll(externals);
		} else {
			knownExternals = preloadedExternals;
		}

		// Construct internal program representation.
		ParseTreeVisitor visitor = new ParseTreeVisitor(knownExternals);
		return visitor.translate(programContext);
	}

	@Override
	public ASPCore2Program parse(InputStream programSource, Map<String, PredicateInterpretation> externalPredicateDefinitions) throws IOException {
		return parse(CharStreams.fromStream(programSource), externalPredicateDefinitions);
	}

	@Override
	public ASPCore2Program parse(Path programPath, Map<String, PredicateInterpretation> externalPredicateDefinitions) throws IOException {
		return parse(CharStreams.fromPath(programPath), externalPredicateDefinitions);
	}

	@Override
	public ASPCore2Program parse(Map<String, PredicateInterpretation> externalPredicateDefinitions, Path... programSources) throws IOException {
		InputProgram.Builder bld = InputProgram.builder();
		for (Path src : programSources) {
			bld.accumulate(parse(src, externalPredicateDefinitions));
		}
		return bld.build();
	}

	@Override
	public ASPCore2Program parse(Iterable<Path> programSources, Map<String, PredicateInterpretation> externalPredicateDefinitions) throws IOException {
		InputProgram.Builder bld = InputProgram.builder();
		for (Path src : programSources) {
			bld.accumulate(parse(src, externalPredicateDefinitions));
		}
		return bld.build();
	}
}
