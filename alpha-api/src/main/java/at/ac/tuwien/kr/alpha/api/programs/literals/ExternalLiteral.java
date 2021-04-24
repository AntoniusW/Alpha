package at.ac.tuwien.kr.alpha.api.programs.literals;

import at.ac.tuwien.kr.alpha.api.programs.atoms.ExternalAtom;

public interface ExternalLiteral extends FixedInterpretationLiteral {

	@Override
	ExternalAtom getAtom();
	
}
