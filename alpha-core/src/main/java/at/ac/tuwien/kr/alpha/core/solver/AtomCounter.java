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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;

/**
 * Counts the number of ground atoms stored for each type (i.e., subclass of {@link AbstractAtom}.
 * For every atom, only the counter for one class (the most specific one) is incremented,
 * not the counters for more general classes of which the atom is also an instance.
 */
public class AtomCounter {

	private final Map<String, Integer> countByType = new HashMap<>();

	public void add(Atom atom) {
		countByType.compute(atom.getClass().getSimpleName(), (k, v) -> (v == null) ? 1 : v + 1);
	}

	/**
	 * @param type the class of atoms to count
	 * @return the number of atoms of the given type
	 */
	public int getNumberOfAtoms(String type) {
		return countByType.getOrDefault(type, 0);
	}

	/**
	 * @return a string giving statistics on numbers of atoms by type
	 */
	public String getStatsByType() {
		List<String> statsList = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : countByType.entrySet()) {
			statsList.add(entry.getKey() + ": " + entry.getValue());
		}
		Collections.sort(statsList);
		return String.join(" ", statsList);
	}

}
