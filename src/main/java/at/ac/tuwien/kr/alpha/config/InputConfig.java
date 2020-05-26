package at.ac.tuwien.kr.alpha.config;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.fixedinterpretations.PredicateInterpretation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.externals.Externals;

public class InputConfig {

	public static final java.util.function.Predicate<Predicate> DEFAULT_FILTER = p -> true;
	public static final boolean DEFAULT_LITERATE = false;
	public static final int DEFAULT_NUM_ANSWER_SETS = 0;
	public static final boolean DEFAULT_WRITE_XLSX = false;
	public static final String DEFAULT_OUTFILE_PATH = "alphaAnswerSet"; // current directory, files named "alphaAnswerSet.{num}.{ext}"

	private List<String> aspStrings = new ArrayList<>();
	private List<String> files = new ArrayList<>();
	private boolean literate = InputConfig.DEFAULT_LITERATE;
	private int numAnswerSets = InputConfig.DEFAULT_NUM_ANSWER_SETS;
	private Set<String> desiredPredicates = new HashSet<>();
	// standard library externals are always loaded
	private Map<String, PredicateInterpretation> predicateMethods = Externals.getStandardLibraryExternals();
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

}
