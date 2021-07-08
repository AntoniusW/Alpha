/*
 * Copyright (c) 2020-2021 Siemens AG
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
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveAtom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveBody;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import org.apache.commons.collections4.SetUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.Util.asSet;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.*;

/**
 * Transforms sign sets as described in Section 5.1 of our paper
 * "Domain-Specific Heuristics in Answer Set Programming: A Declarative Non-Monotonic Approach"
 */
public class SignSetTransformation extends ProgramTransformation<InputProgram, InputProgram> {

	// sign sets to be replaced:
	public static final Set<ThriceTruth> SET_ALL_SIGNS = asSet(ThriceTruth.values());
	public static final Set<ThriceTruth> SET_FM = asSet(FALSE, MBT);
	public static final Set<ThriceTruth> SET_FT = asSet(FALSE, TRUE);
	public static final Set<Set<ThriceTruth>> SETS_SPLITTABLE = asSet(SET_ALL_SIGNS, SET_FM, SET_FT);
	public static final Set<ThriceTruth> SET_M = asSet(MBT);

	// sign sets to be used as replacements:
	public static final Set<ThriceTruth> SET_F = asSet(FALSE);
	public static final Set<ThriceTruth> SET_MT = asSet(MBT, TRUE);
	public static final Set<ThriceTruth> SET_T = asSet(TRUE);

	@Override
	public InputProgram apply(InputProgram inputProgram) {
		Collection<Directive> heuristicDirectives = inputProgram.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
		if (heuristicDirectives == null) {
			return inputProgram;
		}

		Queue<HeuristicDirective> queue = toQueue(heuristicDirectives);
		List<Directive> transformedDirectives = transformDirectives(queue);

		final InlineDirectives copiedDirectives = new InlineDirectives();
		copiedDirectives.accumulate(inputProgram.getInlineDirectives());
		copiedDirectives.replaceDirectives(InlineDirectives.DIRECTIVE.heuristic, transformedDirectives);
		final InputProgram.Builder prgBuilder = InputProgram.builder().addFacts(inputProgram.getFacts()).addRules(inputProgram.getRules());
		prgBuilder.addInlineDirectives(copiedDirectives);

		return prgBuilder.build();
	}

	private static List<Directive> transformDirectives(Queue<HeuristicDirective> queue) {
		final List<Directive> finalDirectives = new ArrayList<>(queue.size());
		HeuristicDirective heuristicDirective;
		loopHeuristicDirectives: while ((heuristicDirective = queue.poll()) != null) {
			for (HeuristicDirectiveAtom atom : heuristicDirective.getBody().getBodyAtomsPositive()) {
				if (SETS_SPLITTABLE.contains(atom.getSigns())) {
					queue.addAll(eliminatePositiveSplittable(heuristicDirective, atom));
					continue loopHeuristicDirectives;
				} else if (SET_M.equals(atom.getSigns())) {
					queue.add(eliminatePositiveM(heuristicDirective, atom));
					continue loopHeuristicDirectives;
				}
			}
			for (HeuristicDirectiveAtom atom : heuristicDirective.getBody().getBodyAtomsNegative()) {
				if (SETS_SPLITTABLE.contains(atom.getSigns())) {
					queue.add(eliminateNegativeSplittable(heuristicDirective, atom));
					continue loopHeuristicDirectives;
				} else if (SET_M.equals(atom.getSigns())) {
					queue.addAll(eliminateNegativeM(heuristicDirective, atom));
					continue loopHeuristicDirectives;
				}
			}
			finalDirectives.add(heuristicDirective);
		}
		return finalDirectives;
	}

	private static Queue<HeuristicDirective> toQueue(Collection<Directive> heuristicDirectives) {
		Queue<HeuristicDirective> queue = new LinkedList<>();
		for (Directive heuristicDirective : heuristicDirectives) {
			queue.add((HeuristicDirective)heuristicDirective);
		}
		return queue;
	}

	private static Collection<HeuristicDirective> eliminatePositiveSplittable(HeuristicDirective heuristicDirective, HeuristicDirectiveAtom atom) {
		final List<HeuristicDirective> transformedDirectives = new ArrayList<>(2);
		transformedDirectives.add(replaceSignInCondition(heuristicDirective, atom, SetUtils.difference(atom.getSigns(), SET_F), true));
		transformedDirectives.add(replaceSignInCondition(heuristicDirective, atom, SET_F, true));
		return transformedDirectives;
	}

	private static HeuristicDirective eliminateNegativeSplittable(HeuristicDirective heuristicDirective, HeuristicDirectiveAtom atom) {
		final List<HeuristicDirectiveAtom> transformedNegativeAtoms = new ArrayList<>(heuristicDirective.getBody().getBodyAtomsNegative());
		transformedNegativeAtoms.remove(atom);
		transformedNegativeAtoms.add(HeuristicDirectiveAtom.body(SetUtils.difference(atom.getSigns(), SET_F), atom.getAtom()));
		transformedNegativeAtoms.add(HeuristicDirectiveAtom.body(SET_F, atom.getAtom()));
		final HeuristicDirectiveBody transformedBody = new HeuristicDirectiveBody(heuristicDirective.getBody().getBodyAtomsPositive(), transformedNegativeAtoms);
		return replaceBody(heuristicDirective, transformedBody);
	}

	private static HeuristicDirective eliminatePositiveM(HeuristicDirective heuristicDirective, HeuristicDirectiveAtom atom) {
		final List<HeuristicDirectiveAtom> transformedPositiveAtoms = new ArrayList<>(heuristicDirective.getBody().getBodyAtomsPositive());
		final List<HeuristicDirectiveAtom> transformedNegativeAtoms = new ArrayList<>(heuristicDirective.getBody().getBodyAtomsNegative());
		transformedPositiveAtoms.remove(atom);
		transformedPositiveAtoms.add(HeuristicDirectiveAtom.body(SET_MT, atom.getAtom()));
		transformedNegativeAtoms.add(HeuristicDirectiveAtom.body(SET_T, atom.getAtom()));
		final HeuristicDirectiveBody transformedBody = new HeuristicDirectiveBody(transformedPositiveAtoms, transformedNegativeAtoms);
		return replaceBody(heuristicDirective, transformedBody);
	}

	private static Collection<HeuristicDirective> eliminateNegativeM(HeuristicDirective heuristicDirective, HeuristicDirectiveAtom atom) {
		final List<HeuristicDirective> transformedDirectives = new ArrayList<>(2);
		HeuristicDirective transformedDirective1 = addAtomToPositiveCondition(heuristicDirective, HeuristicDirectiveAtom.body(SET_FT, atom.getAtom()));
		transformedDirective1 = removeAtomFromNegativeCondition(transformedDirective1, atom);
		HeuristicDirective transformedDirective2 = replaceSignInCondition(heuristicDirective, atom, SET_ALL_SIGNS, false);
		transformedDirectives.add(transformedDirective1);
		transformedDirectives.add(transformedDirective2);
		return transformedDirectives;
	}

	private static HeuristicDirective replaceSignInCondition(HeuristicDirective heuristicDirective, HeuristicDirectiveAtom atom, Set<ThriceTruth> signSet, boolean usePositiveCondition) {
		final HeuristicDirectiveBody body = heuristicDirective.getBody();
		final List<HeuristicDirectiveAtom> transformedAtoms = new ArrayList<>(usePositiveCondition ? body.getBodyAtomsPositive() : body.getBodyAtomsNegative());
		transformedAtoms.remove(atom);
		transformedAtoms.add(HeuristicDirectiveAtom.body(signSet, atom.getAtom()));
		final HeuristicDirectiveBody transformedBody = usePositiveCondition ? new HeuristicDirectiveBody(transformedAtoms, body.getBodyAtomsNegative()) : new HeuristicDirectiveBody(body.getBodyAtomsPositive(), transformedAtoms);
		return replaceBody(heuristicDirective, transformedBody);
	}

	private static HeuristicDirective addAtomToPositiveCondition(HeuristicDirective heuristicDirective, HeuristicDirectiveAtom atom) {
		final HeuristicDirectiveBody body = heuristicDirective.getBody();
		final List<HeuristicDirectiveAtom> transformedPositiveAtoms = new ArrayList<>(body.getBodyAtomsPositive());
		transformedPositiveAtoms.add(atom);
		final HeuristicDirectiveBody transformedBody = new HeuristicDirectiveBody(transformedPositiveAtoms, body.getBodyAtomsNegative());
		return replaceBody(heuristicDirective, transformedBody);
	}

	private static HeuristicDirective removeAtomFromNegativeCondition(HeuristicDirective heuristicDirective, HeuristicDirectiveAtom atom) {
		final HeuristicDirectiveBody body = heuristicDirective.getBody();
		final List<HeuristicDirectiveAtom> transformedNegativeAtoms = new ArrayList<>(body.getBodyAtomsNegative());
		transformedNegativeAtoms.remove(atom);
		final HeuristicDirectiveBody transformedBody = new HeuristicDirectiveBody(body.getBodyAtomsPositive(), transformedNegativeAtoms);
		return replaceBody(heuristicDirective, transformedBody);
	}

	private static HeuristicDirective replaceBody(HeuristicDirective heuristicDirective, HeuristicDirectiveBody transformedBody) {
		return new HeuristicDirective(heuristicDirective.getHead(), transformedBody, heuristicDirective.getWeightAtLevel());
	}
}
