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
package at.ac.tuwien.kr.alpha.core.solver;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.kr.alpha.core.common.NoGood;
import at.ac.tuwien.kr.alpha.core.common.NoGoodInterface;
import at.ac.tuwien.kr.alpha.core.common.NoGoodInterface.Type;

/**
 * Maintains statistics on numbers of various types of {@link NoGood}s.
 */
public class NoGoodCounter {

	private static final int CARD_NARY = 0;
	private static final int CARD_UNARY = 1;
	private static final int CARD_BINARY = 2;
	
	private int[] countByType = new int[Type.values().length];
	private int[] countByCardinality = new int[3];

	/**
	 * Increases counters for the types of the given NoGood
	 * @param noGood
	 */
	void add(NoGoodInterface noGood) {
		countByType[noGood.getType().ordinal()]++;
		countByCardinality[getAbstractCardinality(noGood)]++;
	}

	/**
	 * Decreases counters for the types of the given NoGood
	 * @param noGood
	 */
	void remove(NoGoodInterface noGood) {
		countByType[noGood.getType().ordinal()]--;
		countByCardinality[getAbstractCardinality(noGood)]--;
	}

	private int getAbstractCardinality(NoGoodInterface noGood) {
		if (noGood.isUnary()) {
			return CARD_UNARY;
		}
		if (noGood.isBinary()) {
			return CARD_BINARY;
		}
		return CARD_NARY;
	}
	
	/**
	 * @param type
	 * @return the number of nogoods of the given type
	 */
	public int getNumberOfNoGoods(Type type) {
		return countByType[type.ordinal()];
	}
	
	/**
	 * @return the number of unary nogoods
	 */
	public int getNumberOfUnaryNoGoods() {
		return countByCardinality[CARD_UNARY];
	}
	
	/**
	 * @return the number of binary nogoods
	 */
	public int getNumberOfBinaryNoGoods() {
		return countByCardinality[CARD_BINARY];
	}
	
	/**
	 * @return the number of nogoods that are neither unary nor binary
	 */
	public int getNumberOfNAryNoGoods() {
		return countByCardinality[CARD_NARY];
	}
	
	/**
	 * @return {@code true} iff there is at least one binary nogood
	 */
	public boolean hasBinaryNoGoods() {
		return countByCardinality[CARD_BINARY] > 0;
	}

	/**
	 * @return a string giving statistics on numbers of nogoods by type
	 */
	public String getStatsByType() {
		List<String> statsList = new ArrayList<>(Type.values().length);
		for (Type type : Type.values()) {
			statsList.add(type.name() + ": " + countByType[type.ordinal()]);
		}
		return String.join(" ", statsList);
	}

	/**
	 * @return a string giving statistics on numbers of nogoods by cardinality
	 */
	public String getStatsByCardinality() {
		return "unary: " + getNumberOfUnaryNoGoods() + " binary: " + getNumberOfBinaryNoGoods() + " larger: " + getNumberOfNAryNoGoods();
	}
	
}
