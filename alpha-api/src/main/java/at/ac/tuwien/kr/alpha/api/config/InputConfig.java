package at.ac.tuwien.kr.alpha.api.config;

import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;

import java.util.*;

public class InputConfig {
	
	public static final java.util.function.Predicate<Predicate> DEFAULT_FILTER = p -> true;
	public static final boolean DEFAULT_LITERATE = false;
	public static final int DEFAULT_NUM_ANSWER_SETS = 0;
	public static final boolean DEFAULT_DEBUG_PREPROCESSING = false;
	public static final String DEFAULT_DEPGRAPH_TARGET_FILE = "depgraph.dot";
	public static final String DEFAULT_COMPGRAPH_TARGET_FILE = "compgraph.dot";
	public static final String DEFAULT_NORMALIZED_TARGET_FILE = "input.normalized.asp";
	public static final String DEFAULT_PREPROC_TARGET_FILE = "input.preproc.asp";
	public static final boolean DEFAULT_WRITE_XLSX = false;
	public static final String DEFAULT_XLSX_OUTFILE_PATH = "alphaAnswerSet"; // current directory, files named "alphaAnswerSet.{num}.{ext}"

	private List<String> aspStrings = new ArrayList<>();
	private List<String> files = new ArrayList<>();
	private boolean literate = InputConfig.DEFAULT_LITERATE;
	private int numAnswerSets = InputConfig.DEFAULT_NUM_ANSWER_SETS;
	private Set<String> desiredPredicates = new HashSet<>();
	private boolean debugPreprocessing = InputConfig.DEFAULT_DEBUG_PREPROCESSING;
	private String depgraphPath = InputConfig.DEFAULT_DEPGRAPH_TARGET_FILE;
	private String compgraphPath = InputConfig.DEFAULT_COMPGRAPH_TARGET_FILE;
	private String normalizedPath = InputConfig.DEFAULT_NORMALIZED_TARGET_FILE;
	private String preprocessedPath = InputConfig.DEFAULT_PREPROC_TARGET_FILE;
	// TODO: standard library externals are NOT always loaded, but this was the case before introducing modules
	// TODO: ensure this in parser
	private Map<String, PredicateInterpretation> predicateMethods = new HashMap<>(); // Externals.getStandardLibraryExternals();
	private boolean writeAnswerSetsAsXlsx = InputConfig.DEFAULT_WRITE_XLSX;
	private String answerSetFileOutputPath;

	public static InputConfig forString(String str) {
		InputConfig retVal = new InputConfig();
		retVal.aspStrings.add(str);
		return retVal;
	}

	public List<String> getAspStrings() {
		return this.aspStrings;
	}

	public void setAspStrings(List<String> aspStrings) {
		this.aspStrings = aspStrings;
	}

	public boolean isLiterate() {
		return this.literate;
	}

	public void setLiterate(boolean literate) {
		this.literate = literate;
	}

	public int getNumAnswerSets() {
		return this.numAnswerSets;
	}

	public void setNumAnswerSets(int numAnswerSets) {
		this.numAnswerSets = numAnswerSets;
	}

	public java.util.function.Predicate<Predicate> getFilter() {
		return this.desiredPredicates.isEmpty() ? InputConfig.DEFAULT_FILTER : p -> this.desiredPredicates.contains(p.getName());
	}

	public Map<String, PredicateInterpretation> getPredicateMethods() {
		return this.predicateMethods;
	}

	public void addPredicateMethods(Map<String, PredicateInterpretation> predicateMethods) {
		for (Map.Entry<String, PredicateInterpretation> entry : predicateMethods.entrySet()) {
			if (this.predicateMethods.containsKey(entry.getKey())) {
				throw new IllegalArgumentException("Input config already contains a predicate interpretation with name " + entry.getKey());
			}
			this.predicateMethods.put(entry.getKey(), entry.getValue());
		}
	}

	public void addPredicateMethod(String name, PredicateInterpretation interpretation) {
		this.predicateMethods.put(name, interpretation);
	}

	public List<String> getFiles() {
		return this.files;
	}

	public void setFiles(List<String> files) {
		this.files = files;
	}

	public Set<String> getDesiredPredicates() {
		return this.desiredPredicates;
	}

	public void setDesiredPredicates(Set<String> desiredPredicates) {
		this.desiredPredicates = desiredPredicates;
	}

	public String getDepgraphPath() {
		return this.depgraphPath;
	}

	public void setDepgraphPath(String depgraphPath) {
		this.depgraphPath = depgraphPath;
	}

	public String getCompgraphPath() {
		return this.compgraphPath;
	}

	public void setCompgraphPath(String compgraphPath) {
		this.compgraphPath = compgraphPath;
	}

	public String getPreprocessedPath() {
		return this.preprocessedPath;
	}

	public void setPreprocessedPath(String preprocessedPath) {
		this.preprocessedPath = preprocessedPath;
	}

	public boolean isWriteAnswerSetsAsXlsx() {
		return this.writeAnswerSetsAsXlsx;
	}

	public void setWriteAnswerSetsAsXlsx(boolean writeAnswerSetsAsXslx) {
		this.writeAnswerSetsAsXlsx = writeAnswerSetsAsXslx;
	}

	public String getAnswerSetFileOutputPath() {
		return this.answerSetFileOutputPath;
	}

	public void setAnswerSetFileOutputPath(String answerSetFileOutputPath) {
		this.answerSetFileOutputPath = answerSetFileOutputPath;
	}

	public String getNormalizedPath() {
		return this.normalizedPath;
	}

	public void setNormalizedPath(String normalizedPath) {
		this.normalizedPath = normalizedPath;
	}

	public boolean isDebugPreprocessing() {
		return this.debugPreprocessing;
	}
	
	public void setDebugPreprocessing(boolean debugPreprocessing) {
		this.debugPreprocessing = debugPreprocessing;
	}

}
