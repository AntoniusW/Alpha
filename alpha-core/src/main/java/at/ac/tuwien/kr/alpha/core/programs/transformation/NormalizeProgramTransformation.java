package at.ac.tuwien.kr.alpha.core.programs.transformation;

import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.NormalProgram;
import at.ac.tuwien.kr.alpha.core.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.core.programs.NormalProgramImpl;
import at.ac.tuwien.kr.alpha.core.transformation.ArithmeticTermsRewriting;

/**
 * Encapsulates all transformations necessary to transform a given program into a @{link NormalProgram} that is understood by Alpha internally
 * 
 * Copyright (c) 2019-2021, the Alpha Team.
 */
public class NormalizeProgramTransformation extends ProgramTransformation<ASPCore2Program, NormalProgram> {

	private boolean useNormalizationGrid;

	public NormalizeProgramTransformation(boolean useNormalizationGrid) {
		this.useNormalizationGrid = useNormalizationGrid;
	}

	@Override
	public NormalProgram apply(ASPCore2Program inputProgram) {
		ASPCore2Program tmpPrg;
		// Transform choice rules.
		tmpPrg = new ChoiceHeadToNormal().apply(inputProgram);
		// Transform cardinality aggregates.
		tmpPrg = new CardinalityNormalization(!this.useNormalizationGrid).apply(tmpPrg);
		// Transform sum aggregates.
		tmpPrg = new SumNormalization().apply(tmpPrg);
		// Transform enumeration atoms.
		tmpPrg = new EnumerationRewriting().apply(tmpPrg);
		EnumerationAtom.resetEnumerations();

		// Construct the normal program.
		NormalProgram retVal = NormalProgramImpl.fromInputProgram(tmpPrg);
		// Transform intervals - CAUTION - this MUST come before VariableEqualityRemoval!
		retVal = new IntervalTermToIntervalAtom().apply(retVal);
		// Rewrite ArithmeticTerms.
		retVal = new ArithmeticTermsRewriting().apply(retVal);
		// Remove variable equalities.
		retVal = new VariableEqualityRemoval().apply(retVal);
		return retVal;
	}

}
