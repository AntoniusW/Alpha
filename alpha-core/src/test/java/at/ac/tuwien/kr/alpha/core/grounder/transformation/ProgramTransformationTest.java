package at.ac.tuwien.kr.alpha.core.grounder.transformation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.api.programs.ASPCore2Program;
import at.ac.tuwien.kr.alpha.api.programs.Program;
import at.ac.tuwien.kr.alpha.api.programs.ProgramParser;
import at.ac.tuwien.kr.alpha.core.externals.Externals;
import at.ac.tuwien.kr.alpha.core.parser.ProgramParserImpl;
import at.ac.tuwien.kr.alpha.core.programs.NormalProgramImpl;
import at.ac.tuwien.kr.alpha.core.programs.transformation.ChoiceHeadToNormal;
import at.ac.tuwien.kr.alpha.core.programs.transformation.IntervalTermToIntervalAtom;
import at.ac.tuwien.kr.alpha.core.programs.transformation.ProgramTransformation;

public class ProgramTransformationTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProgramTransformationTest.class);

	private static final String TESTFILES_PATH = "/transform-test/";

	private static final ProgramParser PARSER = new ProgramParserImpl();

	private ChoiceHeadToNormal choiceToNormal = new ChoiceHeadToNormal();
	private IntervalTermToIntervalAtom intervalRewriting = new IntervalTermToIntervalAtom();

	private static String readTestResource(String resource) throws IOException {
		InputStream is = ProgramTransformationTest.class.getResourceAsStream(TESTFILES_PATH + resource);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder bld = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			bld.append(line).append(System.lineSeparator());
		}
		br.close();
		return bld.toString();
	}

	private <I extends Program<?>, O extends Program<?>> void genericTransformationTest(ProgramTransformation<I, O> transform,
			Function<ASPCore2Program, I> prepareFunc, String resourceSet) {
		try {
			String inputCode = ProgramTransformationTest.readTestResource(resourceSet + ".in");
			String expectedResult = ProgramTransformationTest.readTestResource(resourceSet + ".out");
			ASPCore2Program inputProg = PARSER.parse(inputCode, Externals.scan(ProgramTransformationTest.class));
			I transformInput = prepareFunc.apply(inputProg);
			String beforeTransformProg = transformInput.toString();
			O transformedProg = transform.apply(transformInput);
			Assert.assertEquals("Transformation result doesn't match expected result", expectedResult, transformedProg.toString());
			Assert.assertEquals("Transformation modified source program (breaks immutability!)", beforeTransformProg, transformInput.toString());
		} catch (Exception ex) {
			LOGGER.error("Exception in test, nested exception: " + ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
	}

	@Test
	public void choiceHeadToNormalSimpleTest() {
		genericTransformationTest(choiceToNormal, Function.identity(), "choice-to-normal.1");
	}

	@Test
	public void intervalTermToIntervalAtomSimpleTest() {
		genericTransformationTest(intervalRewriting, NormalProgramImpl::fromInputProgram, "interval.1");
	}

	@Test
	public void intervalTermToIntervalAtomExternalAtomTest() {
		genericTransformationTest(intervalRewriting, NormalProgramImpl::fromInputProgram, "interval-external_atom");
	}

	@Test
	public void intervalTermToIntervalAtomComparisonAtomTest() {
		genericTransformationTest(intervalRewriting, NormalProgramImpl::fromInputProgram, "interval-comparison_atom");
	}

	@at.ac.tuwien.kr.alpha.api.externals.Predicate(name = "say_true")
	public static boolean sayTrue(int val) {
		// Dummy method so we can have an external in the transformation test.
		return true;
	}

}