/*
 * Copyright (c) 2017-2021, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.NormalProgram;
import at.ac.tuwien.kr.alpha.grounder.atoms.EnumerationAtom;

/**
 * Encapsulates all transformations necessary to transform a given program into a @{link NormalProgram} that is understood by Alpha internally
 * 
 * Copyright (c) 2019-2021, the Alpha Team.
 */
public class NormalizeProgramTransformation extends ProgramTransformation<InputProgram, NormalProgram> {

	private final boolean useNormalizationGrid;
	private final boolean ignoreDomspecHeuristics;

	public NormalizeProgramTransformation(boolean useNormalizationGrid, boolean ignoreDomspecHeuristics) {
		this.useNormalizationGrid = useNormalizationGrid;
		this.ignoreDomspecHeuristics = ignoreDomspecHeuristics;
	}

	@Override
	public NormalProgram apply(InputProgram inputProgram) {
		InputProgram tmpPrg;
		// Transform choice rules.
		tmpPrg = new ChoiceHeadToNormal().apply(inputProgram);
		// Eliminate any-sign conditions from heuristic directives.
		tmpPrg = new SignSetTransformation().apply(tmpPrg);
		// Translate heuristic directives to rules.
		tmpPrg = new HeuristicDirectiveToRule(!this.ignoreDomspecHeuristics).apply(tmpPrg);
		// Transform cardinality aggregates.
		tmpPrg = new CardinalityNormalization(!this.useNormalizationGrid).apply(tmpPrg);
		// Transform sum aggregates.
		tmpPrg = new SumNormalization().apply(tmpPrg);
		// Transform enumeration atoms.
		tmpPrg = new EnumerationRewriting().apply(tmpPrg);
		EnumerationAtom.resetEnumerations();

		// Construct the normal program.
		NormalProgram retVal = NormalProgram.fromInputProgram(tmpPrg);
		// Transform intervals - CAUTION - this MUST come before VariableEqualityRemoval!
		retVal = new IntervalTermToIntervalAtom().apply(retVal);
		// Rewrite ArithmeticTerms.
		retVal = new ArithmeticTermsRewriting().apply(retVal);
		// Remove variable equalities.
		retVal = new VariableEqualityRemoval().apply(retVal);
		return retVal;
	}

}
