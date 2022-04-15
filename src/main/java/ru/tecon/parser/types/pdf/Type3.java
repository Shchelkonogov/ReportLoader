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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Type3 {

	private static Logger logger = Logger.getLogger(Type3.class.getName());

	private static Type3 instance = new Type3();

	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private static final List<String> testType = Arrays.asList("Месячный протокол учёта тепловой энергии и теплоносителя",
			"по системе.*", ".*с.*по.*", "Потребитель:.*ЦТП №:.*", "Адрес потребителя:.*Телефон:.*",
			"Ответственное лицо:.*", "Прибор:.*Сер.номер:.*", "Модель:.*Версия ПО:.*");

	private static final List<String> testType1 = Arrays.asList("Посуточная ведомость показаний прибора учета",
			".*за.*", ".*с.*по.*", "Потребитель:.*ЦТП №:.*", "Адрес потребителя:.*Телефон:.*",
			"Ответственное лицо:.*", "Прибор:.*Сер.номер:.*", "Модель:.*Версия ПО:.*");

	private static final List<String> testType2 = Arrays.asList("Посуточная ведомость показаний прибора учета",
			".*за.*", ".*с.*по.*", "Потребитель:.*ЦТП №:.*", "Адрес потребителя:.*Телефон:.*",
			"Прибор:.*Сер.номер:.*", "Модель:.*Версия ПО:.*");

	private static List<String> removeData = new ArrayList<>(Arrays.asList("U", "D", "G", "g,G", "U,g,G",
			"D,G", "D,g,G", "g", "U,g", "U,D", "E", "U,G", "U,D,g,G", "D,g", "U,D,g", "U,E", "D,E",
			"U,D,E", "U,g,E", "g,E", "g,G,E", "U,D,G", "D,g,E"));

	private static List<String> paramName = Arrays.asList("Дата", "Q", "Классиф", "Vпод", "Vобр", "Разбор", "tпод", "tобр", "Tхв",
			"Pпод", "Pобр", "Tнар", "Нарастающим", "Q,", "Подпитка", "tпод-tобр", "tхв",
			"Отказа", "Работы", "Q,Гкал", "Классификация", "Отказа, То", "Работы, Tр",
			"Tр", "То", "V", "Pхв", "Классиф ошибок", "Нарастающим итогом на:",
			"Q, [Гкал]", "Объёмный расход, [м3]", "Тнар, [час]", "Q[Гкал]", "Тнар[ч]",
			"Тнар", "Vпод[м3]", "Vпод[м]", "Vобр[м3]", "Vобр[м]", "Mпод", "Mобр", "Мпод.[т]",
			"Мобр.[т]", "Нарастающим итогом", "Q, Гкал", "3, Vпод [м]", "3, Vобр [м]",
			"Дата", "Q", "Gпод", "Gобр", "Подмес", "Утечка", "tпод", "tобр", "Pпод",
			"Pобр", "Tнар", "Классиф", "Нарастающим", "tпод-tобр*", "Tнераб*", "M1", "Q,",
			"Mпод", "Mобр", "Подпитка", "tпод-tобр", "Tр", "То", "Мпод.[т]", "Мобр.[т]",
			"Тнар", "M", "Массовый расход, [т]", "Массовый расход, т", "Vпод", "Vобр", "Vпод[м]", "Vобр[м]",
			"Vпод [м]", "Vобр [м]");

	private static List<String> dbParamName = Arrays.asList("Дата", "Q", "нет", "V1", "V2", "нет", "T1", "T2", "T3",
			"p1", "p2", "Time", "Дата", "Q", "нет", "нет", "T3", "нет", "Time",
			"Q", "нет", "нет", "Time", "Time", "нет", "V1", "нет", "нет", "Дата",
			"Q", "V1", "Time", "Q", "Time", "Time", "V1", "V1", "V2", "V2",
			"G1", "V2", "G1", "V2", "Дата", "Q", "V1", "V2",
			"Дата", "Q", "G1", "G2", "нет", "нет", "T1", "T2", "p1",
			"p2", "Time", "нет", "Дата", "нет", "нет", "G1", "Q", "G1", "G2",
			"нет", "нет", "Time", "нет", "G1", "G2", "Time", "G1", "G1", "G1", "V1",
			"V2", "V1", "V2", "V1", "V2");

	private static List<String> dbParamNameIntegr = Arrays.asList("Дата", "Q", "нет", "V1", "V2", "нет", "T1", "T2", "T3",
			"p1", "p2", "Time", "Дата", "Q", "нет", "нет", "T3", "нет", "Time",
			"Q", "нет", "нет", "Time", "Time", "нет", "V1", "нет", "нет", "Дата",
			"Q", "V1", "Time", "Q", "Time", "Time", "V1", "V1", "V2", "V2",
			"G1", "V2", "G1", "V2", "Дата", "Q", "V1", "V2",
			"Дата", "Q", "G1", "G2", "нет", "нет", "T1", "T2", "p1",
			"p2", "Time", "нет", "Дата", "нет", "нет", "G1", "Q", "G1", "G2",
			"нет", "нет", "Time", "нет", "G1", "G2", "Time", "G1", "G1", "G1", "V1",
			"V2", "V1", "V2", "V1", "V2");

	private Type3() {
	}

	private static List<String> createList(String filePath, PDFTable pdfTable) {
		try (InputStream in = Files.newInputStream(Paths.get(filePath));
			 PDDocument document = PDDocument.load(in)) {
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
		logger.info("Type3 checkType");

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
		boolean test1 = true;

		for (String item: testType1) {
			if (subLines.stream().noneMatch(obj -> obj.trim().matches(item))) {
				test1 = false;
				break;
			}
		}

		boolean test2 = true;

		for (String item: testType2) {
			if (subLines.stream().noneMatch(obj -> obj.trim().matches(item))) {
				test2 = false;
				break;
			}
		}

		if (test || test1 || test2) {
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
			throw new ParseException("empty report");
		}

		//Определяем адрес потребителя.
		String address;
		if ((subLines.get(3).matches("Потребитель:.*")
				&& subLines.get(4).matches("Адрес потребителя:.*")
				&& subLines.get(5).matches("Ответственное лицо:.*"))
				|| (subLines.get(3).matches("Потребитель:.*")
						&& subLines.get(5).matches("Адрес потребителя:.*")
						&& subLines.get(6).matches("Ответственное лицо:.*"))
				|| (subLines.get(2).matches("Потребитель:.*")
						&& subLines.get(3).matches("Адрес потребителя:.*")
						&& subLines.get(4).matches("Прибор:.*"))) {
			address = subLines.stream().filter(obj -> obj.matches("Адрес потребителя:.*")).findFirst().orElse("");
			address = address.substring(address.indexOf("Адрес потребителя:") + "Адрес потребителя:".length()
					, address.indexOf("Телефон:")).trim();
		} else {
			//Если больше одной строки (теоретически надо всегда так делать)
			int indexAdress = subLines.indexOf(subLines.stream()
					.filter(obj -> obj.matches("Адрес потребителя:.*"))
					.findFirst().orElse("-1"));
			int indexFace = subLines.indexOf(subLines.stream()
					.filter(obj -> obj.matches("Ответственное лицо:.*"))
					.findFirst().orElse("-1"));
			if (indexAdress < indexFace) {
				List<Range<Integer>> columnRanges = getColumnRanges(pdfTable.addressLine);

				//Знаем что должно быть 3 колонки и берем 2 колонку
				for (Iterator<TextPosition> i = pdfTable.addressLine.iterator(); i.hasNext();) {
					TextPosition item = i.next();
					Range<Integer> textRange = Range.closed((int) item.getX(),
							(int) (item.getX() + item.getWidth()));
					if (!columnRanges.get(1).encloses(textRange)) {
						i.remove();
					}
				}

				List<Range<Integer>> lineRanges = getLineRanges(pdfTable.addressLine);

				StringBuilder result = new StringBuilder();
				for (Range<Integer> range: lineRanges) {
					ArrayList<TextPosition> line = new ArrayList<>();
					for (Iterator<TextPosition> i = pdfTable.addressLine.iterator(); i.hasNext();) {
						TextPosition item = i.next();
						Range<Integer> textRange = Range.closed((int) item.getY(),
								(int) (item.getY() + item.getHeight()));
						if (range.encloses(textRange)) {
							line.add(item);
							i.remove();
						}
					}
					result.append(pdfTable.buildRow(line));
				}

				address = result.toString();
			} else {
				throw new ParseException("Адресс поребителя больше чем на одной строке");
			}
		}

		//Определяем тип прибора.
		String counterType;
		try {
			counterType = subLines.stream().filter(obj -> obj.matches("Модель:.*")).findFirst().orElse("");
			counterType = counterType.substring(counterType.indexOf("Модель:") + "Модель:".length()
					, counterType.indexOf("Версия ПО:")).trim();
		} catch (NoSuchElementException e) {
			throw new ParseException("Ошибка в типе прибора");
		}

		//Определяем тип отчета.
		String reportName = subLines.stream().filter(obj -> obj.matches("(по системе.*за.*)|(.*за.*г[.])|(.*за.*г[.].*)")).findFirst().orElse("").trim();
		if (reportName.equals("")) {
			throw new ParseException("can't read report name");
		}

		//Определяем серийний номер.
		String counterNumber = subLines.stream().filter(obj -> obj.matches(".*Сер.номер:.*Расход.*")).findFirst().orElse("");
		if (counterNumber.equals("")) {
			throw new ParseException("can't read counter number");
		}
		counterNumber = counterNumber.substring(counterNumber.indexOf("Сер.номер:") + "Сер.номер:".length(),
				counterNumber.indexOf("Расход")).trim();

		//Определяем начальную и конечную дату, а также период.
		String textPart = subLines.stream().filter(obj -> obj.trim().matches("(\\(c.*по.*\\))|(\\(с.*по.*\\))|(.*\\(c.*по.*\\))")).findFirst().orElse("");
		if (textPart.equals("")) {
			throw new ParseException("can't read date");
		}
		textPart = textPart.replace('с', 'c').substring(textPart.indexOf("(c"));
		LocalDate date1 = null;
		LocalDate date2 = null;

		String start = textPart.replace("(c", "").trim().substring(0, 10);
		String end = textPart.substring(textPart.indexOf("по") + 2).trim().substring(0, 10);

		try {
			date1 = LocalDate.parse(end, formatter).plusDays(1);
			date2 = LocalDate.parse(start, formatter);

			if (date1.compareTo(date2) == 0) {
				throw new ParseException("same date");
			}
		} catch (DateTimeParseException e) {
			e.printStackTrace();
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
				throw new ParseException("Не знаю параметра: " + columnParams.stream().map(e -> e = "'" + e + "'").collect(Collectors.toList()));
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
		List<Range<Integer>> dataColumnRanges = getColumnRanges(pdfTable.dataList);

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
					value = value.replace(',', '.');
					if (value.equals("-")) {
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
		ParserUtils.removeNullRows(resultParam);

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
				item = item.trim();
				if (paramName.contains(item)) {
					resultParamIntegr.add(new ParameterData(dbParamNameIntegr.get(paramName.indexOf(item))));
					addStatus = true;
				}
			}
			if (!addStatus) {
				throw new ParseException("Не знаю параметра: " + columnParams.stream().map(e -> e = "'" + e + "'").collect(Collectors.toList()));
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
		dataColumnRanges = getColumnRanges(pdfTable.dataListIntegr);
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

		SimpleDateFormat formatDataShort = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat formatDataLong = new SimpleDateFormat("dd.MM.yyyyHH:mm:ss");

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
					try {
						Date date = formatDataLong.parse(value);
						value = formatDataShort.format(date);
					} catch (java.text.ParseException ignore) {
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

		ParserUtils.removeNullParameters(resultParam);
        ParserUtils.removeNullParameters(resultParamIntegr);

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

	private static List<Range<Integer>> getColumnRanges(List<TextPosition> pageContent) {
        TrapRangeBuilder lineTrapRangeBuilder = new TrapRangeBuilder();
        for (TextPosition textPosition : pageContent) {
            Range<Integer> lineRange = Range.closed((int) textPosition.getX(),
                    (int) (textPosition.getX() + textPosition.getWidth()));
            lineTrapRangeBuilder.addRange(lineRange);
        }
		return lineTrapRangeBuilder.build();
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

    private class PDFTable {

		private boolean loadDataStatus = true;
		private boolean loadDataIntegrateStatus = false;
		private boolean createTableStatus = false;
		private boolean skipLine = true;
		private boolean startLoad = false;
		private boolean loadAddress = false;

		private float rightTrim = 0;

		private List<Range<Integer>> columnRanges = new ArrayList<>();

		private List<TextPosition> headList = new ArrayList<>();
		private List<TextPosition> headListIntegr = new ArrayList<>();
		private List<TextPosition> dataList = new ArrayList<>();
		private List<TextPosition> dataListIntegr = new ArrayList<>();
		private List<TextPosition> addressLine = new ArrayList<>();

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
				} catch (Exception ignore) {
				}

				if (retVal.toString().matches("Дата.*")
						|| retVal.toString().trim().matches("Объёмный расход.*")
						|| startLoad) {
					startLoad = true;
					headList.addAll(rowContent);
				}

				if (retVal.toString().matches("Итого:.*")) {
					loadDataStatus = false;
					loadDataIntegrateStatus = true;
				}
			}

			if (loadDataIntegrateStatus) {
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
				try {
					format.parse(retVal.toString().substring(0, 10));
					startLoad = false;
					if (rightTrim != 0) {
						rowContent.removeIf(textPosition -> textPosition.getX() >= rightTrim);
					}
					dataListIntegr.addAll(rowContent);
				} catch (Exception ignore) {
				}

				if (retVal.toString().trim().matches("Нарастающим.*")
						|| retVal.toString().trim().matches("Наиртаосгтоамю.*")
						|| startLoad) {
					startLoad = true;
					if (rightTrim == 0 && retVal.indexOf("Отчетный период:") != -1) {
						rightTrim = rowContent.get(retVal.indexOf("Отчетный период:")).getX();
					}
					if (rightTrim != 0) {
						rowContent.removeIf(textPosition -> textPosition.getX() >= rightTrim);
					}
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

			if (retVal.toString().matches(("Адрес потребителя:.*")) || loadAddress) {
				if (retVal.toString().matches(("Ответственное лицо:.*"))) {
					loadAddress = false;
				} else {
					loadAddress = true;
					addressLine.addAll(rowContent);
				}
			}
			return retVal;
		}
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
}
