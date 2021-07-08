package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.grounder.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.AggregateRewriting;

/**
 * Encapsulates all transformations necessary to transform a given program into a @{link NormalProgram} that is
 * understood by Alpha internally
 * 
 * Copyright (c) 2019-2020, the Alpha Team.
 */
public class NormalizeProgramTransformation extends ProgramTransformation<InputProgram, NormalProgram> {

	private boolean useNormalizationGrid;

	public NormalizeProgramTransformation(boolean useNormalizationGrid) {
		this.useNormalizationGrid = useNormalizationGrid;
	}

	@Override
	public NormalProgram apply(InputProgram inputProgram) {
		InputProgram tmpPrg;
		// Remove variable equalities.
		tmpPrg = new VariableEqualityRemoval().apply(inputProgram);
		// Transform choice rules.
		tmpPrg = new ChoiceHeadToNormal().apply(tmpPrg);
		// Transform cardinality aggregates.
		tmpPrg = new AggregateRewriting(!useNormalizationGrid).apply(tmpPrg);
		// Transform enumeration atoms.
		tmpPrg = new EnumerationRewriting().apply(tmpPrg);
		EnumerationAtom.resetEnumerations();

		// Construct the normal program.
		NormalProgram retVal = NormalProgram.fromInputProgram(tmpPrg);
		// Transform intervals.
		retVal = new IntervalTermToIntervalAtom().apply(retVal);
		// Rewrite ArithmeticTerms.
		retVal = new ArithmeticTermsRewriting().apply(retVal);
		return retVal;
	}

}
