/*
 *  Copyright (c) 2021 Siemens AG
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1) Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *  2) Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package at.ac.tuwien.kr.alpha.common.heuristics;

import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.MBT;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.TRUE;

public class HeuristicDirectiveAtom implements Comparable<HeuristicDirectiveAtom> {

	public static final ThriceTruth DEFAULT_HEAD_SIGN = TRUE;
	public static final Set<ThriceTruth> DEFAULT_BODY_SIGNS = new HashSet<>(Arrays.asList(TRUE, MBT));

	private final Set<ThriceTruth> signs;
	private final Atom atom;

	private HeuristicDirectiveAtom(Set<ThriceTruth> signs, Atom atom) {
		if (atom instanceof BasicAtom) {
			this.signs = Collections.unmodifiableSet(signs);
		} else {
			this.signs = null;
		}
		this.atom = atom;
	}

	public static HeuristicDirectiveAtom head(ThriceTruth sign, BasicAtom atom) {
		if (sign == ThriceTruth.MBT) {
			throw new IllegalArgumentException("M sign in heuristic head");
		}
		if (sign == null) {
			sign = DEFAULT_HEAD_SIGN;
		}
		return new HeuristicDirectiveAtom(Collections.singleton(sign), atom);
	}

	public static HeuristicDirectiveAtom body(Set<ThriceTruth> signs, Atom atom) {
		if (signs == null || signs.isEmpty()) {
			signs = DEFAULT_BODY_SIGNS;
		} else if (!(atom instanceof BasicAtom)) {
			throw oops("Non-basic heuristic directive atom with non-empty sign list");
		}
		return new HeuristicDirectiveAtom(signs, atom);
	}

	public Set<ThriceTruth> getSigns() {
		return signs;
	}

	public Atom getAtom() {
		return atom;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		HeuristicDirectiveAtom that = (HeuristicDirectiveAtom) o;
		if (this.signs == null || that.signs == null) {
			return this.signs == that.signs && this.atom.equals(that.atom);
		}
		return HeuristicSignSetUtil.toString(this.signs).equals(HeuristicSignSetUtil.toString(that.signs)) &&
				this.atom.equals(that.atom);
	}

	@Override
	public int hashCode() {
		return Objects.hash(signs, atom);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (signs != null) {
			for (ThriceTruth truth : ThriceTruth.values()) {
				if (signs.contains(truth)) {
					sb.append(truth);
				}
			}
			if (!signs.isEmpty()) {
				sb.append(" ");
			}
		}
		sb.append(atom);
		return sb.toString();
	}

	@Override
	public int compareTo(HeuristicDirectiveAtom that) {
		final String strSigns1 = this.signs == null ? null : HeuristicSignSetUtil.toString(this.signs);
		final String strSigns2 = that.signs == null ? null : HeuristicSignSetUtil.toString(that.signs);
		final int diffSignSets = StringUtils.compare(strSigns1, strSigns2);
		if (diffSignSets != 0) {
			return diffSignSets;
		}
		// transforming atoms to strings for comparison due to limitations of Term#priority (TODO: why these limitations?)
		return StringUtils.compare(this.atom.toString(), that.atom.toString());
	}
}
