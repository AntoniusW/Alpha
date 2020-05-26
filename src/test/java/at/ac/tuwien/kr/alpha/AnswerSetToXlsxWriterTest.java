package at.ac.tuwien.kr.alpha;

import at.ac.tuwien.kr.alpha.api.mapper.impl.AnswerSetToWorkbookMapperTest;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.AnswerSetBuilder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
		AnswerSetToWorkbookMapperTest.assertWorkbookMatchesAnswerSet(wb, as);
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

}
