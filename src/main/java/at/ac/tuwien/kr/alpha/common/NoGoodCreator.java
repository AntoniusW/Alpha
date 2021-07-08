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

import java.util.List;

import static at.ac.tuwien.kr.alpha.common.Literals.negateLiteral;
import static at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type;
import static at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type.INTERNAL;
import static at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type.LEARNT;
import static at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type.STATIC;
import static at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type.SUPPORT;

/**
 * Offers static methods to create {@link NoGood}s
 */
public class NoGoodCreator {
	public static NoGood learnt(int... literals) {
		return new NoGood(LEARNT, literals);
	}

	public static NoGood internal(int... literals) {
		return new NoGood(NoGood.Type.INTERNAL, literals);
	}

	public static NoGood headFirst(int... literals) {
		return headFirst(STATIC, literals);
	}

	public static NoGood headFirstInternal(int... literals) {
		return headFirst(INTERNAL, literals);
	}

	public static NoGood headFirst(Type type, int... literals) {
		return new NoGood(type, literals, true);
	}

	public static NoGood fact(int literal) {
		return headFirst(literal);
	}

	public static NoGood support(int headLiteral, int bodyRepresentingLiteral) {
		return new NoGood(SUPPORT, headLiteral, negateLiteral(bodyRepresentingLiteral));
	}

	public static NoGood fromConstraint(List<Integer> posLiterals, List<Integer> negLiterals) {
		return new NoGood(addPosNeg(new int[posLiterals.size() + negLiterals.size()], posLiterals, negLiterals, 0));
	}

	public static NoGood fromBody(List<Integer> posLiterals, List<Integer> negLiterals, int bodyRepresentingLiteral) {
		return fromBody(STATIC, posLiterals, negLiterals, bodyRepresentingLiteral);
	}

	public static NoGood fromBodyInternal(List<Integer> posLiterals, List<Integer> negLiterals, int bodyRepresentingLiteral) {
		return fromBody(INTERNAL, posLiterals, negLiterals, bodyRepresentingLiteral);
	}

	public static NoGood fromBody(Type type, List<Integer> posLiterals, List<Integer> negLiterals, int bodyRepresentingLiteral) {
		int[] bodyLiterals = new int[posLiterals.size() + negLiterals.size() + 1];
		bodyLiterals[0] = negateLiteral(bodyRepresentingLiteral);
		return headFirst(type, addPosNeg(bodyLiterals, posLiterals, negLiterals, 1));
	}

	private static int[] addPosNeg(int[] literals, List<Integer> posLiterals, List<Integer> negLiterals, int offset) {
		int i = offset;
		for (Integer literal : posLiterals) {
			literals[i++] = literal;
		}
		for (Integer literal : negLiterals) {
			literals[i++] = negateLiteral(literal);
		}
		return literals;
	}
}
