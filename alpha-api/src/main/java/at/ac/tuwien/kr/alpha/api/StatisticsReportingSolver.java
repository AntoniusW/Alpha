package at.ac.tuwien.kr.alpha.api;

import java.io.PrintStream;

public interface StatisticsReportingSolver extends Solver {
	
	int getNumberOfChoices();

	int getNumberOfBacktracks();

	int getNumberOfBacktracksWithinBackjumps();

	int getNumberOfBackjumps();

	int getNumberOfBacktracksDueToRemnantMBTs();

	int getNumberOfDeletedNoGoods();
	
	/**
	 * @return the number of times the solver had to backtrack after closing unassigned atoms
	 */
	int getNumberOfConflictsAfterClosing();

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
