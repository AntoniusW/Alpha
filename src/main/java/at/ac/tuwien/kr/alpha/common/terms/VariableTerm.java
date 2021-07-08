/*
 * Copyright (c) 2016-2018, 2021, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.common.terms;

import at.ac.tuwien.kr.alpha.common.Interner;
import at.ac.tuwien.kr.alpha.grounder.IntIdGenerator;
import at.ac.tuwien.kr.alpha.grounder.Substitution;
import com.google.common.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.List;

public class VariableTerm extends Term {
	private static final Interner<VariableTerm> INTERNER = new Interner<>();

	private static final String ANONYMOUS_VARIABLE_PREFIX = "_";

	@VisibleForTesting
	public static final IntIdGenerator ANONYMOUS_VARIABLE_COUNTER = new IntIdGenerator();

	private final String variableName;

	private VariableTerm(String variableName) {
		this.variableName = variableName;
	}

	public static VariableTerm getInstance(String variableName) {
		return INTERNER.intern(new VariableTerm(variableName));
	}

	public static VariableTerm getAnonymousInstance() {
		return getInstance(ANONYMOUS_VARIABLE_PREFIX + ANONYMOUS_VARIABLE_COUNTER.getNextId());
	}

	@Override
	public boolean isGround() {
		return false;
	}

	@Override
	public List<VariableTerm> getOccurringVariables() {
		return Collections.singletonList(this);
	}

	@Override
	public Term substitute(Substitution substitution) {
		Term groundTerm = substitution.eval(this);
		if (groundTerm == null) {
			// If variable is not substituted, keep term as is.
			return this;
		}
		return  groundTerm;
	}

	@Override
	public String toString() {
		return variableName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		VariableTerm that = (VariableTerm) o;

		return variableName.equals(that.variableName);
	}

	@Override
	public int hashCode() {
		return variableName.hashCode();
	}

	@Override
	public int compareTo(Term o) {
		if (this == o) {
			return 0;
		}

		if (!(o instanceof VariableTerm)) {
			return super.compareTo(o);
		}

		VariableTerm other = (VariableTerm)o;
		return variableName.compareTo(other.variableName);
	}

	@Override
	public Term renameVariables(String renamePrefix) {
		return VariableTerm.getInstance(renamePrefix + variableName);
	}

	@Override
	public Term normalizeVariables(String renamePrefix, RenameCounter counter) {
		VariableTerm renamedThis = counter.renamedVariables.get(this);
		if (renamedThis != null) {
			return renamedThis;
		} else {
			VariableTerm renamedVariable = VariableTerm.getInstance(renamePrefix + counter.counter++);
			counter.renamedVariables.put(this, renamedVariable);
			return renamedVariable;
		}
	}
}