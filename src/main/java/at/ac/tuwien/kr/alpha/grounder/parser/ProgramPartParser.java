/*
 * Copyright (c) 2018-2021, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.antlr.AlphaASPLexer;
import at.ac.tuwien.kr.alpha.antlr.AlphaASPParser;
import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.Collections;

/**
 * A parser that, in contrast to {@link ProgramParser}, does not parse full programs but only program parts like
 * atoms, terms and such.
 */
public class ProgramPartParser {
	private final ParseTreeVisitor visitor = new ParseTreeVisitor(Collections.emptyMap(), true);

	public Term parseTerm(String s) {
		final AlphaASPParser parser = getParser(s);
		return (Term)parse(parser.term());
	}

	public BasicAtom parseBasicAtom(String s) {
		final AlphaASPParser parser = getParser(s);
		return (BasicAtom)parse(parser.classical_literal());
	}

	public Literal parseLiteral(String s) {
		final AlphaASPParser parser = getParser(s);
		return (Literal)parse(parser.naf_literal());
	}

	public BasicRule parseBasicRule(String s) {
		final AlphaASPParser parser = getParser(s);
		return (BasicRule)parse(parser.statement());
	}

	public HeuristicDirective parseHeuristicDirective(String s) {
		final AlphaASPParser parser = getParser(s);
		return (HeuristicDirective)parse(parser.directive_heuristic());
	}

	private AlphaASPParser getParser(String s) {
		return new AlphaASPParser(new CommonTokenStream(new AlphaASPLexer(CharStreams.fromString(s))));
	}

	private Object parse(ParserRuleContext context) {
		try {
			visitor.initialize();
			return visitor.visit(context);
		} catch (RecognitionException | ParseCancellationException e) {
			// If there were issues parsing the given string, we
			// throw something that suggests that the input string
			// is malformed.
			throw new IllegalArgumentException("Could not parse term.", e);
		}
	}
}
