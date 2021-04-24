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

import org.slf4j.Logger;

/**
 * Collects performance data (mainly number of decisions per second) and outputs them on demand.
 */
public class PerformanceLog {

	private ChoiceManager choiceManager;
	private TrailAssignment assignment;
	private long msBetweenOutputs;
	
	private Long timeFirstEntry;
	private Long timeLastPerformanceLog;
	private int numberOfChoicesLastPerformanceLog;

	/**
	 * @param msBetweenOutputs
	 */
	public PerformanceLog(ChoiceManager choiceManager, TrailAssignment assignment, long msBetweenOutputs) {
		super();
		this.choiceManager = choiceManager;
		this.assignment = assignment;
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
		int currentNumberOfChoices = choiceManager.getChoices();
		if (currentTime >= timeLastPerformanceLog + msBetweenOutputs) {
			logger.info("Decisions in {}s: {}", (currentTime - timeLastPerformanceLog) / 1000.0f, currentNumberOfChoices - numberOfChoicesLastPerformanceLog);
			timeLastPerformanceLog = currentTime;
			numberOfChoicesLastPerformanceLog = currentNumberOfChoices;
			float overallTime = (currentTime - timeFirstEntry) / 1000.0f;
			float decisionsPerSec = currentNumberOfChoices / overallTime;
			logger.info("Overall performance: {} decisions in {}s or {} decisions per sec. Overall replayed assignments: {}.", currentNumberOfChoices, overallTime, decisionsPerSec, assignment.replayCounter);
		}
	}
}