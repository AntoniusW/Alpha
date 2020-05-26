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
package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.solver.heuristics.BranchingHeuristic;
import at.ac.tuwien.kr.alpha.solver.heuristics.ChainedBranchingHeuristics;
import at.ac.tuwien.kr.alpha.solver.heuristics.VSIDSWithPhaseSaving;
import org.slf4j.Logger;

/**
 * Collects performance data and outputs them on demand.
 *
 * Copyright (c) 2019, the Alpha Team.
 */
public class PerformanceLog {

	private final ChoiceManager choiceManager;
	private final TrailAssignment assignment;
	private final MixedRestartStrategy restartStrategy;
	private final LearnedNoGoodDeletion learnedNoGoodDeletion;
	private final BranchingHeuristic branchingHeuristic;
	private long msBetweenOutputs;
	
	private Long timeFirstEntry;
	private Long timeLastPerformanceLog;
	private int numberOfChoicesLastPerformanceLog;
	private int numberOfRestartsLastPerformanceLog;
	private int numberOfConflictsLastPerformanceLog;
	private int numberOfDeletedNoGoodsLastPerformanceLog;

	public PerformanceLog(ChoiceManager choiceManager, TrailAssignment assignment, MixedRestartStrategy restartStrategy, LearnedNoGoodDeletion learnedNoGoodDeletion, BranchingHeuristic branchingHeuristic, long msBetweenOutputs) {
		super();
		this.choiceManager = choiceManager;
		this.assignment = assignment;
		this.restartStrategy = restartStrategy;
		this.learnedNoGoodDeletion = learnedNoGoodDeletion;
		this.branchingHeuristic = branchingHeuristic;
		this.msBetweenOutputs = msBetweenOutputs;
	}

	public void initialize() {
		timeFirstEntry = System.currentTimeMillis();
		timeLastPerformanceLog = timeFirstEntry;
	}

	/**
	 * @param logger
	 */
	public void infoIfTimeForOutput(Logger logger) {
		long currentTime = System.currentTimeMillis();
		if (currentTime < timeLastPerformanceLog + msBetweenOutputs) {
			return;
		}
		int currentNumberOfChoices = choiceManager.getChoices();
		float timeSinceLastLog = (currentTime - timeLastPerformanceLog) / 1000.0f;
		logger.info("Decisions in {}s: {}", timeSinceLastLog, currentNumberOfChoices - numberOfChoicesLastPerformanceLog);
		timeLastPerformanceLog = currentTime;
		numberOfChoicesLastPerformanceLog = currentNumberOfChoices;
		float overallTime = (currentTime - timeFirstEntry) / 1000.0f;
		float decisionsPerSec = currentNumberOfChoices / overallTime;
		logger.info("Overall performance: {} decisions in {}s or {} decisions per sec. Overall replayed assignments: {}.", currentNumberOfChoices, overallTime, decisionsPerSec, assignment.replayCounter);
		if (restartStrategy != null) {
			int totalRestarts = restartStrategy.getTotalRestarts();
			int currentNumberOfRestarts = totalRestarts - numberOfRestartsLastPerformanceLog;
			logStatsPerTime(logger, "Restarts", timeSinceLastLog, overallTime, totalRestarts, currentNumberOfRestarts);
			numberOfRestartsLastPerformanceLog = totalRestarts;
		}
		if (branchingHeuristic != null && branchingHeuristic instanceof ChainedBranchingHeuristics) {
			BranchingHeuristic firstHeuristic = ((ChainedBranchingHeuristics) branchingHeuristic).getFirstElement();
			if (firstHeuristic instanceof VSIDSWithPhaseSaving) {
				VSIDSWithPhaseSaving vsidsWithPhaseSaving = (VSIDSWithPhaseSaving) firstHeuristic;
				long numThrownAway = vsidsWithPhaseSaving.getNumThrownAway();
				long numNoChoicePoint = vsidsWithPhaseSaving.getNumNoChoicePoint();
				long numNotActiveChoicePoint = vsidsWithPhaseSaving.getNumNotActiveChoicePoint();
				double activityDecrease = vsidsWithPhaseSaving.getActivityDecrease();
				logger.info("Heuristic threw away {} preferred choices ({} no choice, {} not active choice points) averaging {} thrown away per choice done.", numThrownAway, numNoChoicePoint, numNotActiveChoicePoint, (float) numThrownAway / currentNumberOfChoices);
				logger.info("HeapOfActiveAtoms had {} choices added due to increased activity.", vsidsWithPhaseSaving.getNumAddedToHeapByActivity());
				logger.info("Atom activity decreased overall by {} or {} per choice on average", activityDecrease, activityDecrease / currentNumberOfChoices);
			}
		}
		if (learnedNoGoodDeletion != null) {
			int totalConflicts = learnedNoGoodDeletion.getNumTotalConflicts();
			int currentNumConflicts = totalConflicts - numberOfConflictsLastPerformanceLog;
			int totalDeletedNogoods = learnedNoGoodDeletion.getNumberOfDeletedNoGoods();
			int currenDeletedNoGoods = totalDeletedNogoods - numberOfDeletedNoGoodsLastPerformanceLog;
			logStatsPerTime(logger, "Conflicts", timeSinceLastLog, overallTime, totalConflicts, currentNumConflicts);
			logStatsPerTime(logger, "Deleted NoGoods", timeSinceLastLog, overallTime, totalDeletedNogoods, currenDeletedNoGoods);
			numberOfConflictsLastPerformanceLog = totalConflicts;
			numberOfDeletedNoGoodsLastPerformanceLog = totalDeletedNogoods;
		}
	}

	private void logStatsPerTime(Logger logger, String statName, float timeSinceLastLog, float overallTime, int total, int differenceSinceLast) {
		logger.info(statName + " in {}s: {}", timeSinceLastLog, differenceSinceLast);
		logger.info("Overall performance: {} " + statName + " in {}s or {} " + statName + " per sec", total, overallTime, total / overallTime);

	}
}