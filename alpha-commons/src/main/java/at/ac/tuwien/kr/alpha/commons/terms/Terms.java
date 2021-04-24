package at.ac.tuwien.kr.alpha.commons.terms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.terms.ArithmeticOperator;
import at.ac.tuwien.kr.alpha.api.terms.ArithmeticTerm;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.substitutions.Unifier;
import at.ac.tuwien.kr.alpha.commons.terms.ArithmeticTermImpl.MinusTerm;

/**
 * Convenience methods for {@link Term}s. The methods provided here are an
 * attempt to avoid repeating commonly used code snippets, like wrapping sets of
 * values in {@link Term}s and creating lists of those terms, etc.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public final class Terms {

	/**
	 * Since this is purely a utility class, it may not be instantiated.
	 * 
	 * @throws AssertionError if called
	 */
	private Terms() {
		throw new AssertionError(Terms.class.getSimpleName() + " is a non-instantiable utility class!");
	}

	public static <T extends Comparable<T>> ConstantTerm<T> newConstant(T constantObject) {
		return ConstantTermImpl.getInstance(constantObject);
	}

	public static ConstantTerm<String> newSymbolicConstant(String symbol) {
		return ConstantTermImpl.getSymbolicInstance(symbol);
	}

	public static VariableTerm newVariable(String varName) {
		return VariableTermImpl.getInstance(varName);
	}

	public static VariableTerm newAnonymousVariable() {
		return VariableTermImpl.getAnonymousInstance();
	}

	public static FunctionTerm newFunctionTerm(String functionSymbol, List<Term> functionArgs) {
		return FunctionTermImpl.getInstance(functionSymbol, functionArgs);
	}

	public static FunctionTerm newFunctionTerm(String functionSmybol, Term... functionArgs) {
		return FunctionTermImpl.getInstance(functionSmybol, functionArgs);
	}

	public static Term newArithmeticTerm(Term leftOperand, ArithmeticOperator operator, Term rightOperand) {
		return ArithmeticTermImpl.getInstance(leftOperand, operator, rightOperand);
	}

	// TODO see comment in MinusTerm, should be merged with normal arithmetic term!
	public static Term newMinusTerm(Term negatedTerm) {
		return MinusTerm.getInstance(negatedTerm);
	}

	@SafeVarargs
	public static <T extends Comparable<T>> List<ConstantTerm<T>> asTermList(T... values) {
		List<ConstantTerm<T>> retVal = new ArrayList<>();
		for (T value : values) {
			retVal.add(ConstantTermImpl.getInstance(value));
		}
		return retVal;
	}

	public static List<Term> renameTerms(List<Term> terms, String prefix, int counterStartingValue) {
		List<Term> renamedTerms = new ArrayList<>(terms.size());
		AbstractTerm.RenameCounterImpl renameCounter = new AbstractTerm.RenameCounterImpl(counterStartingValue);
		for (Term term : terms) {
			renamedTerms.add(term.normalizeVariables(prefix, renameCounter));
		}
		return renamedTerms;
	}

	/**
	 * Renames variables in given set of terms.
	 * @param varNamePrefix
	 * @return
	 */
	public static Substitution renameVariables(Set<VariableTerm> terms, String varNamePrefix) {
		Unifier renamingSubstitution = new Unifier();
		int counter = 0;
		for (VariableTerm variable : terms) {
			renamingSubstitution.put(variable, Terms.newVariable(varNamePrefix + counter++));
		}
		return renamingSubstitution;
	}
	
	// TODO break this into separate class to avoid cyclic dependency Terms <-> ArithmeticTermImpl
	public static Integer evaluateGroundTerm(Term term) {
		if (!term.isGround()) {
			throw new RuntimeException("Cannot evaluate arithmetic term since it is not ground: " + term);
		}
		return evaluateGroundTermHelper(term);
	}

	// TODO break this into separate class to avoid cyclic dependency Terms <-> ArithmeticTermImpl	
	static Integer evaluateGroundTermHelper(Term term) {
		if (term instanceof ConstantTerm
				&& ((ConstantTerm<?>) term).getObject() instanceof Integer) {
			// Extract integer from the constant.
			return (Integer) ((ConstantTerm<?>) term).getObject();
		} else if (term instanceof ArithmeticTerm) {
			return ((ArithmeticTerm) term).evaluateExpression();
		} else {
			// ASP Core 2 standard allows non-integer terms in arithmetic expressions, result is to simply ignore the ground instance.
			return null;
		}
	}

}
