package at.ac.tuwien.kr.alpha;

import at.ac.tuwien.kr.alpha.api.mapper.AnswerSetToObjectMapper;
import at.ac.tuwien.kr.alpha.api.mapper.impl.AnswerSetToWorkbookMapper;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;

public class AnswerSetToXlsxWriter implements BiConsumer<Integer, AnswerSet> {

	private String targetBasePath;
	private AnswerSetToObjectMapper<Workbook> answerSetMapper;

	public AnswerSetToXlsxWriter(String targetBasePath) {
		this.targetBasePath = targetBasePath;
		this.answerSetMapper = new AnswerSetToWorkbookMapper();
	}

	@Override
	public void accept(Integer num, AnswerSet as) {
		try {
			Path outputPath = Paths.get(this.targetBasePath + "." + num + ".xlsx");
			OutputStream os = Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			Workbook wb = this.answerSetMapper.mapFromAnswerSet(as);
			wb.write(os);
			wb.close();
			os.close();
			System.out.println("Answer set written to file " + outputPath.toString());
		} catch (IOException ex) {
			System.err.println("Failed writing answer set as xlsx file! (" + ex.getMessage() + ")");
		}
	}

	public static void writeUnsatInfo(Path path) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		// first, create a worksheet for 0-arity predicates
		Sheet sheet = workbook.createSheet("Unsatisfiable");
		Row row = sheet.createRow(0);
		Cell cell = row.createCell(0);
		cell.setCellValue("Input is unsatisfiable - No answer sets!");
		sheet.autoSizeColumn(0);
		OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		workbook.write(os);
		workbook.close();
		os.close();
	}

}
