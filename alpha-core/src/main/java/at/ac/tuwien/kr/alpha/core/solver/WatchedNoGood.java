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
package at.ac.tuwien.kr.alpha.core.solver;

import java.util.Iterator;

import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.common.NoGoodInterface;

import static at.ac.tuwien.kr.alpha.api.Util.oops;
import static at.ac.tuwien.kr.alpha.core.atoms.Literals.literalToString;

public final class WatchedNoGood implements NoGoodInterface, Antecedent {
	private int activity;
	private final int[] literals;
	private int alpha;
	private int head;
	private final Type type;
	private boolean isLbdLessOrEqual2;

	WatchedNoGood(NoGood noGood, int a, int b, int alpha) {
		if (noGood.size() < 3) {
			throw oops("WatchedNoGood should not be used for small NoGoods.");
		}
		literals = new int[noGood.size()];
		checkPointers(a, b, alpha);
		int i = 0;
		for (Integer lit : noGood) {
			literals[i++] = lit;
		}
		this.alpha = alpha;
		head = noGood.hasHead() ? 0 : -1;
		activity = 0;
		if (b == 0) {
			swap(1, a);
		} else {
			swap(0, a);
			swap(1, b);
		}
		this.type = noGood.getType();
	}

	private void checkPointers(int a, int b, int alpha) {
		if (a == b) {
			throw new IllegalArgumentException("First two pointers must not point at the same literal.");
		}
		if (a < 0 || b < 0 || alpha < -1 || a >= literals.length || b >= literals.length || alpha >= literals.length) {
			throw new IllegalArgumentException("Pointers must be within bounds.");
		}
	}

	private void swap(int a, int b) {
		int tmp = literals[a];
		literals[a] = literals[b];
		literals[b] = tmp;
		if (hasHead()) {
			// If WatchedNoGood has a head, ensure the head pointer and alpha watch follow the swap.
			if (head == a) {
				head = b;
			} else if (head == b) {
				head = a;
			}
			if (alpha == a) {
				alpha = b;
			} else if (alpha == b) {
				alpha = a;
			}
		}
	}

	@Override
	public boolean hasHead() {
		return head != -1;
	}

	@Override
	public int getHead() {
		return literals[head];
	}

	int getHeadIndex() {
		return head;
	}

	void setWatch(int index, int value) {
		if (index != 0 && index != 1) {
			throw new IndexOutOfBoundsException();
		}
		swap(index, value);
	}

	@Override
	public int getLiteral(int index) {
		return literals[index];
	}

	int getAlphaPointer() {
		return alpha;
	}

	void setAlphaPointer(int value) {
		alpha = value;
	}

	int getLiteralAtAlpha() {
		return literals[alpha];
	}

	@Override
	public int size() {
		return literals.length;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Antecedent asAntecedent() {
		return this;
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (hasHead()) {
			sb.append("*");
		}

		sb.append("{ ");

		int hcount = 0;
		for (int literal : literals) {
			sb.append(literalToString(literal));
			sb.append(hasHead() && head == hcount ? "h" : "");
			sb.append(" ");
			hcount++;
		}

		sb.append("}{");
		sb.append(alpha);
		sb.append("}");
		return sb.toString();
	}

	@Override
	public int[] getReasonLiterals() {
		return literals;
	}

	public int getActivity() {
		return activity;
	}

	@Override
	public void decreaseActivity() {
		activity >>= 1;
	}

	@Override
	public void bumpActivity() {
		activity++;
	}

	void setLBD(int lbd) {
		isLbdLessOrEqual2 = lbd <= 2;
	}

	boolean isLbdLessOrEqual2() {
		return isLbdLessOrEqual2;
	}
}
