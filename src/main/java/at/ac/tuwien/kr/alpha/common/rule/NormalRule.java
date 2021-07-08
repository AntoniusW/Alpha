/*
 * Copyright (c) 2016-2021, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.common.rule;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.grounder.atoms.HeuristicAtom;

import java.util.ArrayList;

/**
 * A rule that has a normal head, i.e. just one head atom, no disjunction or choice heads allowed.
 * Currently, any constructs such as aggregates, intervals, etc. in the rule body are allowed.
 * 
 * Copyright (c) 2019, the Alpha Team.
 */
public class NormalRule extends AbstractRule<NormalHead> {

	public NormalRule(NormalHead head, Iterable<Literal> body) {
		super(head, body);
	}

	public static NormalRule fromBasicRule(BasicRule rule) {
		Atom headAtom = null;
		if (!rule.isConstraint()) {
			if (!(rule.getHead() instanceof NormalHead)) {
				throw Util.oops("Trying to construct a NormalRule from rule with non-normal head! Head type is: " + rule.getHead().getClass().getSimpleName());
			}
			headAtom = ((NormalHead) rule.getHead()).getAtom();
		}
		return new NormalRule(headAtom != null ? new NormalHead(headAtom) : null, new ArrayList<>(rule.getBody()));
	}

	public boolean isGround() {
		if (!isConstraint() && !this.getHead().isGround()) {
			return false;
		}
		for (Literal bodyElement : this.getBody()) {
			if (!bodyElement.isGround()) {
				return false;
			}
		}
		return true;
	}

	public Atom getHeadAtom() {
		return this.isConstraint() ? null : this.getHead().getAtom();
	}

	public boolean isHeuristicRule() {
		return getHeadAtom() instanceof HeuristicAtom;
	}

}
