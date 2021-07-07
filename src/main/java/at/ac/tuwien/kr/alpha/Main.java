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
import at.ac.tuwien.kr.alpha.common.AnswerSetFormatter;
import at.ac.tuwien.kr.alpha.common.SimpleAnswerSetFormatter;
import at.ac.tuwien.kr.alpha.common.WeightedAnswerSet;
import at.ac.tuwien.kr.alpha.common.depgraph.ComponentGraph;
import at.ac.tuwien.kr.alpha.common.depgraph.DependencyGraph;
import at.ac.tuwien.kr.alpha.common.graphio.ComponentGraphWriter;
import at.ac.tuwien.kr.alpha.common.graphio.DependencyGraphWriter;
import at.ac.tuwien.kr.alpha.common.program.AnalyzedProgram;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.InternalProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.config.AlphaConfig;
import at.ac.tuwien.kr.alpha.config.CommandLineParser;
import at.ac.tuwien.kr.alpha.config.InputConfig;
import at.ac.tuwien.kr.alpha.solver.Solver;
import at.ac.tuwien.kr.alpha.solver.SolverMaintainingStatistics;
import org.antlr.v4.runtime.RecognitionException;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		Alpha alpha = new Alpha(cfg.getSystemConfig());

		InputProgram program = null;
		try {
			program = alpha.readProgram(cfg.getInputConfig());
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

		NormalProgram normalized = alpha.normalizeProgram(program);
		InternalProgram preprocessed;
		InputConfig inputCfg = cfg.getInputConfig();
		if (!(inputCfg.isWriteDependencyGraph() || inputCfg.isWriteComponentGraph())) {
			LOGGER.debug("Not writing dependency or component graphs, starting preprocessing...");
			preprocessed = alpha.performProgramPreprocessing(InternalProgram.fromNormalProgram(normalized));
		} else {
			LOGGER.debug("Performing program analysis in preparation for writing dependency and/or component graph file...");
			AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normalized);
			if (cfg.getInputConfig().isWriteDependencyGraph()) {
				Main.writeDependencyGraph(analyzed.getDependencyGraph(), cfg.getInputConfig().getDepgraphPath());
			}
			if (cfg.getInputConfig().isWriteComponentGraph()) {
				Main.writeComponentGraph(analyzed.getComponentGraph(), cfg.getInputConfig().getCompgraphPath());
			}
			preprocessed = alpha.performProgramPreprocessing(analyzed);
		}
		if (cfg.getInputConfig().isWritePreprocessed()) {
			Main.writeInternalProgram(preprocessed, cfg.getInputConfig().getPreprocessedPath());
		}
		Main.computeAndConsumeAnswerSets(alpha, cfg.getInputConfig(), preprocessed);
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
	 * Writes the given {@link InternalProgram} to the destination passed as the second parameter
	 * 
	 * @param prg   the program to write
	 * @param path the path to write the program to
	 */
	private static void writeInternalProgram(InternalProgram prg, String path) {
		LOGGER.debug("Writing preprocessed program to {}", path);
		PrintStream ps;
		try {
			if (path.equals(InputConfig.PREPROC_STDOUT_PATH)) {
				ps = System.out;
			} else {
				ps = new PrintStream(new File(path));
			}
			ps.println(prg.toString());
		} catch (IOException ex) {
			LOGGER.error("Failed writing preprocessed program file", ex);
			Main.bailOut("Failed writing preprocessed program file " + ex.getMessage());
		}
	}

	private static void computeAndConsumeAnswerSets(Alpha alpha, InputConfig inputCfg, InternalProgram program) {
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
			final AnswerSetFormatter<String> fmt = new SimpleAnswerSetFormatter(alpha.getConfig().getAtomSeparator());
			BiConsumer<Integer, AnswerSet> stdoutPrinter = (n, as) -> {
				System.out.println("Answer set " + n + ":" + System.lineSeparator() + fmt.format(as));
				if (program.containsWeakConstraints()) {
					// If weak constraints are presents, all answer sets are weighted.
					System.out.println("Optimization: " + ((WeightedAnswerSet) as).getWeightsAsString());
				}
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
				if (program.containsWeakConstraints() && counter.get() < limit) {
					// If less answer sets were found than requested and optimisation is enabled, then the last one is an optimal answer set.
					System.out.println("OPTIMUM PROVEN");
				}
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
