/**
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
package at.ac.tuwien.kr.alpha.core.grounder;

import at.ac.tuwien.kr.alpha.api.config.InputConfig;
import at.ac.tuwien.kr.alpha.api.grounder.heuristics.GrounderHeuristicsConfiguration;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.core.common.AtomStore;
import at.ac.tuwien.kr.alpha.core.grounder.bridges.Bridge;
import at.ac.tuwien.kr.alpha.core.programs.CompiledProgram;

public final class GrounderFactory {
	public static Grounder getInstance(String name, CompiledProgram program, AtomStore atomStore, java.util.function.Predicate<Predicate> filter,
			GrounderHeuristicsConfiguration heuristicsConfiguration, boolean debugInternalChecks, Bridge... bridges) {
		switch (name.toLowerCase()) {
			case "naive":
				return new NaiveGrounder(program, atomStore, filter, heuristicsConfiguration, debugInternalChecks, bridges);
		}
		throw new IllegalArgumentException("Unknown grounder requested.");
	}
	
	public static Grounder getInstance(String name, CompiledProgram program, AtomStore atomStore, java.util.function.Predicate<Predicate> filter,
			GrounderHeuristicsConfiguration heuristicsConfiguration, boolean debugInternalChecks) {
		return getInstance(name, program, atomStore, filter, heuristicsConfiguration, debugInternalChecks, new Bridge[] {});
	}

	public static Grounder getInstance(String name, CompiledProgram program, AtomStore atomStore, boolean debugInternalChecks) {
		return getInstance(name, program, atomStore, InputConfig.DEFAULT_FILTER,
				new GrounderHeuristicsConfiguration(), debugInternalChecks);
	}
}
