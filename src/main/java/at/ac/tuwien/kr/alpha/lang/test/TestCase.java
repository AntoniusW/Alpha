/**
 * Copyright (c) 2016-2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.lang.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.kr.alpha.common.Program;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;

/**
 * Representation of a unit-test case as specified in the respective
 * <a href="https://github.com/alpha-asp/Alpha/issues/237">github issue</a>.
 * An instance of this class contains all code and configuration tied to a
 * single "#test" marker in ASP source code.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public class TestCase {

	private final String name;
	private final int expectedAnswerSets;
	private final List<Atom> input;
	private final Program verifier;

	public TestCase(String name, int expectedAnswerSets, List<Atom> input, Program verifier) {
		this.name = name;
		this.expectedAnswerSets = expectedAnswerSets;
		this.input = Collections.unmodifiableList(new ArrayList<>(input)); // defensive copy to ensure immutability
		this.verifier = verifier;
	}

	public String getName() {
		return this.name;
	}

	public int getExpectedAnswerSets() {
		return this.expectedAnswerSets;
	}

	public List<Atom> getInput() {
		return this.input;
	}

	public Program getVerifier() {
		return this.verifier;
	}

}
