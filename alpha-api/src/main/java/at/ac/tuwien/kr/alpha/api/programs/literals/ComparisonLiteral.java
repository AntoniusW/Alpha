package at.ac.tuwien.kr.alpha.api.programs.literals;

public interface ComparisonLiteral extends Literal, FixedInterpretationLiteral {

	boolean isLeftOrRightAssigning();

	boolean isNormalizedEquality();

}
