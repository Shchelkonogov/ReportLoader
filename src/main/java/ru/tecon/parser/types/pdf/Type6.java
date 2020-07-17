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
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Type6 {

	private static Logger logger = Logger.getLogger(Type6.class.getName());

	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");

	private static Type6 instance = new Type6();

	private static List<String> testType = Arrays.asList("Месячный протокол учёта тепловой энергии",
			"и теплоносителя за.*", ".*с.*по.*", "Ответственное лицо:.*", "Прибор:.*Сер.номер:.*Расход.*",
			"Модель:.*Версия ПО:.*");
	private static Map<String, String> variableTestType =
			Arrays.stream(new String[][] {
					{"Потребитель:.*Абонент:.*", "Потребитель:.*Район:.*"},
					{"Адрес потребителя:.*Телефон:.*", "Адрес потребителя:.*Абонент:.*"}
			}).collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

	private static final List<String> removeData = new ArrayList<>(Arrays.asList("<E:Co", "d>",
			"<", "#", "T", "R", "X"));

	private static List<String> paramName = Arrays.asList("Дата /", "Q", "tпод", "tобр", "tхв", "Vпод", "Vобр", "Рпод", "Робр",
			"Тнар", "Tнар", "Pпод", "Pобр", "Pхв", "Vхв", "Нарастающим", "Gпод", "Gобр",
			"tокр", "Gдоп", "Vдоп", "Подмес", "Утечка",
			"Дата /", "Q", "tпод", "tобр", "Gпод", "Gобр", "Pпод", "Pобр", "Tнар", "Тнар",
			"Нарастающим", "tхв", "Vхв", "Vпод", "Vобр", "tокр", "Gдоп", "Vдоп", "Рпод",
			"Робр", "Pхв", "Подмес", "Утечка",
			"Дата", "t1", "V1", "P1", "Tнар", "Нарастающим", "Тнар",
			"Дата /", "Q", "tпод", "tобр", "Gпод", "Gобр", "Тнар", "Подмес", "Утечка",
			"Нарастающим", "Tнар", "Pпод", "Pобр");

	private static List<String> dbParamName = Arrays.asList("Дата", "Q", "T1", "T2", "T3", "V1", "V2", "p1", "p2",
			"Time", "Time", "p1", "p2", "нет", "V3", "Дата", "G1", "G2", "T3",
			"нет", "нет", "нет", "нет",
			"Дата", "Q", "T1", "T2", "G1", "G2", "p1", "p2", "Time",
			"Time", "Дата", "T3", "V3", "V1", "V2", "T3", "нет", "нет", "p1",
			"p2", "нет", "нет", "нет",
			"Дата", "T3", "V3", "нет", "нет", "Дата", "нет",
			"Дата", "Q", "T1", "T2", "G1", "G2", "Time", "нет", "нет",
			"Дата", "Time", "p1", "p2");

	private static List<String> dbParamNameIntegr = Arrays.asList("Дата", "Q", "T1", "T2", "T3", "V1", "V2", "p1", "p2",
			"Time", "Time", "p1", "p2", "нет", "V3", "Дата", "G1", "G2", "T3",
			"нет", "нет", "нет", "нет",
			"Дата", "Q", "T1", "T2", "G1", "G2", "p1", "p2", "Time",
			"Time", "Дата", "T3", "V3", "V1", "V2", "T3", "нет", "нет", "p1",
			"p2", "нет", "нет", "нет",
			"Дата", "T3", "V3", "нет", "нет", "Дата", "нет",
			"Дата", "Q", "T1", "T2", "G1", "G2", "Time", "нет", "нет",
			"Дата", "Time", "p1", "p2");

	private Type6() {
	}

	private static List<String> createList(String filePath, PDFTable pdfTable) {
		try (PDDocument document = PDDocument.load(new FileInputStream(new File(filePath)))) {
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
							break;
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
			if (subLines.stream().noneMatch(obj -> obj.trim().matches(item))) {
				test = false;
				break;
			}
		}
		for (Map.Entry<String, String> entry: variableTestType.entrySet()) {
			if (subLines.stream().noneMatch(obj -> obj.trim().matches(entry.getKey()))
					&& subLines.stream().noneMatch(obj -> obj.trim().matches(entry.getValue()))) {
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

		final String addressStart = "Адрес потребителя:";
		address = subLines.stream().filter(obj -> obj.matches(addressStart + ".*")).findFirst().orElse("");

		if (address.equals("")) {
			throw new ParseException("can't read address");
		}

		int addressIndex = subLines.indexOf(address);
		if (addressIndex != 0 &&
				subLines.get(addressIndex - 1).matches("Потребитель:.*") &&
				subLines.get(addressIndex + 1).matches("Ответственное лицо:.*")) {
			final List<String> addressEndList = Arrays.asList("Телефон:", "Абонент:");
			String addressEnd = null;
			for (String obj : addressEndList) {
				if (address.matches(".*" + obj + ".*")) {
					addressEnd = obj;
					break;
				}
			}
			if (addressEnd != null) {
				address = address.substring(address.indexOf(addressStart) + addressStart.length(),
						address.indexOf(addressEnd)).trim();
			} else {
				throw new ParseException("Ошибка в адресе потребителя");
			}
		} else {
			throw new ParseException("Адрес потребителя больше чем на одной строке");
		}

		//Определяем тип прибора.
		final String counterTypeStart = "Модель:";
		String counterType = subLines.stream().filter(obj -> obj.matches(counterTypeStart + ".*"))
				.findFirst()
				.orElse("");
		if (counterType.equals("")) {
			throw new ParseException("can't read counter type");
		}
		counterType = counterType
				.substring(counterType.indexOf(counterTypeStart) + counterTypeStart.length(),
						counterType.indexOf("Версия ПО:")).trim();

		//Определяем тип отчета.
		String reportName = null;
		for (int i = subLines.size() - 1; i >= 0; i--) {
			if (!subLines.get(i).trim().equals("")) {
				reportName = subLines.get(i).trim();
				break;
			}
		}

		//Определяем серийний номер.
		final String counterNumberStart = "Сер.номер:";
		String counterNumber = subLines.stream()
				.filter(obj -> obj.matches(".*" + counterNumberStart + ".*"))
				.findFirst()
				.orElse("");
		if (counterNumber.equals("")) {
			throw new ParseException("can't read counter number");
		}
		counterNumber = counterNumber
				.substring(counterNumber.indexOf(counterNumberStart) + counterNumberStart.length(),
						counterNumber.indexOf("Расход")).trim();

		//Определяем начальную и конечную дату, а также период.
		LocalDate date1;
		LocalDate date2;
		final String from = "с";
		final String to = "по";

		String textPart = subLines.stream()
				.filter(obj -> obj.matches(".*" + from + ".*" + to + ".*"))
				.findFirst().orElse("");

		if (textPart.equals("")) {
			throw new ParseException("can't read date");
		}

		String start = textPart.substring(textPart.indexOf(from) + from.length())
				.trim().substring(0, 8);
		String end = textPart.substring(textPart.indexOf(to) + to.length())
				.trim().substring(0, 8);

		try {
			date1 = LocalDate.parse(end, formatter);
			date2 = LocalDate.parse(start, formatter);
		} catch (DateTimeParseException e) {
			throw new ParseException("Ошибка датах");
		}

		if (date1.compareTo(date2) == 0) {
			throw new ParseException("Одинаковые даты");
		}


		HeaderRange headerRangeClass = instance.new HeaderRange();
		//Определяем основные данные
		headerRangeClass.headTableList.add(pdfTable.headList);
		headerRangeClass.getHeader(headerRangeClass.headTableList.get(0));

		//Добавляем из-за возможоной погрешности разбега колонок
		for (int i = 1; i < headerRangeClass.headerRange.size(); i++) {
			headerRangeClass.headerRange.set(i, Range.closed(headerRangeClass.headerRange.get(i).lowerEndpoint(),
					headerRangeClass.headerRange.get(i).upperEndpoint() + 5));
		}

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
				String item = columnParams.get(i).trim();
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
		List<Range<Integer>> dataColumnRanges = headerRangeClass.getColumnRanges1(pdfTable.dataList);

		//Объединяем колонки значений и шапки
		pdfTable.columnRanges.clear();
		pdfTable.columnRanges.addAll(dataColumnRanges);
		for (Range<Integer> rangeHead : headColumnRanges) {
			boolean connected = false;
			for (Range<Integer> rangeData : dataColumnRanges) {
				if (rangeHead.isConnected(rangeData)) {
					connected = true;
					break;
				}
			}
			if (!connected) {
				pdfTable.columnRanges.add(Range.closed(rangeHead.lowerEndpoint(), rangeHead.upperEndpoint() + 5));
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

		SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yy");
		SimpleDateFormat format2 = new SimpleDateFormat("dd.MM.yyyy");
		for (String line: subList) {
			String value;
			String[] items = line.split("\\|");
			if (items.length == resultParam.size()) {
				for (int i = 0; i < resultParam.size(); i++) {
					value = items[i].trim();
					if (removeData.contains(value)) {
						value = "";
					}
					for (String obj : removeData) {
						if (value.matches(".*" + obj + ".*")) {
							value = value.replaceAll(obj, "").trim();
						}
					}
					if (i == 0 && value.matches("\\d{2}.\\d{2}.\\d{2}")) {
						try {
							value = format2.format(format1.parse(value));
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
				if (!item.equals("") && !item.equals("-")) {
					try {
						new BigDecimal(item);
					} catch (NumberFormatException e) {
						throw new ParseException("Не числовое значение: " + item);
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
				String value = item.trim();
				if (paramName.contains(value)) {
					resultParamIntegr.add(new ParameterData(dbParamNameIntegr.get(paramName.indexOf(value))));
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
		dataColumnRanges = headerRangeClass.getColumnRanges1(pdfTable.dataListIntegr);
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
		for (String line : subList) {
			String value;
			String[] items = line.split("\\|");
			if (items.length == resultParamIntegr.size()) {
				for (int i = 0; i < resultParamIntegr.size(); i++) {
					value = items[i].trim();
					if (removeData.contains(value)) {
						value = "";
					}
					if (i == 0 && value.matches("\\d{2}.\\d{2}.\\d{2}")) {
						try {
							value = format2.format(format1.parse(value));
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
						throw new ParseException("Не числовое значение: " + item);
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

    private class HeaderRange {

		private List<List<TextPosition>> headTableList = new ArrayList<>();
		private int index = 0;

		private List<Range<Integer>> headerRange = new ArrayList<>();

		private List<Range<Integer>> getColumnRanges1(List<TextPosition> pageContent) {
			TrapRangeBuilder lineTrapRangeBuilder = new TrapRangeBuilder();
			for (TextPosition textPosition : pageContent) {
				Range<Integer> lineRange = Range.closed((int) textPosition.getX(),
						(int) (textPosition.getX() + textPosition.getWidth() + 2));
				lineTrapRangeBuilder.addRange(lineRange);
			}
			return lineTrapRangeBuilder.build();
		}

		//Метод который обходит шапку по ячейкам и формирует границы нижних ячеек
		private void getHeader(List<TextPosition> list) {
			List<TextPosition> tempList = new ArrayList<>(list);
			List<List<TextPosition>> listTP = new ArrayList<>();

			List<Range<Integer>> columnRanges = getColumnRanges1(list);
			if (columnRanges.size() > 1) {
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
						Range<Integer> range = getColumnRanges1(item).get(0);
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
					dataList.addAll(rowContent);
				} catch (Exception ignored) {
				}

				if (retVal.toString().matches("Дата.*")
						|| startLoad) {
					startLoad = true;
					headList.addAll(rowContent);
				}

				if (retVal.toString().matches("Итого:.*") || retVal.toString().matches("Итого :.*")) {
					loadDataStatus = false;
					loadDataIntegrateStatus = true;
				}
			}

			if (loadDataIntegrateStatus) {
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy");
				try {
					format.parse(retVal.toString().substring(0, 8));
					startLoad = false;
					dataListIntegr.addAll(rowContent);
				} catch (Exception ignored) {
				}

				if (retVal.toString().trim().matches("Нарастающим.*")
						|| startLoad) {
					startLoad = true;
					headListIntegr.addAll(rowContent);
				}

				if (retVal.toString().matches("Итого:.*")) {
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
}
