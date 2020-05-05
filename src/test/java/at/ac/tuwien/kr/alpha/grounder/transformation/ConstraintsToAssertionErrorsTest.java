package at.ac.tuwien.kr.alpha.grounder.transformation;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.common.DisjunctiveHead;
import at.ac.tuwien.kr.alpha.common.Head;
import at.ac.tuwien.kr.alpha.common.Program;
import at.ac.tuwien.kr.alpha.common.Rule;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;

public class ConstraintsToAssertionErrorsTest {

	@Test
	@SuppressWarnings("unchecked")
	public void singleConstraint() {
		Alpha alpha = new Alpha();
		String asp = ":- p(X), q(Y), not r(X, Y).";
		Program prog = alpha.readProgramString(asp, null);
		Program transformed = new ConstraintsToAssertionErrors().apply(prog);
		Assert.assertEquals(1, transformed.getRules().size());
		Rule transformedConstraint = transformed.getRules().get(0);
		Assert.assertFalse(transformedConstraint.isConstraint());
		Head assertionErrorHead = transformedConstraint.getHead();
		Assert.assertTrue(assertionErrorHead.isNormal());
		Atom headAtom = ((DisjunctiveHead) assertionErrorHead).disjunctiveAtoms.get(0);
		Assert.assertEquals(3, headAtom.getTerms().size());
		Assert.assertEquals(VariableTerm.getInstance("X"), headAtom.getTerms().get(0));
		Assert.assertEquals(VariableTerm.getInstance("Y"), headAtom.getTerms().get(1));
		Assert.assertEquals(asp, ((ConstantTerm<String>) headAtom.getTerms().get(2)).getObject());
	}

}
