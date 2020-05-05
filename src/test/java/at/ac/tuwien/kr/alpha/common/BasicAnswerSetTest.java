package at.ac.tuwien.kr.alpha.common;

import at.ac.tuwien.kr.alpha.api.answersets.AtomQuery;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static at.ac.tuwien.kr.alpha.common.terms.ConstantTerm.getInstance;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Copyright (c) 2016, the Alpha Team.
 */
public class BasicAnswerSetTest {
	@Test
	public void areAnswerSetsEqual() throws Exception {
		Predicate a = Predicate.getInstance("a", 0);
		Predicate foo = Predicate.getInstance("foo", 1);
		SortedSet<Predicate> fooAndA = new TreeSet<>(asList(foo, a));

		Predicate q = Predicate.getInstance("q", 0);
		Predicate p = Predicate.getInstance("p", 1);
		SortedSet<Predicate> qAndP = new TreeSet<>(asList(q, p));

		ConstantTerm bar = getInstance("bar");
		ConstantTerm baz = getInstance("baz");

		Map<Predicate, SortedSet<Atom>> inst1 = new HashMap<>();
		inst1.put(a, new TreeSet<>(singleton(new BasicAtom(a))));
		inst1.put(foo, new TreeSet<>(asList(
				new BasicAtom(foo, bar),
				new BasicAtom(foo, baz))));
		// as1 = { a, foo(bar), foo(baz) }

		Map<Predicate, SortedSet<Atom>> inst2 = new HashMap<>();
		inst2.put(a, new TreeSet<>(singleton(new BasicAtom(a))));
		inst2.put(foo, new TreeSet<>(asList(
				new BasicAtom(foo, baz),
				new BasicAtom(foo, bar))));
		// as1 = { a, foo(baz), foo(bar) }

		Map<Predicate, SortedSet<Atom>> inst3 = new HashMap<>();
		inst3.put(q, new TreeSet<>(singleton(new BasicAtom(q))));
		inst3.put(p, new TreeSet<>(asList(
				new BasicAtom(p, bar),
				new BasicAtom(p, baz))));
		// as3 = { q, p(bar), p(baz) }

		Map<Predicate, SortedSet<Atom>> inst4 = new HashMap<>();
		inst4.put(a, new TreeSet<>(singleton(new BasicAtom(a))));
		inst4.put(foo, new TreeSet<>(asList(
				new BasicAtom(foo, bar),
				new BasicAtom(foo, baz),
				new BasicAtom(foo, getInstance("batsinga")))));
		// as4 = { a, foo(bar), foo(baz), foo(batsinga) }

		final List<AnswerSet> answerSets = asList(
				new BasicAnswerSet(fooAndA, inst1),
				new BasicAnswerSet(fooAndA, inst2),
				new BasicAnswerSet(qAndP, inst3),
				new BasicAnswerSet(fooAndA, inst4));

		assertEquals(answerSets.get(0), answerSets.get(1));
		assertEquals(answerSets.get(1), answerSets.get(0));

		assertNotEquals(answerSets.get(0), answerSets.get(2));
		assertNotEquals(answerSets.get(0), answerSets.get(3));
	}

	@Test
	public void flattenAnswerSet() {
		Predicate p0 = Predicate.getInstance("p", 0);
		Atom p0Inst = new BasicAtom(p0);
		Predicate q1 = Predicate.getInstance("q", 1);
		Atom q1Inst1 = new BasicAtom(q1, ConstantTerm.getInstance("bla"));
		Atom q1Inst2 = new BasicAtom(q1, ConstantTerm.getInstance("blub"));
		Predicate r2 = Predicate.getInstance("r", 2);
		Atom r2Inst1 = new BasicAtom(r2, ConstantTerm.getInstance("foo"), ConstantTerm.getInstance("bar"));
		SortedSet<Predicate> predicates = new TreeSet<>();
		predicates.add(p0);
		predicates.add(q1);
		predicates.add(r2);
		Map<Predicate, SortedSet<Atom>> instances = new HashMap<>();
		SortedSet<Atom> p0Instances = new TreeSet<>();
		SortedSet<Atom> q1Instances = new TreeSet<>();
		SortedSet<Atom> r2Instances = new TreeSet<>();
		p0Instances.add(p0Inst);
		q1Instances.add(q1Inst1);
		q1Instances.add(q1Inst2);
		r2Instances.add(r2Inst1);
		instances.put(p0, p0Instances);
		instances.put(q1, q1Instances);
		instances.put(r2, r2Instances);
		AnswerSet as = new BasicAnswerSet(predicates, instances);
		List<Atom> flatAs = as.flatten();
		Assert.assertTrue(flatAs.contains(p0Inst));
		Assert.assertTrue(flatAs.contains(q1Inst1));
		Assert.assertTrue(flatAs.contains(q1Inst2));
		Assert.assertTrue(flatAs.contains(r2Inst1));
	}

	@Test
	public void queryAnswerSet() {
		Predicate p0 = Predicate.getInstance("p", 0);
		Atom p0Inst = new BasicAtom(p0);
		Predicate q1 = Predicate.getInstance("q", 1);
		Atom q1Inst1 = new BasicAtom(q1, ConstantTerm.getInstance("bla"));
		Atom q1Inst2 = new BasicAtom(q1, ConstantTerm.getInstance("blub"));
		Predicate r2 = Predicate.getInstance("r", 2);
		Atom r2Inst1 = new BasicAtom(r2, ConstantTerm.getInstance("foo"), ConstantTerm.getInstance("bar"));
		SortedSet<Predicate> predicates = new TreeSet<>();
		predicates.add(p0);
		predicates.add(q1);
		predicates.add(r2);
		Map<Predicate, SortedSet<Atom>> instances = new HashMap<>();
		SortedSet<Atom> p0Instances = new TreeSet<>();
		SortedSet<Atom> q1Instances = new TreeSet<>();
		SortedSet<Atom> r2Instances = new TreeSet<>();
		p0Instances.add(p0Inst);
		q1Instances.add(q1Inst1);
		q1Instances.add(q1Inst2);
		r2Instances.add(r2Inst1);
		instances.put(p0, p0Instances);
		instances.put(q1, q1Instances);
		instances.put(r2, r2Instances);
		AnswerSet as = new BasicAnswerSet(predicates, instances);
		AtomQuery query = AtomQuery.forPredicate(Predicate.getInstance("q", 1)).withStringEquals(0, "blub");
		List<Atom> queryResult = as.query(query);
		Assert.assertEquals(1, queryResult.size());
		Assert.assertTrue(queryResult.contains(q1Inst2));
	}
}