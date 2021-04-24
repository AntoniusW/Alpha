/**
 * Copyright (c) 2016-2018, the Alpha Team.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.grounder.Instance;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;

/**
 * Copyright (c) 2016, the Alpha Team.
 */
public class IndexedInstanceStorageTest {
	@Test
	public void testIndexedInstanceStorage() {
		IndexedInstanceStorage storage = new IndexedInstanceStorage(Predicates.getPredicate("p", 4), true);
		storage.addIndexPosition(0);
		storage.addIndexPosition(2);
		ConstantTerm<String> t0 = Terms.newConstant("0");
		ConstantTerm<String> t1 = Terms.newConstant("1");
		ConstantTerm<String> t2 = Terms.newConstant("2");
		ConstantTerm<String> t3 = Terms.newConstant("3");
		ConstantTerm<String> t4 = Terms.newConstant("4");
		ConstantTerm<String> t5 = Terms.newConstant("5");

		Instance badInst1 = new Instance(t1, t1, t0);
		Instance badInst2 = new Instance(t5, t5, t5, t5, t5);

		try {
			storage.addInstance(badInst1);
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("Instance length does not match arity of IndexedInstanceStorage"));
		}

		try {
			storage.addInstance(badInst2);
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("Instance length does not match arity of IndexedInstanceStorage"));
		}

		Instance inst1 = new Instance(t1, t1, t1, t1);
		Instance inst2 = new Instance(t1, t2, t3, t4);
		Instance inst3 = new Instance(t4, t3, t3, t5);
		Instance inst4 = new Instance(t1, t2, t1, t1);
		Instance inst5 = new Instance(t5, t4, t3, t2);

		storage.addInstance(inst1);
		storage.addInstance(inst2);
		storage.addInstance(inst3);
		storage.addInstance(inst4);
		storage.addInstance(inst5);

		List<Instance> matching3 = storage.getInstancesMatchingAtPosition(t3, 2);
		assertEquals(matching3.size(), 3);
		assertTrue(matching3.contains(new Instance(t1, t2, t3, t4)));
		assertTrue(matching3.contains(new Instance(t4, t3, t3, t5)));
		assertTrue(matching3.contains(new Instance(t5, t4, t3, t2)));
		assertFalse(matching3.contains(new Instance(t1, t1, t1, t1)));

		List<Instance> matching1 = storage.getInstancesMatchingAtPosition(t2, 0);
		assertEquals(matching1.size(), 0);
	}

}