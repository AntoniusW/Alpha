/*
 * Copyright (c) 2016-2020, the Alpha Team.
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

import at.ac.tuwien.kr.alpha.solver.Antecedent;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;
import static at.ac.tuwien.kr.alpha.common.Literals.isNegated;
import static at.ac.tuwien.kr.alpha.common.Literals.isPositive;
import static at.ac.tuwien.kr.alpha.common.Literals.negateLiteral;
import static at.ac.tuwien.kr.alpha.common.Literals.positiveLiteral;
import static at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type.INTERNAL;
import static at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type.LEARNT;
import static at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type.STATIC;
import static at.ac.tuwien.kr.alpha.common.NoGoodInterface.Type.SUPPORT;

public class NoGood implements NoGoodInterface, Comparable<NoGood> {
	public static final int HEAD = 0;
	public static final NoGood UNSAT = new NoGood();

	protected final int[] literals;
	private final boolean head;
	private final Type type;

	public NoGood(int... literals) {
		this(STATIC, literals, false);
	}
	
	public NoGood(Type type, int... literals) {
		this(type, literals, false);
	}
	
	private NoGood(Type type, int[] literals, boolean head) {
		this.type = type;
		this.head = head;
		if (head && !isNegated(literals[0])) {
			throw oops("Head is not negative");
		}

		// HINT: this might decrease performance if NoGoods are mostly small.
		Arrays.sort(literals, head ? 1 : 0, literals.length);

		int shift = 0;
		for (int i = 1; i < literals.length; i++) {
			if (literals[i - 1] == literals[i]) { // check for duplicate
				shift++;
			}
			literals[i - shift] = literals[i]; // Remove duplicates in place by shifting remaining literals.
		}

		// copy-shrink array if needed.
		this.literals = shift <= 0 ? literals : Arrays.copyOf(literals, literals.length - shift);
	}

	protected NoGood(NoGood noGood) {
		this.literals = noGood.literals.clone();
		this.head = noGood.head;
		this.type = noGood.type;
	}
	
	public static NoGood learnt(int... literals) {
		return new NoGood(LEARNT, literals);
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
		return NoGood.headFirst(type, addPosNeg(bodyLiterals, posLiterals, negLiterals, 1));
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

	@Override
	public int size() {
		return literals.length;
	}

	@Override
	public Antecedent asAntecedent() {
		return new Antecedent() {

			@Override
			public int[] getReasonLiterals() {
				return NoGood.this.literals; // Beware: returned array must not be modified!
			}

			@Override
			public void bumpActivity() {
			}

			@Override
			public void decreaseActivity() {
			}
		};
	}

	public NoGood withoutHead() {
		return new NoGood(type, literals.clone());
	}

	/**
	 * A shorthand for <code>Literals.positiveLiteral(getLiteral(...))</code>
	 */
	public int getPositiveLiteral(int index) {
		return positiveLiteral(getLiteral(index));
	}

	@Override
	public int getLiteral(int index) {
		return literals[index];
	}

	@Override
	public boolean hasHead() {
		return head;
	}

	@Override
	public int getHead() {
		return getLiteral(HEAD);
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			private int i;

			@Override
			public boolean hasNext() {
				return literals.length > i;
			}

			@Override
			public Integer next() {
				return literals[i++];
			}
		};
	}

	public IntStream stream() {
		return Arrays.stream(literals);
	}

	@Override
	public int compareTo(NoGood o) {
		if (o == null) {
			throw new NullPointerException("Cannot compare against null.");
		}

		if (o.head && !head) {
			return -1;
		}

		if (!o.head && head) {
			return +1;
		}

		if (o.literals.length > literals.length) {
			return -1;
		}

		if (o.literals.length < literals.length) {
			return +1;
		}

		for (int i = 0; i < literals.length; i++) {
			if (o.literals[i] > literals[i]) {
				return -1;
			}
			if (o.literals[i] < literals[i]) {
				return +1;
			}
		}

		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		NoGood noGood = (NoGood) o;

		return head == noGood.head && Arrays.equals(literals, noGood.literals);
	}

	@Override
	public int hashCode() {
		return 31 * Arrays.hashCode(literals) * (head ? +1 : -1);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (head) {
			sb.append("*");
		}

		sb.append("{ ");

		for (int literal : literals) {
			sb.append(isPositive(literal) ? "+" : "-");
			sb.append(atomOf(literal));
			sb.append(" ");
		}

		sb.append("}");

		return sb.toString();
	}
}
