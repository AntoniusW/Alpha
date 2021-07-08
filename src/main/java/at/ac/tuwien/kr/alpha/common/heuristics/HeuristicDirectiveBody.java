/*
 *  Copyright (c) 2020 Siemens AG
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1) Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *  2) Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package at.ac.tuwien.kr.alpha.common.heuristics;

import at.ac.tuwien.kr.alpha.common.atoms.FixedInterpretationLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static at.ac.tuwien.kr.alpha.Util.join;

public class HeuristicDirectiveBody {

	private final Set<HeuristicDirectiveAtom> bodyAtomsPositive;
	private final Set<HeuristicDirectiveAtom> bodyAtomsNegative;

	public HeuristicDirectiveBody(Collection<HeuristicDirectiveAtom> bodyAtomsPositive, Collection<HeuristicDirectiveAtom> bodyAtomsNegative) {
		// transform list to linked set to enable equality check with set semantics:
		this.bodyAtomsPositive = Collections.unmodifiableSet(new LinkedHashSet<>(bodyAtomsPositive));
		this.bodyAtomsNegative = Collections.unmodifiableSet(new LinkedHashSet<>(bodyAtomsNegative));
	}

	public HeuristicDirectiveBody(List<HeuristicDirectiveLiteral> bodyLiterals) {
		this(
				bodyLiterals.stream().filter(l -> !l.isNegated()).map(HeuristicDirectiveLiteral::getAtom).collect(Collectors.toList()),
				bodyLiterals.stream().filter(l -> l.isNegated()).map(HeuristicDirectiveLiteral::getAtom).collect(Collectors.toList())
		);
	}

	public Collection<HeuristicDirectiveAtom> getBodyAtomsPositive() {
		return bodyAtomsPositive;
	}

	public Collection<HeuristicDirectiveAtom> getBodyAtomsNegative() {
		return bodyAtomsNegative;
	}

	/**
	 * Returns relevant parts of this body as a rule body, which is used to represent heuristic directives as rules.
	 * @return the basic atoms in the positive part of this body whose heuristic signs do not include F, without heuristic signs; and all non-basic literals in this body
	 */
	public List<Literal> toReducedRuleBody() {
		final List<Literal> relevantLiterals = new ArrayList<>();
		for (HeuristicDirectiveAtom heuristicDirectiveAtom : bodyAtomsPositive) {
			final Literal literal = heuristicDirectiveAtom.getAtom().toLiteral();
			if (literal instanceof FixedInterpretationLiteral) {
				relevantLiterals.add(literal);
			} else {
				final Set<ThriceTruth> signs = heuristicDirectiveAtom.getSigns();
				if (signs == null || !signs.contains(ThriceTruth.FALSE)) {
					relevantLiterals.add(literal);
				}
			}
		}
		for (HeuristicDirectiveAtom heuristicDirectiveAtom : bodyAtomsNegative) {
			final Literal literal = heuristicDirectiveAtom.getAtom().toLiteral(false);
			if (literal instanceof FixedInterpretationLiteral) {
				relevantLiterals.add(literal);
			}
		}
		return relevantLiterals;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		HeuristicDirectiveBody that = (HeuristicDirectiveBody) o;
		return bodyAtomsPositive.equals(that.bodyAtomsPositive) &&
				bodyAtomsNegative.equals(that.bodyAtomsNegative);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bodyAtomsPositive, bodyAtomsNegative);
	}

	@Override
	public String toString() {
		return join(
				join(
						"",
						bodyAtomsPositive,
						bodyAtomsNegative.size() > 0 ? ", not " : ""
				),
				bodyAtomsNegative,
				", not ",
				""
		);
	}
}
