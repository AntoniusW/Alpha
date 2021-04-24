package at.ac.tuwien.kr.alpha.api;

import at.ac.tuwien.kr.alpha.api.programs.NormalProgram;
import at.ac.tuwien.kr.alpha.api.programs.analysis.ComponentGraph;
import at.ac.tuwien.kr.alpha.api.programs.analysis.DependencyGraph;

public interface DebugSolvingContext {
	
	NormalProgram getNormalizedProgram();
	
	NormalProgram getPreprocessedProgram();
	
	DependencyGraph getDependencyGraph();
	
	ComponentGraph getComponentGraph();
	
	Solver getSolver();

}
