package at.ac.tuwien.kr.alpha.grounder;

import at.ac.tuwien.kr.alpha.common.terms.*;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Substitution {
	private TreeMap<Variable, Term> substitution;

	private Substitution(TreeMap<Variable, Term> substitution) {
		this.substitution = substitution;
	}

	public Substitution() {
		this(new TreeMap<>());
	}

	public Substitution(Substitution clone) {
		this(new TreeMap<>(clone.substitution));
	}

	/**
	 * Computes the unifier of the atom and the instance and stores it in the variable substitution.
	 * @param atom the body atom to unify
	 * @param instance the ground instance
	 * @param substitution if the atom does not unify, this is left unchanged.
	 * @return true if the atom and the instance unify. False otherwise
	 */
	static Substitution unify(Atom atom, Instance instance, Substitution substitution) {
		for (int i = 0; i < instance.terms.size(); i++) {
			if (instance.terms.get(i) == atom.getTerms().get(i) ||
				substitution.unifyTerms(atom.getTerms().get(i), instance.terms.get(i))) {
				continue;
			}
			return null;
		}
		return substitution;
	}

	/**
	 * Checks if the left possible non-ground term unifies with the ground term.
	 * @param termNonGround
	 * @param termGround
	 * @return
	 */
	public boolean unifyTerms(Term termNonGround, Term termGround) {
		if (termNonGround == termGround) {
			// Both terms are either the same constant or the same variable term
			return true;
		} else if (termNonGround instanceof ConstantTerm) {
			// Since right term is ground, both terms differ
			return false;
		} else if (termNonGround instanceof Variable) {
			Variable variableTerm = (Variable)termNonGround;
			// Left term is variable, bind it to the right term.
			Term bound = eval(variableTerm);

			if (bound != null) {
				// Variable is already bound, return true if binding is the same as the current ground term.
				return termGround == bound;
			}

			substitution.put(variableTerm, termGround);
			return true;
		} else if (termNonGround instanceof FunctionTerm && termGround instanceof FunctionTerm) {
			// Both terms are function terms
			FunctionTerm ftNonGround = (FunctionTerm) termNonGround;
			FunctionTerm ftGround = (FunctionTerm) termGround;

			if (!(ftNonGround.getSymbol().equals(ftGround.getSymbol()))) {
				return false;
			}

			// Iterate over all subterms of both function terms
			for (int i = 0; i < ftNonGround.getTerms().size(); i++) {
				if (!unifyTerms(ftNonGround.getTerms().get(i), ftGround.getTerms().get(i))) {
					return false;
				}
			}

			return true;
		}
		return false;
	}

	/**
	 * This method should be used to obtain the {@link Term} to be used in place of
	 * a given {@link Variable} under this substitution.
	 *
	 * @param variableTerm the variable term to substitute, if possible
	 * @return a constant term if the substitution contains the given variable, {@code null} otherwise.
	 */
	public Term eval(Variable variableTerm) {
		return this.substitution.get(variableTerm);
	}

	public <T extends Comparable<T>> Term put(Variable variableTerm, Term groundTerm) {
		// Note: We're destroying type information here.
		return substitution.put(variableTerm, groundTerm);
	}

	public boolean isEmpty() {
		return substitution.isEmpty();
	}

	public Term apply(Term term) {
		if (term.isGround()) {
			return term;
		}

		if (term instanceof FunctionTerm) {
			return apply((FunctionTerm) term);
		} else if (term instanceof Variable) {
			return apply((Variable) term);
		} else if (term instanceof IntervalTerm) {
			return apply((IntervalTerm) term);
		} else {
			throw new RuntimeException("Unknown term type discovered.");
		}
	}

	public FunctionTerm apply(FunctionTerm ft) {
		if (ft.isGround()) {
			return ft;
		}

		List<Term> groundTermList = new ArrayList<>(ft.getTerms().size());
		for (Term term : ft.getTerms()) {
			groundTermList.add(apply(term));
		}
		return FunctionTerm.getInstance(ft.getSymbol(), groundTermList);
	}

	public IntervalTerm apply(IntervalTerm it) {
		if (it.isGround()) {
			return it;
		}

		throw new UnsupportedOperationException("");
		//return IntervalTerm.getInstance(apply(it.getLowerBound()), apply(it.getUpperBound()));
	}

	public Term apply(Variable variable) {
		return eval(variable);
	}

	/**
	 * Prints the variable substitution in a uniform way (sorted by variable names).
	 *
	 * @return
	 */
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		for (Map.Entry<Variable, Term> e : substitution.entrySet()) {
			ret.append("_").append(e.getKey()).append(":").append(e.getValue());
		}
		return ret.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Substitution that = (Substitution) o;

		return substitution != null ? substitution.equals(that.substitution) : that.substitution == null;
	}

	@Override
	public int hashCode() {
		return substitution != null ? substitution.hashCode() : 0;
	}

	public static Substitution findEqualizingSubstitution(BasicAtom generalAtom, BasicAtom specificAtom) {
		// Some hard examples:
		// p(A,f(A)) with p(X,A) where variable occurs as subterm again and where some variable is shared!
		// First, rename all variables in the specific
		if (!generalAtom.getPredicate().equals(specificAtom.getPredicate())) {
			return null;
		}
		Substitution specializingSubstitution = new Substitution();
		String renamedVariablePrefix = "_Vrenamed_";	// Pick prefix guaranteed to not occur in generalAtom.
		for (int i = 0; i < generalAtom.getPredicate().getArity(); i++) {
			specializingSubstitution = specializeSubstitution(specializingSubstitution,
				generalAtom.getTerms().get(i),
				specificAtom.getTerms().get(i).renameVariables(renamedVariablePrefix));
			if (specializingSubstitution == null) {
				return null;
			}
		}
		return specializingSubstitution;
	}

	private static Substitution specializeSubstitution(Substitution substitution, Term generalTerm, Term specificTerm) {
		if (generalTerm == specificTerm) {
			return substitution;
		}
		// If the general term is a variable, check its current substitution and see whether this matches the specific term.
		if (generalTerm instanceof Variable) {
			Term substitutedGeneralTerm = substitution.eval((Variable) generalTerm);
			// If the variable is not bound already, bind it to the specific term.
			if (substitutedGeneralTerm == null) {
				substitution.put((Variable) generalTerm, specificTerm);
				return substitution;
			}
			// The variable is bound, check whether its result is exactly the specific term.
			// Note: checking whether the bounded term is more general than the specific one would yield
			//       wrong results, e.g.: p(X,X) and p(f(A),f(g(B))) are incomparable, but f(A) is more general than f(g(B)).
			if (substitutedGeneralTerm != specificTerm) {
				return null;
			}
		}
		if (generalTerm instanceof FunctionTerm) {
			// Check if both given terms are function terms.
			if (!(specificTerm instanceof FunctionTerm)) {
				return null;
			}
			// Check that they are the same function.
			FunctionTerm fgeneralTerm = (FunctionTerm) generalTerm;
			FunctionTerm fspecificTerm = (FunctionTerm) specificTerm;
			if (fgeneralTerm.getSymbol() != fspecificTerm.getSymbol()
				|| fgeneralTerm.getTerms().size() != fspecificTerm.getTerms().size()) {
				return null;
			}
			// Check/specialize their subterms.
			for (int i = 0; i < fgeneralTerm.getTerms().size(); i++) {
				substitution = specializeSubstitution(substitution, fgeneralTerm, fspecificTerm);
				if (substitution == null) {
					return null;
				}
			}
		}
		if (generalTerm instanceof ConstantTerm) {
			// Equality was already checked above, so terms are different.
			return null;
		}
		throw new RuntimeException("Trying to specialize a term that is neither variable, constant, nor function. Should not happen");
	}
}
