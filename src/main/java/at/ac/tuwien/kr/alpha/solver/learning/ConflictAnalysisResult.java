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
package at.ac.tuwien.kr.alpha.solver.learning;

import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.NonGroundNoGood;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static at.ac.tuwien.kr.alpha.Util.oops;
import static at.ac.tuwien.kr.alpha.solver.NoGoodStore.LBD_NO_VALUE;

public class ConflictAnalysisResult {
	public static final ConflictAnalysisResult UNSAT = new ConflictAnalysisResult();

	public final NoGood learnedNoGood;
	public final int backjumpLevel;
	public final Collection<Integer> resolutionAtoms;
	public final int lbd;

	private List<NoGood> additionalLearnedNoGoods;
	private NonGroundNoGood learnedNonGroundNoGood;
	private List<NonGroundNoGood> additionalLearnedNonGroundNoGoods;

	private ConflictAnalysisResult() {
		learnedNoGood = null;
		backjumpLevel = -1;
		resolutionAtoms = null;
		lbd = LBD_NO_VALUE;
	}

	public ConflictAnalysisResult(NoGood learnedNoGood, int backjumpLevel, Collection<Integer> resolutionAtoms) {
		this(learnedNoGood, backjumpLevel, resolutionAtoms, LBD_NO_VALUE);
	}

	public ConflictAnalysisResult(NoGood learnedNoGood, int backjumpLevel, Collection<Integer> resolutionAtoms, int lbd) {
		if (backjumpLevel < 0) {
			throw oops("Backjumping level is smaller than 0");
		}

		this.learnedNoGood = learnedNoGood;
		this.backjumpLevel = backjumpLevel;
		this.resolutionAtoms = resolutionAtoms;
		this.lbd = lbd;
	}

	/**
	 * Adds nogoods that have been learned additionally to the primary learned nogood on non-first UIPs.
	 * @param additionalLearnedNoGoods the nogoods learnt on non-first UIPs
	 */
	void addLearnedNoGoods(List<NoGood> additionalLearnedNoGoods) {
		if (this.additionalLearnedNoGoods == null) {
			this.additionalLearnedNoGoods = additionalLearnedNoGoods;
		} else {
			this.additionalLearnedNoGoods.addAll(additionalLearnedNoGoods);
		}
	}

	/**
	 * Gets a list of nogoods that have been learned additionally to the primary learned nogood on non-first UIPs.
	 * @return a list of additional learned nogoods
	 */
	List<NoGood> getAdditionalLearnedNoGoods() {
		return additionalLearnedNoGoods == null ? Collections.emptyList() : Collections.unmodifiableList(additionalLearnedNoGoods);
	}

	/**
	 * Adds non-ground nogoods that have been learned additionally to the primary learned nogood on non-first UIPs.
	 * @param additionalLearnedNonGroundNoGoods the non-ground nogoods learnt on non-first UIPs
	 */
	void addLearnedNonGroundNoGoods(List<NonGroundNoGood> additionalLearnedNonGroundNoGoods) {
		if (this.additionalLearnedNonGroundNoGoods == null) {
			this.additionalLearnedNonGroundNoGoods = additionalLearnedNonGroundNoGoods;
		} else {
			this.additionalLearnedNonGroundNoGoods.addAll(additionalLearnedNonGroundNoGoods);
		}
	}

	/**
	 * Gets a list of non-ground nogoods that have been learned additionally to the primary learned nogood on non-first UIPs.
	 * @return a list of additional learned non-ground nogoods
	 */
	List<NonGroundNoGood> getAdditionalLearnedNonGroundNoGoods() {
		return additionalLearnedNonGroundNoGoods == null ? Collections.emptyList() : Collections.unmodifiableList(additionalLearnedNonGroundNoGoods);
	}

	void setLearnedNonGroundNoGood(NonGroundNoGood learnedNonGroundNoGood) {
		this.learnedNonGroundNoGood = learnedNonGroundNoGood;
	}

	public NonGroundNoGood getLearnedNonGroundNoGood() {
		return learnedNonGroundNoGood;
	}

	@Override
	public String toString() {
		if (this == UNSAT) {
			return "UNSATISFIABLE";
		}
		return learnedNoGood + "@" + backjumpLevel;
	}
}