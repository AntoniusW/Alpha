/**
 * Copyright (c) 2017-2019 Siemens AG
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
package at.ac.tuwien.kr.alpha.solver;

import java.io.PrintStream;
import java.util.Map;

public interface SolverMaintainingStatistics {

	int getNumberOfChoices();

	Map<String, Integer> getNumberOfChoicesPerBranchingHeuristic();

	int getNumberOfBacktracks();

	int getNumberOfBacktracksWithinBackjumps();

	int getNumberOfBackjumps();

	int getNumberOfBacktracksDueToRemnantMBTs();

	int getNumberOfDeletedNoGoods();
	
	/**
	 * @return the number of times the solver had to backtrack after closing unassigned atoms
	 */
	int getNumberOfConflictsAfterClosing();

	NoGoodCounter getNoGoodCounter();

	default String getStatisticsString() {
		return "g=" + getNumberOfChoices() + ", bt=" + getNumberOfBacktracks() + ", bj=" + getNumberOfBackjumps() + ", bt_within_bj="
				+ getNumberOfBacktracksWithinBackjumps() + ", mbt=" + getNumberOfBacktracksDueToRemnantMBTs() + ", cac=" + getNumberOfConflictsAfterClosing()
				+ ", del_ng=" + getNumberOfDeletedNoGoods();
	}
	
	default String getStatisticsCSV() {
		return String.format("%d,%d,%d,%d,%d,%d,%d", getNumberOfChoices(), getNumberOfBacktracks(), getNumberOfBackjumps(), getNumberOfBacktracksWithinBackjumps(), getNumberOfBacktracksDueToRemnantMBTs(), getNumberOfConflictsAfterClosing(), getNumberOfDeletedNoGoods());
	}

	default void printStatistics(PrintStream out) {
		out.println(getStatisticsString());
	}

	default void printStatistics() {
		printStatistics(System.out);
	}
}
