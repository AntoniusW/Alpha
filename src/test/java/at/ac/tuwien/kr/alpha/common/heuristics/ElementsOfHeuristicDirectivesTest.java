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

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static at.ac.tuwien.kr.alpha.Util.asSet;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.FALSE;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.MBT;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests some properties (e.g., equals(), hashCode(), and toString() implementations) of {@link at.ac.tuwien.kr.alpha.common.HeuristicDirective},
 * {@link HeuristicDirectiveBody}, {@link HeuristicDirectiveLiteral}, and {@link HeuristicDirectiveAtom}.
 */
public class ElementsOfHeuristicDirectivesTest {

	@Test
	public void heuristicAtomsWithDifferentSigns() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final HeuristicDirectiveAtom atom1 = HeuristicDirectiveAtom.body(asSet(TRUE), a);
		final HeuristicDirectiveAtom atom2 = HeuristicDirectiveAtom.body(asSet(TRUE, FALSE), a);
		assertNotEquals(atom1, atom2);
		assertNotEquals(atom1.hashCode(), atom2.hashCode());
		assertNotEquals(atom1.toString(), atom2.toString());
		assertEquals(2, asSet(atom1, atom2).size());
	}

	@Test
	public void heuristicAtomsWithDifferentBasicAtoms() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final BasicAtom b = new BasicAtom(Predicate.getInstance("b", 0));
		final HeuristicDirectiveAtom atom1 = HeuristicDirectiveAtom.body(asSet(), a);
		final HeuristicDirectiveAtom atom2 = HeuristicDirectiveAtom.body(asSet(), b);
		assertNotEquals(atom1, atom2);
		assertNotEquals(atom1.hashCode(), atom2.hashCode());
		assertNotEquals(atom1.toString(), atom2.toString());
		assertEquals(2, asSet(atom1, atom2).size());
	}

	@Test
	public void equalHeuristicAtoms() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final HeuristicDirectiveAtom atom1 = HeuristicDirectiveAtom.body(asSet(MBT, TRUE), a);
		final HeuristicDirectiveAtom atom2 = HeuristicDirectiveAtom.body(asSet(MBT, TRUE), a);
		assertEquals(atom1, atom2);
		assertEquals(atom1.hashCode(), atom2.hashCode());
		assertEquals(atom1.toString(), atom2.toString());
		assertEquals(1, asSet(atom1, atom2).size());
	}

	@Test
	public void heuristicLiteralsWithDifferentSigns() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final HeuristicDirectiveLiteral literal1 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(TRUE), a), true);
		final HeuristicDirectiveLiteral literal2 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(TRUE, FALSE), a), true);
		assertNotEquals(literal1, literal2);
		assertNotEquals(literal1.hashCode(), literal2.hashCode());
		assertNotEquals(literal1.toString(), literal2.toString());
		assertEquals(2, asSet(literal1, literal2).size());
	}

	@Test
	public void heuristicLiteralsWithDifferentBasicAtoms() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final BasicAtom b = new BasicAtom(Predicate.getInstance("b", 0));
		final HeuristicDirectiveLiteral literal1 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(), a), false);
		final HeuristicDirectiveLiteral literal2 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(), b), false);
		assertNotEquals(literal1, literal2);
		assertNotEquals(literal1.hashCode(), literal2.hashCode());
		assertNotEquals(literal1.toString(), literal2.toString());
		assertEquals(2, asSet(literal1, literal2).size());
	}

	@Test
	public void heuristicLiteralsWithDifferentNegatedness() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final HeuristicDirectiveLiteral literal1 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(MBT, TRUE), a), false);
		final HeuristicDirectiveLiteral literal2 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(MBT, TRUE), a), true);
		assertNotEquals(literal1, literal2);
		assertNotEquals(literal1.hashCode(), literal2.hashCode());
		assertNotEquals(literal1.toString(), literal2.toString());
		assertEquals(2, asSet(literal1, literal2).size());
	}

	@Test
	public void equalHeuristicLiterals() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final HeuristicDirectiveLiteral literal1 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(MBT, TRUE), a), false);
		final HeuristicDirectiveLiteral literal2 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(MBT, TRUE), a), false);
		assertEquals(literal1, literal2);
		assertEquals(literal1.hashCode(), literal2.hashCode());
		assertEquals(literal1.toString(), literal2.toString());
		assertEquals(1, asSet(literal1, literal2).size());
	}

	@Test
	public void heuristicDirectiveBodiesWithDifferentPositiveBodies() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final BasicAtom b = new BasicAtom(Predicate.getInstance("b", 0));
		final BasicAtom c = new BasicAtom(Predicate.getInstance("c", 0));
		final HeuristicDirectiveLiteral posLiteral1 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(MBT, TRUE), a), true);
		final HeuristicDirectiveLiteral posLiteral2 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(TRUE), b), true);
		final HeuristicDirectiveLiteral negLiteral = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(FALSE), c), false);
		final HeuristicDirectiveBody body1 = new HeuristicDirectiveBody(Arrays.asList(posLiteral1, negLiteral));
		final HeuristicDirectiveBody body2 = new HeuristicDirectiveBody(Arrays.asList(posLiteral1, posLiteral2, negLiteral));
		assertNotEquals(body1, body2);
		assertNotEquals(body1.hashCode(), body2.hashCode());
		assertNotEquals(body1.toString(), body2.toString());
		assertEquals(2, asSet(body1, body2).size());
	}

	@Test
	public void heuristicDirectiveBodiesWithDifferentNegativeBodies() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final BasicAtom b = new BasicAtom(Predicate.getInstance("b", 0));
		final HeuristicDirectiveLiteral posLiteral = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(MBT, TRUE), a), true);
		final HeuristicDirectiveLiteral negLiteral1 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(TRUE), b), false);
		final HeuristicDirectiveLiteral negLiteral2 = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(MBT, TRUE), b), false);
		final HeuristicDirectiveBody body1 = new HeuristicDirectiveBody(Arrays.asList(posLiteral, negLiteral1));
		final HeuristicDirectiveBody body2 = new HeuristicDirectiveBody(Arrays.asList(posLiteral, negLiteral2));
		assertNotEquals(body1, body2);
		assertNotEquals(body1.hashCode(), body2.hashCode());
		assertNotEquals(body1.toString(), body2.toString());
		assertEquals(2, asSet(body1, body2).size());
	}

	@Test
	public void equalEmptyHeuristicDirectiveBodies() {
		final HeuristicDirectiveBody body1 = new HeuristicDirectiveBody(Collections.emptyList());
		final HeuristicDirectiveBody body2 = new HeuristicDirectiveBody(Collections.emptyList());
		assertEquals(body1, body2);
		assertEquals(body1.hashCode(), body2.hashCode());
		assertEquals(body1.toString(), body2.toString());
		assertEquals(1, asSet(body1, body2).size());
	}

	@Test
	public void equalNonEmptyHeuristicDirectiveBodies() {
		final BasicAtom a = new BasicAtom(Predicate.getInstance("a", 0));
		final HeuristicDirectiveLiteral posLiteral = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(MBT), a), true);
		final HeuristicDirectiveLiteral negLiteral = new HeuristicDirectiveLiteral(HeuristicDirectiveAtom.body(asSet(TRUE), a), false);
		final HeuristicDirectiveBody body1 = new HeuristicDirectiveBody(Arrays.asList(posLiteral, negLiteral));
		final HeuristicDirectiveBody body2 = new HeuristicDirectiveBody(Arrays.asList(posLiteral, negLiteral));
		assertEquals(body1, body2);
		assertEquals(body1.hashCode(), body2.hashCode());
		assertEquals(body1.toString(), body2.toString());
		assertEquals(1, asSet(body1, body2).size());
	}
}
