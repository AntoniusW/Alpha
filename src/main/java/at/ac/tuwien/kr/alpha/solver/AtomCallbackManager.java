package at.ac.tuwien.kr.alpha.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static at.ac.tuwien.kr.alpha.Util.arrayGrowthSize;
import static at.ac.tuwien.kr.alpha.Util.oops;

/**
 * Manages the execution of {@link AtomCallback}s, i.e., callbacks when atoms change their truth value.
 *
 * Copyright (c) 2020, the Alpha Team.
 */
class AtomCallbackManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(AtomCallbackManager.class);

	private AtomCallback[] atomCallbacks = new AtomCallback[0];

	void recordCallback(int atom, AtomCallback atomCallback) {
		if (atomCallbacks[atom] != null && !atomCallbacks[atom].equals(atomCallback)) {
			throw oops("Recording different callbacks for one atom. Atom: " + atom);
		}
		atomCallbacks[atom] = atomCallback;
	}

	void callbackOnChanged(int atom) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Callback received on callback atom: {}", atom);
		}
		AtomCallback atomCallback = atomCallbacks[atom];
		if (atomCallback != null) {
			atomCallback.processCallback();
		}
	}

	void growForMaxAtomId(int maxAtomId) {
		// Grow arrays only if needed.
		if (atomCallbacks.length > maxAtomId) {
			return;
		}
		// Grow to default size, except if bigger array is required due to maxAtomId.
		int newCapacity = arrayGrowthSize(atomCallbacks.length);
		if (newCapacity < maxAtomId + 1) {
			newCapacity = maxAtomId + 1;
		}
		atomCallbacks = Arrays.copyOf(atomCallbacks, newCapacity);
	}

	/**
	 * Interface for callbacks to be called when atoms change their truth value.
	 */
	interface AtomCallback {

		void processCallback();
	}
}
