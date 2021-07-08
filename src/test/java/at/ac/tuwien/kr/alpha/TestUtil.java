/*
 * Copyright (c) 2019-2020 Siemens AG
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
package at.ac.tuwien.kr.alpha;

import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

/**
 * Provides utility methods for test cases
 *
 */
public class TestUtil {

	public static Atom atom(String predicateName, String... termStrings) {
		Term[] terms = new Term[termStrings.length];
		for (int i = 0; i < termStrings.length; i++) {
			String termString = termStrings[i];
			if (StringUtils.isAllUpperCase(termString.substring(0, 1))) {
				terms[i] = VariableTerm.getInstance(termString);
			} else {
				terms[i] = ConstantTerm.getInstance(termString);
			}
		}
		return new BasicAtom(Predicate.getInstance(predicateName, terms.length), terms);
	}

	public static BasicAtom atom(String predicateName, int... termInts) {
		Term[] terms = new Term[termInts.length];
		for (int i = 0; i < termInts.length; i++) {
			terms[i] = ConstantTerm.getInstance(termInts[i]);
		}
		return new BasicAtom(Predicate.getInstance(predicateName, terms.length), terms);
	}
	
	public static void printNoGoods(AtomStore atomStore, Collection<NoGood> noGoods) {
		System.out.println(noGoods.stream().map(atomStore::noGoodToString).collect(Collectors.toSet()));
	}

	public static void checkExpectedAtomsInAnswerSet(AnswerSet answerSet, List<BasicAtom> atomsExpectedInAnswerSet, List<BasicAtom> atomsNotExpectedInAnswerSet) {
		for (BasicAtom atom : atomsExpectedInAnswerSet) {
			if (!answerSet.getPredicateInstances(atom.getPredicate()).contains(atom)) {
				fail("Answer set does not contain " + atom);
			}
		}
		for (BasicAtom atom : atomsNotExpectedInAnswerSet) {
			if (answerSet.getPredicateInstances(atom.getPredicate()).contains(atom)) {
				fail("Answer set contains " + atom);
			}
		}
	}

}
