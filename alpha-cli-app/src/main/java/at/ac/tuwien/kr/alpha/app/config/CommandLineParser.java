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
package at.ac.tuwien.kr.alpha.app.config;

import at.ac.tuwien.kr.alpha.api.config.AlphaConfig;
import at.ac.tuwien.kr.alpha.api.config.InputConfig;
import at.ac.tuwien.kr.alpha.api.config.SystemConfig;
import at.ac.tuwien.kr.alpha.api.solver.BinaryNoGoodPropagationEstimationStrategy;
import at.ac.tuwien.kr.alpha.api.solver.heuristics.Heuristic;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Parses given argument lists (as passed when Alpha is called from command line) into {@link SystemConfig}s and
 * {@link InputConfig}s.
 *
 */
public class CommandLineParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineParser.class);

	//@formatter:off
	/*
	 * Whenever a new command line option is added, perform the following steps: 
	 * 1. Add it as a constant option below. 
	 * 2. Add the constant option into the Options "CLI_OPTS" in the static initializer.
	 * 3. Add a handler method for it and add the respective map entry in initializeGlobalOptionHandlers 
	 *    or initializeInputOptionHandlers with a method reference to the handler.
	 */
	
	// "special", i.e. non-configuration options
	private static final Option OPT_HELP = Option.builder("h").longOpt("help").hasArg(false).desc("shows this help").build();

	// input-specific options
	private static final Option OPT_INPUT = Option.builder("i").longOpt("input").hasArg(true).argName("file").type(FileInputStream.class)
			.desc("read ASP program from this file").build();
	private static final Option OPT_NUM_ANSWER_SETS = Option.builder("n").longOpt("numAS").hasArg(true).argName("number").type(Integer.class)
			.desc("the number of answer sets to compute (default: compute all)").build();
	private static final Option OPT_FILTER = Option.builder("f").longOpt("filter").hasArg(true).argName("filter").valueSeparator(',')
			.desc("predicates to show when printing answer sets").build();
	private static final Option OPT_ASPSTRING = Option.builder("str").longOpt("aspstring").hasArg(true).argName("program").type(String.class)
			.desc("provide the asp program as a string").build();
	private static final Option OPT_LITERATE = Option.builder("l").longOpt("literate")
			.desc("enable literate programming mode (default: " + InputConfig.DEFAULT_LITERATE + ")").build();
	private static final Option OPT_DEBUG_PREPROCESSING = Option.builder("dbgp").longOpt("DebugPreprocessing")
			.desc("write files for normalized and preprocessed programs, also emit dependency- and component graphs as (graphviz) dot files").build();
	private static final Option OPT_WRITE_XSLX = Option.builder("wx").longOpt("write-xlsx").hasArg(true).argName("path").type(String.class)
			.desc("Write answer sets to excel files, i.e. xlsx workbooks (one workbook per answer set)").build();

	// general system-wide config
	private static final Option OPT_GROUNDER = Option.builder("g").longOpt("grounder").hasArg(true).argName("grounder")
			.desc("the grounder implementation to use (default: " + SystemConfig.DEFAULT_GROUNDER_NAME + ")").build();
	private static final Option OPT_SOLVER = Option.builder("s").longOpt("solver").hasArg(true).argName("solver")
			.desc("the solver implementation to use (default: " + SystemConfig.DEFAULT_SOLVER_NAME + ")").build();
	private static final Option OPT_NOGOOD_STORE = Option.builder("r").longOpt("store").hasArg(true).argName("store")
			.desc("the nogood store to use (default: " + SystemConfig.DEFAULT_NOGOOD_STORE_NAME + ")").build();
	private static final Option OPT_SORT = Option.builder("sort").longOpt("sort").hasArg(false)
			.desc("sort answer sets (default: " + SystemConfig.DEFAULT_SORT_ANSWER_SETS + ")").build();
	private static final Option OPT_DETERMINISTIC = Option.builder("d").longOpt("deterministic").hasArg(false)
			.desc("disables randomness (default: " + SystemConfig.DEFAULT_DETERMINISTIC + ")").build();
	private static final Option OPT_SEED = Option.builder("e").longOpt("seed").hasArg(true).argName("seed").type(Integer.class)
			.desc("set seed (default: System.nanoTime())").build();
	private static final Option OPT_DEBUG_INTERNAL_CHECKS = Option.builder("dbgs").longOpt("DebugEnableInternalChecks")
			.desc("run additional (time-consuming) safety checks (default: " + SystemConfig.DEFAULT_DEBUG_INTERNAL_CHECKS + ")").build();
	private static final Option OPT_BRANCHING_HEURISTIC = Option.builder("b").longOpt("branchingHeuristic").hasArg(true).argName("heuristic")
			.desc("the branching heuristic to use (default: " + SystemConfig.DEFAULT_BRANCHING_HEURISTIC.name() + ")").build();
	private static final Option OPT_MOMS_STRATEGY = Option.builder("ms").longOpt("momsStrategy").hasArg(true).argName("strategy")
			.desc("strategy for mom's heuristic (CountBinaryWatches or BinaryNoGoodPropagation, default: " + SystemConfig.DEFAULT_MOMS_STRATEGY.name() + ")")
			.build();
	private static final Option OPT_REPLAY_CHOICES = Option.builder("rc").longOpt("replayChoices").hasArg().argName("choices")
			.desc("comma-separated list of choices to be replayed (each choice is represented by a signed integer whose absolute value designates an atom ID and whose sign designates a truth value)")
			.build();
	private static final Option OPT_QUIET = Option.builder("q").longOpt("quiet").desc("do not print answer sets (default: " + SystemConfig.DEFAULT_QUIET)
			.build();
	private static final Option OPT_STATS = Option.builder("st").longOpt("stats").desc("print statistics (default: " + SystemConfig.DEFAULT_PRINT_STATS + ")")
			.build();
	private static final Option OPT_NO_JUSTIFICATION = Option.builder("dj").longOpt("disableJustifications")
			.desc("disable the search for justifications on must-be-true assigned atoms in the solver (default: "
					+ SystemConfig.DEFAULT_DISABLE_JUSTIFICATION_SEARCH + ")")
			.build();
	private static final Option OPT_NORMALIZATION_GRID = Option.builder("ng").longOpt("normalizationCountingGrid")
			.desc("use counting grid normalization instead of sorting circuit for #count (default: " + SystemConfig.DEFAULT_USE_NORMALIZATION_GRID + ")")
			.build();
	private static final Option OPT_NO_EVAL_STRATIFIED = Option.builder("dse").longOpt("disableStratifiedEvaluation")
			.desc("Disable stratified evaluation")
			.build();
	private static final Option OPT_NO_NOGOOD_DELETION = Option.builder("dnd").longOpt("disableNoGoodDeletion")
			.desc("disable the deletion of (learned, little active) nogoods (default: " 
					+ SystemConfig.DEFAULT_DISABLE_NOGOOD_DELETION + ")")
			.build();
	private static final Option OPT_GROUNDER_TOLERANCE_CONSTRAINTS = Option.builder("gtc").longOpt("grounderToleranceConstraints")
			.desc("grounder tolerance for constraints (default: " + SystemConfig.DEFAULT_GROUNDER_TOLERANCE_CONSTRAINTS + ")")
			.hasArg().argName("tolerance")
			.build();
	private static final Option OPT_GROUNDER_TOLERANCE_RULES = Option.builder("gtr").longOpt("grounderToleranceRules")
			.desc("grounder tolerance for rules (default: " + SystemConfig.DEFAULT_GROUNDER_TOLERANCE_RULES + ")")
			.hasArg().argName("tolerance")
			.build();
	private static final Option OPT_GROUNDER_ACCUMULATOR_ENABLED = Option.builder("acc").longOpt("enableAccumulator")
			.desc("activates the accumulator grounding strategy by disabling removal of instances from grounder memory in certain cases (default: " 
					+ SystemConfig.DEFAULT_GROUNDER_ACCUMULATOR_ENABLED + ")")
			.build();
	private static final Option OPT_OUTPUT_ATOM_SEPARATOR = Option.builder("sep").longOpt("atomSeparator").hasArg(true).argName("separator")
			.desc("a character (sequence) to use as separator for atoms in printed answer sets (default: "
					+ SystemConfig.DEFAULT_ATOM_SEPARATOR + ")")
			.build();
	//@formatter:on

	private static final Options CLI_OPTS = new Options();

	static {
		/*
		 * Below code adds all options defined above to CLI_OPTS - needed for parsing
		 */
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_HELP);

		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_NUM_ANSWER_SETS);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_FILTER);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_LITERATE);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_INPUT);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_ASPSTRING);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_DEBUG_PREPROCESSING);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_WRITE_XSLX);

		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_GROUNDER);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_SOLVER);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_NOGOOD_STORE);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_SORT);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_DETERMINISTIC);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_SEED);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_DEBUG_INTERNAL_CHECKS);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_BRANCHING_HEURISTIC);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_MOMS_STRATEGY);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_REPLAY_CHOICES);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_QUIET);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_STATS);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_NO_JUSTIFICATION);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_NORMALIZATION_GRID);

		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_NO_EVAL_STRATIFIED);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_NO_NOGOOD_DELETION);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_GROUNDER_TOLERANCE_CONSTRAINTS);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_GROUNDER_TOLERANCE_RULES);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_GROUNDER_ACCUMULATOR_ENABLED);
		CommandLineParser.CLI_OPTS.addOption(CommandLineParser.OPT_OUTPUT_ATOM_SEPARATOR);
	}

	/*
	 * Below maps map commandline options to handler methods. If a new option is added, the appropriate put into the map
	 * must be added in the constructor
	 */
	private final Map<String, CliOptionHandler<SystemConfig>> globalOptionHandlers = new HashMap<>();
	private final Map<String, CliOptionHandler<InputConfig>> inputOptionHandlers = new HashMap<>();
	private Consumer<String> abortAction;
	private String cmdSyntax;

	/**
	 * Creates a new <code>CommandLineParser</code>. The abortAction described below is passed into the constructor
	 * externally in order to avoid strongly coupling this class to any other part of the application. Especially an
	 * abortAction - which likely will include some call like System.exit({state}) - should not be specified by utility
	 * classes themselves, but rather by the application's main class.
	 * 
	 * @param cmdLineSyntax a string describing the basic call syntax for the application binary, e.g. "java -jar
	 *                      somejar.jar"
	 * @param abortAction   a <code>Consumer<String></code> that is called when option parsing is aborted, as is the case
	 *                      when the "help" option is encountered.
	 *                      It expects a string parameter, which is a message accompanying the abort
	 */
	public CommandLineParser(String cmdLineSyntax, Consumer<String> abortAction) {
		this.cmdSyntax = cmdLineSyntax;
		this.abortAction = abortAction;
		this.initializeGlobalOptionHandlers();
		this.initializeInputOptionHandlers();
	}

	private void initializeGlobalOptionHandlers() {
		/*
		 * below put invocations are used to "register" the handler methods for each commandline option
		 */
		// help is handled separately, therefore dummy handler
		this.globalOptionHandlers.put(CommandLineParser.OPT_HELP.getOpt(), (o, c) -> { });
		this.globalOptionHandlers.put(CommandLineParser.OPT_GROUNDER.getOpt(), this::handleGrounder);
		this.globalOptionHandlers.put(CommandLineParser.OPT_SOLVER.getOpt(), this::handleSolver);
		this.globalOptionHandlers.put(CommandLineParser.OPT_NOGOOD_STORE.getOpt(), this::handleNogoodStore);
		this.globalOptionHandlers.put(CommandLineParser.OPT_SORT.getOpt(), this::handleSort);
		this.globalOptionHandlers.put(CommandLineParser.OPT_DETERMINISTIC.getOpt(), this::handleDeterministic);
		this.globalOptionHandlers.put(CommandLineParser.OPT_SEED.getOpt(), this::handleSeed);
		this.globalOptionHandlers.put(CommandLineParser.OPT_DEBUG_INTERNAL_CHECKS.getOpt(), this::handleInternalChecks);
		this.globalOptionHandlers.put(CommandLineParser.OPT_BRANCHING_HEURISTIC.getOpt(), this::handleBranchingHeuristic);
		this.globalOptionHandlers.put(CommandLineParser.OPT_MOMS_STRATEGY.getOpt(), this::handleMomsStrategy);
		this.globalOptionHandlers.put(CommandLineParser.OPT_REPLAY_CHOICES.getOpt(), this::handleReplayChoices);
		this.globalOptionHandlers.put(CommandLineParser.OPT_QUIET.getOpt(), this::handleQuiet);
		this.globalOptionHandlers.put(CommandLineParser.OPT_STATS.getOpt(), this::handleStats);
		this.globalOptionHandlers.put(CommandLineParser.OPT_NO_JUSTIFICATION.getOpt(), this::handleNoJustification);
		this.globalOptionHandlers.put(CommandLineParser.OPT_NORMALIZATION_GRID.getOpt(), this::handleNormalizationGrid);
		this.globalOptionHandlers.put(CommandLineParser.OPT_NO_EVAL_STRATIFIED.getOpt(), this::handleDisableStratifedEval);
		this.globalOptionHandlers.put(CommandLineParser.OPT_NO_NOGOOD_DELETION.getOpt(), this::handleNoNoGoodDeletion);
		this.globalOptionHandlers.put(CommandLineParser.OPT_GROUNDER_TOLERANCE_CONSTRAINTS.getOpt(), this::handleGrounderToleranceConstraints);
		this.globalOptionHandlers.put(CommandLineParser.OPT_GROUNDER_TOLERANCE_RULES.getOpt(), this::handleGrounderToleranceRules);
		this.globalOptionHandlers.put(CommandLineParser.OPT_GROUNDER_ACCUMULATOR_ENABLED.getOpt(), this::handleGrounderNoInstanceRemoval);
		this.globalOptionHandlers.put(CommandLineParser.OPT_OUTPUT_ATOM_SEPARATOR.getOpt(), this::handleAtomSeparator);
	}

	private void initializeInputOptionHandlers() {
		this.inputOptionHandlers.put(CommandLineParser.OPT_NUM_ANSWER_SETS.getOpt(), this::handleNumAnswerSets);
		this.inputOptionHandlers.put(CommandLineParser.OPT_INPUT.getOpt(), this::handleInput);
		this.inputOptionHandlers.put(CommandLineParser.OPT_FILTER.getOpt(), this::handleFilters);
		this.inputOptionHandlers.put(CommandLineParser.OPT_ASPSTRING.getOpt(), this::handleAspString);
		this.inputOptionHandlers.put(CommandLineParser.OPT_LITERATE.getOpt(), this::handleLiterate);
		this.inputOptionHandlers.put(CommandLineParser.OPT_DEBUG_PREPROCESSING.getOpt(), this::handleDebugPreprocessing);
		this.inputOptionHandlers.put(CommandLineParser.OPT_WRITE_XSLX.getOpt(), this::handleWriteXlsx);
	}

	public AlphaConfig parseCommandLine(String[] args) throws ParseException {
		CommandLine commandLine = new DefaultParser().parse(CommandLineParser.CLI_OPTS, args);
		if (commandLine.getArgs().length > 0) {
			throw new ParseException("Positional arguments { " + StringUtils.join(args, ' ') + " } are invalid!");
		}
		AlphaConfig retVal = new AlphaConfig();
		SystemConfig sysConf = new SystemConfig();
		InputConfig inputConf = new InputConfig();
		if (commandLine.hasOption(CommandLineParser.OPT_HELP.getOpt())) {
			LOGGER.debug("Found help option!");
			this.handleHelp();
		} else {
			this.validate(commandLine);
		}
		for (Option opt : commandLine.getOptions()) {
			this.handleOption(opt, sysConf, inputConf);
		}
		retVal.setSystemConfig(sysConf);
		retVal.setInputConfig(inputConf);
		return retVal;
	}

	private void handleOption(Option opt, SystemConfig sysConf, InputConfig inputConf) throws ParseException {
		CliOptionHandler<SystemConfig> globalOptionHandler;
		CliOptionHandler<InputConfig> inputOptionHandler;
		globalOptionHandler = this.globalOptionHandlers.get(opt.getOpt());
		if (globalOptionHandler != null) {
			globalOptionHandler.handleOption(opt, sysConf);
		} else {
			inputOptionHandler = this.inputOptionHandlers.get(opt.getOpt());
			if (inputOptionHandler != null) {
				inputOptionHandler.handleOption(opt, inputConf);
			} else {
				throw new ParseException("Cannot handle option: " + opt.getOpt());
			}
		}
	}

	public String getUsageMessage() {
		HelpFormatter formatter = new HelpFormatter();
		// Unfortunately, commons-cli does not offer a method of simply rendering a help
		// message into a string, therefore the ByteArrayOutputStream..
		ByteArrayOutputStream helpBuffer = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(helpBuffer);
		formatter.printHelp(pw, HelpFormatter.DEFAULT_WIDTH, this.cmdSyntax, "", CommandLineParser.CLI_OPTS, HelpFormatter.DEFAULT_LEFT_PAD,
				HelpFormatter.DEFAULT_DESC_PAD, "");
		pw.flush();
		return helpBuffer.toString();
	}

	private void validate(CommandLine commandLine) throws ParseException {
		if (!commandLine.hasOption(CommandLineParser.OPT_INPUT.getOpt()) && !commandLine.hasOption(CommandLineParser.OPT_ASPSTRING.getOpt())) {
			throw new ParseException("Missing input source - need to specifiy either a file (" + CommandLineParser.OPT_INPUT.getOpt() + ") or a string ("
					+ CommandLineParser.OPT_ASPSTRING.getOpt() + " - or both)!");
		}
	}

	private void handleNumAnswerSets(Option opt, InputConfig cfg) {
		String optVal = opt.getValue();
		int limit;
		if (optVal != null) {
			limit = Integer.valueOf(optVal);
			cfg.setNumAnswerSets(limit);
		} else {
			cfg.setNumAnswerSets(InputConfig.DEFAULT_NUM_ANSWER_SETS);
		}
	}

	private void handleHelp() {
		this.abortAction.accept(this.getUsageMessage());
	}

	private void handleInput(Option opt, InputConfig cfg) {
		String optVal = opt.getValue().trim();
		cfg.getFiles().add(optVal);
	}

	private void handleGrounder(Option opt, SystemConfig cfg) {
		cfg.setGrounderName(opt.getValue(SystemConfig.DEFAULT_GROUNDER_NAME));
	}

	private void handleSolver(Option opt, SystemConfig cfg) {
		cfg.setSolverName(opt.getValue(SystemConfig.DEFAULT_SOLVER_NAME));
	}

	private void handleNogoodStore(Option opt, SystemConfig cfg) {
		cfg.setNogoodStoreName(opt.getValue(SystemConfig.DEFAULT_NOGOOD_STORE_NAME));
	}

	private void handleFilters(Option opt, InputConfig cfg) {
		String pred = opt.getValue().trim();
		cfg.getDesiredPredicates().add(pred);
	}

	private void handleAspString(Option opt, InputConfig cfg) {
		String optVal = opt.getValue().trim();
		cfg.getAspStrings().add(optVal);
	}

	private void handleSort(Option opt, SystemConfig cfg) {
		cfg.setSortAnswerSets(true);
	}

	private void handleDeterministic(Option opt, SystemConfig cfg) {
		cfg.setDeterministic(true);
		cfg.setSeed(0);
	}

	private void handleSeed(Option opt, SystemConfig cfg) {
		cfg.setDeterministic(false);
		String optVal = opt.getValue();
		long seed;
		if (optVal != null) {
			seed = Long.valueOf(optVal);
			cfg.setSeed(seed);
		} else {
			cfg.setSeed(SystemConfig.DEFAULT_SEED);
		}
	}

	private void handleInternalChecks(Option opt, SystemConfig cfg) {
		cfg.setDebugInternalChecks(true);
	}

	private void handleBranchingHeuristic(Option opt, SystemConfig cfg) throws ParseException {
		String branchingHeuristicName = opt.getValue(SystemConfig.DEFAULT_BRANCHING_HEURISTIC.name());
		try {
			cfg.setBranchingHeuristicName(branchingHeuristicName);
		} catch (IllegalArgumentException e) {
			throw new ParseException(
					"Unknown branching heuristic: " + branchingHeuristicName + ". Please try one of the following: " + Heuristic.listAllowedValues());
		}
	}

	private void handleMomsStrategy(Option opt, SystemConfig cfg) throws ParseException {
		String momsStrategyName = opt.getValue(SystemConfig.DEFAULT_MOMS_STRATEGY.name());
		try {
			cfg.setMomsStrategyName(momsStrategyName);
		} catch (IllegalArgumentException e) {
			throw new ParseException("Unknown mom's strategy: " + momsStrategyName + ". Please try one of the following: "
					+ BinaryNoGoodPropagationEstimationStrategy.listAllowedValues());
		}
	}

	private void handleReplayChoices(Option opt, SystemConfig cfg) throws ParseException {
		String replayChoices = opt.getValue(SystemConfig.DEFAULT_REPLAY_CHOICES.toString());
		try {
			cfg.setReplayChoices(replayChoices);
		} catch (NumberFormatException e) {
			throw new ParseException("Cannot parse list of signed integers indicating choices to be replayed: " + replayChoices);
		}
	}

	private void handleQuiet(Option opt, SystemConfig cfg) {
		cfg.setQuiet(true);
	}

	private void handleLiterate(Option opt, InputConfig cfg) {
		cfg.setLiterate(true);
	}

	private void handleWriteXlsx(Option opt, InputConfig cfg) {
		cfg.setWriteAnswerSetsAsXlsx(true);
		String outputPath = opt.getValue(InputConfig.DEFAULT_XLSX_OUTFILE_PATH);
		cfg.setAnswerSetFileOutputPath(outputPath);
	}

	private void handleStats(Option opt, SystemConfig cfg) {
		cfg.setPrintStats(true);
	}

	private void handleNoJustification(Option opt, SystemConfig cfg) {
		cfg.setDisableJustificationSearch(true);
	}

	private void handleNormalizationGrid(Option opt, SystemConfig cfg) {
		cfg.setUseNormalizationGrid(true);
	}

	private void handleDisableStratifedEval(Option opt, SystemConfig cfg) {
		cfg.setEvaluateStratifiedPart(false);
	}

	private void handleDebugPreprocessing(Option opt, InputConfig cfg) {
		cfg.setDebugPreprocessing(true);
	}

	private void handleNoNoGoodDeletion(Option opt, SystemConfig cfg) {
		cfg.setDisableNoGoodDeletion(true);
	}

	private void handleGrounderToleranceConstraints(Option opt, SystemConfig cfg) {
		String grounderToleranceConstraints = opt.getValue(SystemConfig.DEFAULT_GROUNDER_TOLERANCE_CONSTRAINTS);
		cfg.setGrounderToleranceConstraints(grounderToleranceConstraints);
	}

	private void handleGrounderToleranceRules(Option opt, SystemConfig cfg) {
		String grounderToleranceRules = opt.getValue(SystemConfig.DEFAULT_GROUNDER_TOLERANCE_RULES);
		cfg.setGrounderToleranceRules(grounderToleranceRules);
	}

	private void handleGrounderNoInstanceRemoval(Option opt, SystemConfig cfg) {
		cfg.setGrounderAccumulatorEnabled(true);
	}

	private void handleAtomSeparator(Option opt, SystemConfig cfg) {
		cfg.setAtomSeparator(StringEscapeUtils.unescapeJava(opt.getValue(SystemConfig.DEFAULT_ATOM_SEPARATOR)));
	}
	
}
