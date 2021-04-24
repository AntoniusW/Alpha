/**
 * Copyright (c) 2019, the Alpha Team.
 * All rights reserved.
 * <p>
 * Additional changes made by Siemens.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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
package at.ac.tuwien.kr.alpha.core.programs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.InlineDirectives;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.rules.Head;
import at.ac.tuwien.kr.alpha.api.rules.Rule;
import at.ac.tuwien.kr.alpha.core.parser.InlineDirectivesImpl;

/**
 * Alpha-internal representation of an ASP program, i.e., a set of ASP rules.
 * <p>
 * Copyright (c) 2017-2019, the Alpha Team.
 */
public class InputProgram extends AbstractProgram<Rule<Head>> implements ASPCore2Program{

	public static final InputProgram EMPTY = new InputProgram(Collections.emptyList(), Collections.emptyList(), new InlineDirectivesImpl());

	public InputProgram(List<Rule<Head>> rules, List<Atom> facts, InlineDirectives inlineDirectives) {
		super(rules, facts, inlineDirectives);
	}

	public InputProgram() {
		super(new ArrayList<>(), new ArrayList<>(), new InlineDirectivesImpl());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(ASPCore2Program prog) {
		return new Builder(prog);
	}

	/**
	 * Builder for more complex program construction scenarios, ensuring that an {@link InputProgram} is immutable
	 */
	public static class Builder {

		private List<Rule<Head>> rules = new ArrayList<>();
		private List<Atom> facts = new ArrayList<>();
		private InlineDirectives inlineDirectives = new InlineDirectivesImpl();

		public Builder(ASPCore2Program prog) {
			this.addRules(prog.getRules());
			this.addFacts(prog.getFacts());
			this.addInlineDirectives(prog.getInlineDirectives());
		}

		public Builder() {

		}

		public Builder addRules(List<Rule<Head>> rules) {
			this.rules.addAll(rules);
			return this;
		}

		public Builder addRule(Rule<Head> r) {
			this.rules.add(r);
			return this;
		}

		public Builder addFacts(List<Atom> facts) {
			this.facts.addAll(facts);
			return this;
		}

		public Builder addFact(Atom fact) {
			this.facts.add(fact);
			return this;
		}

		public Builder addInlineDirectives(InlineDirectives inlineDirectives) {
			this.inlineDirectives.accumulate(inlineDirectives);
			return this;
		}

		public Builder accumulate(ASPCore2Program prog) {
			return this.addRules(prog.getRules()).addFacts(prog.getFacts()).addInlineDirectives(prog.getInlineDirectives());
		}

		public InputProgram build() {
			return new InputProgram(this.rules, this.facts, this.inlineDirectives);
		}
	}

}
