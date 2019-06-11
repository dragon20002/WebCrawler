package parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelModelExtractor {

	public void extract(String excelFilepath, String packageStr) throws IOException {
		String excelName = getExcelName(excelFilepath);
		FileInputStream in = new FileInputStream(excelFilepath);
		XSSFWorkbook workbook = new XSSFWorkbook(in);
		int nSheets = workbook.getNumberOfSheets();

		String repoDir = excelName + "\\\\repo\\\\";
		chkAndMkDir(repoDir);
		String modelDir = excelName + "\\\\model\\\\";
		chkAndMkDir(modelDir);
		
		String sqlFilename = excelName + "\\\\" + excelName + ".sql";
		writeFile(sqlFilename, "", false);

		for (int sh_i = 0; sh_i < nSheets; sh_i++) {
			StringBuilder clsAppender = new StringBuilder(modelHeader(packageStr))
					.append("public class ");
			StringBuilder repoAppender = new StringBuilder()
					.append("package ")
					.append(packageStr)
					.append(".repo;\n\n")
					.append("import org.springframework.data.repository.CrudRepository;\n")
					.append("import org.springframework.stereotype.Repository;\n\n");
			StringBuilder tblAppender = new StringBuilder();
			XSSFSheet sheet = workbook.getSheetAt(sh_i);

			String sheetName = sheet.getSheetName();
			String className = snake2UpCamel(sheetName);
			clsAppender.append(className)
				.append(" {\n");
			repoAppender.append("import ")
				.append(packageStr)
				.append(".model.")
				.append(className)
				.append(";\n\n")
				.append("@Repository\n")
				.append("public interface ")
				.append(className)
				.append("Repository extends CrudRepository<")
				.append(className)
				.append(", Long> {\n\n");
			tblAppender.append("DROP TABLE IF EXISTS `")
				.append(sheetName)
				.append("`;\n\n")
				.append("CREATE TABLE `")
				.append(sheetName)
				.append("` (\n");

			if (sheet.getPhysicalNumberOfRows() == 0)
				continue;

			XSSFRow row = sheet.getRow(0);
			int nCols = row.getPhysicalNumberOfCells();
			XSSFRow typeRow = sheet.getRow(1);

			for (int ce_i = 0; ce_i < nCols; ce_i++) {
				XSSFCell cell = row.getCell(ce_i);
				XSSFCell typeCell = typeRow.getCell(ce_i);
				if (cell == null || typeCell == null)
					continue;

				String cellValue = cell.getStringCellValue();
				String type = typeCell.getStringCellValue();

				if (cellValue.contentEquals("id")) {
					clsAppender.append("    @Id\n");
				}
				clsAppender.append("    private ")
					.append(getJavaType(type))
					.append(" ")
					.append(snake2LoCamel(cellValue))
					.append(";\n");

				tblAppender.append("  `")
					.append(cellValue)
					.append("` ")
					.append(getMySqlType(type));
				tblAppender.append(",\n");
			}

			clsAppender.append("}");
			repoAppender.append("}");
			tblAppender.append("  PRIMARY KEY(`id`)\n) DEFAULT CHARSET=utf8;\n\n")
				.append("LOAD DATA LOCAL INFILE '")
				.append(sheetName)
				.append(".csv' INTO TABLE ")
				.append(sheetName)
				.append("    \nFIELDS TERMINATED BY ','")
				.append("    LINES TERMINATED BY '\\n'")
				.append("    IGNORE 1 ROWS\n;\n\n\n");

			writeFile(modelDir + className + ".java", clsAppender.toString(), false);
			writeFile(repoDir + className + "Repository.java", repoAppender.toString(), false);
			writeFile(sqlFilename, tblAppender.toString(), true);
		}
		
		workbook.close();
		in.close();
	}
	
	private String getExcelName(String excelFilepath) {
		String[] toks = excelFilepath.split("\\\\");
		return toks[toks.length - 1].split("\\.")[0];
	}

	private void chkAndMkDir(String dirpath) {
		File dir = new File(dirpath);
		if (!dir.exists())
			dir.mkdirs();
	}
	
	private String modelHeader(String packageStr) {
		return new StringBuilder()
			.append("package ")
			.append(packageStr)
			.append(".model;\n\n")
			.append("import javax.persistence.Entity;\n")
			.append("import javax.persistence.Id;\n")
			.append("import javax.persistence.Table;\n\n")
//			.append("import lombok.NoArgsConstructor;\n")
//			.append("import lombok.Getter;\n")
//			.append("import lombok.Setter;\n\n")
//			.append("@NoArgsConstructor\n")
//			.append("@Getter\n")
//			.append("@Setter\n")
			.append("@Entity\n")
			.append("@Table\n")
			.toString();
	}

	private String firstCharUpper(String src) {
		if (src.length() == 0) return "";
		return (char) (src.charAt(0) - 'a' + 'A') + src.substring(1);
	}

	private String getJavaType(String type) {
		switch (type) {
		case "text":
		case "string": return "String";
		case "long":
		case "int":
		case "float":
			return type;
		}
		return null;
	}
	
	private String getMySqlType(String type) {
		switch (type) {
		case "text": return "varchar(500)";
		case "string": return "varchar(45)";
		case "long": return "bigint";
		case "int":
		case "float":
			return type;
		}
		return null;
	}

	private String snake2UpCamel(String snakeCase) {
		String[] toks = snakeCase.split("_");
		if (toks.length < 1)
			return snakeCase;

		StringBuilder appender = new StringBuilder();
		for (int i = 0; i < toks.length; i++)
			appender.append(firstCharUpper(toks[i]));
		return appender.toString();
	}
	
	private String snake2LoCamel(String snakeCase) {
		String[] toks = snakeCase.split("_");
		if (toks.length < 2)
			return snakeCase;

		StringBuilder appender = new StringBuilder(toks[0]);
		for (int i = 1; i < toks.length; i++)
			appender.append(firstCharUpper(toks[i]));
		return appender.toString();
	}
	
	private void writeFile(String filepath, String str, boolean append) throws IOException {
		File file = new File(filepath);
		PrintWriter writer = new PrintWriter(new FileWriter(file, append));
		writer.printf(str);
		writer.close();
	}
}
