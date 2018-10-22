/**
 * Copyright (c) 2018 Siemens AG
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
package at.ac.tuwien.kr.alpha.solver;

/**
 * Offers methods to estimate the effect of propagating binary nogoods.
 */
public class BinaryNoGoodPropagationEstimation {
	
	private final WritableAssignment assignment;
	private final NoGoodStorePrivilegingBinaryNoGoods store;
	
	public BinaryNoGoodPropagationEstimation(WritableAssignment assignment, NoGoodStorePrivilegingBinaryNoGoods store) {
		super();
		this.assignment = assignment;
		this.store = store;
	}
	
	public boolean hasBinaryNoGoods() {
		return store.hasBinaryNoGoods();
	}

	/**
	 * Computes the number of direct consequences of propagating binary nogoods after
	 * assigning {@code value} to {@code atom}.
	 * 
	 * In other words, assigns {@code value} to {@code atom}, propagates only binary nogoods,
	 * backtracks, and returns the number of atoms that have been assigned additionally during
	 * this process.
	 * 
	 * @param atom
	 * @param value
	 * @return
	 */
	public int estimate(int atom, ThriceTruth value) {
		int assignedBefore = assignment.getNumberOfAssignedAtoms();
		assignment.choose(atom, value);
		store.propagateOnlyBinaryNoGoods();
		int assignedNewly = assignment.getNumberOfAssignedAtoms() - assignedBefore;
		assignment.backtrack();
		return assignedNewly;
	}

}
