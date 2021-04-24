/**
 * Copyright (c) 2019 Siemens AG
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
package at.ac.tuwien.kr.alpha.commons.terms;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.Term.RenameCounter;
import at.ac.tuwien.kr.alpha.commons.terms.AbstractTerm.RenameCounterImpl;
import at.ac.tuwien.kr.alpha.commons.terms.ArithmeticTermImpl.MinusTerm;

/**
 * Tests {@link MinusTerm}
 */
public class TestMinusTerm {
	
	private final String renamePrefix = "Renamed";
	private final RenameCounter counter = new RenameCounterImpl(0);

	@Test
	public void testNormalizeVariablesNoVariable() {
		Term m2 = MinusTerm.getInstance(ConstantTermImpl.getInstance(2));
		assertEquals(m2, m2.normalizeVariables(renamePrefix, counter));
	}

	@Test
	public void testNormalizeVariablesWithVariable() {
		Term mX = MinusTerm.getInstance(VariableTermImpl.getInstance("X"));
		Term expected = MinusTerm.getInstance(VariableTermImpl.getInstance(renamePrefix + 0));
		assertEquals(expected, mX.normalizeVariables(renamePrefix, counter));
	}

}
