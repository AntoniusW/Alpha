/*
 * Copyright (c) 2016-2018, 2020, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.common.Truth;

public enum ThriceTruth implements Truth {
	TRUE("T", true),
	FALSE("F", false),
	MBT("M", true);

	private final String asString;
	private final boolean asBoolean;

	ThriceTruth(String asString, boolean asBoolean) {
		this.asString = asString;
		this.asBoolean = asBoolean;
	}

	@Override
	public boolean toBoolean() {
		return asBoolean;
	}

	@Override
	public String toString() {
		return asString;
	}

	public static ThriceTruth valueOf(boolean value) {
		return value ? TRUE : FALSE;
	}

	public static ThriceTruth fromChar(char signChar) {
		for (ThriceTruth value : values()) {
			if (value.asString.equals(String.valueOf(signChar))) {
				return value;
			}
		}
		throw new IllegalArgumentException("Unknown " + ThriceTruth.class.getSimpleName() + ": " + signChar);
	}

	/**
	 * @return true if this is MBT.
	 */
	public boolean isMBT() {
		return this == ThriceTruth.MBT;
	}

	/**
	 * Returns true if the two truth values are not compatible with each other.
	 * Each truth value is compatible with itself and both MBT and TRUE are compatible. All other combinations are
	 * conflicting.
	 * @param value1 the first truth value.
	 * @param value2 the second truth value.
	 * @return true iff first and second truth value are conflicting (i.e., not compatible).
	 */
	public static boolean isConflicting(ThriceTruth value1, ThriceTruth value2) {
		return (FALSE == value1 || FALSE == value2) && value1 != value2;
	}
}