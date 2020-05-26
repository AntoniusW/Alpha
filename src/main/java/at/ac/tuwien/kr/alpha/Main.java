/**
 * Copyright (c) 2016-2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.Program;
import at.ac.tuwien.kr.alpha.config.AlphaConfig;
import at.ac.tuwien.kr.alpha.config.CommandLineParser;
import at.ac.tuwien.kr.alpha.config.InputConfig;
import at.ac.tuwien.kr.alpha.solver.Solver;
import at.ac.tuwien.kr.alpha.solver.SolverMaintainingStatistics;
import org.antlr.v4.runtime.RecognitionException;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main entry point for Alpha.
 */
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	private static final String ALPHA_CALL_SYNTAX = "java -jar alpha-bundled.jar" + System.lineSeparator() + "java -jar alpha.jar";

	public static void main(String[] args) {
		CommandLineParser commandLineParser = new CommandLineParser(Main.ALPHA_CALL_SYNTAX, (msg) -> Main.exitWithMessage(msg, 0));
		AlphaConfig ctx = null;
		try {
			ctx = commandLineParser.parseCommandLine(args);
		} catch (ParseException ex) {
			System.err.println("Invalid usage: " + ex.getMessage());
			Main.exitWithMessage(commandLineParser.getUsageMessage(), 1);
		}

		Alpha alpha = new Alpha(ctx.getAlphaConfig());

		Program program = null;
		try {
			program = alpha.readProgram(ctx.getInputConfig());
		} catch (RecognitionException e) {
			// In case a recognition exception occurred, parseVisit will
			// already have printed an error message, so we just exit
			// at this point without further logging.
			System.exit(1);
		} catch (FileNotFoundException e) {
			Main.bailOut(e.getMessage());
		} catch (IOException e) {
			Main.bailOut("Failed to parse program.", e);
		}

		Main.computeAndConsumeAnswerSets(alpha, ctx.getInputConfig(), program);
	}

	private static void computeAndConsumeAnswerSets(Alpha alpha, InputConfig inputCfg, Program program) {
		Solver solver = alpha.prepareSolverFor(program, inputCfg.getFilter());
		Stream<AnswerSet> stream = solver.stream();
		if (alpha.getConfig().isSortAnswerSets()) {
			stream = stream.sorted();
		}

		int limit = inputCfg.getNumAnswerSets();
		if (limit > 0) {
			stream = stream.limit(limit);
		}

		if (!alpha.getConfig().isQuiet()) {
			AtomicInteger counter = new AtomicInteger(0);
			final BiConsumer<Integer, AnswerSet> answerSetHandler;
			BiConsumer<Integer, AnswerSet> stdoutPrinter = (n, as) -> {
				System.out.println("Answer set " + Integer.toString(n) + ":" + System.lineSeparator() + as.toString());
			};
			if (inputCfg.isWriteAnswerSetsAsXlsx()) {
				BiConsumer<Integer, AnswerSet> xlsxWriter = new AnswerSetToXlsxWriter(inputCfg.getAnswerSetFileOutputPath());
				answerSetHandler = stdoutPrinter.andThen(xlsxWriter);
			} else {
				answerSetHandler = stdoutPrinter;
			}
			stream.forEach(as -> {
				int cnt = counter.incrementAndGet();
				answerSetHandler.accept(cnt, as);
			});
			if (counter.get() == 0) {
				System.out.println("UNSATISFIABLE");
				if (inputCfg.isWriteAnswerSetsAsXlsx()) {
					try {
						AnswerSetToXlsxWriter.writeUnsatInfo(Paths.get(inputCfg.getAnswerSetFileOutputPath() + ".UNSAT.xlsx"));
					} catch (IOException ex) {
						System.err.println("Failed writing unsat file!");
					}
				}
			} else {
				System.out.println("SATISFIABLE");
			}
		} else {
			// Note: Even though we are not consuming the result, we will still compute
			// answer sets.
			stream.collect(Collectors.toList());
		}
		if (alpha.getConfig().isPrintStats()) {
			((SolverMaintainingStatistics) solver).printStatistics();
		}
	}

	private static void exitWithMessage(String msg, int exitCode) {
		System.out.println(msg);
		System.exit(exitCode);
	}

	private static void bailOut(String format, Object... arguments) {
		LOGGER.error(format, arguments);
		System.exit(1);
	}

}
