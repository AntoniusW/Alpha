/*
 * Copyright (c) 2018-2021 Siemens AG
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
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;
import at.ac.tuwien.kr.alpha.solver.heuristics.BranchingHeuristicFactory;
import org.antlr.v4.runtime.CharStreams;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Tests {@link AbstractSolver} using some HRP (House Reconfiguration Problem) test cases.
 *
 */
public class HouseTest extends AbstractSolverTests {
	private final ProgramParser parser = new ProgramParser();

	@Before
	public void setUp() {
		ignoreTestForNaiveSolver();
		assumeTrue(heuristicsConfiguration.getHeuristic() == BranchingHeuristicFactory.Heuristic.VSIDS);
		// test only on one heuristics to save resources during CI
	}

	@Test(timeout = 60000)
	public void testHouse_emptyconfig_p05t025() throws IOException {
		testHouse("emptyconfig_p05t025");
	}

	@Test
	@Ignore("ignore to save resources during CI")
	public void testHouse_emptyconfig_p10t050() throws IOException {
		testHouse("emptyconfig_p10t050");
	}

	@Test
	@Ignore("ignore to save resources during CI")
	public void testHouse_emptyconfig_p15t075() throws IOException {
		testHouse("emptyconfig_p15t075");
	}
	
	@Test
	@Ignore("ignore to save resources during CI")
	public void testHouse_longthings_2_p02t030c3() throws IOException {
		testHouse("longthings_2_p02t030c3");
	}
	
	@Test
	@Ignore("ignore to save resources during CI")
	public void testHouse_longthings_newroom_p02t024c3() throws IOException {
		testHouse("longthings_newroom_p02t024c3");
	}
	
	@Test
	@Ignore("ignore to save resources during CI")
	public void testHouse_switchthingsize_r02t035() throws IOException {
		testHouse("switchthingsize_r02t035");
	}

	private void testHouse(String instanceId) throws IOException {

		InputProgram parsedProgram = parser
				.parse(CharStreams.fromPath(Paths.get("src", "test", "resources", "DomainHeuristics", "House", "house_alpha_2021-02-04.asp")));
		parsedProgram = InputProgram.builder(parsedProgram).accumulate(
				parser.parse(CharStreams
						.fromPath(Paths.get("src", "test", "resources", "DomainHeuristics", "House", "instances_alpha", instanceId + ".edb"))))
				.build();

		Solver solver = getInstance(parsedProgram);
		Optional<AnswerSet> answerSet = solver.stream().findFirst();
		assertTrue(answerSet.isPresent());
		checkAnswerSet(answerSet.get());
	}

	/**
	 * Checks the given answer set for the HRP:
	 * <ul>
	 * <li>Each cabinet stores at most 5 things.</li>
	 * <li>Each room houses at most 4 cabinets.</li>
	 * <li>Each room houses things of only one persons in its cabinets.</li>
	 * <li>Long things have to be packed in high cabinets</li>
	 * <li>At most either 2 high things or 1 high and 2 short or 4 short cabinets are allowed to be in a room</li>
	 * </ul>
	 * 
	 * @param answerSet
	 */
	private void checkAnswerSet(AnswerSet answerSet) {
		Map<Integer, Integer> thingToPersonMapping = getThingToPersonMapping(answerSet);
		Map<Integer, Integer> thingToCabinetMapping = getReverseMapping(answerSet, "cabinetTOthing");
		checkEveryThingAssigned(thingToPersonMapping, thingToCabinetMapping);
		Map<Integer, Integer> cabinetToRoomMapping = getReverseMapping(answerSet, "roomTOcabinet");
		checkMaxKeysForSameValue(thingToCabinetMapping, 5);
		checkMaxKeysForSameValue(cabinetToRoomMapping, 4);
		getAndCheckTransitiveMapping(thingToCabinetMapping, cabinetToRoomMapping);
		Set<Integer> longThings = getInstanceTerms(answerSet, "thingLong");
		Set<Integer> highCabinets = getInstanceTerms(answerSet, "cabinetHigh");
		checkLongThingsInHighCabinets(thingToCabinetMapping, longThings, highCabinets);
		checkRoomCapacity(cabinetToRoomMapping, highCabinets);
	}

	private void checkEveryThingAssigned(Map<Integer, Integer> thingToPersonMapping, Map<Integer, Integer> thingToCabinetMapping) {
		for (Integer thing : thingToPersonMapping.keySet()) {
			if (!thingToCabinetMapping.containsKey(thing)) {
				fail("Thing " + thing + " not assigned to a cabinet");
			}
		}
	}

	private Map<Integer, Integer> getThingToPersonMapping(AnswerSet answerSet) {
		Map<Integer, Integer> map = new HashMap<>();
		for (Atom lc : answerSet.getPredicateInstances(Predicate.getInstance("legacyConfig", 1))) {
			FunctionTerm p2t = (FunctionTerm) lc.getTerms().get(0);
			if (p2t.getSymbol().equals("personTOthing")) {
				@SuppressWarnings("unchecked")
				int person = ((ConstantTerm<Integer>) p2t.getTerms().get(0)).getObject();
				@SuppressWarnings("unchecked")
				int thing = ((ConstantTerm<Integer>) p2t.getTerms().get(1)).getObject();
				map.put(thing, person);
			}
		}
		return map;
	}

	private Map<Integer, Integer> getReverseMapping(AnswerSet answerSet, String predicateName) {
		Map<Integer, Integer> map = new HashMap<>();
		for (Atom pair : answerSet.getPredicateInstances(Predicate.getInstance(predicateName, 2))) {
			@SuppressWarnings("unchecked")
			int o1 = ((ConstantTerm<Integer>) pair.getTerms().get(0)).getObject();
			@SuppressWarnings("unchecked")
			int o2 = ((ConstantTerm<Integer>) pair.getTerms().get(1)).getObject();
			Integer previousMapping = map.put(o2, o1);
			if (previousMapping != null) {
				fail("Duplicate mapping: " + predicateName + "(" + o1 + "," + o2 + ")");
			}
		}
		return map;
	}

	private void checkMaxKeysForSameValue(Map<Integer, Integer> mapping, int maxKeys) {
		Map<Integer, Set<Integer>> inverseMapping = new HashMap<>();
		for (Entry<Integer, Integer> entry : mapping.entrySet()) {
			Integer key = entry.getKey();
			Integer value = entry.getValue();
			Set<Integer> keys = inverseMapping.get(value);
			if (keys == null) {
				keys = new HashSet<>();
				inverseMapping.put(value, keys);
			}
			keys.add(key);
			if (keys.size() > maxKeys) {
				fail("Too many keys for value " + value);
			}
		}
	}

	private Map<Integer, Integer> getAndCheckTransitiveMapping(Map<Integer, Integer> mapping1, Map<Integer, Integer> mapping2) {
		Map<Integer, Integer> result = new HashMap<>();
		for (Entry<Integer, Integer> entry1 : mapping1.entrySet()) {
			Integer key1 = entry1.getKey();
			Integer key2 = entry1.getValue();
			Integer value = mapping2.get(key2);
			Integer previousMapping = result.put(key1, value);
			if (previousMapping != null) {
				fail("Duplicate transitive mapping: " + previousMapping);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Set<Integer> getInstanceTerms(AnswerSet answerSet, String predicateName) {
		SortedSet<Atom> predicateInstances = answerSet.getPredicateInstances(Predicate.getInstance(predicateName, 1));
		if (predicateInstances == null) {
			return Collections.emptySet();
		}
		return predicateInstances.stream()
				.map(a -> ((ConstantTerm<Integer>) a.getTerms().get(0)).getObject()).collect(Collectors.toSet());
	}

	private void checkLongThingsInHighCabinets(Map<Integer, Integer> thingToCabinetMapping, Set<Integer> longThings, Set<Integer> highCabinets) {
		for (Integer longThing : longThings) {
			Integer cabinet = thingToCabinetMapping.get(longThing);
			if (!highCabinets.contains(cabinet)) {
				fail("Long thing " + longThing + " not in high cabinet");
			}
		}
	}

	private void checkRoomCapacity(Map<Integer, Integer> cabinetToRoomMapping, Set<Integer> highCabinets) {
		final int sizeSmallCabinet = 1;
		final int sizeHighCabinet = 2;
		final int roomCapacity = 4;
		Map<Integer, Integer> roomToUsedCapacity = new HashMap<>();
		for (Entry<Integer, Integer> entry : cabinetToRoomMapping.entrySet()) {
			Integer cabinet = entry.getKey();
			Integer room = entry.getValue();
			Integer usedCapacity = roomToUsedCapacity.get(room);
			if (usedCapacity == null) {
				usedCapacity = 0;
			}
			if (highCabinets.contains(cabinet)) {
				usedCapacity += sizeHighCabinet;
			} else {
				usedCapacity += sizeSmallCabinet;
			}
			roomToUsedCapacity.put(room, usedCapacity);
			if (usedCapacity > roomCapacity) {
				fail("Capacity of room " + room + " exceeded");
			}
		}
	}
}
