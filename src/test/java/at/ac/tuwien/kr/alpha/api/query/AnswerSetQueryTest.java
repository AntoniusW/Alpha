package at.ac.tuwien.kr.alpha.api.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.AnswerSetBuilder;
import at.ac.tuwien.kr.alpha.common.BasicAnswerSet;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;

public class AnswerSetQueryTest {

	@Test
	public void matchPredicate() {
		AnswerSetBuilder bld = new AnswerSetBuilder();
		//@formatter:off
		bld.predicate("p")
				.symbolicInstance("a")
				.symbolicInstance("b")
			.predicate("q")
				.symbolicInstance("x");
		//@formatter:on
		AnswerSet as = bld.build();
		List<Atom> queryResult = as.query(AnswerSetQuery.forPredicate(Predicate.getInstance("p", 1)));
		Assert.assertEquals(2, queryResult.size());
	}

	@Test
	public void matchSymbolicConstant() {
		AnswerSetBuilder bld = new AnswerSetBuilder();
		bld.predicate("p")
				.symbolicInstance("a")
				.instance("a");
		AnswerSet as = bld.build();
		AnswerSetQuery constantQuery = AnswerSetQuery
				.forPredicate(Predicate.getInstance("p", 1))
				.withConstantEquals(0, "a");
		List<Atom> queryResult = as.query(constantQuery);
		Assert.assertEquals(1, queryResult.size());
	}

	@Test
	public void matchString() {
		AnswerSetBuilder bld = new AnswerSetBuilder();
		bld.predicate("p")
				.symbolicInstance("a")
				.instance("a");
		AnswerSet as = bld.build();
		AnswerSetQuery stringQuery = AnswerSetQuery
				.forPredicate(Predicate.getInstance("p", 1))
				.withStringEquals(0, "a");
		List<Atom> queryResult = as.query(stringQuery);
		Assert.assertEquals(1, queryResult.size());
	}

	@Test
	public void matchEvenIntegers() {
		AnswerSetBuilder bld = new AnswerSetBuilder();
		bld.predicate("p")
				.instance(1).instance(2).instance(3).instance(4).instance(5)
				.instance("bla").symbolicInstance("blubb");
		AnswerSet as = bld.build();
		java.util.function.Predicate<Term> isInteger = (term) -> {
			if (!(term instanceof ConstantTerm<?>)) {
				return false;
			}
			String strValue = ((ConstantTerm<?>) term).getObject().toString();
			return strValue.matches("[0-9]+");
		};
		AnswerSetQuery evenIntegers = AnswerSetQuery.forPredicate(Predicate.getInstance("p", 1))
				.withFilter(0, isInteger.and(
						(term) -> Integer.valueOf(((ConstantTerm<?>) term).getObject().toString()) % 2 == 0));
		List<Atom> queryResult = as.query(evenIntegers);
		Assert.assertEquals(2, queryResult.size());
		for (Atom atom : queryResult) {
			ConstantTerm<?> term = (ConstantTerm<?>) atom.getTerms().get(0);
			Assert.assertTrue(Integer.valueOf(term.getObject().toString()) % 2 == 0);
		}
	}

	@Test
	public void matchXWithFuncTerm() {
		Predicate p = Predicate.getInstance("p", 2);
		Atom a1 = new BasicAtom(p, ConstantTerm.getSymbolicInstance("x"), FunctionTerm.getInstance("f", ConstantTerm.getSymbolicInstance("x")));
		Atom a2 = new BasicAtom(p, ConstantTerm.getSymbolicInstance("y"), FunctionTerm.getInstance("f", ConstantTerm.getSymbolicInstance("y")));
		Atom a3 = new BasicAtom(p, ConstantTerm.getSymbolicInstance("y"), FunctionTerm.getInstance("f", ConstantTerm.getSymbolicInstance("x")));
		Atom a4 = new BasicAtom(p, ConstantTerm.getSymbolicInstance("x"), FunctionTerm.getInstance("f", ConstantTerm.getSymbolicInstance("y")));
		Atom a5 = new BasicAtom(p, ConstantTerm.getSymbolicInstance("x"), ConstantTerm.getSymbolicInstance("f"));
		SortedSet<Predicate> predicates = new TreeSet<>();
		predicates.add(p);
		Map<Predicate, SortedSet<Atom>> instances = new HashMap<>();
		SortedSet<Atom> ps = new TreeSet<>();
		ps.add(a1);
		ps.add(a2);
		ps.add(a3);
		ps.add(a4);
		ps.add(a5);
		instances.put(p, ps);
		AnswerSet as = new BasicAnswerSet(predicates, instances);
		AnswerSetQuery query = AnswerSetQuery.forPredicate(Predicate.getInstance("p", 2)).withConstantEquals(0, "x").withFunctionTerm(1, "f", 1);
		List<Atom> queryResult = as.query(query);
		Assert.assertEquals(2, queryResult.size());
	}

}
