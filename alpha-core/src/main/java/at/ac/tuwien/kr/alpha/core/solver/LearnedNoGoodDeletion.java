package at.ac.tuwien.kr.alpha.core.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.core.common.Assignment;

import static at.ac.tuwien.kr.alpha.core.atoms.Literals.atomOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Realizes a learned NoGood deletion strategy based on LBD and activity of NoGoods.
 *
 * Copyright (c) 2019, the Alpha Team.
 */
class LearnedNoGoodDeletion {
	private static final Logger LOGGER = LoggerFactory.getLogger(LearnedNoGoodDeletion.class);
	public static final int RESET_SEQUENCE_AFTER = 20;
	public static final int RUN_AFTER_AT_LEAST = 2000;
	public static final int GROWTH_FACTOR = 100;
	private final ArrayList<WatchedNoGood> learnedNoGoods = new ArrayList<>();	// List of learned NoGoods that can be removed again. Note: should only contain NoGoods of size > 2.
	private final NoGoodStoreAlphaRoaming store;
	private final Assignment assignment;
	private int conflictCounter;
	private int cleanupCounter;
	private int numberOfDeletedNoGoods;

	LearnedNoGoodDeletion(NoGoodStoreAlphaRoaming store, Assignment assignment) {
		this.store = store;
		this.assignment = assignment;
	}

	void reset() {
		learnedNoGoods.clear();
		conflictCounter = 0;
		cleanupCounter = 0;
		numberOfDeletedNoGoods = 0;
	}

	/**
	 * Returns WatchedNoGoods known to {@link LearnedNoGoodDeletion}.
	 * Note: this is likely just a subset of all learned nogoods.
	 * @return an unmodifiable list of {@link WatchedNoGood}s.
	 */
	public List<WatchedNoGood> inspectLearnedNoGoods() {
		return Collections.unmodifiableList(learnedNoGoods);
	}

	void recordLearnedNoGood(WatchedNoGood learnedWatchedNoGood) {
		learnedNoGoods.add(learnedWatchedNoGood);
	}

	void increaseConflictCounter() {
		conflictCounter++;
	}

	boolean needToRunNoGoodDeletion() {
		return conflictCounter > RUN_AFTER_AT_LEAST + (GROWTH_FACTOR * cleanupCounter);
	}

	void runNoGoodDeletion() {
		conflictCounter = 0;
		cleanupCounter++;
		// Reset the sequence after enough growth cycles.
		if (cleanupCounter > RESET_SEQUENCE_AFTER) {
			cleanupCounter = 0;
		}
		int deletedNoGoods = 0;
		int originalSize = learnedNoGoods.size();
		if (originalSize == 0) {
			return;
		}
		int toDeleteMax = originalSize / 2;
		long activitySum = 0;
		for (WatchedNoGood learnedNoGood : learnedNoGoods) {
			activitySum += learnedNoGood.getActivity();
		}
		double avgActivity = (double) activitySum / originalSize;
		double scoreThreshold = avgActivity * 1.5;
		for (Iterator<WatchedNoGood> iterator = learnedNoGoods.iterator(); iterator.hasNext();) {
			WatchedNoGood learnedNoGood = iterator.next();
			if (deletedNoGoods >= toDeleteMax) {
				break;
			}
			boolean keepNoGood = isLocked(learnedNoGood, assignment)
				|| learnedNoGood.getActivity() > scoreThreshold
				|| learnedNoGood.isLbdLessOrEqual2();
			if (!keepNoGood) {
				iterator.remove();
				store.removeFromWatches(learnedNoGood);
				learnedNoGood.decreaseActivity();
				deletedNoGoods++;
				LOGGER.trace("Removed from store the NoGood: {}", learnedNoGood);
			}
		}
		LOGGER.debug("Removed {} NoGoods from store.", deletedNoGoods);
		this.numberOfDeletedNoGoods += deletedNoGoods;
	}

	private boolean isLocked(WatchedNoGood noGood, Assignment assignment) {
		int watchedAtom1 = atomOf(noGood.getLiteral(0));
		int watchedAtom2 = atomOf(noGood.getLiteral(1));
		if (!assignment.isAssigned(watchedAtom1) || !assignment.isAssigned(watchedAtom2)) {
			return false;
		}
		return noGood == assignment.getImpliedBy(watchedAtom1)
			|| noGood == assignment.getImpliedBy(watchedAtom2);
	}

	public int getNumberOfDeletedNoGoods() {
		return numberOfDeletedNoGoods;
	}
}
