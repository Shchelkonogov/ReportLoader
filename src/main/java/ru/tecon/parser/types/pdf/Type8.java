package ru.tecon.parser.types.pdf;

import ru.tecon.parser.ParseException;
import ru.tecon.parser.model.ParameterData;
import ru.tecon.parser.model.ReportData;
import ru.tecon.parser.types.ParserUtils;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;

public class Type8 {

	private static Logger logger = Logger.getLogger(Type8.class.getName());

	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private static final List<String> testType =
			Arrays.asList(".*МЕСЯЧНЫЙ ПРОТОКОЛ УЧЕТА ТЕПЛОВОЙ ЭНЕРГИИ.*",
					".*И ТЕПЛОНОСИТЕЛЯ.*", ".*Hазвание потребителя.*Абонент.*", ".*Адрес потребителя.*Телефон.*",
					".*Ответственное лицо.*", ".*ычислитель.*Сер.ном..*");

	private static List<String> removeData = new ArrayList<String>(Arrays.asList("<", "T", "#", "R", "X", ">",
			"C", "\\*", "t", "!"));

	private static List<String> paramName = Arrays.asList("Дата", "Qтеп", "tпод", "tобp", "tп", "Vпод", "Vобр", "Vпод-Vобр", "pпод",
			"pобр", "Tнар", "tокр", "Vпод-Vпод", "Gпод-Gобр", "Gпод", "Gобр", "Gпод-Gпод",
			"t1", "t2", "V1", "V2", "V1-V2", "p1", "p2", "Vп", "pп", "tдоп",
			"Дата", "Qтеп", "tпод", "tобp", "Gпод", "Gобр", "Gпод-Gобр", "pпод", "pобр",
			"Tнар", "Gпод-Gпод", "tокр", "t1", "t2", "G1", "G2", "Gп", "G1-G2", "Vпод",
			"Vобр", "Vпод-Vобр", "p1", "p2", "Vдоп", "tп");

	private static List<String> dbParamName = Arrays.asList("Дата", "Q", "T1", "T2", "T3", "V1", "V2", "нет", "p1",
			"p2", "Time", "нет", "нет", "нет", "G1", "G2", "нет", "T1", "T2",
			"V1", "V2", "нет", "p1", "p2", "нет", "нет", "T3",
			"Дата", "Q", "T1", "T2", "G1", "G2", "нет", "p1", "p2",
			"Time", "нет", "нет", "T1", "T2", "G1", "G2", "нет", "нет",
			"V1", "V2", "нет", "p1", "p2", "нет", "нет");

	private static List<String> dbParamNameIntegr = Arrays.asList("Дата", "Q", "T1", "T2", "T3", "V1", "V2", "нет", "p1",
			"p2", "Time", "нет", "нет", "нет", "G1", "G2", "нет", "T1", "T2",
			"V1", "V2", "нет", "p1", "p2", "нет", "нет", "T3",
			"Дата", "Q", "T1", "T2", "G1", "G2", "нет", "p1", "p2",
			"Time", "нет", "нет", "T1", "T2", "G1", "G2", "нет", "нет",
			"V1", "V2", "нет", "p1", "p2", "нет", "нет");

	private static String updateContent(String content) {
		return content.replaceAll(String.valueOf((char) 160), " ");
	}

	private static List<String> createList(String content) {
		List<String> list = new ArrayList<>(Arrays.asList(content.split("\n")));
		list.removeIf(s -> s.trim().equals(""));

		return list;
	}

	private static List<String> createSubList(List<String> list) {
		List<String> subLines = new ArrayList<>();
		for (String obj: list) {
			if (obj.matches(".*═════.*")) {
				break;
			}
			subLines.add(obj);
		}

		return subLines;
	}

	public static boolean checkType(String content) {
		logger.info("Type8 checkType");

		content = updateContent(content);

		List<String> list = createList(content);

		if (list.stream().anyMatch(obj -> obj.matches(".*═════.*"))) {
			List<String> subLines = createSubList(list);

			boolean test = true;
			for (String item : testType) {
				if (subLines.stream().noneMatch(obj -> obj.trim().matches(item))) {
					test = false;
					break;
				}
			}

			if (test) {
				logger.info("checkType is successful");
				return true;
			}
		}

		logger.info("checkType is unsuccessful");
		return false;
	}

	public static ReportData getData(String content, String filePath) throws ParseException {
		if (content.indexOf("МЕСЯЧНЫЙ ПРОТОКОЛ УЧЕТА ТЕПЛОВОЙ ЭНЕРГИИ") != content.lastIndexOf("МЕСЯЧНЫЙ ПРОТОКОЛ УЧЕТА ТЕПЛОВОЙ ЭНЕРГИИ")) {
			throw new ParseException("Больше одной страницы");
		}

		String textPart;
		List<String> list = createList(content);
		List<String> subLines = createSubList(list);

		//Определяем адрес потребителя.

		int index1 = subLines.indexOf(subLines.stream().filter(obj -> obj.matches(testType.get(2)))
				.findFirst().orElse("-1"));
		int index2 = subLines.indexOf(subLines.stream().filter(obj -> obj.matches(testType.get(3)))
				.findFirst().orElse("-1"));
		int index3 = subLines.indexOf(subLines.stream().filter(obj -> obj.matches(testType.get(4)))
				.findFirst().orElse("-1"));
		if ((index2 - index1 != 1) && (index3 - index2 != 1)) {
			throw new ParseException("Адресс поребителя больше чем на одной строке");
		}

		String address = subLines.stream().filter(obj -> obj.matches(testType.get(3)))
				.findFirst().orElse("");
		if (address.equals("")) {
			throw new ParseException("can't read address");
		}
		final String aStart = "Адрес потребителя";
		final String aEnd = "Телефон";
		address = address.substring(address.indexOf(aStart) + aStart.length(), address.indexOf(aEnd)).trim();
		if (address.length() == 0) {
			address = filePath.substring(filePath.lastIndexOf("\\") + 1);
		}

		//Определяем тип прибора.
		String counterType = subLines.stream().filter(obj -> obj.matches(testType.get(5)))
				.findFirst().orElse("");
		if (content.equals("")) {
			throw new ParseException("can't read counter type");
		}
		final String cStart = "ычислитель";
		final String cEnd = "Сер.ном.";
		counterType = counterType.substring(counterType.indexOf(cStart) + cStart.length(),
				counterType.indexOf(cEnd)).trim();

		//Определяем тип отчета.
		String reportName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf(".")).toLowerCase();

		//Определяем серийний номер.
		String counterNumber = subLines.stream().filter(obj -> obj.matches(testType.get(5)))
				.findFirst().orElse("");
		if (counterNumber.equals("")) {
			throw new ParseException("can't read counter number");
		}
		final String cNStart = "Сер.ном.";
		final String cNEnd = "Расход";
		counterNumber = counterNumber.substring(counterNumber.indexOf(cNStart) + cNStart.length()).trim();
		if (counterNumber.matches(".*" + cNEnd + ".*")) {
			counterNumber = counterNumber.substring(0, counterNumber.indexOf(cNEnd)).trim();
		}

		//Определяем начальную и конечную дату, а также период.
		LocalDate date1;
		LocalDate date2;
		String daysLine = list.stream().filter(obj -> obj.matches(testType.get(1)))
				.findFirst().orElse("");
		if (daysLine.equals("")) {
			throw new ParseException("can't read date line");
		}

		String month;
		if (daysLine.contains("ЗА")) {
			month = daysLine.substring(daysLine.indexOf("ЗА") + 2, daysLine.indexOf("мес")).trim();
		} else {
			month = daysLine.substring(daysLine.indexOf("И ТЕПЛОНОСИТЕЛЯ") + "И ТЕПЛОНОСИТЕЛЯ".length(), daysLine.indexOf("мес")).trim();
		}

		String year = daysLine.substring(daysLine.indexOf("мес") + 3).trim();
		year = "20" + year.substring(0, 2);

		YearMonth yearMonthObject = YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
		int daysInMonth = yearMonthObject.lengthOfMonth();

		String start = "01." + month + "." + year;
		String end = daysInMonth + "." + month + "." + year;

		try {
			date1 = LocalDate.parse(end, formatter).plusDays(1);
			date2 = LocalDate.parse(start, formatter);
		} catch (DateTimeParseException e) {
			throw new ParseException("can't parse date");
		}
		if (date1.compareTo(date2) == 0) {
			throw new ParseException("Одинаковые даты");
		}

		//Определяем основные данные

		//Выделяем строку параметров
		textPart = content.substring(content.indexOf("═════"));
		ArrayList<String> lines = new ArrayList<>(Arrays.asList(textPart.split("\n")));

		lines.removeIf(item -> item.equals(""));

		List<String> param = new ArrayList<>(Arrays.asList(lines.get(1).trim().substring(1).split("[║│]")));
		for (int i = 0; i < param.size(); i++) {
			param.set(i, param.get(i).trim());
		}

		//Переводим строку в массив id и statAgr
		ArrayList<ParameterData> resultParam = new ArrayList<>();
		for (String value: param) {
			value = value.replaceAll(" ", "");
			if (paramName.contains(value)) {
				resultParam.add(new ParameterData(dbParamName.get(paramName.indexOf(value))));
			} else {
				throw new ParseException("Не знаю параметра: " + value);
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
		textPart = textPart.substring(textPart.indexOf("\n") + 1);
		textPart = textPart.substring(textPart.indexOf("═════"));
		textPart = textPart.substring(textPart.indexOf("\n") + 1);
		String textPartIntegr = textPart.substring(textPart.indexOf("═════"));
		textPart = textPart.substring(0, textPart.indexOf("═════"));
		textPart = textPart.substring(0, textPart.lastIndexOf("\n"));
		ArrayList<String> subList = new ArrayList<>(Arrays.asList(textPart.split("\n")));

		SimpleDateFormat format = new SimpleDateFormat("dd-MM");
		SimpleDateFormat format3 = new SimpleDateFormat("dd.MM");
		String removeItem;
		for (Iterator<String> i = subList.iterator(); i.hasNext();) {
			String item = i.next().trim();
			if (item.equals("")) {
				i.remove();
			} else {
				removeItem = item.trim().split("[║│]")[1].trim();
				if (removeItem.matches("\\d{2}-\\d{2}")) {
					try {
						format.parse(removeItem);
					} catch (Exception e) {
						i.remove();
					}
				} else {
					if (removeItem.matches("\\d{2}.\\d{2}")) {
						try {
							format3.parse(removeItem);
						} catch (Exception e) {
							i.remove();
						}
					} else {
						i.remove();
					}
				}
			}
		}

		year = String.valueOf(date2.getYear());
		for (String line: subList) {
			String value;
			String[] items = line.trim().substring(1).split("[║│]");
			if (items.length == resultParam.size()) {
				for (int i = 0; i < resultParam.size(); i++) {
					value = items[i].trim();
					if (resultParam.get(i).getName().equals("нет")) {
						value = "";
					}
					for (String rItem : removeData) {
						if (value.matches(".*" + rItem + ".*")) {
							value = value.replaceAll(rItem, "").trim();
						}
					}
					if (value.length() == 1 && value.equals("-")) {
						value = "";
					}
					if (i == 0 && value.matches("\\d{2}-\\d{2}")) {
						value = value.replaceAll("-", ".") + "." + year;
					}
					if (i == 0 && value.matches("\\d{2}.\\d{2}")) {
						value = value + "." + year;
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
						throw new ParseException("Не числовое значение: " + item);
					}
				}
			}
		}

		//Выделяем строку параметров интеграторов
		textPartIntegr = textPartIntegr.substring(textPartIntegr.indexOf("\n") + 1);
		textPartIntegr = textPartIntegr.substring(textPartIntegr.indexOf("═════"));
		textPartIntegr = textPartIntegr.substring(textPartIntegr.indexOf("\n") + 1);

		lines = new ArrayList<>(Arrays.asList(textPartIntegr.split("\n")));
		lines.removeIf(item -> item.equals(""));
		String paramLine = lines.get(0).trim().substring(1);
		paramLine = paramLine.substring(0, paramLine.lastIndexOf("│"));
		param = new ArrayList<>(Arrays.asList(paramLine.split("[║│]")));
		for (int i = 0; i < param.size(); i++) {
			param.set(i, param.get(i).trim());
		}

		//Переводим строку в массив id и statAgr инттеграторов
		ArrayList<ParameterData> resultParamIntegr = new ArrayList<>();
		for (String value: param) {
			if (value.matches(".*Т/C.*")) {
				value = "Дата";
			}
			if (paramName.contains(value)) {
				resultParamIntegr.add(new ParameterData(dbParamNameIntegr.get(paramName.indexOf(value))));
			} else {
				throw new ParseException("Не знаю параметра: " + value);
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
		textPartIntegr = textPartIntegr.substring(textPartIntegr.indexOf("═════"), textPartIntegr.indexOf("─────"));

		subList = new ArrayList<>(Arrays.asList(textPartIntegr.split("\n")));
		SimpleDateFormat format2 = new SimpleDateFormat("dd-MM-yy HH:mm");
		for (Iterator<String> i = subList.iterator(); i.hasNext();) {
			String item = i.next();
			if (item.equals("")) {
				i.remove();
			} else {
				try {
					format2.parse(item.trim().substring(1).split("[║│]")[0].trim());
				} catch (Exception e) {
					i.remove();
				}
			}
		}

		SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
		for (String line: subList) {
			String value;
			String[] items = line.trim().substring(1, line.trim().lastIndexOf("│")).split("[║│]");
			if (items.length == resultParamIntegr.size()) {
				for (int i = 0; i < resultParamIntegr.size(); i++) {
					value = items[i].trim();
					if (i == 0) {
						try {
							Date date = format2.parse(value);
							value = format1.format(date);
						} catch(java.text.ParseException ignore) {
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

		ParserUtils.removeNullParameters(resultParam);
		ParserUtils.removeNullParameters(resultParamIntegr);

		if (!reportData.checkData()) {
			reportData.print();

			throw new ParseException("Проверка не пройдена");
		}

		ParserUtils.updateValue("Time", reportData.getParam(), 3600);
		ParserUtils.updateValue("Time", reportData.getParamIntegr(), 3600);
		ParserUtils.updateValue("p1", reportData.getParam(), 0.101325f);
		ParserUtils.updateValue("p2", reportData.getParam(), 0.101325f);
		ParserUtils.updateValue("p3", reportData.getParam(), 0.101325f);

		return reportData;
	}
}
