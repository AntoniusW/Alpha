/**
 * Copyright (c) 2017 Siemens AG
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
package at.ac.tuwien.kr.alpha.solver;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;

/**
 * Tests {@link AbstractSolver} using some configuration test cases in which subparts are assigned to parts.
 *
 */
public class PartSubpartConfigurationTest extends AbstractSolverTests {
	@Test(timeout = 60000)
	public void testN2() {
		testPartSubpart(2);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testN4() {
		testPartSubpart(4);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testN8() {
		testPartSubpart(8);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testN16() {
		testPartSubpart(16);
	}

	@Test(timeout = 61000)
	@Ignore("disabled to save resources during CI")
	public void testN32() {
		testPartSubpart(32);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testN60() {
		testPartSubpart(60);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testN75() {
		testPartSubpart(75);
	}

	@Test(timeout = 60000)
	@Ignore("disabled to save resources during CI")
	public void testN100() {
		testPartSubpart(100);
	}

	private void testPartSubpart(int n) {
		List<String> rules = new ArrayList<>();
		for (int i = 1; i <= n; i++) {
			rules.add(String.format("n(%d).", i));
		}

		rules.addAll(Arrays.asList(
			"part(N) :- n(N), not no_part(N).",
			"no_part(N) :- n(N), not part(N).",
			"subpartid(SP,ID) :- subpart(SP,P), n(ID), not no_subpartid(SP,ID).",
			"no_subpartid(SP,ID) :- subpart(SP,P), n(ID), not subpartid(SP,ID).",
			"subpart(SP,P) :- part(P), part(SP), P != SP, not no_subpart(SP,P).",
			"no_subpart(SP,P) :- part(P), part(SP), P != SP, not subpart(SP,P).",
			":- subpart(SP,P1), subpart(SP,P2), P1 != P2.",
			":- subpart(SP1,P), subpart(SP2, P), SP1!=SP2, subpartid(SP1,ID), subpartid(SP2,ID)."
		));

		assertFalse(collectSet(concat(rules)).isEmpty());
		// TODO: check correctness of answer set
	}

	private String concat(List<String> rules) {
		String ls = System.lineSeparator();
		return String.join(ls, rules);
	}
}
