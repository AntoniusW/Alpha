/*
 * Copyright (c) 2019-2021 Siemens AG
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.common.heuristics;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests {@link HeuristicDirectiveValues}
 */
public class HeuristicDirectiveValuesTest {

	@Test
	public void testToString_positive() {
		final int headAtomId = 2;
		final int weight = 5;
		final int level = 3;
		HeuristicDirectiveValues values = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight, level, true);
		assertEquals(String.format("T %d [%d@%d]", headAtomId, weight, level), values.toString());
	}

	@Test
	public void testToString_negative() {
		final int headAtomId = 26;
		final int weight = 2;
		final int level = 4;
		HeuristicDirectiveValues values = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight, level, false);
		assertEquals(String.format("F %d [%d@%d]", headAtomId, weight, level), values.toString());
	}

	@Test
	public void testEquals() {
		final int headAtomId = 2;
		final int weight = 5;
		final int level = 3;
		HeuristicDirectiveValues values1 = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight, level, true);
		HeuristicDirectiveValues values2 = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight, level, true);
		assertEquals(values1, values2);
		assertEquals(values1.hashCode(), values2.hashCode());
	}

	@Test
	public void testNotEquals_weight() {
		final int headAtomId = 2;
		final int weight1 = 5;
		final int weight2 = 6;
		final int level = 3;
		HeuristicDirectiveValues values1 = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight1, level, true);
		HeuristicDirectiveValues values2 = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight2, level, true);
		assertNotEquals(values1, values2);
		assertNotEquals(values1.hashCode(), values2.hashCode());
	}

	@Test
	public void testNotEquals_level() {
		final int headAtomId = 2;
		final int weight = 5;
		final int level1 = 3;
		final int level2 = 2;
		HeuristicDirectiveValues values1 = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight, level1, true);
		HeuristicDirectiveValues values2 = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight, level2, true);
		assertNotEquals(values1, values2);
		assertNotEquals(values1.hashCode(), values2.hashCode());
	}

	@Test
	public void testNotEquals_headAtomId() {
		final int headAtomId1 = 2;
		final int headAtomId2 = 27;
		final int weight = 5;
		final int level = 3;
		HeuristicDirectiveValues values1 = new HeuristicDirectiveValues(headAtomId1, new BasicAtom(Predicate.getInstance("a", 0)), weight, level, true);
		HeuristicDirectiveValues values2 = new HeuristicDirectiveValues(headAtomId2, new BasicAtom(Predicate.getInstance("a", 0)), weight, level, true);
		assertNotEquals(values1, values2);
		assertNotEquals(values1.hashCode(), values2.hashCode());
	}

	@Test
	public void testNotEquals_sign() {
		final int headAtomId = 2;
		final int weight = 5;
		final int level = 3;
		HeuristicDirectiveValues values1 = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight, level, true);
		HeuristicDirectiveValues values2 = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight, level, false);
		assertNotEquals(values1, values2);
		assertNotEquals(values1.hashCode(), values2.hashCode());
	}

	@Test
	public void testNotEquals_null() {
		final int headAtomId = 2;
		final int weight = 5;
		final int level = 3;
		HeuristicDirectiveValues values1 = new HeuristicDirectiveValues(headAtomId, new BasicAtom(Predicate.getInstance("a", 0)), weight, level, true);
		HeuristicDirectiveValues values2 = null;
		assertNotEquals(values1, values2);
	}
}
