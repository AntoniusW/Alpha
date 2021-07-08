/**
 * Copyright (c) 2018-2019 Siemens AG
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
package at.ac.tuwien.kr.alpha.common;

import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

/**
 * Represents a weight-level pair defined within an annotation (either for heuristic values or for a weak constraint)
 *
 */
public class WeightAtLevel implements Substitutable<WeightAtLevel> {
	
	public static final int DEFAULT_WEIGHT = 0;
	public static final int DEFAULT_LEVEL = 0;
	public static final Term DEFAULT_WEIGHT_TERM = ConstantTerm.getInstance(DEFAULT_WEIGHT);
	public static final Term DEFAULT_LEVEL_TERM = ConstantTerm.getInstance(DEFAULT_LEVEL);

	private Term weight;
	private Term level;

	public WeightAtLevel(Term weight, Term level) {
		this.weight = weight != null ? weight : DEFAULT_WEIGHT_TERM;
		this.level = level != null ? level : DEFAULT_LEVEL_TERM;
	}
	
	public Term getWeight() {
		return weight;
	}

	public Term getLevel() {
		return level;
	}
	
	@Override
	public WeightAtLevel substitute(Substitution substitution) {
		return new WeightAtLevel(weight.substitute(substitution), level.substitute(substitution));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(weight);
		if (level != null) {
			sb.append("@");
			sb.append(level);
		}
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		WeightAtLevel that = (WeightAtLevel) o;

		if (weight != that.weight) {
			return false;
		}
		return level.equals(that.level);
	}
	
	@Override
	public int hashCode() {
		return 31 * weight.hashCode() + level.hashCode();
	}

}
