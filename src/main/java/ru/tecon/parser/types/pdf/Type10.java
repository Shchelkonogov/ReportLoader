package ru.tecon.parser.types.pdf;

import ru.tecon.parser.ParseException;
import ru.tecon.parser.model.ParameterData;
import ru.tecon.parser.model.ReportData;
import ru.tecon.parser.types.ParserUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;

public class Type10 {

	private static Logger logger = Logger.getLogger(Type10.class.getName());

	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

	private static final List<String> testType =
			Arrays.asList("ОТЧЕТ", "о суточных параметрах теплоснабжения.*",
					".*\\d{2}/\\d{2}/\\d{2}.*\\d{2}/\\d{2}/\\d{2}.*", "Адрес.*",
					"Тепловычислитель.*", "Заводской номер.*");

	private static List<String> paramName = Arrays.asList("Дата", "t1", "t2", "dt", "V1", "M1", "V2", "M2", "Mг", "P1", "P2", "Qо",
			"Qг", "BНP", "BOC", "НС", "t3", "V3", "P3", "M3", "tх", "Дaтa", "Н.С.", "DI",
			"ta",
			"Дата", "t1", "t2", "dt", "V1", "M1", "V2", "M2", "Mг", "P1", "P2", "Qо",
			"BНP", "BOC", "НС", "V3", "Qг", "tх", "M3", "P3", "ta", "Н.С.", "DI", "t3",
			"Дaтa");

	private static List<String> dbParamName = Arrays.asList("Дата", "T1", "T2", "нет", "V1", "G1", "V2", "G2", "нет",
			"p1", "p2", "нет", "Q", "Time", "нет", "нет", "нет", "нет", "нет", "нет",
			"T3", "Дата", "нет", "нет", "нет", "Дата", "T1", "T2", "нет", "V1", "G1", "V2", "G2", "нет",
			"p1", "p2", "Q", "Time", "нет", "нет", "нет", "нет", "T3", "нет",
			"нет", "нет", "нет", "нет", "нет", "Дата");

	private static List<String> dbParamNameIntegr = Arrays.asList("Дата", "T1", "T2", "нет", "V1", "G1", "V2", "G2", "нет",
			"p1", "p2", "нет", "Q", "Time", "нет", "нет", "нет", "нет", "нет", "нет",
			"T3", "Дата", "нет", "нет", "нет", "Дата", "T1", "T2", "нет", "V1", "G1", "V2", "G2", "нет",
			"p1", "p2", "Q", "Time", "нет", "нет", "нет", "нет", "T3", "нет",
			"нет", "нет", "нет", "нет", "нет", "Дата");

	private Type10() {
	}

	private static List<String> createList(String content) {
		List<String> list = new ArrayList<>(Arrays.asList(content.split("\n")));
		list.removeIf(s -> s.trim().equals(""));
		return list;
	}

	private static List<String> createSubList(List<String> list) {
		List<String> subLines = new ArrayList<>();
		for (String obj: list) {
			if (obj.trim().matches("Дата.*")
					|| obj.trim().matches("Дaтa.*")) {
				break;
			}
			subLines.add(obj);
		}

		return subLines;
	}

	public static boolean checkType(String content) {
		logger.info("Type10 checkType");

		List<String> subLines = createSubList(createList(content));

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
		logger.info("checkType is unsuccessful");
		return false;
	}

	public static ReportData getData(String content, String filePath) throws ParseException {
		if (content.indexOf(testType.get(0)) != content.lastIndexOf(testType.get(0))) {
			throw new ParseException("Больше одной страницы");
		}

		List<String> list = createList(content);

		//Определяем адрес потребителя.
		String address = list.stream()
				.filter(obj -> obj.trim().matches(testType.get(3)))
				.findFirst().orElse("");
		if (address.equals("")) {
			throw new ParseException("can't read address");
		}
		if (address.matches(".*Тип расходомера.*") || address.matches(".*Телефон.*")) {
			address = address.substring(address.indexOf("Адрес") + "Адрес".length())
					.replaceAll("_", "").trim();
			if (address.contains("Тип расходомера")) {
				address = address.substring(0, address.indexOf("Тип расходомера")).trim();
			} else {
				if (address.contains("Телефон")) {
					address = address.substring(0, address.indexOf("Телефон")).trim();
				}
			}
			if (address.matches(":.*")) {
				address = address.substring(1);
			}

			if (address.length() == 0) {
				address = filePath.substring(filePath.lastIndexOf("\\") + 1);
			}
		} else {
			throw new ParseException("Ошибка в адресе потребителя");
		}

		//Определяем тип прибора.
		String counterType = list.stream()
				.filter(obj -> obj.trim().matches(testType.get(4)))
				.findFirst().orElse("");
		if (counterType.equals("")) {
			throw new ParseException("can't read counter type");
		}
		if (counterType.matches(".*сет.N.*")) {
			counterType = counterType
					.substring(counterType.indexOf("Тепловычислитель") + "Тепловычислитель".length(),
							counterType.indexOf("сет.N")).replaceAll("_", "").trim();
		} else {
			throw new ParseException("Ошибка в типе прибора");
		}

		//Определяем тип отчета.
		String reportName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))
				.toLowerCase();

		//Определяем серийний номер.
		String counterNumber = list.stream()
				.filter(obj -> obj.trim().matches(testType.get(5)))
				.findFirst().orElse("");
		if (counterNumber.equals("")) {
			throw new ParseException("can't read counter number");
		}
		counterNumber = counterNumber
				.substring(counterNumber.indexOf("Заводской номер") + "Заводской номер".length())
				.replaceAll("_", "").trim();
		counterNumber = counterNumber.substring(0, counterNumber.indexOf(" "));
		if (counterNumber.length() == 0) {
			throw new ParseException("Ошибка в серийном номере");
		}

		//Определяем начальную и конечную дату, а также период.
		String dateLine = list.stream()
				.filter(obj -> obj.trim().matches(testType.get(2)))
				.findFirst().orElse("").trim();
		if (dateLine.equals("")) {
			throw new ParseException("can't read date line");
		}

		if (dateLine.trim().startsWith("за")) {
			dateLine = dateLine.replaceFirst("за", "").trim();
		}

		LocalDate date1;
		LocalDate date2;
		String startDate = dateLine.substring(0, 8);
		String endDate = dateLine.substring(dateLine.indexOf("-") + 1).substring(0, 8);

		try {
			date1 = LocalDate.parse(endDate, formatter).plusDays(1);
			date2 = LocalDate.parse(startDate, formatter);
		} catch (DateTimeParseException e) {
			throw new ParseException("date parse exception");
		}

		if (date1.compareTo(date2) == 0) {
			throw new ParseException("Одинаковые даты");
		}

		//Определяем основные данные

		//Выделяем строку параметров
		List<String> dataList = new ArrayList<>();
		int indexIntegr = -1;
		boolean start = false;
		for (String elem : list) {
			if (elem.trim().matches("Итого.*")) {
				indexIntegr = list.indexOf(elem);
				break;
			}
			if (elem.trim().matches("Дата.*")
					|| elem.trim().matches("Дaтa.*")
					|| start) {
				start = true;
				dataList.add(elem);
			}
		}

		List<String> paramList = new ArrayList<>(Arrays.asList(dataList.get(0)
				.trim().split("[|\t]")));

		//Переводим строку в массив id и statAgr
		List<ParameterData> resultParam = new ArrayList<>();
		String value;
		for (String elem: paramList) {
			value = elem.trim();
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
		for (Iterator<String> i = dataList.iterator(); i.hasNext();) {
			String item = i.next().trim();
			if (item.equals("") || !item.matches(".*/.*/\\d{2}.*")) {
				i.remove();
			}
		}

		String[] items;
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yy");
		SimpleDateFormat format2 = new SimpleDateFormat("MM/dd/yy");
		List<String> temp;
		for (String elem: dataList) {
			items = elem.trim().split("[|\t]");


			if ((filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase().equals("xls")
					|| filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase().equals("xlsx"))
					&& (resultParam.size() - items.length == 1)
					&& (resultParam.get(resultParam.size() -1).getName().equals("НС"))) {
				temp = new ArrayList<>(Arrays.asList(items));
				temp.add("");
				items = new String[temp.size()];
				for (int i = 0; i < temp.size(); i++) {
					items[i] = temp.get(i);
				}
			}

			if (items.length == resultParam.size()) {
				for (int i = 0; i < resultParam.size(); i++) {
					value = items[i].trim();
					if (i == 0) {
						try {
							value = format.format(
									elem.contains("	") ? format2.parse(value) : format1.parse(value));
						} catch (Exception ignore) {
						}
					} else {
						value = value.replaceAll(",", ".");
					}
					value = value.replaceAll("\\*", "");
					value = value.replaceAll("----", "");
					value = value.replaceAll("Ош.д.", "");
					value = value.replaceAll("Ош.д", "");
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

		List<String> translateSI = Arrays.asList("P1", "P2", "P3");
		//Переводим измерения в нужный формат.
		int index;
		for (ParameterData elem : resultParam) {
			if (translateSI.stream().anyMatch(obj -> obj.equals(elem.getName()))) {
				index = resultParam.indexOf(elem);
				List<String> valuesList = new ArrayList<>();
				for (String item : elem.getData()) {
					if (item.equals("")) {
						valuesList.add("");
					} else {
						valuesList.add(new BigDecimal(item)
								.multiply(new BigDecimal("0.098"))
								.setScale(2, RoundingMode.HALF_EVEN).toString());
					}
				}
				resultParam.get(index).setData(valuesList);
			}
		}

		//Выделяем строку параметров интеграторов
		start = false;
		dataList.clear();
		for (int i = indexIntegr + 1; i < list.size(); i++) {
			if (list.get(i).trim().matches("Дата.*")
					|| list.get(i).trim().matches("Дaтa.*")
					|| start) {
				start = true;
				dataList.add(list.get(i));
			}
		}

		ArrayList<ParameterData> resultParamIntegr = new ArrayList<>();
		if (dataList.size() != 0) {
			paramList = new ArrayList<>(Arrays.asList(dataList.get(0).trim().split("[|\t]")));

			//Переводим строку в массив id и statAgr инттеграторов
			for (String elem : paramList) {
				value = elem.trim();
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
			duplicates.remove("нет");
			if (!duplicates.isEmpty()) {
				throw new ParseException("duplicate parameters " + duplicates);
			}

			//Получаем значения таблицы
			for (Iterator<String> i = dataList.iterator(); i.hasNext(); ) {
				String item = i.next().trim();
				if (item.equals("")
						|| (!item.matches(".*/.*/\\d{2}.*\\d{2}:\\d{2}.*")
						&& !item.matches(".*/.*/\\d{2}.*\\d{2}.*"))) {
					i.remove();
				}
			}

			if (dataList.size() == 0) {
				throw new ParseException("Ошибка в интеграторах");
			}

			SimpleDateFormat format3 = new SimpleDateFormat("dd/MM/yy HH:mm");
			SimpleDateFormat format4 = new SimpleDateFormat("dd/MM/yy");
			for (String elem : dataList) {
				items = elem.trim().split("[|\t]");
				if (items.length == resultParamIntegr.size()) {
					for (int i = 0; i < resultParamIntegr.size(); i++) {
						value = items[i].trim();
						if (i == 0) {
							try {
								value = format.format(format3.parse(value));
							} catch (Exception e) {
								try {
									value = format.format(format4.parse(value));
								} catch (Exception ignore) {
								}
							}
						} else {
							value = value.replaceAll(",", ".");
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
}
