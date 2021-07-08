/*
 * Copyright (c) 2018-2021 Siemens AG
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

import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.WeightAtLevel;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.grounder.atoms.HeuristicAtom;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;

import java.util.Comparator;
import java.util.Objects;

/**
 * Holds values defined by a {@link HeuristicDirective} to steer domain-specific heuristic choice for a single ground heuristic directive.
 *
 */
public class HeuristicDirectiveValues {

	private final int headAtomId;
	private final BasicAtom groundHeadAtom;
	private final int weight;
	private final int level;
	private final boolean sign;

	public HeuristicDirectiveValues(int headAtomId, BasicAtom groundHeadAtom, int weight, int level, boolean sign) {
		this.headAtomId = headAtomId;
		this.groundHeadAtom = groundHeadAtom;
		this.weight = weight;
		this.level = level;
		this.sign = sign;
	}

	public int getHeadAtomId() {
		return headAtomId;
	}
	
	public BasicAtom getGroundHeadAtom() {
		return groundHeadAtom;
	}

	public int getWeight() {
		return weight;
	}

	public int getLevel() {
		return level;
	}

	public boolean getSign() {
		return sign;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		HeuristicDirectiveValues that = (HeuristicDirectiveValues) o;
		return headAtomId == that.headAtomId &&
				weight == that.weight &&
				level == that.level &&
				groundHeadAtom.equals(that.groundHeadAtom) &&
				sign == that.sign;
	}

	@Override
	public int hashCode() {
		return Objects.hash(headAtomId, groundHeadAtom, weight, level, sign);
	}

	@Override
	public String toString() {
		return String.format(ThriceTruth.valueOf(sign) + " %d [%d@%d]", headAtomId, weight, level);
	}

	/**
	 * Reads values from a ground {@link HeuristicAtom}.
	 * @param groundHeuristicAtom a ground heuristic atom
	 * @param headAtomId the ID of the head atom this heuristic applies to
	 * @return a new instance of {@link HeuristicDirectiveValues} with the values from {@code groundHeuristicAtom}.
	 */
	@SuppressWarnings("unchecked")
	public static HeuristicDirectiveValues fromHeuristicAtom(HeuristicAtom groundHeuristicAtom, int headAtomId) {
		WeightAtLevel weightAtLevel = groundHeuristicAtom.getWeightAtLevel();
		BasicAtom groundHeuristicHead = groundHeuristicAtom.getHeadAtom().toAtom();
		return new HeuristicDirectiveValues(headAtomId, groundHeuristicHead, ((ConstantTerm<Integer>)weightAtLevel.getWeight()).getObject(), ((ConstantTerm<Integer>)weightAtLevel.getLevel()).getObject(), groundHeuristicAtom.getHeadSign().toBoolean());
	}
	
	public static class PriorityComparator implements Comparator<HeuristicDirectiveValues> {

		@Override
		public int compare(HeuristicDirectiveValues v1, HeuristicDirectiveValues v2) {
			int difference = v1.level - v2.level;
			if (difference == 0) {
				difference = v1.weight - v2.weight;
			}
			if (difference == 0) {
				difference = v1.headAtomId - v2.headAtomId;
			}
			if (difference == 0) {
				difference = Boolean.compare(v1.sign, v2.sign);
			}
			return difference;
		}
		
	}

}
