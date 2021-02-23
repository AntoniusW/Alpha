package at.ac.tuwien.kr.alpha.grounder.instantiation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;
import at.ac.tuwien.kr.alpha.api.program.Literal;
import at.ac.tuwien.kr.alpha.api.program.Predicate;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.core.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.core.atoms.ComparisonAtom;
import at.ac.tuwien.kr.alpha.core.atoms.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.core.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.core.atoms.EnumerationLiteral;
import at.ac.tuwien.kr.alpha.core.common.ComparisonOperatorImpl;
import at.ac.tuwien.kr.alpha.core.common.CorePredicate;
import at.ac.tuwien.kr.alpha.core.grounder.SubstitutionImpl;
import at.ac.tuwien.kr.alpha.core.grounder.WorkingMemory;
import at.ac.tuwien.kr.alpha.core.grounder.instantiation.AssignmentStatus;
import at.ac.tuwien.kr.alpha.core.grounder.instantiation.LiteralInstantiationResult;
import at.ac.tuwien.kr.alpha.core.grounder.instantiation.LiteralInstantiator;
import at.ac.tuwien.kr.alpha.core.grounder.instantiation.WorkingMemoryBasedInstantiationStrategy;

public class LiteralInstantiatorTest {

	@Test
	public void instantiateSatisfiedFixedInterpretationLiteral() {
		ComparisonAtom equalsThree = new ComparisonAtom(Terms.newConstant(3), Terms.newVariable("THREE"), ComparisonOperatorImpl.EQ);
		Literal lit = new ComparisonLiteral(equalsThree, true);
		Substitution substitution = new SubstitutionImpl();
		LiteralInstantiator instantiator = new LiteralInstantiator(new WorkingMemoryBasedInstantiationStrategy(null));
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution);
		Assert.assertEquals(LiteralInstantiationResult.Type.CONTINUE, result.getType());
		List<ImmutablePair<Substitution, AssignmentStatus>> resultSubstitutions = result.getSubstitutions();
		Assert.assertEquals(1, resultSubstitutions.size());
		Assert.assertEquals(AssignmentStatus.TRUE, resultSubstitutions.get(0).right);
		Substitution extendedSubstitution = resultSubstitutions.get(0).left;
		Assert.assertTrue(extendedSubstitution.isVariableSet(Terms.newVariable("THREE")));
		Assert.assertEquals(Terms.newConstant(3), extendedSubstitution.eval(Terms.newVariable("THREE")));
	}

	@Test
	public void instantiateUnsatisfiedFixedInterpretationLiteral() {
		ComparisonAtom fiveEqualsThree = new ComparisonAtom(Terms.newVariable("FIVE"), Terms.newVariable("THREE"), ComparisonOperatorImpl.EQ);
		Literal lit = new ComparisonLiteral(fiveEqualsThree, true);
		Substitution substitution = new SubstitutionImpl();
		substitution.put(Terms.newVariable("FIVE"), Terms.newConstant(5));
		substitution.put(Terms.newVariable("THREE"), Terms.newConstant(3));
		LiteralInstantiator instantiator = new LiteralInstantiator(new WorkingMemoryBasedInstantiationStrategy(null));
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution);
		Assert.assertEquals(LiteralInstantiationResult.Type.STOP_BINDING, result.getType());
	}

	@Test
	public void instantiateEnumLiteral() {
		VariableTerm enumTerm = Terms.newVariable("E");
		VariableTerm idTerm = Terms.newVariable("X");
		VariableTerm indexTerm = Terms.newVariable("I");
		List<Term> termList = new ArrayList<>();
		termList.add(enumTerm);
		termList.add(idTerm);
		termList.add(indexTerm);
		EnumerationAtom enumAtom = new EnumerationAtom(termList);
		EnumerationLiteral lit = new EnumerationLiteral(enumAtom);
		Substitution substitution = new SubstitutionImpl();
		substitution.put(enumTerm, Terms.newSymbolicConstant("enum1"));
		substitution.put(idTerm, Terms.newSymbolicConstant("someElement"));
		LiteralInstantiator instantiator = new LiteralInstantiator(new WorkingMemoryBasedInstantiationStrategy(null));
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution);
		Assert.assertEquals(LiteralInstantiationResult.Type.CONTINUE, result.getType());
		List<ImmutablePair<Substitution, AssignmentStatus>> resultSubstitutions = result.getSubstitutions();
		Assert.assertEquals(1, resultSubstitutions.size());
		Assert.assertEquals(AssignmentStatus.TRUE, resultSubstitutions.get(0).right);
		Assert.assertTrue(resultSubstitutions.get(0).left.isVariableSet(indexTerm));
	}

	@Test
	public void workingMemoryBasedVerifyPositiveGroundLiteralSatisfied() {
		Predicate p = CorePredicate.getInstance("p", 2);
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		workingMemory.addInstance(new BasicAtom(p, Terms.newSymbolicConstant("x"), Terms.newSymbolicConstant("y")), true);
		VariableTerm x = Terms.newVariable("X");
		VariableTerm y = Terms.newVariable("Y");
		Literal lit = new BasicLiteral(new BasicAtom(p, x, y), true);
		Substitution substitution = new SubstitutionImpl();
		substitution.put(x, Terms.newSymbolicConstant("x"));
		substitution.put(y, Terms.newSymbolicConstant("y"));
		LiteralInstantiator instantiator = new LiteralInstantiator(new WorkingMemoryBasedInstantiationStrategy(workingMemory));
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution);
		Assert.assertEquals(LiteralInstantiationResult.Type.CONTINUE, result.getType());
		List<ImmutablePair<Substitution, AssignmentStatus>> substitutions = result.getSubstitutions();
		Assert.assertEquals(1, substitutions.size());
		Assert.assertEquals(AssignmentStatus.TRUE, substitutions.get(0).right);
		Substitution resultSubstitution = substitutions.get(0).left;
		// With the given input substitution, lit is ground and satisfied -
		// we expect the instantiator to verify that.
		Assert.assertEquals(substitution, resultSubstitution);
	}

	@Test
	public void workingMemoryBasedVerifyPositiveGroundLiteralUnsatisfied() {
		Predicate p = CorePredicate.getInstance("p", 2);
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		VariableTerm x = Terms.newVariable("X");
		VariableTerm y = Terms.newVariable("Y");
		Literal lit = new BasicLiteral(new BasicAtom(p, x, y), true);
		Substitution substitution = new SubstitutionImpl();
		substitution.put(x, Terms.newSymbolicConstant("x"));
		substitution.put(y, Terms.newSymbolicConstant("y"));
		LiteralInstantiator instantiator = new LiteralInstantiator(new WorkingMemoryBasedInstantiationStrategy(workingMemory));
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution);
		// With the given input substitution, lit is ground, but not satisfied -
		// we expect the instantiator to verify that and return an empty list of
		// substitutions.
		Assert.assertEquals(LiteralInstantiationResult.Type.STOP_BINDING, result.getType());
	}

	@Test
	public void workingMemoryBasedInstantiatePositiveBasicLiteral() {
		Predicate p = CorePredicate.getInstance("p", 2);
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		workingMemory.addInstance(new BasicAtom(p, Terms.newSymbolicConstant("x"), Terms.newSymbolicConstant("y")), true);
		workingMemory.addInstance(new BasicAtom(p, Terms.newSymbolicConstant("x"), Terms.newSymbolicConstant("z")), true);
		VariableTerm x = Terms.newVariable("X");
		VariableTerm y = Terms.newVariable("Y");
		Literal lit = new BasicLiteral(new BasicAtom(p, x, y), true);
		Substitution substitution = new SubstitutionImpl();
		substitution.put(x, Terms.newSymbolicConstant("x"));
		LiteralInstantiator instantiator = new LiteralInstantiator(new WorkingMemoryBasedInstantiationStrategy(workingMemory));
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution);
		Assert.assertEquals(LiteralInstantiationResult.Type.CONTINUE, result.getType());
		List<ImmutablePair<Substitution, AssignmentStatus>> substitutions = result.getSubstitutions();
		Assert.assertEquals(2, substitutions.size());
		boolean ySubstituted = false;
		boolean zSubstituted = false;
		for (ImmutablePair<Substitution, AssignmentStatus> resultSubstitution : substitutions) {
			Assert.assertTrue(resultSubstitution.left.isVariableSet(y));
			Assert.assertEquals(AssignmentStatus.TRUE, resultSubstitution.right);
			if (resultSubstitution.left.eval(y).equals(Terms.newSymbolicConstant("y"))) {
				ySubstituted = true;
			} else if (resultSubstitution.left.eval(y).equals(Terms.newSymbolicConstant("z"))) {
				zSubstituted = true;
			} else {
				Assert.fail("Invalid substitution for variable Y");
			}
		}
		Assert.assertTrue(ySubstituted && zSubstituted);
	}
}
