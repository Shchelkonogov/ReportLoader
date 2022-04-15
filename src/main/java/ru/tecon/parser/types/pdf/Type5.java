package ru.tecon.parser.types.pdf;

import ru.tecon.parser.ParseException;
import ru.tecon.parser.model.ParameterData;
import ru.tecon.parser.model.ReportData;
import ru.tecon.parser.types.ParserUtils;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;

public class Type5 {

	private static Logger logger = Logger.getLogger(Type5.class.getName());

	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private static List<String> removeData = new ArrayList<>(Arrays.asList("-", "D", "ED", "U", "E", "UE",
			"EG", "UED", "UEG", "UD", "G", "Eg", "UEDG", "EDG", "g"));

	private static List<String> paramName = Arrays.asList("Дата", "Q", "t1", "t2", "tхв", "V1", "V2", "P1", "P2", "Нераб.", "Работы",
			"Классиф", "V1-V2", "t1-t2", "Дата Время", "Тр", "М1", "М2", "М1-M2", "М2-M1",
			"Vи", "V имп", "Px", "ta", "Р3", "Pх", "ошибок", "работы", "v1", "V1, м3",
			"V2, м3", "V1 м3", "V1, М³", "V2, М³", "%V1",
			"Дата", "Q", "М1", "М2", "М1-M2", "М2-M1", "t1", "t2", "t1-t2", "Нераб.",
			"Работы", "Классиф", "Дата Время", "Тр", "P1", "P2", "V1", "V2", "V1-V2", "tхв",
			"Vи", "Утечка", "Подмес", "ошибок", "ta", "Vi", "V имп", "Px", "работы", "M2",
			"M1-M2", "M1", "v1",
			"Дата", "Q", "М1", "М2", "М1-M2", "М2-M1", "Vи", "t1", "t2", "t1-t2",
			"Нераб.", "Работы", "Классиф", "Дата Время", "Тр", "V1", "V2", "V1-V2", "P1",
			"P2", "tхв", "Утечка", "Подмес");

	private static List<String> dbParamName = Arrays.asList("Дата", "Q", "T1", "T2", "T3", "V1", "V2", "p1", "p2",
			"нет", "Time", "нет", "нет", "нет", "Дата", "Time", "G1", "G2", "нет", "нет",
			"нет", "нет", "нет", "нет", "нет", "нет", "нет", "Time", "V1", "V1", "V2",
			"V1", "V1", "V2", "V1",
			"Дата", "Q", "G1", "G2", "нет", "нет", "T1", "T2", "нет", "нет",
			"Time", "нет", "Дата", "Time", "p1", "p2", "V1", "V2", "нет", "T3",
			"нет", "нет", "нет", "нет", "нет", "нет", "нет", "нет", "Time", "G2",
			"нет", "G1", "V1",
			"Дата", "Q", "G1", "G2", "нет", "нет", "нет", "T1", "T2", "нет",
			"нет", "Time", "нет", "Дата", "Time", "G1", "G2", "нет", "p1", "p2",
			"T3", "нет", "нет");

	private static List<String> dbParamNameIntegr = Arrays.asList("Дата", "Q", "T1", "T2", "T3", "V1", "V2", "p1", "p2",
			"нет", "Time", "нет", "нет", "нет", "Дата", "Time", "G1", "G2", "нет", "нет",
			"нет", "нет", "нет", "нет", "нет", "нет", "нет", "Time", "V1", "V1", "V2",
			"V1", "V1", "V2", "V1",
			"Дата", "Q", "G1", "G2", "нет", "нет", "T1", "T2", "нет", "нет",
			"Time", "нет", "Дата", "Time", "p1", "p2", "V1", "V2", "нет", "T3",
			"нет", "нет", "нет", "нет", "нет", "нет", "нет", "нет", "Time", "G2",
			"нет", "G1", "V1",
			"Дата", "Q", "G1", "G2", "нет", "нет", "нет", "T1", "T2", "нет",
			"нет", "Time", "нет", "Дата", "Time", "G1", "G2", "нет", "p1", "p2",
			"T3", "нет", "нет");

	private Type5() {
	}

	private static String updateContent(String content) {
		content = content.replaceAll(String.valueOf((char) 160), " ");
		content = content.replaceAll(String.valueOf((char) 8208), "-");
		content = content.replaceAll("\r\n", "\n");

		//Выпрямляем первую строку если надо
		String textPart = content.substring(0, content.indexOf("\n"));
		if (textPart.trim().matches("Организация .* Номер договора .*Адрес.* Телефон .*")) {
			content = content.substring(0, content.indexOf("Адрес"))
					+ "\n" + content.substring(content.indexOf("Адрес"));
		}
		return content;
	}

	private static List<String> createList(String content) {
		List<String> list = new ArrayList<>(Arrays.asList(content.split("\n")));
		list.removeIf(s -> s.trim().equals(""));

		if (list.get(1).trim().matches(".*Организация.*")) {
			list.remove(0);
		}

		return list;
	}

	public static boolean checkType(String content) {
		logger.info("Type5 checkType");

		content = updateContent(content);

		List<String> list = createList(content);

		List<String> subList = list.subList(0, 4);

		if ((subList.get(0).trim().matches("Организация .* Номер.*")
				|| subList.get(0).trim().matches("Организация .* Абонент.*")
				|| subList.get(0).trim().matches(".Организация .* Номер.*"))
				&& subList.get(1).trim().matches("Адрес.* Телефон .*")
				&& subList.get(2).trim().matches("Тип теплосчетчика .* Версия .* Номер теплосчетчика .*")
				&& subList.get(3).trim().matches("Дата последующей поверки теплосчетчика .*")
				&& (content.contains("Посуточная ведомость учета параметров теплопотребления")
				|| content.contains("ОТЧЕТНАЯ ВЕДОМОСТЬ ТЕПЛОВОДОПОТРЕБЛЕНИЯ В СИСТЕМЕ ГВС")
				|| content.contains("ОТЧЁТНАЯ ВЕДОМОСТЬ ТЕПЛОПОТРЕБЛЕНИЯ В СИСТЕМЕ ОТОПЛЕНИЯ"))
				&& content.contains("|-------")) {
			logger.info("checkType is successful");
			return true;
		} else {
			logger.info("checkType is unsuccessful");
			return false;
		}
	}

	public static ReportData getData(String content, String fileName) throws ParseException {
		content = updateContent(content);
		List<String> list = createList(content);

		if (content.indexOf("Посуточная ведомость учета параметров теплопотребления") != content.lastIndexOf("Посуточная ведомость учета параметров теплопотребления")
				|| content.indexOf("ОТЧЕТНАЯ ВЕДОМОСТЬ ТЕПЛОВОДОПОТРЕБЛЕНИЯ В СИСТЕМЕ ГВС") != content.lastIndexOf("ОТЧЕТНАЯ ВЕДОМОСТЬ ТЕПЛОВОДОПОТРЕБЛЕНИЯ В СИСТЕМЕ ГВС")
				|| content.indexOf("ОТЧЁТНАЯ ВЕДОМОСТЬ ТЕПЛОПОТРЕБЛЕНИЯ В СИСТЕМЕ ОТОПЛЕНИЯ") != content.lastIndexOf("ОТЧЁТНАЯ ВЕДОМОСТЬ ТЕПЛОПОТРЕБЛЕНИЯ В СИСТЕМЕ ОТОПЛЕНИЯ")) {
			throw new ParseException("more one page");
		}

		String textPart = "";

		//Определяем адрес потребителя.
		String address = list.get(1).trim();
		if (address.matches("Адрес.* Телефон .*")) {
			address = address.substring(address.indexOf(" "), address.indexOf("Телефон"))
					.replaceAll("_", "").trim();
			if (address.length() == 0) {
				address = fileName.substring(fileName.lastIndexOf("\\") + 1);
			}
		} else {
			throw new ParseException("can't read address");
		}

		//Определяем тип прибора.
		String counterType = list.get(2).trim();
		if (counterType.matches("Тип теплосчетчика .* Версия .* Номер теплосчетчика .*")) {
			counterType = counterType.substring(counterType.indexOf("Тип теплосчетчика") + 17,
					counterType.indexOf("Версия")).replaceAll("_", "").trim();
		} else {
			throw new ParseException("can't read counter type");
		}

		//Определяем серийний номер.
		String counterNumber = list.get(2).trim();
		if (counterNumber.matches("Тип теплосчетчика .* Версия .* Номер теплосчетчика .*")) {
			counterNumber = counterNumber.substring(counterNumber.indexOf("Номер теплосчетчика") + 19)
					.replaceAll("_", "").trim();
		} else {
			throw new ParseException("can't read counter number");
		}

		//Определяем начальную и конечную дату, а также период.
		String reportName = "";
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).trim().matches("Посуточная ведомость учета параметров теплопотребления")
					|| list.get(i).trim().matches("ОТЧЕТНАЯ ВЕДОМОСТЬ ТЕПЛОВОДОПОТРЕБЛЕНИЯ В СИСТЕМЕ ГВС")
					|| list.get(i).trim().matches("ОТЧЁТНАЯ ВЕДОМОСТЬ ТЕПЛОПОТРЕБЛЕНИЯ В СИСТЕМЕ ОТОПЛЕНИЯ")) {
				textPart = list.get(i + 1);
				reportName = list.get(i).trim();
				break;
			}
		}

		textPart = textPart.replaceAll("_", " ").trim();
		LocalDate date1;
		LocalDate date2;
		if (textPart.matches("с.*по.*")) {
			String start = textPart.replace("с", "").trim().substring(0, 10);
			String end = textPart.substring(textPart.indexOf("по") + 2).trim();

			try {
				date1 = LocalDate.parse(end, formatter).plusDays(1);
				date2 = LocalDate.parse(start, formatter);
			} catch (DateTimeParseException e) {
				throw new ParseException("date parse exception");
			}
			if (date1.compareTo(date2) == 0) {
				throw new ParseException("same dates");
			}
		} else {
			throw new ParseException("can't read date");
		}

		//Определяем основные данные

		//Выделяем строку параметров
		int index = content.indexOf("Посуточная ведомость учета параметров теплопотребления");
		if (index == -1) {
			index = content.indexOf("ОТЧЕТНАЯ ВЕДОМОСТЬ ТЕПЛОВОДОПОТРЕБЛЕНИЯ В СИСТЕМЕ ГВС");
			if (index == -1) {
				index = content.indexOf("ОТЧЁТНАЯ ВЕДОМОСТЬ ТЕПЛОПОТРЕБЛЕНИЯ В СИСТЕМЕ ОТОПЛЕНИЯ");
			}
		}

		textPart = content.substring(index, content.indexOf("Итого"));
		if (!textPart.contains("| Дата")) {
			textPart = textPart.substring(textPart.indexOf("|Дата"));
		} else {
			textPart = textPart.substring(textPart.indexOf("| Дата"));
		}

		ArrayList<String> lines = new ArrayList<>(Arrays.asList(textPart.split("\n")));

		lines.removeIf(item -> item.equals(""));

		ArrayList<String> param = new ArrayList<>(Arrays.asList(lines.get(0).trim().substring(1).split("\\|")));
		for (int i = 0; i < param.size(); i++) {
			param.set(i, param.get(i).trim());
		}

		//Если параметр не виден "" берем его из следущей строки
		ArrayList<String> paramNext = new ArrayList<>(Arrays.asList(lines.get(1).trim().substring(1).split("\\|")));
		for (int i = 0; i < paramNext.size(); i++) {
			paramNext.set(i, paramNext.get(i).trim());
		}

		//Переводим строку в массив id и statAgr
		ArrayList<ParameterData> resultParam = new ArrayList<>();
		String valueNew;
		for (String value: param) {
			if (paramName.contains(value)) {
				resultParam.add(new ParameterData(dbParamName.get(paramName.indexOf(value))));
			} else {
				if (value.equals("")) {
					valueNew = paramNext.get(param.indexOf(value));
					if (paramName.contains(valueNew)) {
						resultParam.add(new ParameterData(dbParamName.get(paramName.indexOf(valueNew))));
					} else {
						throw new ParseException("Не знаю замененного параметра: " + valueNew);
					}
				} else {
					throw new ParseException("Не знаю параметра: " + value);
				}
			}
		}

		//Проверка на повторяющиеся параметры
		Set<String> foundStrings = new HashSet<>();
		Set<String> duplicates = new HashSet<>();
		for (ParameterData item: resultParam) {
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
		ArrayList<String> subList = new ArrayList<>(Arrays.asList(textPart.split("\n")));
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yy");
		SimpleDateFormat formatNew = new SimpleDateFormat("dd.MM");
		for (Iterator<String> i = subList.iterator(); i.hasNext();) {
			String item = i.next();
			if (item.equals("")) {
				i.remove();
			} else {
				try {
					format1.parse(item.split("\\|")[1]);
				} catch (Exception e) {
					try {
						formatNew.parse(item.split("\\|")[1]);
					} catch (Exception e1) {
						i.remove();
					}
				}
			}
		}

		String year = String.valueOf(date2.getYear());
		for (String line: subList) {
			String value;
			String[] items = line.trim().substring(1).split("\\|");
			if (items.length == resultParam.size()) {
				for (int i = 0; i < resultParam.size(); i++) {
					value = items[i].trim();
					if (removeData.contains(value)) {
						value = "";
					}
					if (value.equals("U00.0") && i == (resultParam.size() - 1)) {
						value = "";
					}
					if (i == 0) {
						if (value.matches("\\d\\d.\\d\\d")) {
							value = value + "." + year;
						}
						try {
							value = format.format(format1.parse(value));
						} catch (Exception ignored) {
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
						throw new ParseException("Не числовое значение: " + item);
					}
				}
			}
		}

		// Выделяем строку параметров интеграторов
		textPart = content.substring(content.indexOf("Показания интеграторов"));
		textPart = textPart.substring(textPart.indexOf("Дата"), textPart.indexOf("Итого"));

		lines = new ArrayList<>(Arrays.asList(textPart.split("\n")));
		param = new ArrayList<>(Arrays.asList(lines.get(0).trim().split("\\|")));
		for (int i = 0; i < param.size(); i++) {
			param.set(i, param.get(i).trim());
		}

		//Переводим строку в массив id и statAgr инттеграторов
		ArrayList<ParameterData> resultParamIntegr = new ArrayList<>();
		for (String value: param) {
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
		subList = new ArrayList<>(Arrays.asList(textPart.split("\n")));
		SimpleDateFormat format2 = new SimpleDateFormat("dd.MM.yy HH:mm");
		for (Iterator<String> i = subList.iterator(); i.hasNext();) {
			String item = i.next();
			if (item.equals("")) {
				i.remove();
			} else {
				try {
					format2.parse(item.split("\\|")[1]);
				} catch (Exception e) {
					i.remove();
				}
			}
		}

		for (String line: subList) {
			String value;
			String[] items = line.trim().substring(1).split("\\|");
			if (items.length == resultParamIntegr.size()) {
				for (int i = 0; i < resultParamIntegr.size(); i++) {
					value = items[i].trim();
					value = value.replaceAll(",", ".");
					if (removeData.contains(value)) {
						value = "";
					} else {
						try {
							Date date = format2.parse(value);
							value = format.format(date);
						} catch(java.text.ParseException ignore) {
						}
					}
					resultParamIntegr.get(i).getData().add(value);
				}
			} else {
				throw new ParseException("can't read data");
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
		reportData.setFileName(Paths.get(fileName).getFileName().toString());
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
