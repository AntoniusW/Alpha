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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.DebugSolvingContext;
import at.ac.tuwien.kr.alpha.api.Solver;
import at.ac.tuwien.kr.alpha.api.StatisticsReportingSolver;
import at.ac.tuwien.kr.alpha.api.config.AlphaConfig;
import at.ac.tuwien.kr.alpha.api.config.InputConfig;
import at.ac.tuwien.kr.alpha.api.config.SystemConfig;
import at.ac.tuwien.kr.alpha.api.impl.AlphaImpl;
import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.NormalProgram;
import at.ac.tuwien.kr.alpha.api.programs.analysis.ComponentGraph;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;
import at.ac.tuwien.kr.alpha.api.util.AnswerSetFormatter;
import at.ac.tuwien.kr.alpha.app.ComponentGraphWriter;
import at.ac.tuwien.kr.alpha.app.DependencyGraphWriter;
import at.ac.tuwien.kr.alpha.app.config.CommandLineParser;
import at.ac.tuwien.kr.alpha.commons.util.SimpleAnswerSetFormatter;

/**
 * Main entry point for Alpha.
 */
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	private static final String ALPHA_CALL_SYNTAX = "java -jar alpha-bundled.jar" + System.lineSeparator() + "java -jar alpha.jar";

	public static void main(String[] args) {
		CommandLineParser commandLineParser = new CommandLineParser(Main.ALPHA_CALL_SYNTAX, (msg) -> Main.exitWithMessage(msg, 0));
		AlphaConfig cfg = null;
		try {
			cfg = commandLineParser.parseCommandLine(args);
		} catch (ParseException ex) {
			System.err.println("Invalid usage: " + ex.getMessage());
			Main.exitWithMessage(commandLineParser.getUsageMessage(), 1);
		}

		Alpha alpha = new AlphaImpl(cfg.getSystemConfig());

		ASPCore2Program program = null;
		try {
			program = alpha.readProgram(cfg.getInputConfig());
		} catch (FileNotFoundException e) {
			Main.bailOut(e.getMessage());
		} catch (IOException e) {
			Main.bailOut("Failed to parse program.", e);
		}

		InputConfig inputCfg = cfg.getInputConfig();
		Solver solver;
		if (inputCfg.isDebugPreprocessing()) {
			DebugSolvingContext dbgCtx = alpha.prepareDebugSolve(program);
			Main.writeNormalProgram(dbgCtx.getNormalizedProgram(), inputCfg.getNormalizedPath());
			Main.writeNormalProgram(dbgCtx.getPreprocessedProgram(), inputCfg.getPreprocessedPath());
			Main.writeDependencyGraph(dbgCtx.getDependencyGraph(), inputCfg.getDepgraphPath());
			Main.writeComponentGraph(dbgCtx.getComponentGraph(), inputCfg.getCompgraphPath());
			solver = dbgCtx.getSolver();
		} else {
			solver = alpha.prepareSolverFor(program, inputCfg.getFilter());
		}
		Main.computeAndConsumeAnswerSets(solver, cfg);
	}

	/**
	 * Writes the given {@link DependencyGraph} to the destination passed as the second parameter
	 * 
	 * @param dg   the dependency graph to write
	 * @param path the path to write the graph to
	 */
	private static void writeDependencyGraph(DependencyGraph dg, String path) {
		DependencyGraphWriter depGraphWriter = new DependencyGraphWriter();
		try (FileOutputStream os = new FileOutputStream(new File(path))) {
			depGraphWriter.writeAsDot(dg, os);
		} catch (IOException ex) {
			Main.bailOut("Error writing dependency graph: " + ex.getMessage());
		}
	}

	/**
	 * Writes the given {@link ComponentGraph} to the destination passed as the second parameter
	 * 
	 * @param cg   the component graph to write
	 * @param path the path to write the graph to
	 */
	private static void writeComponentGraph(ComponentGraph cg, String path) {
		ComponentGraphWriter compGraphWriter = new ComponentGraphWriter();
		try (FileOutputStream os = new FileOutputStream(new File(path))) {
			compGraphWriter.writeAsDot(cg, os);
		} catch (IOException ex) {
			Main.bailOut("Error writing component graph: " + ex.getMessage());
		}

	}

	/**
	 * Writes the given {@link CompiledProgram} to the destination passed as the second parameter
	 * 
	 * @param prg  the program to write
	 * @param path the path to write the program to
	 */
	private static void writeNormalProgram(NormalProgram prg, String path) {
		LOGGER.debug("Writing program to {}", path);
		PrintStream ps;
		try {
			ps = new PrintStream(new File(path));
			ps.println(prg.toString());
		} catch (IOException ex) {
			LOGGER.error("Failed writing program file", ex);
			Main.bailOut("Failed writing program file " + ex.getMessage());
		}
	}

	private static void computeAndConsumeAnswerSets(Solver solver, AlphaConfig alphaCfg) {
		SystemConfig sysCfg = alphaCfg.getSystemConfig();
		InputConfig inputCfg = alphaCfg.getInputConfig();
		Stream<AnswerSet> stream = solver.stream();
		if (sysCfg.isSortAnswerSets()) {
			stream = stream.sorted();
		}

		int limit = inputCfg.getNumAnswerSets();
		if (limit > 0) {
			stream = stream.limit(limit);
		}

		if (!sysCfg.isQuiet()) {
			AtomicInteger counter = new AtomicInteger(0);
			final BiConsumer<Integer, AnswerSet> answerSetHandler;
			final AnswerSetFormatter<String> fmt = new SimpleAnswerSetFormatter(sysCfg.getAtomSeparator());
			BiConsumer<Integer, AnswerSet> stdoutPrinter = (n, as) -> {
				System.out.println("Answer set " + Integer.toString(n) + ":" + System.lineSeparator() + fmt.format(as));
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
		if (sysCfg.isPrintStats()) {
			if (solver instanceof StatisticsReportingSolver) {
				((StatisticsReportingSolver) solver).printStatistics();
			} else {
				// Note: Should not happen with proper validation of commandline args
				System.err.println("Solver of type " + solver.getClass().getSimpleName() + " does not support solving statistics!");
			}
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
