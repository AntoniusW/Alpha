package at.ac.tuwien.kr.alpha.api.answersets;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;

public class AtomQueryTest {

	@Test
	public void singleAtomMatchPredicate() {
		Predicate matchingPredicate = Predicate.getInstance("this_one", 2);
		Predicate otherPredicate = Predicate.getInstance("that_one", 3);
		Atom matchingAtom = new BasicAtom(matchingPredicate, ConstantTerm.getInstance("bla"), ConstantTerm.getInstance("blubb"));
		Atom nonMatchingAtom = new BasicAtom(otherPredicate, ConstantTerm.getInstance("foo"), ConstantTerm.getInstance("bar"), ConstantTerm.getInstance("baz"));
		AtomQuery query = AtomQuery.forPredicate(matchingPredicate);
		Assert.assertTrue(query.test(matchingAtom));
		Assert.assertFalse(query.test(nonMatchingAtom));
	}

	@Test
	public void singleAtomMatchString() {
		Predicate p = Predicate.getInstance("p", 2);
		Atom matchingAtom = new BasicAtom(p, ConstantTerm.getInstance("a string constant"), ConstantTerm.getInstance("blaaa"));
		Atom nonMatchingAtom = new BasicAtom(p, ConstantTerm.getInstance("another string constant"), ConstantTerm.getInstance("blubb"));
		AtomQuery query = AtomQuery.forPredicate(p).withStringEquals(0, "a string constant");
		Assert.assertTrue(query.test(matchingAtom));
		Assert.assertFalse(query.test(nonMatchingAtom));
	}

	@Test
	public void singleAtomMatchConstantSymbol() {
		Predicate p = Predicate.getInstance("p", 1);
		Atom matchingAtom = new BasicAtom(p, ConstantTerm.getSymbolicInstance("symbol")); // plain constant in ASP
		Atom nonMatchingAtom = new BasicAtom(p, ConstantTerm.getInstance("symbol")); // quoted string in ASP
		AtomQuery query = AtomQuery.forPredicate(p).withConstantSymbolEquals(0, "symbol");
		Assert.assertTrue(query.test(matchingAtom));
		Assert.assertFalse(query.test(nonMatchingAtom));
	}

	@Test
	public void singleAtomMatchTerm() {
		Predicate p = Predicate.getInstance("p", 3);
		Term termToMatch = FunctionTerm.getInstance("foo", ConstantTerm.getInstance(42));
		Atom matchingAtom = new BasicAtom(p, ConstantTerm.getInstance("bla"), ConstantTerm.getInstance(123), termToMatch);
		Atom nonMatchingAtom = new BasicAtom(p, ConstantTerm.getInstance("bla"), ConstantTerm.getInstance(123),
				FunctionTerm.getInstance("foo", ConstantTerm.getInstance(1312)));
		AtomQuery query = AtomQuery.forPredicate(p).withTermEquals(2, FunctionTerm.getInstance("foo", ConstantTerm.getInstance(42)));
		Assert.assertTrue(query.test(matchingAtom));
		Assert.assertFalse(query.test(nonMatchingAtom));
	}

	@Test
	public void singleAtomMatchMultipleTerms() {
		Predicate p = Predicate.getInstance("p", 3);
		Atom matchingAtom = new BasicAtom(p, ConstantTerm.getInstance("foo"), ConstantTerm.getInstance("bar"), ConstantTerm.getInstance(10));
		Atom nonMatchingAtom = new BasicAtom(p, ConstantTerm.getInstance("foo"), ConstantTerm.getInstance("blaa"), ConstantTerm.getInstance(11));
		java.util.function.Predicate<Term> intEven = (term) -> {
			if (!(term instanceof ConstantTerm<?>)) {
				return false;
			}
			ConstantTerm<?> constTerm = (ConstantTerm<?>) term;
			if (!(constTerm.getObject() instanceof Integer)) {
				return false;
			}
			int value = (Integer) constTerm.getObject();
			return value % 2 == 0;
		};
		AtomQuery query = AtomQuery.forPredicate(p).withStringEquals(0, "foo").withFilter(2, intEven);
		Assert.assertTrue(query.test(matchingAtom));
		Assert.assertFalse(query.test(nonMatchingAtom));
	}

	@Test
	public void singleAtomMultipleFiltersPerTerm() {
		Predicate p = Predicate.getInstance("p", 1);
		Atom matchingAtom = new BasicAtom(p, ConstantTerm.getInstance("A"));
		Atom nonMatchingAtom = new BasicAtom(p, ConstantTerm.getInstance("a"));
		java.util.function.Predicate<Term> stringStartsUpperCase = (term) -> {
			if (!(term instanceof ConstantTerm<?>)) {
				return false;
			}
			ConstantTerm<?> constTerm = (ConstantTerm<?>) term;
			if (!(constTerm.getObject() instanceof String)) {
				return false;
			}
			String value = (String) constTerm.getObject();
			return value.matches("[A-Z].*");
		};
		java.util.function.Predicate<Term> stringLengthOne = (term) -> {
			if (!(term instanceof ConstantTerm<?>)) {
				return false;
			}
			ConstantTerm<?> constTerm = (ConstantTerm<?>) term;
			if (!(constTerm.getObject() instanceof String)) {
				return false;
			}
			String value = (String) constTerm.getObject();
			return value.length() == 1;
		};
		AtomQuery query = AtomQuery.forPredicate(p).withFilter(0, stringStartsUpperCase).withFilter(0, stringLengthOne);
		Assert.assertTrue(query.test(matchingAtom));
		Assert.assertFalse(query.test(nonMatchingAtom));
	}

}
