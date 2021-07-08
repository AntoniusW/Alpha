/**
 * Copyright (c) 2016-2019, the Alpha Team.
 * All rights reserved.
 *
 * Additional changes made by Siemens.
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
package at.ac.tuwien.kr.alpha.common;

import org.junit.Test;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static at.ac.tuwien.kr.alpha.common.Literals.atomToLiteral;
import static at.ac.tuwien.kr.alpha.common.Literals.atomToNegatedLiteral;
import static org.junit.Assert.*;

public class NoGoodTest {
	/**
	 * Constructs an array of literals with the new representation (least-significant bit is polarity, other bits
	 * are atom id) from an array of literals from the old representation (positive literal is atom id, negative
	 * literal is negated atom id).
	 * @param literals
	 * @return
	 */
	public static int[] fromOldLiterals(int... literals) {
		int[] newLiterals = new int[literals.length];
		for (int i = 0; i < literals.length; i++) {
			newLiterals[i] = literals[i] >= 0 ? atomToLiteral(literals[i]) : atomToNegatedLiteral(-literals[i]);
		}
		return newLiterals;
	}

	public static int fromOldLiterals(int literal) {
			return literal >= 0 ? atomToLiteral(literal) : atomToNegatedLiteral(-literal);
	}

	@Test
	public void iteration() throws Exception {
		Iterator<Integer> i = new NoGood(1).iterator();
		assertEquals(1, (int)i.next());
		assertFalse(i.hasNext());
	}

	@Test(expected = NullPointerException.class)
	public void compareToNull() throws Exception {
		new NoGood().compareTo(null);
	}

	@Test
	public void compareToSame() throws Exception {
		assertEquals(0, new NoGood(1).compareTo(new NoGood(1)));
	}

	@Test
	public void compareToLengthShort() throws Exception {
		assertEquals(-1, new NoGood(1).compareTo(new NoGood(1, 2)));
	}

	@Test
	public void compareToLengthLong() throws Exception {
		assertEquals(+1, new NoGood(1, 2).compareTo(new NoGood(1)));
	}

	@Test
	public void compareToLexicographicSmall() throws Exception {
		assertEquals(-1, new NoGood(1, 2).compareTo(new NoGood(2, 3)));
	}

	@Test
	public void compareToLexicographicBig() throws Exception {
		assertEquals(+1, new NoGood(2, 3).compareTo(new NoGood(1, 2)));
	}

	@Test
	public void deleteDuplicates() {
		NoGood ng = NoGoodCreator.headFirst(fromOldLiterals(-3, 1, -2, -2));
		assertEquals("Duplicate entry must be removed.", 3, ng.size());
		assertEquals(fromOldLiterals(-3), ng.getLiteral(0));
		assertEquals(fromOldLiterals(1), ng.getLiteral(1));
		assertEquals(fromOldLiterals(-2), ng.getLiteral(2));

		NoGood ng2 = NoGoodCreator.headFirst(fromOldLiterals(-2, 3, 3, -6, -1, 5, 5, -6, 7));
		assertEquals("Duplicate entries must be removed.", 6, ng2.size());
		assertEquals(fromOldLiterals(-2), ng2.getLiteral(0));
		assertEquals(fromOldLiterals(-1), ng2.getLiteral(1));
		assertEquals(fromOldLiterals(3), ng2.getLiteral(2));
		assertEquals(fromOldLiterals(5), ng2.getLiteral(3));
		assertEquals(fromOldLiterals(-6), ng2.getLiteral(4));
		assertEquals(fromOldLiterals(7), ng2.getLiteral(5));

		NoGood ng3 = NoGoodCreator.headFirst(fromOldLiterals(-1, 2, -3, -4));
		assertEquals("NoGood contains no duplicates, size must stay the same.", 4, ng3.size());
		assertEquals(fromOldLiterals(-1), ng3.getLiteral(0));
		assertEquals(fromOldLiterals(2), ng3.getLiteral(1));
		assertEquals(fromOldLiterals(-3), ng3.getLiteral(2));
		assertEquals(fromOldLiterals(-4), ng3.getLiteral(3));

	}

	@Test
	public void noGoodsInHashMap() {
		NoGood ng1 = NoGoodCreator.headFirst(-1, 2, -3, -4);
		NoGood ng2 = NoGoodCreator.headFirst(-1, 2, -4);
		NoGood ng3 = NoGoodCreator.headFirst(-1, 2, -3, -4);
		Map<NoGood, Integer> noGoodIdentifiers = new LinkedHashMap<>();
		noGoodIdentifiers.put(ng1, 1);
		noGoodIdentifiers.put(ng1, 2);
		noGoodIdentifiers.put(ng2, 4);
		assertTrue(noGoodIdentifiers.containsKey(ng3));
		noGoodIdentifiers.put(ng3, 5);
		assertEquals(2, noGoodIdentifiers.size());
	}
}