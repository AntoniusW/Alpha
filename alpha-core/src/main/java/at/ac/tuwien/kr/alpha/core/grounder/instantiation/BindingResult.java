/**
 * Copyright (c) 2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.core.grounder.instantiation;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.grounder.Substitution;

/**
 * Contains substitutions produced for generating ground substitutions of a rule,
 * and for every substitution the number of positive body atoms still unassigned in the respective ground rule.
 */
public class BindingResult {

	private final List<Substitution> generatedSubstitutions = new ArrayList<>();
	private final List<Integer> numbersOfUnassignedPositiveBodyAtoms = new ArrayList<>();

	public void add(Substitution generatedSubstitution, int numberOfUnassignedPositiveBodyAtoms) {
		generatedSubstitutions.add(generatedSubstitution);
		numbersOfUnassignedPositiveBodyAtoms.add(numberOfUnassignedPositiveBodyAtoms);
	}

	public void add(BindingResult otherBindingResult) {
		generatedSubstitutions.addAll(otherBindingResult.generatedSubstitutions);
		numbersOfUnassignedPositiveBodyAtoms.addAll(otherBindingResult.numbersOfUnassignedPositiveBodyAtoms);
	}

	public int size() {
		return generatedSubstitutions.size();
	}

	public static BindingResult empty() {
		return new BindingResult();
	}

	public static BindingResult singleton(Substitution generatedSubstitution, int numberOfUnassignedPositiveBodyAtoms) {
		BindingResult bindingResult = new BindingResult();
		bindingResult.add(generatedSubstitution, numberOfUnassignedPositiveBodyAtoms);
		return bindingResult;
	}

	public List<Substitution> getGeneratedSubstitutions() {
		return generatedSubstitutions;
	}

	public List<Integer> getNumbersOfUnassignedPositiveBodyAtoms() {
		return numbersOfUnassignedPositiveBodyAtoms;
	}

}
