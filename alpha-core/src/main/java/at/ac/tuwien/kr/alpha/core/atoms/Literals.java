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
package at.ac.tuwien.kr.alpha.core.atoms;

/**
 * Provides methods to convert atoms to literals and vice versa,
 * and to obtain and change information on the polarity of a literal.
 * A literal is represented by an integer whose least significant bit is
 * 1 if the literal is negated and 0 otherwise, and whose other bits
 * encode the atom.
 */
public final class Literals {
	/**
	 * Given a literal, returns the corresponding atom.
	 * @param literal the literal to translate.
	 * @return the corresponding atom.
	 */
	public static int atomOf(int literal) {
		return literal >> 1;
	}

	/**
	 * A utility to check if a given literal is negated.
	 * @param literal the literal to check.
	 * @return {@code true} iff the literal is negated, {@code false} otherwise.
	 */
	public static boolean isNegated(int literal) {
		return (literal & 0x1) == 1;
	}

	public static boolean isPositive(int literal) {
		return (literal & 0x1) == 0;
	}

	public static int negateLiteral(int literal) {
		return literal ^ 0x1;
	}

	public static int atomToLiteral(int atom, boolean truth) {
		return truth ? atomToLiteral(atom) : atomToNegatedLiteral(atom);
	}

	public static int atomToLiteral(int atom) {
		return atom << 1;
	}

	public static int atomToNegatedLiteral(int atom) {
		return negateLiteral(atomToLiteral(atom));
	}

	public static int positiveLiteral(int literal) {
		return literal & ~0x1;
	}

	public static String literalToString(int literal) {
		return (isPositive(literal) ? "+" : "-") + atomOf(literal);
	}
}
