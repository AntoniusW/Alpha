package at.ac.tuwien.kr.alpha;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.commons.AnswerSetBuilder;

public class AnswerSetToXlsxWriterTest {

	@Test
	public void writeAnswerSetFilesTest() throws IOException {
		AnswerSet as = new AnswerSetBuilder().predicate("bla").instance("blubb", "blubb").instance("foo", "bar").predicate("foo").instance("bar")
				.instance("baz").predicate("complex").instance(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)).build();
		Path tmpDir = Files.createTempDirectory("alpha-test-xlsx-output");
		AnswerSetToXlsxWriter writer = new AnswerSetToXlsxWriter(tmpDir.toString() + "/alphaAnswerSet");
		writer.accept(0, as);
		File tmpDirFile = tmpDir.toFile();
		File[] generatedFiles = tmpDirFile.listFiles();
		Assert.assertEquals(generatedFiles.length, 1);
		File answerSetFile = generatedFiles[0];
		Assert.assertEquals("alphaAnswerSet.0.xlsx", answerSetFile.getName());
		Workbook wb = WorkbookFactory.create(answerSetFile);
		assertWorkbookMatchesAnswerSet(wb, as);
		wb.close();
		// clean up
		answerSetFile.delete();
		tmpDirFile.delete();
	}

	@Test
	public void writeUnsatTest() throws IOException {
		Path tmpDir = Files.createTempDirectory("alpha-test-xlsx-unsat");
		AnswerSetToXlsxWriter.writeUnsatInfo(Paths.get(tmpDir.toString() + "/alphaAnswerSet.UNSAT.xlsx"));
		File tmpDirFile = tmpDir.toFile();
		File[] generatedFiles = tmpDirFile.listFiles();
		Assert.assertEquals(generatedFiles.length, 1);
		File unsatFile = generatedFiles[0];
		Assert.assertEquals("alphaAnswerSet.UNSAT.xlsx", unsatFile.getName());
		Workbook wb = WorkbookFactory.create(unsatFile);
		Sheet unsatSheet = wb.getSheet("Unsatisfiable");
		Assert.assertNotNull(unsatSheet);
		Cell cell = unsatSheet.getRow(0).getCell(0);
		Assert.assertNotNull(cell);
		String cellValue = cell.getStringCellValue();
		Assert.assertEquals("Input is unsatisfiable - No answer sets!", cellValue);
		wb.close();
		// clean up
		unsatFile.delete();
		tmpDirFile.delete();
	}

	private static void assertWorkbookMatchesAnswerSet(Workbook wb, AnswerSet as) {
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
						if (rowMatchesAtom(row, atom)) {
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
