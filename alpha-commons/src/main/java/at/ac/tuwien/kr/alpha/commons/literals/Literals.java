package at.ac.tuwien.kr.alpha.commons.literals;

import at.ac.tuwien.kr.alpha.api.programs.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.ComparisonAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.ExternalAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.AggregateLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.BasicLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.ExternalLiteral;

public final class Literals {

	private Literals() {
		throw new AssertionError("Cannot instantiate utility class");
	}

	public static BasicLiteral fromAtom(BasicAtom atom, boolean positive) {
		return new BasicLiteralImpl(atom, positive);
	}

	public static AggregateLiteral fromAtom(AggregateAtom atom, boolean positive) {
		return new AggregateLiteralImpl(atom, positive);
	}

	public static ComparisonLiteral fromAtom(ComparisonAtom atom, boolean positive) {
		return new ComparisonLiteralImpl(atom, positive);
	}

	public static ExternalLiteral fromAtom(ExternalAtom atom, boolean positive) {
		return new ExternalLiteralImpl(atom, positive);
	}
	
	public static ComparisonLiteral newComparisonLiteral(ComparisonAtom atom, boolean positive) {
		return new ComparisonLiteralImpl(atom, positive);
	}
	
}
