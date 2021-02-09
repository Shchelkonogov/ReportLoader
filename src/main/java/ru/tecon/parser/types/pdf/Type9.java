package ru.tecon.parser.types.pdf;

import com.google.common.collect.Range;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import ru.tecon.parser.ParseException;
import ru.tecon.parser.model.ParameterData;
import ru.tecon.parser.model.ReportData;
import ru.tecon.parser.types.ParserUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;

public class Type9 {

	private static Logger logger = Logger.getLogger(Type9.class.getName());

	private static Type9 instance = new Type9();

	private static final List<String> testType =
			Arrays.asList("Посуточная ведомость уч.та параметров теплопотребления",
					"Организац.*Номер.*", "Адрес.*Телефон.*", "Тип.*Версия.*Номер.*",
					"Дата последующей поверки.*");

	private static List<String> removeData = new ArrayList<>(Arrays.asList("U", "D", "G", "g,G", "U,g,G",
			"D,G", "D,g,G", "g", "U,g", "U,D", "E", "U,G", "U,D,g,G", "D,g", "U,D,g", "U,E", "D,E",
			"U,D,E", "U,g,E", "g,E", "g,G,E", "U,D,G", "D,g,E", "UE", "ED", "UED", "UEG", "UD"));

	private static List<String> paramName = Arrays.asList("Дата", "Q, Гкал", "t1", "t2", "tхв", "V1", "V2", "P1", "P2",
			"V1-V2", "t1-t2", "Tн", "Ошибки", "Tр", "V3", "Дата, время", "V1, м3",
			"V2, м3", "V3, м3", "Tр, час", "Q", "Тн", "Тр", "Дата Время", "М1", "М2",
			"М1-M2", "М2-M1", "Vи", "ошибок", "Р3", "P2Н", "M1", "M2", "M1-M2", "M1, т",
			"M2, т", "M2-M1", "тхв", "Тн.", "Тр.", "V1, М³", "V2, М³", "ta",
			"Дата", "Q", "М1", "М2", "t1", "t2", "Тр", "P1", "P2", "V1", "V2",
			"М1-M2", "М2-M1", "t1-t2", "Тн", "Дата Время", "Vи", "V1-V2", "ошибок",
			"Q, Гкал", "M1", "M2", "M1-M2", "M2-M1", "Tн", "Tр", "Ошибки", "Дата, время",
			"M1, т", "M2, т", "Tр, час", "t1-t2Н", "ta", "Тн.", "Тр.", "tхв",
			"Дата", "Q", "М1", "М2", "М1-M2", "М2-M1", "Vи", "t1", "t2", "t1-t2",
			"Тн", "Тр", "ошибок", "Дата Время", "Q, Гкал", "M1", "M2", "M1-M2", "M2-M1",
			"Ошибки", "Tн", "P1", "P2", "Tр", "Дата, время", "M1, т", "M2, т", "Tр, час");

	private static List<String> dbParamName = Arrays.asList("Дата", "Q", "T1", "T2", "T3", "V1", "V2", "p1", "p2",
			"нет", "нет", "нет", "нет", "Time", "нет", "Дата", "V1", "V2", "нет",
			"Time", "Q", "нет", "Time", "Дата", "G1", "G2", "нет", "нет", "нет",
			"нет", "нет", "p2", "G1", "G2", "нет", "G1", "G2", "нет", "T3",
			"нет", "Time", "V1", "V2", "нет",
			"Дата", "Q", "G1", "G2", "T1", "T2", "Time", "p1", "p2",
			"V1", "V2", "нет", "нет", "нет", "нет", "Дата", "нет", "нет", "нет",
			"Q", "G1", "G2", "нет", "нет", "нет", "Time", "нет", "Дата", "G1",
			"G2", "Time", "нет", "нет", "нет", "Time", "T3",
			"Дата", "Q", "G1", "G2", "нет", "нет", "нет", "T1", "T2", "нет",
			"нет", "Time", "нет", "Дата", "Q", "G1", "G2", "нет", "нет", "нет", "нет",
			"p1", "p2", "Time", "Дата", "G1", "G2", "Time");

	private static List<String> dbParamNameIntegr = Arrays.asList("Дата", "Q", "T1", "T2", "T3", "V1", "V2", "p1", "p2",
			"нет", "нет", "нет", "нет", "Time", "нет", "Дата", "V1", "V2", "нет",
			"Time", "Q", "нет", "Time", "Дата", "G1", "G2", "нет", "нет", "нет",
			"нет", "нет", "p2", "G1", "G2", "нет", "G1", "G2", "нет", "T3",
			"нет", "Time", "V1", "V2", "нет",
			"Дата", "Q", "G1", "G2", "T1", "T2", "Time", "p1", "p2",
			"V1", "V2", "нет", "нет", "нет", "нет", "Дата", "нет", "нет", "нет",
			"Q", "G1", "G2", "нет", "нет", "нет", "Time", "нет", "Дата", "G1",
			"G2", "Time", "нет", "нет", "нет", "Time", "T3",
			"Дата", "Q", "G1", "G2", "нет", "нет", "нет", "T1", "T2", "нет",
			"нет", "Time", "нет", "Дата", "Q", "G1", "G2", "нет", "нет", "нет", "нет",
			"p1", "p2", "Time", "Дата", "G1", "G2", "Time");

	private Type9() {
	}

	private static List<String> createList(String filePath, PDFTable pdfTable) {
		try (FileInputStream stream = new FileInputStream(new File(filePath));
			 PDDocument document = PDDocument.load(stream)) {
			if (document.getNumberOfPages() == 1) {
				List<TextPosition> texts = extractTextPositions(document);
				List<Range<Integer>> lineRanges = getLineRanges(texts);
				StringBuilder table = pdfTable.buildTable(texts, lineRanges);

				List<String> lines = new ArrayList<>(Arrays.asList(table.toString().split("\n")));

				if (lines.stream().anyMatch(obj -> obj.matches("Дата.*"))) {

					//Вытаскиваем шапку
					List<String> subLines = new ArrayList<>();
					for (String item: lines) {
						if (!item.matches("Дата.*")) {
							subLines.add(item);
						} else {
							if (item.matches("Дата последующей.*")) {
								subLines.add(item);
							} else {
								break;
							}
						}
					}

					return subLines;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean checkType(String filePath) {
		logger.info("Type6 checkType");

		List<String> subLines = createList(filePath, instance.new PDFTable());

		if (subLines == null) {
			logger.info("checkType is unsuccessful");
			return false;
		}

		//Проверяем подходит ли нам этот отчет
		boolean test = true;
		for (String item: testType) {
			if (subLines.stream().noneMatch(obj -> obj.matches(item))) {
				test = false;
				break;
			}
		}

		if (test) {
			logger.info("checkType is successful");
			return true;
		}

		logger.info("checkType is unsuccessful");
		return false;
	}

	public static ReportData getData(String filePath) throws ParseException {
		PDFTable pdfTable = instance.new PDFTable();

		List<String> subLines = createList(filePath, pdfTable);

		if (subLines == null) {
			throw new ParseException("parse error");
		}

		//Проверяем есть ли данные в отчете
		if (pdfTable.dataList.size() == 0 && pdfTable.dataListIntegr.size() == 0) {
			throw new ParseException("Пустой отчет");
		}

		//Определяем адрес потребителя.
		String address;
		int line1 = subLines.indexOf(subLines.stream().filter(obj -> obj.matches(testType.get(1)))
				.findFirst().orElse("-1"));
		int line2 = subLines.indexOf(subLines.stream().filter(obj -> obj.matches(testType.get(2)))
				.findFirst().orElse("-1"));
		int line3 = subLines.indexOf(subLines.stream().filter(obj -> obj.matches(testType.get(3)))
				.findFirst().orElse("-1"));

		if ((line3 - line2 == 1) && (line2 - line1 == 1)) {
			address = subLines.stream().filter(obj -> obj.matches(testType.get(2))).findFirst().orElse("");
			if (address.equals("")) {
				throw new ParseException("can't read address");
			}
			String start = "Адрес";
			String end = "Телефон";
			address = address.substring(address.indexOf(start) + start.length(), address.indexOf(end)).trim();
			if (address.matches(":.*")) {
				address = address.replaceFirst(":", "");
			}
			address = address.replaceAll("_", "");
			if (address.length() == 0) {
				address = filePath.substring(filePath.lastIndexOf("\\") + 1);
			}
		} else {
			throw new ParseException("Адресс поребителя больше чем на одной строке");
		}

		//Определяем тип прибора.
		String counterType;
		String start = "Тип";
		String end = "Версия";
		counterType = subLines.stream().filter(obj -> obj.matches(testType.get(3))).findFirst().orElse("");
		if (counterType.equals("")) {
			throw new ParseException("can't read counter type");
		}
		counterType = counterType.substring(counterType.indexOf(start) + start.length()
				, counterType.indexOf(end)).trim();
		if (counterType.contains("тчика")) {
			counterType = counterType.substring(counterType.indexOf("тчика") + "тчика".length()).trim();
			counterType = counterType.replaceFirst(":", "");
		}
		counterType = counterType.replaceAll("_", "");

		//Определяем тип отчета.
		String reportName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))
				.toLowerCase();

		//Определяем серийний номер.
		String counterNumber;
		start = "Номер";
		counterNumber = subLines.stream().filter(obj -> obj.matches(testType.get(3))).findFirst().orElse("");
		if (counterNumber.equals("")) {
			throw new ParseException("can't read counter number");
		}
		counterNumber = counterNumber.substring(counterNumber.indexOf(start) + start.length()).trim();
		if (counterNumber.matches(".*тчика.*")) {
			counterNumber = counterNumber.substring(counterNumber.indexOf("тчика") + "тчика".length()).trim();
		}
		if (counterNumber.matches(":.*")) {
			counterNumber = counterNumber.replaceFirst(":", "").trim();
		}
		counterNumber = counterNumber.replaceAll("_", "");

		//Определяем начальную и конечную дату, а также период.
		String textPart;
		LocalDate date1;
		LocalDate date2;
		textPart = subLines.stream().filter(obj -> obj.matches(testType.get(0))).findFirst().orElse("");
		if (textPart.equals("")) {
			throw new ParseException("can't read date");
		}
		textPart = subLines.get(subLines.indexOf(textPart) + 1);
		textPart = textPart.replaceAll("_", "").trim();
		textPart = textPart.replaceAll(" ", "").trim();
		textPart = textPart.replaceAll("c", "с").trim();

		DateTimeFormatter formatter;

		start = textPart.replace("с", "").trim();
		start = start.substring(0, start.indexOf("по")).trim();
		if (start.length() == 10) {
			formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		} else {
			formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
		}

		end = textPart.substring(textPart.indexOf("по") + 2).trim().substring(0, start.length());

		try {
			date1 = LocalDate.parse(end,formatter).plusDays(1);
			date2 = LocalDate.parse(start, formatter);
		} catch (DateTimeParseException e) {
			throw new ParseException("date parse exception");
		}
		if (date1.compareTo(date2) == 0) {
			throw new ParseException("Одинаковые даты");
		}

		HeaderRange headerRangeClass = instance.new HeaderRange();
		//Определяем основные данные
		headerRangeClass.headTableList.add(pdfTable.headList);
		headerRangeClass.getHeader(headerRangeClass.headTableList.get(0));

		//Получаем строку параметров
		ArrayList<ParameterData> resultParam = new ArrayList<>();
		List<Range<Integer>> headColumnRanges = headerRangeClass.headerRange;
		List<List<TextPosition>> headTP = new ArrayList<>();
		List<TextPosition> tempList = new ArrayList<>(pdfTable.headList);

		//Получаем символы по границам колонок
		for (Range<Integer> range: headColumnRanges) {
			headTP.add(new ArrayList<>());
			for (Iterator<TextPosition> i = tempList.iterator(); i.hasNext();) {
				TextPosition item = i.next();
				Range<Integer> textRange = Range.closed((int) item.getX(),
						(int) (item.getX() + item.getWidth()));
				if (range.encloses(textRange)) {
					headTP.get(headTP.size() -1).add(item);
					i.remove();
				}
			}
		}

		for (List<TextPosition> list: headTP) {
			StringBuilder retVal = new StringBuilder();
			List<Range<Integer>> lineRanges = getLineRanges(list);
			List<String> columnParams = new ArrayList<>();
			//Формируем список возможный параметров для каждой колонки
			for (Range<Integer> range: lineRanges) {
				retVal.setLength(0);
				for (Iterator<TextPosition> i = list.iterator(); i.hasNext();) {
					TextPosition item = i.next();
					Range<Integer> textRange = Range.closed((int) item.getY(),
							(int) (item.getY() + item.getHeight()));
					if (range.encloses(textRange)) {
						retVal.append(item.toString());
						i.remove();
					}
				}
				columnParams.add(retVal.toString());
			}

			boolean addStatus = false;
			for (int i = columnParams.size() - 1; i >= 0; i--) {
				String item = columnParams.get(i);
				if (paramName.contains(item)) {
					resultParam.add(new ParameterData(dbParamName.get(paramName.indexOf(item))));
					addStatus = true;
					break;
				}
			}
			if (!addStatus) {
				throw new ParseException("Не знаю параметра: " + columnParams);
			}
		}

		//Проверка на повторяющиеся параметры
		Set<String> foundStrings = new HashSet<>();
		Set<String> duplicates = new HashSet<>();
		for (ParameterData item : resultParam) {
			if (foundStrings.contains(item.getName())) {
				duplicates.add(item.getName());
			} else {
				foundStrings.add(item.getName());
			}
		}
		duplicates.remove("нет");
		if (!duplicates.isEmpty()) {
			throw new ParseException("duplicate parameters " + duplicates);
		}

		//Получаем значения таблицы
		List<Range<Integer>> dataColumnRanges = headerRangeClass.getColumnRanges(pdfTable.dataList);

		//Объединяем колонки значений и шапки
		pdfTable.columnRanges.clear();
		pdfTable.columnRanges.addAll(dataColumnRanges);
		if (dataColumnRanges.size() != headColumnRanges.size()) {
			for (Range<Integer> rangeHead : headColumnRanges) {
				boolean connected = false;
				for (Range<Integer> rangeData : dataColumnRanges) {
					if (rangeHead.isConnected(rangeData)) {
						connected = true;
						break;
					}
				}
				if (!connected) {
					pdfTable.columnRanges.add(Range.closed(rangeHead.lowerEndpoint(), rangeHead.upperEndpoint()));
				}
			}
		}

		TrapRangeBuilder lineTrapRangeBuilder = new TrapRangeBuilder();
		pdfTable.columnRanges.forEach(lineTrapRangeBuilder::addRange);
		pdfTable.columnRanges = lineTrapRangeBuilder.build();

		//По полученному columnRanges формируем таблицу с данными
		List<Range<Integer>> lineRanges = getLineRanges(pdfTable.dataList);
		pdfTable.createTableStatus = true;
		StringBuilder table = pdfTable.buildTable(pdfTable.dataList, lineRanges);
		pdfTable.createTableStatus = false;

		ArrayList<String> subList = new ArrayList<>(Arrays.asList(table.toString().split("\n")));

		SimpleDateFormat formatDataShort = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat formatData = new SimpleDateFormat("dd.MM.yy");

		for (String line: subList) {
			String value;
			String[] items = line.split("\\|");
			if (items.length == resultParam.size()) {
				for (int i = 0; i < resultParam.size(); i++) {
					value = items[i].trim();
					if (removeData.contains(value)) {
						value = "";
					}
					value = value.replace(',', '.');
					value = value.replaceAll("#", "");
					if (value.equals("-") || (value.equals("---"))) {
						value = "";
					}
					if (value.length() > 1) {
						//В тысячных значениях бывает пробел 1 000
						value = value.replaceAll(" ", "");
						//Бывает - попадает в конец
						if (value.substring(value.length() - 1).matches("-")) {
							value = "-" + value.substring(0, value.length() - 1);
						}
					}
					if (i == 0) {
						try {
							Date date = formatData.parse(value);
							value = formatDataShort.format(date);
						} catch (java.text.ParseException ignore) {
						}
					}
					resultParam.get(i).getData().add(value);
				}
			} else {
				throw new ParseException("Нечитаемые данные");
			}
		}

		//Убираем строки где только дата, а остальные значения пустые
		List<Integer> removeList = new ArrayList<>();
		boolean flag;
		for (int i = 0; i < resultParam.get(0).getData().size(); i++) {
			flag = true;
			for (int j = 1; j < resultParam.size(); j++) {
				if (!resultParam.get(j).getData().get(i).equals("")) {
					flag = false;
					break;
				}
			}
			if (flag) {
				removeList.add(i);
			}
		}
		for (int i = removeList.size() - 1; i >= 0; i--) {
			for (ParameterData item: resultParam) {
				item.getData().remove(removeList.get(i).intValue());
			}
		}

		//Проверяем на наличие не числовых значений
		for (int i = 1; i < resultParam.size(); i++) {
			for (String item: resultParam.get(i).getData()) {
				if (!item.equals("")) {
					try {
						new BigDecimal(item);
					} catch (NumberFormatException e) {
						throw new ParseException("Не числовое значение: " + item.replace('.', ','));
					}
				}
			}
		}

		//Выделяем строку параметров интеграторов
		ArrayList<ParameterData> resultParamIntegr = new ArrayList<>();

		headerRangeClass.headTableList.clear();
		headerRangeClass.index = 0;
		headerRangeClass.headerRange.clear();

		headerRangeClass.headTableList.add(pdfTable.headListIntegr);
		headerRangeClass.getHeader(headerRangeClass.headTableList.get(0));

		for (int i = 1; i < headerRangeClass.headerRange.size(); i++) {
			headerRangeClass.headerRange.set(i, Range.closed(headerRangeClass.headerRange.get(i).lowerEndpoint(),
					headerRangeClass.headerRange.get(i).upperEndpoint() + 10));
		}

		TrapRangeBuilder trapRangeBuilder = new TrapRangeBuilder();
		headerRangeClass.headerRange.forEach(trapRangeBuilder::addRange);
		headerRangeClass.headerRange = trapRangeBuilder.build();

		headColumnRanges = headerRangeClass.headerRange;

		headTP = new ArrayList<>();
		tempList = new ArrayList<>(pdfTable.headListIntegr);
		for (Range<Integer> range: headColumnRanges) {
			headTP.add(new ArrayList<>());
			for (Iterator<TextPosition> i = tempList.iterator(); i.hasNext();) {
				TextPosition item = i.next();
				Range<Integer> textRange = Range.closed((int) item.getX(),
						(int) (item.getX() + item.getWidth()));
				if (range.encloses(textRange)) {
					headTP.get(headTP.size() -1).add(item);
					i.remove();
				}
			}
		}

		for (List<TextPosition> list: headTP) {
			StringBuilder retVal = new StringBuilder();
			List<Range<Integer>> lineRangesIntegr = getLineRanges(list);
			List<String> columnParams = new ArrayList<>();
			for (Range<Integer> range: lineRangesIntegr) {
				retVal.setLength(0);
				for (Iterator<TextPosition> i = list.iterator(); i.hasNext();) {
					TextPosition item = i.next();
					Range<Integer> textRange = Range.closed((int) item.getY(),
							(int) (item.getY() + item.getHeight()));
					if (range.encloses(textRange)) {
						retVal.append(item.toString());
						i.remove();
					}
				}
				columnParams.add(retVal.toString());
			}

			boolean addStatus = false;
			for (String item : columnParams) {
				if (paramName.contains(item)) {
					resultParamIntegr.add(new ParameterData(dbParamNameIntegr.get(paramName.indexOf(item))));
					addStatus = true;
				}
			}
			if (!addStatus) {
				throw new ParseException("Не знаю параметра: " + columnParams);
			}
		}

		//Проверка на повторяющиеся параметры
		foundStrings = new HashSet<>();
		duplicates = new HashSet<>();
		for (ParameterData item : resultParamIntegr) {
			if (foundStrings.contains(item.getName())) {
				duplicates.add(item.getName());
			} else {
				foundStrings.add(item.getName());
			}
		}
		if (!duplicates.isEmpty()) {
			throw new ParseException("duplicate parameters " + duplicates);
		}

		//Получаем значения таблицы
		dataColumnRanges = headerRangeClass.getColumnRanges(pdfTable.dataListIntegr);
		if (dataColumnRanges.size() >= 2
				&& (dataColumnRanges.get(1).lowerEndpoint() - dataColumnRanges.get(0).upperEndpoint() <= 2)) {
			dataColumnRanges.set(0, Range.closed(dataColumnRanges.get(0).lowerEndpoint(), dataColumnRanges.get(1).upperEndpoint()));
			TrapRangeBuilder lineTrapRangeBuilder2 = new TrapRangeBuilder();
			dataColumnRanges.forEach(lineTrapRangeBuilder2::addRange);
			dataColumnRanges = lineTrapRangeBuilder2.build();
		}

		TrapRangeBuilder lineTrapRangeBuilder1 = new TrapRangeBuilder();
		dataColumnRanges.forEach(lineTrapRangeBuilder1::addRange);
		headColumnRanges.forEach(lineTrapRangeBuilder1::addRange);

		if (dataColumnRanges.size() == headColumnRanges.size()) {
			pdfTable.columnRanges = dataColumnRanges;
		} else {
			pdfTable.columnRanges = lineTrapRangeBuilder1.build();
		}

		lineRanges = getLineRanges(pdfTable.dataListIntegr);
		pdfTable.createTableStatus = true;
		table = pdfTable.buildTable(pdfTable.dataListIntegr, lineRanges);
		pdfTable.createTableStatus = false;

		subList = new ArrayList<>(Arrays.asList(table.toString().split("\n")));

		SimpleDateFormat formatDataLong1 = new SimpleDateFormat("dd.MM.yyHH:mm");
		SimpleDateFormat formatDataLong2 = new SimpleDateFormat("dd.MM.yyHH.mm");

		for (String line : subList) {
			String value;
			String[] items = line.split("\\|");
			if (items.length == resultParamIntegr.size()) {
				for (int i = 0; i < resultParamIntegr.size(); i++) {
					value = items[i].trim();
					if (removeData.contains(value)) {
						value = "";
					}
					value = value.replaceAll(",", ".");
					value = value.replaceAll(" ", "");
					value = value.replaceAll("#", "");
					if (i == 0) {
						try {
							Date date = formatDataLong1.parse(value);
							value = formatDataShort.format(date);
						} catch (java.text.ParseException ignore) {
						}
						try {
							Date date = formatDataLong2.parse(value);
							value = formatDataShort.format(date);
						} catch (java.text.ParseException ignore) {
						}
					}
					resultParamIntegr.get(i).getData().add(value);
				}
			} else {
				throw new ParseException("Нечитаемые данные");
			}
		}

		//Проверяем на наличие не числовых значений
		for (int i = 1; i < resultParamIntegr.size(); i++) {
			for (String item: resultParamIntegr.get(i).getData()) {
				if (!item.equals("")) {
					try {
						new BigDecimal(item);
					} catch (NumberFormatException e) {
						throw new ParseException("Не числовое значение: " + item.replace('.', ','));
					}
				}
			}
		}

		ReportData reportData = new ReportData();
		reportData.setFileName(Paths.get(filePath).getFileName().toString());
		reportData.setAddress(address);
		reportData.setCounterType(counterType);
		reportData.setCounterNumber(counterNumber);
		reportData.setParam(resultParam);
		reportData.setParamIntegr(resultParamIntegr);
		reportData.setStartDate(date2);
		reportData.setEndDate(date1);
		reportData.setReportType(reportName);

		if (!reportData.checkData()) {
			reportData.print();

			throw new ParseException("Проверка не пройдена");
		}

		ParserUtils.updateValue("Time", reportData.getParam(), 3600);
		ParserUtils.updateValue("Timei", reportData.getParamIntegr(), 3600);
		ParserUtils.updateValue("p1", reportData.getParam(), 0.101325f);
		ParserUtils.updateValue("p2", reportData.getParam(), 0.101325f);
		ParserUtils.updateValue("p3", reportData.getParam(), 0.101325f);

		return reportData;
	}

//
//			//Кладем значения в кеш
//			boolean check;
//			synchronized (Cache.paramData.get(filePath)) {
//				Cache.paramData.get(filePath).setAddress(address);
//				Cache.paramData.get(filePath).setCounterType(counterType);
//				Cache.paramData.get(filePath).setCounterNumber(counterNumber);
//				Cache.paramData.get(filePath).setParam(resultParam);
//				Cache.paramData.get(filePath).setParamIntegr(resultParamIntegr);
//				Cache.paramData.get(filePath).setStartDate(date2);
//				Cache.paramData.get(filePath).setEndDate(date1);
//				Cache.paramData.get(filePath).setPeriod(hours);
//				Cache.paramData.get(filePath).setFile(new File(filePath));
//				Cache.paramData.get(filePath).setReportType(reportType);
//				Cache.paramData.get(filePath).print();
//				check = Cache.paramData.get(filePath).checkData();
//			}
//
//			if (!check) {
//				synchronized (Cache.paramError.get(filePath)) {
//					Cache.paramError.get(filePath).add("Проверка не пройдена");
//				}
//			}
//		} catch (Exception e) {
//			synchronized (Cache.paramError.get(filePath)) {
//				if (Cache.paramError.get(filePath).size() == 0) {
//					Cache.paramError.get(filePath).add("Неизвестная ошибка");
//					e.printStackTrace();
//				}
//			}
//		}
//
//		boolean removeData = false;
//		synchronized (Cache.paramError.get(filePath)) {
//			if (!Cache.paramError.get(filePath).isEmpty()) {
//				System.out.println("ERROR: " + Cache.paramError.get(filePath) + " " + filePath);
//				removeData = true;
//			} else {
//				parseStatus = true;
//				Cache.paramError.remove(filePath);
//			}
//		}
//
//		if (removeData) {
//			synchronized (Cache.paramData.get(filePath)) {
//				Cache.paramData.remove(filePath);
//			}
//			synchronized (Cache.paramTypeError) {
//				if (!Cache.paramTypeError.containsKey(typeName)) {
//					Cache.paramTypeError.put(typeName, new ArrayList<String>());
//				}
//				Cache.paramTypeError.get(typeName).add(filePath + " " + Cache.paramError.get(filePath));
//			}
//		}
//	}
//
//	private List<Range<Integer>> getColumnRanges(List<TextPosition> pageContent) {

//    }
//
	private static List<Range<Integer>> getLineRanges(List<TextPosition> pageContent) {
        TrapRangeBuilder lineTrapRangeBuilder = new TrapRangeBuilder();
        for (TextPosition textPosition : pageContent) {
            Range<Integer> lineRange = Range.closed((int) textPosition.getY(),
                    (int) (textPosition.getY() + textPosition.getHeight()));
            lineTrapRangeBuilder.addRange(lineRange);
        }
		return lineTrapRangeBuilder.build();
    }

	private static List<TextPosition> extractTextPositions(PDDocument pdDoc) throws IOException {
        TextPositionExtractor extractor = new TextPositionExtractor(pdDoc);
        return extractor.extract();
    }

	private static class TextPositionExtractor extends PDFTextStripper {

        private final List<TextPosition> textPositions = new ArrayList<>();
        private final PDDocument doc;

        private TextPositionExtractor(PDDocument doc) throws IOException {
            super.setSortByPosition(true);
            this.doc = doc;
        }

        protected void writeString(String string, List<TextPosition> textPositions) {
        	this.textPositions.addAll(textPositions);
        }

        private List<TextPosition> extract() throws IOException {
        	Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        	super.writeText(doc, dummy);
            this.textPositions.sort((o1, o2) -> {
				int retVal = 0;
				if (o1.getY() < o2.getY()) {
					retVal = -1;
				} else if (o1.getY() > o2.getY()) {
					retVal = 1;
				}
				return retVal;

			});
            return this.textPositions;
        }
    }

	private class PDFTable {

		private boolean loadDataStatus = true;
		private boolean loadDataIntegrateStatus = false;
		private boolean createTableStatus = false;
		private boolean skipLine = true;
		private boolean startLoad = false;

		private List<Range<Integer>> columnRanges = new ArrayList<>();

		private List<TextPosition> headList = new ArrayList<>();
		private List<TextPosition> headListIntegr = new ArrayList<>();
		private List<TextPosition> dataList = new ArrayList<>();
		private List<TextPosition> dataListIntegr = new ArrayList<>();

		private StringBuilder buildTable(List<TextPosition> tableContent, List<Range<Integer>> rowTrapRanges) {
			StringBuilder retVal = new StringBuilder();
			int idx = 0;
			int rowIdx = 0;
			List<TextPosition> rowContent = new ArrayList<>();
			while (idx < tableContent.size()) {
				TextPosition textPosition = tableContent.get(idx);
				Range<Integer> rowTrapRange = rowTrapRanges.get(rowIdx);
				Range<Integer> textRange = Range.closed((int) textPosition.getY(),
						(int) (textPosition.getY() + textPosition.getHeight()));
				if (rowTrapRange.encloses(textRange)) {
					rowContent.add(textPosition);
					idx++;
				} else {
					if (retVal.length() > 0) {
						retVal.append("\n");
					}
					StringBuilder row = buildRow(rowContent);
					retVal.append(row);
					rowContent.clear();
					rowIdx++;
				}
			}
			if (!rowContent.isEmpty() && rowIdx < rowTrapRanges.size()) {
				if (retVal.length() > 0) {
					retVal.append("\n");
				}
				StringBuilder row = buildRow(rowContent);
				retVal.append(row);
			}
			return retVal;
		}

		private StringBuilder buildRow(List<TextPosition> rowContent) {
			StringBuilder retVal = new StringBuilder();
			rowContent.sort((o1, o2) -> {
				int retVal1 = 0;
				if (o1.getX() < o2.getX()) {
					retVal1 = -1;
				} else if (o1.getX() > o2.getX()) {
					retVal1 = 1;
				}
				return retVal1;
			});

			if (!createTableStatus) {
				for (TextPosition item: rowContent) {
					retVal.append(item.toString());
				}
			} else {
				for (Range<Integer> range: columnRanges) {
					for (Iterator<TextPosition> i = rowContent.iterator(); i.hasNext();) {
						TextPosition item = i.next();
						Range<Integer> textRange = Range.closed((int) item.getX(),
								(int) (item.getX() + item.getWidth()));
						if (range.encloses(textRange)) {
							retVal.append(item.toString());
							i.remove();
						}
					}
					retVal.append(" |");
				}
			}

			if (loadDataStatus) {
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy");
				try {
					format.parse(retVal.toString().substring(0, 8));
					startLoad = false;
					rowContent.removeIf(item -> item.toString().equals(" "));
					while (!rowContent.get(rowContent.size() - 1).toString().matches("\\d")) {
						rowContent.remove(rowContent.size() - 1);
					}
					for (Iterator<TextPosition> i = rowContent.iterator(); i.hasNext();) {
						TextPosition item = i.next();
						int index = rowContent.indexOf(item);
						if (index != rowContent.size() - 1) {
							if (item.toString().equals("-")
									&& rowContent.get(index + 1).toString().equals("-")) {
								i.remove();
							} else {
								if (item.toString().equals("-")
										&& ((int) rowContent.get(index + 1).getX() - (int) item.getEndX() > 5)) {
									i.remove();
								}
							}
						} else {
							if (item.toString().equals("-")) {
								i.remove();
							}
						}
					}
					dataList.addAll(rowContent);
				} catch (Exception ignored) {
				}

				if ((retVal.toString().matches("Дата.*") && (!retVal.toString().matches("Дата последующей.*")))
						|| startLoad) {
					startLoad = true;
					headList.addAll(rowContent);
				}

				if (retVal.toString().matches("Итого.*") || retVal.toString().matches("Итого.*")) {
					loadDataStatus = false;
					loadDataIntegrateStatus = true;
				}
			}

			if (loadDataIntegrateStatus) {
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy");
				try {
					format.parse(retVal.toString().substring(0, 8));
					startLoad = false;
					rowContent.removeIf(item -> item.toString().equals(" "));
					dataListIntegr.addAll(rowContent);
				} catch (Exception ignored) {
				}

				if (retVal.toString().trim().matches("Дата, время.*")
						|| retVal.toString().trim().matches("Дата Время.*")
						|| startLoad) {
					startLoad = true;
					headListIntegr.addAll(rowContent);
				}

				if (retVal.toString().matches("Итого.*")) {
					loadDataIntegrateStatus = false;
				}

				if (skipLine) {
					loadDataIntegrateStatus = true;
					skipLine = false;
				}
			}
			return retVal;
		}
	}

	private class HeaderRange {

		private List<List<TextPosition>> headTableList = new ArrayList<>();
		private int index = 0;

		private List<Range<Integer>> headerRange = new ArrayList<>();

		private List<Range<Integer>> getColumnRanges(List<TextPosition> pageContent) {
			TrapRangeBuilder lineTrapRangeBuilder = new TrapRangeBuilder();
			BigDecimal bigDEnd;
			for (TextPosition textPosition : pageContent) {
				bigDEnd = new BigDecimal(textPosition.getEndX()).setScale(0, RoundingMode.HALF_EVEN);
				Range<Integer> lineRange = Range.closed((int) textPosition.getX(), bigDEnd.intValue());
				lineTrapRangeBuilder.addRange(lineRange);
			}
			return lineTrapRangeBuilder.build();
		}

		//Метод который обходит шапку по ячейкам и формирует границы нижних ячеек
		private void getHeader(List<TextPosition> list) {
			List<TextPosition> tempList = new ArrayList<>(list);
			List<List<TextPosition>> listTP = new ArrayList<>();

			List<Range<Integer>> columnRanges = getColumnRanges(list);
			if (columnRanges.size() != 1) {
				headTableList.remove(index);

				int lowRange = columnRanges.get(0).lowerEndpoint();
				int upRange = columnRanges.get(columnRanges.size() - 1).upperEndpoint();
				int startValue = -1;
				int endValue = -1;
				//Определяем какую границу надо заменить
				for (int i = 0; i < headerRange.size(); i++) {
					if (headerRange.get(i).contains(lowRange)) {
						startValue = i;
					} else {
						if (headerRange.get(i).upperEndpoint() < lowRange
								&& headerRange.get(i + 1).lowerEndpoint() > lowRange) {
							startValue = i + 1;
						}
					}
					if (headerRange.get(i).contains(upRange)) {
						endValue = i;
					} else {
						if (headerRange.get(i).upperEndpoint() < upRange
								&& headerRange.get(i + 1).lowerEndpoint() > upRange) {
							endValue = i;
						}
					}
				}

				if (startValue != -1 && endValue != -1) {
					if (endValue >= startValue) {
						headerRange.subList(startValue, endValue + 1).clear();
					}
					headerRange.addAll(startValue, columnRanges);
				} else {
					if (headerRange.size() == 0) {
						headerRange.addAll(columnRanges);
					}
				}

				for (Range<Integer> range: columnRanges) {
					listTP.add(new ArrayList<>());
					for (Iterator<TextPosition> i = tempList.iterator(); i.hasNext();) {
						TextPosition item = i.next();
						Range<Integer> textRange = Range.closed((int) item.getX(),
								(int) (item.getX() + item.getWidth()));
						if (range.encloses(textRange)) {
							listTP.get(listTP.size() -1).add(item);
							i.remove();
						}
					}
				}

				//Выкидываем значения ввиде непонятных пробелов отдельно стоящих
				for (Iterator<List<TextPosition>> i = listTP.iterator(); i.hasNext();) {
					List<TextPosition> item = i.next();
					if (item.size() == 1 && item.get(0).toString().equals(" ")) {
						i.remove();
						Range<Integer> range = getColumnRanges(item).get(0);
						headerRange.removeIf(rangeItem -> rangeItem.encloses(range));
					}
				}

				headTableList.addAll(index, listTP);
				index--;

				for (List<TextPosition> textPositions : listTP) {
					index++;
					getHeader(textPositions);
				}
			} else {
				List<Range<Integer>> lineRanges = getLineRanges(list);
				if (lineRanges.size() != 1) {
					headTableList.remove(index);

					for (Range<Integer> range: lineRanges) {
						listTP.add(new ArrayList<>());
						for (Iterator<TextPosition> i = tempList.iterator(); i.hasNext();) {
							TextPosition item = i.next();
							Range<Integer> textRange = Range.closed((int) item.getY(),
									(int) (item.getY() + item.getHeight()));
							if (range.encloses(textRange)) {
								listTP.get(listTP.size() -1).add(item);
								i.remove();
							}
						}
					}

					listTP.removeIf(item -> item.size() == 1 && item.get(0).toString().equals(" "));

					headTableList.addAll(index, listTP);
					index--;

					for (List<TextPosition> textPositions : listTP) {
						index++;
						getHeader(textPositions);
					}
				}
			}
		}
	}
}
