/*
 * Copyright (c) 2018-2021 Siemens AG
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
package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.Directive;
import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.head.Head;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.grounder.atoms.HeuristicAtom;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;
import at.ac.tuwien.kr.alpha.solver.heuristics.HeuristicsConfiguration;

import java.util.Collection;
import java.util.Iterator;

/**
 * Converts all {@link HeuristicDirective}s to {@link BasicRule}s if
 * {@link HeuristicsConfiguration#isRespectDomspecHeuristics()} is {@code true},
 * otherwise removes all heuristic directives.
 */
public class HeuristicDirectiveToRule extends ProgramTransformation<InputProgram, InputProgram> {

	private final boolean respectDomspecHeuristics;

	public HeuristicDirectiveToRule(HeuristicsConfiguration heuristicsConfiguration) {
		this.respectDomspecHeuristics = heuristicsConfiguration.isRespectDomspecHeuristics();
	}

	public HeuristicDirectiveToRule(boolean respectDomspecHeuristics) {
		this.respectDomspecHeuristics = respectDomspecHeuristics;
	}

	@Override
	public InputProgram apply(InputProgram inputProgram) {
		Collection<Directive> heuristicDirectives = inputProgram.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		if (heuristicDirectives == null) {
			return inputProgram;
		}

		final InputProgram.Builder prgBuilder = InputProgram.builder().addFacts(inputProgram.getFacts()).addRules(inputProgram.getRules());
		final InlineDirectives copiedDirectives = new InlineDirectives();
		copiedDirectives.accumulate(inputProgram.getInlineDirectives());

		if (respectDomspecHeuristics) {
			Iterator<Directive> directivesIterator = copiedDirectives.getDirectives(InlineDirectives.DIRECTIVE.heuristic).iterator();
			while (directivesIterator.hasNext()) {
				Directive directive = directivesIterator.next();
				transformAndAddToProgram((HeuristicDirective) directive, prgBuilder);
				directivesIterator.remove();
			}
		} else {
			copiedDirectives.getDirectives(InlineDirectives.DIRECTIVE.heuristic).clear();
		}
		prgBuilder.addInlineDirectives(copiedDirectives);

		return prgBuilder.build();
	}

	private void transformAndAddToProgram(HeuristicDirective heuristicDirective, InputProgram.Builder prgBuilder) {
		final Head head = new NormalHead(HeuristicAtom.fromHeuristicDirective(heuristicDirective));
		final BasicRule rule = new BasicRule(head, heuristicDirective.getBody().toReducedRuleBody());
		prgBuilder.addRule(rule);
	}
}
