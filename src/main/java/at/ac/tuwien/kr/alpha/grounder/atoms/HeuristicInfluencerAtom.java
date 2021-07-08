/*
 * Copyright (c) 2018-2021, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.grounder.atoms;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.Substitution;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;

import java.util.List;
import java.util.Set;

import static at.ac.tuwien.kr.alpha.Util.join;
import static at.ac.tuwien.kr.alpha.common.heuristics.HeuristicSignSetUtil.toName;
import static java.util.Arrays.asList;

public class HeuristicInfluencerAtom extends BasicAtom {
	public static final Predicate ON = Predicate.getInstance("HeuOn", 2, true);
	public static final Predicate OFF = Predicate.getInstance("HeuOff", 2, true);

	private HeuristicInfluencerAtom(Predicate predicate, Term heuristicID, Term signSetName) {
		super(predicate, asList(heuristicID, signSetName));
	}

	private HeuristicInfluencerAtom(Predicate predicate, int id, String signSetName) {
		this(predicate, ConstantTerm.getInstance(Integer.toString(id)), ConstantTerm.getInstance(signSetName));
	}

	public static HeuristicInfluencerAtom on(int id, Set<ThriceTruth> signSet) {
		return new HeuristicInfluencerAtom(ON, id, toName(signSet));
	}

	public static HeuristicInfluencerAtom off(int id, Set<ThriceTruth> signSet) {
		return new HeuristicInfluencerAtom(OFF, id, toName(signSet));
	}

	public static HeuristicInfluencerAtom get(boolean on, int id, Set<ThriceTruth> signSet) {
		return new HeuristicInfluencerAtom(on ? ON : OFF, id, toName(signSet));
	}

	@Override
	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public List<Term> getTerms() {
		return terms;
	}

	@Override
	public ChoiceAtom withTerms(List<Term> terms) {
		throw new UnsupportedOperationException("Changing terms is not supported for HeuristicInfluencerAtoms!");
	}

	@Override
	public boolean isGround() {
		// NOTE: terms are ConstantTerms, which are ground by definition.
		return true;
	}
	
	@Override
	public BasicLiteral toLiteral(boolean negated) {
		throw new UnsupportedOperationException(this.getClass().getName() + " cannot be literalized");
	}

	@Override
	public HeuristicInfluencerAtom substitute(Substitution substitution) {
		return this;
	}

	@Override
	public String toString() {
		return join(predicate.getName() + "(", terms, ")");
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		HeuristicInfluencerAtom that = (HeuristicInfluencerAtom) o;

		return predicate.equals(that.predicate) && terms.equals(that.terms);
	}

	@Override
	public int hashCode() {
		return 31 * predicate.hashCode() + terms.hashCode();
	}
}