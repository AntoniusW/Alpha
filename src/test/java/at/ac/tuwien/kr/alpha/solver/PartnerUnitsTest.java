/*
 * Copyright (c) 2018, 2020 Siemens AG
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
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

import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests {@link AbstractSolver} using some partner units test cases.
 *
 */
public class PartnerUnitsTest extends AbstractSolverTests {
	private final ProgramParser parser = new ProgramParser();
	
	@Test
	public void testPartnerUnits_generated_003() throws IOException {
		testPartnerUnits_generated("simple003_bf_startZ1.asp");
	}

	@Test
	@Ignore("ignore to save resources during CI")
	public void testPartnerUnits_generated_010() throws IOException {
		testPartnerUnits_generated("simple010_bf_startZ1.asp");
	}

	@Test
	@Ignore("ignore to save resources during CI")
	public void testPartnerUnits_generated_020() throws IOException {
		testPartnerUnits_generated("simple020_bf_startZ1.asp");
	}

	@Test
	@Ignore("ignore to save resources during CI")
	public void testPartnerUnits_generated_030() throws IOException {
		testPartnerUnits_generated("simple030_bf_startZ1.asp");
	}
	
	private void testPartnerUnits_generated(String instanceId) throws IOException {
		InputProgram parsedProgram = parser
				.parse(CharStreams.fromPath(Paths.get("src", "test", "resources", "DomainHeuristics", "PartnerUnits", "pup.alpha_heu_20210205.asp")));
		parsedProgram = InputProgram.builder(parsedProgram)
				.accumulate(parser.parse(CharStreams
						.fromPath(Paths.get("src", "test", "resources", "DomainHeuristics", "PartnerUnits", "instances", "generated", instanceId))))
		.build();

		Solver solver = getInstance(parsedProgram);
		Optional<AnswerSet> answerSet = solver.stream().findFirst();
		assertTrue(answerSet.isPresent());
		checkAnswerSet(answerSet.get());
	}

	/**
	 * Checks the given answer set for the partner units problem:
	 * <ul>
	 * <li>Each zone and sensor is connected to exactly one unit.</li>
	 * <li>Each unit is connected to at most UCAP zones, UCAP sensors, and IUCAP partner units.</li>
	 * <li>If any related sensor and zone are connected to different units, then these two units must be partner units.</li>
	 * </ul>
	 * In the present implementation, UCAP = 2 is fixated and maxPU(IUCAP) is given in the answer set.
	 * 
	 * @param answerSet
	 */
	private void checkAnswerSet(AnswerSet answerSet) {
		final int ucap = 2;
		final int iucap = findIUCAP(answerSet);
		Set<Pair<Integer, Integer>> zone2sensorAssignments = getZone2sensorAssignments(answerSet);

		Map<Integer, Integer> zoneToUnit = new HashMap<>();
		Map<Integer, Integer> sensorToUnit = new HashMap<>();
		Map<Integer, Integer> unitToNZones = new HashMap<>();
		Map<Integer, Integer> unitToNSensors = new HashMap<>();

		for (Atom assign : answerSet.getPredicateInstances(Predicate.getInstance("assign", 3))) {
			if (assignsZone(assign)) {
				addAndCheckUnitToElement(assign, zoneToUnit, unitToNZones, ucap);
			} else if (assignsSensor(assign)) {
				addAndCheckUnitToElement(assign, sensorToUnit, unitToNSensors, ucap);
			} else {
				fail("Unknown assign: " + assign);
			}
		}
		checkAtLeastOneUnitPerElement(getZones(zone2sensorAssignments), zoneToUnit);
		checkAtLeastOneUnitPerElement(getSensors(zone2sensorAssignments), sensorToUnit);

		Map<Integer, Set<Integer>> unitToPartnerUnits = new HashMap<>();
		for (Atom partnerunits : answerSet.getPredicateInstances(Predicate.getInstance("partnerunits", 2))) {
			addAndCheckPartnerUnits(partnerunits, unitToPartnerUnits, iucap);
		}

		checkPartnerUnits(zone2sensorAssignments, zoneToUnit, sensorToUnit, unitToPartnerUnits);
	}

	@SuppressWarnings("unchecked")
	private int findIUCAP(AnswerSet answerSet) {
		return ((ConstantTerm<Integer>) answerSet.getPredicateInstances(Predicate.getInstance("maxPU", 1)).first().getTerms().get(0)).getObject();
	}

	private boolean assignsZone(Atom assign) {
		return assignsElement(assign, "z");
	}

	private boolean assignsSensor(Atom assign) {
		return assignsElement(assign, "s");
	}

	private boolean assignsElement(Atom assign, String elementType) {
		return "assign".equals(assign.getPredicate().getName()) && elementType.equals(assign.getTerms().get(1).toString());
	}

	@SuppressWarnings("unchecked")
	private void addAndCheckUnitToElement(Atom assign, Map<Integer, Integer> elementToUnit, Map<Integer, Integer> unitToNElements, int ucap) {
		int unit = ((ConstantTerm<Integer>) assign.getTerms().get(0)).getObject();
		int element = ((ConstantTerm<Integer>) assign.getTerms().get(2)).getObject();
		if (elementToUnit.containsKey(element) && !elementToUnit.get(element).equals(unit)) {
			fail("Element assigned to more than one unit: " + element); // TODO: print element type
		} else {
			elementToUnit.put(element, unit);
		}
		int nElements = unitToNElements.getOrDefault(unit, 0) + 1;
		if (nElements > ucap) {
			fail("Unit assigned to more than UCAP elements: " + unit);
		}
		unitToNElements.put(unit, nElements);
	}

	@SuppressWarnings("unchecked")
	private void addAndCheckPartnerUnits(Atom partnerunits, Map<Integer, Set<Integer>> unitToPartnerUnits, int iucap) {
		int unit1 = ((ConstantTerm<Integer>) partnerunits.getTerms().get(0)).getObject();
		int unit2 = ((ConstantTerm<Integer>) partnerunits.getTerms().get(1)).getObject();
		addAndCheckPartnerUnits(unit1, unit2, unitToPartnerUnits, iucap);
		addAndCheckPartnerUnits(unit2, unit1, unitToPartnerUnits, iucap);
	}

	private void addAndCheckPartnerUnits(int unit1, int unit2, Map<Integer, Set<Integer>> unitToPartnerUnits, int iucap) {
		unitToPartnerUnits.putIfAbsent(unit1, new HashSet<>());
		Set<Integer> partnerUnits = unitToPartnerUnits.get(unit1);
		partnerUnits.add(unit2);
		if (partnerUnits.size() > iucap) {
			fail("Unit has more than IUCAP partner units: " + unit1 + " -> " + partnerUnits);
		}
	}

	@SuppressWarnings("unchecked")
	private Set<Pair<Integer, Integer>> getZone2sensorAssignments(AnswerSet answerSet) {
		return answerSet.getPredicateInstances(Predicate.getInstance("zone2sensor", 2)).stream()
				.map(z2s -> Pair.of(
						((ConstantTerm<Integer>) z2s.getTerms().get(0)).getObject(),
						((ConstantTerm<Integer>) z2s.getTerms().get(1)).getObject()))
				.collect(Collectors.toSet());
	}

	private void checkPartnerUnits(Set<Pair<Integer, Integer>> zone2sensorAssignments, Map<Integer, Integer> zoneToUnit, Map<Integer, Integer> sensorToUnit,
			Map<Integer, Set<Integer>> unitToPartnerUnits) {
		for (Pair<Integer, Integer> zone2sensor : zone2sensorAssignments) {
			int zone = zone2sensor.getLeft();
			int sensor = zone2sensor.getRight();
			int zoneUnit = zoneToUnit.get(zone);
			int sensorUnit = sensorToUnit.get(sensor);
			if (zoneUnit != sensorUnit) {
				if (!unitToPartnerUnits.get(zoneUnit).contains(sensorUnit)) {
					fail(String.format("Unit %d of sensor %d is not a partner unit of unit %d of connected zone %d", sensorUnit, sensor, zoneUnit, zone));
				}
			}
		}
	}

	private Set<Integer> getZones(Set<Pair<Integer, Integer>> zone2sensorAssignments) {
		return zone2sensorAssignments.stream().map(Pair::getLeft).collect(Collectors.toSet());
	}

	private Set<Integer> getSensors(Set<Pair<Integer, Integer>> zone2sensorAssignments) {
		return zone2sensorAssignments.stream().map(Pair::getRight).collect(Collectors.toSet());
	}

	private void checkAtLeastOneUnitPerElement(Set<Integer> elements, Map<Integer, Integer> elementToUnit) {
		for (Integer element : elements) {
			if (!elementToUnit.containsKey(element)) {
				fail("Element has no unit: " + element); // TODO: print element type
			}
		}
	}
}
