/*
 * Copyright (c) 2019-2021 Siemens AG
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
package at.ac.tuwien.kr.alpha.solver.heuristics.domspec;

import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNull;

/**
 * Test {@link EmptyDomainSpecificHeuristicsStore}
 */
public class EmptyDomainSpecificHeuristicsStoreTest {
	private static final int INITIAL_GENERATED_ID = 100;

	private DefaultDomainSpecificHeuristicsStore store;
	private AtomicInteger idGenerator = new AtomicInteger(INITIAL_GENERATED_ID);

	@Before
	public void setUp() {
		this.store = new DefaultDomainSpecificHeuristicsStore();
		this.store.setChoiceManager(null);
	}

	@Test
	public void testInsert_1_atom() {
		int id = idGenerator.getAndIncrement();
		HeuristicDirectiveValues info1 = info(1, 1, 1);
		store.addInfo(id, info1);
		assertNull(store.poll());
	}

	private HeuristicDirectiveValues info(int atom, int weight, int level) {
		return new HeuristicDirectiveValues(atom, null, weight, level, true);
	}

}
