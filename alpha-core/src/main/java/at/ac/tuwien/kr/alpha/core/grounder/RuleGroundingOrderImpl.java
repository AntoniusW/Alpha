/**
 * Copyright (c) 2019 Siemens AG
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
package at.ac.tuwien.kr.alpha.core.grounder;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.core.rules.InternalRule;

/**
 * A grounding order computed by {@link RuleGroundingInfoImpl} for a specific {@link InternalRule} and a specific starting literal.
 */
public class RuleGroundingOrderImpl implements RuleGroundingOrder{

	private Literal startingLiteral;
	private List<Literal> otherLiterals;
	private int positionLastVarBound;
	private int stopBindingAtOrderPosition;
	private final boolean ground;
	
	RuleGroundingOrderImpl(Literal startingLiteral, List<Literal> otherLiterals, int positionLastVarBound, boolean isGround) {
		super();
		this.startingLiteral = startingLiteral;
		this.otherLiterals = otherLiterals;
		this.positionLastVarBound = positionLastVarBound;
		this.stopBindingAtOrderPosition = otherLiterals.size();
		this.ground = isGround;
	}
	
	private RuleGroundingOrderImpl(RuleGroundingOrderImpl otherRuleGroundingOrder) {
		this(otherRuleGroundingOrder.startingLiteral, new ArrayList<>(otherRuleGroundingOrder.otherLiterals), otherRuleGroundingOrder.positionLastVarBound, otherRuleGroundingOrder.ground);
		this.stopBindingAtOrderPosition = otherRuleGroundingOrder.stopBindingAtOrderPosition;
	}
	
	/**
	 * Returns the literal at the given position in the grounding order,
	 * except it is already known that this literal is not able to yield new bindings.
	 * 
	 * A literal cannot yield new bindings if it has been copied to the end of the grounding order
	 * when no bindings could be found, and no bindings for other literals could be found in the meantime.
	 * 
	 * @param orderPosition zero-based index into list of literals except the starting literal
	 * @return the literal at the given position, or {@code null} if it is already known that this literal is not able to yield new bindings
	 */
	@Override
	public Literal getLiteralAtOrderPosition(int orderPosition) {
		if (orderPosition >= stopBindingAtOrderPosition) {
			return null;
		}
		return otherLiterals.get(orderPosition);
	}

	/**
	 * @return the zero-based position from which on all variables are bound in list of literals except the starting literal
	 */
	@Override
	public int getPositionFromWhichAllVarsAreBound() {
		return positionLastVarBound + 1;
	}

	public boolean isGround() {
		return ground;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(startingLiteral);
		sb.append(" : ");
		for (int i = 0; i < otherLiterals.size(); i++) {
			if (i == positionLastVarBound + 1) {
				sb.append("| ");
			}
			sb.append(otherLiterals.get(i));
			if (i < otherLiterals.size() - 1) {
				sb.append(", ");
			}
		}
		
		return sb.toString();
	}

	/**
	 * "Pushes a literal back" in a grounding order because the literal cannot be used to generate substitutions now but
	 * maybe later. Pushing back means adding the literal again at the end of the grounding order. Since the literal will
	 * occur twice in the new grounding order returned by this method, we assume that the grounding order is processed
	 * from left to right and the literal at {@code orderPosition} will not be considered again.
	 *
	 * @param orderPosition the position in the grounding order of the literal to be pushed back.
	 * @return a new grounding order in which the atom is pushed back,
	 * or {@code null} if the position of the grounding order after which no new bindings can be found has been reached.
	 */
	@Override
	public RuleGroundingOrderImpl pushBack(int orderPosition) {
		if (orderPosition >= stopBindingAtOrderPosition - 1) {
			return null;
		}
		RuleGroundingOrderImpl reorderedGroundingOrder = new RuleGroundingOrderImpl(this);
		reorderedGroundingOrder.otherLiterals.add(otherLiterals.get(orderPosition));
		return reorderedGroundingOrder;
	}
	
	@Override
	public void considerUntilCurrentEnd() {
		this.stopBindingAtOrderPosition = this.otherLiterals.size();
	}

	@Override
	public Literal getStartingLiteral() {
		return this.startingLiteral;
	}

}
