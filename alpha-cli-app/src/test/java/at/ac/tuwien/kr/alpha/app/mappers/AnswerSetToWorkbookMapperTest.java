package at.ac.tuwien.kr.alpha.app.mappers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.Alpha;
import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.impl.AlphaImpl;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.AnswerSetBuilder;

public class AnswerSetToWorkbookMapperTest {

	private AnswerSetToWorkbookMapper mapper = new AnswerSetToWorkbookMapper();

	@Test
	public void smokeTest() throws IOException {
		AnswerSet as = new AnswerSetBuilder().predicate("bla").instance("blubb", "blubb").instance("foo", "bar").predicate("foo").instance("bar")
				.instance("baz").predicate("complex").instance(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)).build();
		Workbook wb = this.mapper.mapFromAnswerSet(as);
		Assert.assertNotNull(wb.getSheet("Flags"));
		Assert.assertNotNull(wb.getSheet("bla_2"));
		Assert.assertNotNull(wb.getSheet("foo_1"));
		Assert.assertNotNull(wb.getSheet("complex_3"));
		wb.close();
	}

	@Test
	public void solveAndWriteWorkbookTest() {
		//@formatter:off
		String progstr = "aFlag. oneMoreFlag. yetAnotherFlag. createPs. maxP(5). r(s(1, 2, 3), 4). r(bla, blubb). r(foo, bar(baaz))."
				+ "p(0) :- createPs. "
				+ "p(N) :- p(I), N = I + 1, N <= MX, maxP(MX)."
				+ "q(A, B) :- p(A), p(B).";
		//@formatter:on
		Alpha alpha = new AlphaImpl();
		List<AnswerSet> answerSets = alpha.solve(alpha.readProgramString(progstr)).collect(Collectors.toList());
		Assert.assertEquals(1, answerSets.size());
		AnswerSet as = answerSets.get(0);
		Workbook answerSetWorkbook = this.mapper.mapFromAnswerSet(as);
		AnswerSetToWorkbookMapperTest.assertWorkbookMatchesAnswerSet(answerSetWorkbook, as);
	}

	public static void assertWorkbookMatchesAnswerSet(Workbook wb, AnswerSet as) {
		for (Predicate pred : as.getPredicates()) {
			if (pred.getArity() == 0) {
				boolean flagFound = false;
				Sheet flagsSheet = wb.getSheet("Flags");
				Assert.assertNotNull(flagsSheet);
				for (Row row : flagsSheet) {
					if (row.getCell(0).getStringCellValue().equals(pred.getName())) {
						flagFound = true;
						break;
					}
				}
				Assert.assertTrue("0-arity predicate " + pred.getName() + " not found in workbook!", flagFound);
			} else {
				Sheet predicateSheet = wb.getSheet(pred.getName() + "_" + pred.getArity());
				for (Atom atom : as.getPredicateInstances(pred)) {
					boolean atomFound = false;
					Assert.assertNotNull(predicateSheet);
					for (Row row : predicateSheet) {
						if (AnswerSetToWorkbookMapperTest.rowMatchesAtom(row, atom)) {
							atomFound = true;
							break;
						}
					}
					Assert.assertTrue("Atom " + atom.toString() + " not found in workbook!", atomFound);
				}
			}
		}
	}

	private static boolean rowMatchesAtom(Row row, Atom atom) {
		List<Term> terms = atom.getTerms();
		Cell cell;
		for (int i = 0; i < terms.size(); i++) {
			cell = row.getCell(i);
			if (cell == null) {
				return false;
			}
			if (!(cell.getStringCellValue().equals(terms.get(i).toString()))) {
				return false;
			}
		}
		return true;
	}

}
