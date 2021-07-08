/*
 * Copyright (c) 2017-2020, the Alpha Team.
 * All rights reserved.
 *
 * Additional changes made by Siemens.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.grounder.parser;

import at.ac.tuwien.kr.alpha.CustomErrorListener;
import at.ac.tuwien.kr.alpha.antlr.AlphaASPLexer;
import at.ac.tuwien.kr.alpha.antlr.AlphaASPParser;
import at.ac.tuwien.kr.alpha.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class ProgramParser {
	private final Map<String, PredicateInterpretation> externals;

	public ProgramParser(Map<String, PredicateInterpretation> externals) {
		this.externals = externals;
	}

	public ProgramParser() {
		this(Collections.emptyMap());
	}

	public InputProgram parse(String s) {
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
			throw new IllegalArgumentException("Could not parse input program.", e);
		}
	}

	public InputProgram parse(CharStream stream) throws IOException {
		//@formatter:off
		/*
		 * // In order to require less memory: use unbuffered streams and avoid constructing a full parse tree.
		 * ASPCore2Lexer lexer = new ASPCore2Lexer(new UnbufferedCharStream(is));
		 * lexer.setTokenFactory(new CommonTokenFactory(true));
		 * final ASPCore2Parser parser = new ASPCore2Parser(new UnbufferedTokenStream<>(lexer));
		 * parser.setBuildParseTree(false);
		 */
		//@formatter:on
		CommonTokenStream tokens = new CommonTokenStream(new AlphaASPLexer(stream));
		final AlphaASPParser parser = new AlphaASPParser(tokens);

		// Try SLL parsing mode (faster but may terminate incorrectly).
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		parser.removeErrorListeners();
		parser.setErrorHandler(new BailErrorStrategy());

		final CustomErrorListener errorListener = new CustomErrorListener(stream.getSourceName());

		AlphaASPParser.ProgramContext programContext;
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

		// Construct internal program representation.
		ParseTreeVisitor visitor = new ParseTreeVisitor(externals);
		return visitor.translate(programContext);
	}
}
