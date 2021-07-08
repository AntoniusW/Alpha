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
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link HeuristicDirectiveValues.PriorityComparator}.
 */
public class PriorityComparatorTest {

	private final HeuristicDirectiveValues.PriorityComparator comparator = new HeuristicDirectiveValues.PriorityComparator();

	@Test
	public void testAllEqual() {
		final BasicAtom headAtom = new BasicAtom(Predicate.getInstance("a", 0));
		final int headAtomId = 1;
		final int weight = 10;
		final int level = 5;
		final boolean sign = true;
		HeuristicDirectiveValues v1 = new HeuristicDirectiveValues(headAtomId, headAtom, weight, level, sign);
		HeuristicDirectiveValues v2 = new HeuristicDirectiveValues(headAtomId, headAtom, weight, level, sign);
		assertEquals(0, comparator.compare(v1, v2));
		assertEquals(0, comparator.compare(v2, v1));
	}

	@Test
	public void testDifferentWeight() {
		final BasicAtom headAtom = new BasicAtom(Predicate.getInstance("a", 0));
		final int headAtomId = 1;
		final int weight1 = 10;
		final int weight2 = 11;
		final int level = 5;
		final boolean sign = true;
		HeuristicDirectiveValues v1 = new HeuristicDirectiveValues(headAtomId, headAtom, weight1, level, sign);
		HeuristicDirectiveValues v2 = new HeuristicDirectiveValues(headAtomId, headAtom, weight2, level, sign);
		assertTrue(comparator.compare(v1, v2) < 0);
		assertTrue(comparator.compare(v2, v1) > 0);
	}

	@Test
	public void testDifferentLevel() {
		final BasicAtom headAtom = new BasicAtom(Predicate.getInstance("a", 0));
		final int headAtomId = 1;
		final int weight = 10;
		final int level1 = 5;
		final int level2 = 10;
		final boolean sign = true;
		HeuristicDirectiveValues v1 = new HeuristicDirectiveValues(headAtomId, headAtom, weight, level1, sign);
		HeuristicDirectiveValues v2 = new HeuristicDirectiveValues(headAtomId, headAtom, weight, level2, sign);
		assertTrue(comparator.compare(v1, v2) < 0);
		assertTrue(comparator.compare(v2, v1) > 0);
	}

	@Test
	public void testDifferentSign() {
		final BasicAtom headAtom = new BasicAtom(Predicate.getInstance("a", 0));
		final int headAtomId = 1;
		final int weight = 10;
		final int level = 5;
		final boolean sign1 = false;
		final boolean sign2 = true;
		HeuristicDirectiveValues v1 = new HeuristicDirectiveValues(headAtomId, headAtom, weight, level, sign1);
		HeuristicDirectiveValues v2 = new HeuristicDirectiveValues(headAtomId, headAtom, weight, level, sign2);
		assertTrue(comparator.compare(v1, v2) < 0);
		assertTrue(comparator.compare(v2, v1) > 0);
	}

	@Test
	public void testDifferentHeadAtomId() {
		final BasicAtom headAtom1 = new BasicAtom(Predicate.getInstance("a", 0));
		final BasicAtom headAtom2 = new BasicAtom(Predicate.getInstance("b", 0));
		final int headAtomId1 = 1;
		final int headAtomId2 = 2;
		final int weight = 10;
		final int level = 5;
		final boolean sign = true;
		HeuristicDirectiveValues v1 = new HeuristicDirectiveValues(headAtomId1, headAtom1, weight, level, sign);
		HeuristicDirectiveValues v2 = new HeuristicDirectiveValues(headAtomId2, headAtom2, weight, level, sign);
		assertTrue(comparator.compare(v1, v2) < 0);
		assertTrue(comparator.compare(v2, v1) > 0);
	}

}
