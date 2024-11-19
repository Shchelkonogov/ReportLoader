package ru.tecon.parser.types.pdf;

import ru.tecon.parser.ParseException;
import ru.tecon.parser.model.ParameterData;
import ru.tecon.parser.model.ReportData;
import ru.tecon.parser.types.ParserUtils;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

public class Type7 {

    private static Logger logger = Logger.getLogger(Type7.class.getName());

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static List<String> remove = Arrays.asList("Прям.", "Обр.", "Количество", "тепловой", "энергии",
            "Расход", "теплоносителя", "Температура", "Гкал", "м.куб.", "час", "Время", "наработки",
            "МПа", "Давление", "наработк", "и", "т", "Разн.", "°C", "м.куб", "град.С", "ч", "Количеств",
            "о", "наработ", "ки", "теплоноси", "теля", "ед.", "теплоносите", "ля");
    private static List<String> removeFirst = Arrays.asList("Qг", "Qго", "Qх", "Qц", "Qцо");
    private static List<String> removeData = Arrays.asList("T#", "T");

    private static List<String> paramName = Arrays.asList("День", "Дата", "Eг", "Mг", "Vг", "Mго", "Vго",
            "Tг", "Tго", "Tх", "pг", "pго", "Chг", "Eг-х", "Chг-х", "dVг-го", "pх", "Eцо-ц", "Mц", "Mцо",
            "Tц", "Tцо", "pц", "pцо", "Chцо-ц", "Eв-во", "Mв", "Tв", "Tво", "pв", "pво", "Chв-во", "Mх",
            "Vх", "Chх", "Eц-цо", "Chц-цо", "Eво-в", "Mво", "Chво-в", "Eг-го", "Chг-го", "Eго-х", "Vц",
            "Tн", "Qг", "Qго", "Qх", "Vцо", "Qц", "Qцо", "dVг2-го2", "dVг3-го3", "Eг2", "Mг2", "Mго2",
            "Vг2", "Vго2", "Tг2", "Tго2", "Tх2", "pг2", "pго2", "Chг2", "Eг2-х", "Eго2-х", "Eг3", "Mг3",
            "Mго3", "Vг3", "Vго3", "Tг3", "Tго3", "Tх3", "pг3", "pго3", "Chг3", "pх2", "Eц2-цо2", "Mц2",
            "Vц2", "Tц2", "Tцо2", "pц2", "pцо2", "Chц2-цо2");

    private static List<String> dbParamName = Arrays.asList("Дата", "Дата", "Q", "G1", "V1", "G2", "V2",
            "T1", "T2", "T3", "p1", "p2", "Time", "нет", "нет", "нет", "нет", "Q", "G1",
            "G2", "T1", "T2", "p1", "p2", "Time", "Q", "G1", "T1", "T2", "p1", "p2",
            "Time", "нет", "нет", "нет", "Q", "Time", "нет", "G2", "нет", "нет", "Time", "нет",
            "V1", "нет", "нет", "нет", "нет", "V2", "нет", "нет", "нет", "нет", "Q", "G1", "G2",
            "V1", "V2", "T1", "T2", "T3", "p1", "p2", "Time", "нет", "нет", "Q", "G1",
            "G2", "V1", "V2", "T1", "нет", "T3", "p1", "p2", "Time", "p3", "Q", "G1",
            "V1", "T1", "T2", "p1", "p2", "Time");

    private static List<String> dbParamNameIntegr = Arrays.asList("Дата", "Дата", "Q", "G1", "V1", "G2", "V2",
            "нет", "нет", "нет", "нет", "нет", "Time", "нет", "нет", "нет", "нет", "Q", "G1",
            "G2", "нет", "нет", "нет", "нет", "Time", "Q", "G1", "нет", "нет", "нет", "нет",
            "Time", "нет", "нет", "нет", "Q", "Time", "нет", "G2", "нет", "нет", "Time", "нет",
            "V1", "нет", "нет", "нет", "нет", "V2", "нет", "нет", "нет", "нет", "Q", "G1", "G2",
            "V1", "V2", "нет", "нет", "нет", "нет", "нет", "Time", "нет", "нет", "Q", "G1",
            "G2", "V1", "V2", "нет", "нет", "нет", "нет", "нет", "Time", "нет", "Q", "G1",
            "V1", "нет", "нет", "нет", "нет", "Time");

    private Type7() {
    }

    public static boolean checkType(String content) {
        logger.info("Type7 checkType");
        if (content.contains("Печать ЭЛТЕКО")) {
            logger.info("checkType is successful");
            return true;
        } else {
            logger.info("checkType is unsuccessful");
            return false;
        }
    }

    public static ReportData getData(String content, String fileName) throws ParseException {
        if (content.indexOf("Печать ЭЛТЕКО") != content.lastIndexOf("Печать ЭЛТЕКО")) {
            throw new ParseException("more one page");
        }

        String pageHeader = content.substring(0, content.indexOf("ОТЧЕТНАЯ ВЕДОМОСТЬ"));
        pageHeader = pageHeader.replaceAll("\n", " ");
        pageHeader = pageHeader.replaceAll(" {2}", " ");

        String reportName = content.substring(content.indexOf("ОТЧЕТНАЯ ВЕДОМОСТЬ"));
        reportName = reportName.substring(0, reportName.indexOf("\n"));

        String address = pageHeader.substring(pageHeader.indexOf("Адрес") + 5, pageHeader.indexOf("Тип теплосчетчика")).trim();
        if (address.length() == 0) {
            address = fileName.substring(fileName.lastIndexOf("\\") + 1);
        }

        String counterType = pageHeader.substring(pageHeader.indexOf("Тип теплосчетчика") + 17, pageHeader.indexOf("Номер абонента")).trim();

        String counterNumber = pageHeader.substring(pageHeader.indexOf("Номер теплосчетчика") + 19).trim();
        if (counterNumber.contains(" ")) {
            counterNumber = counterNumber.substring(0, counterNumber.indexOf(" ")).trim();
        }

        String pagePath = content.substring(content.indexOf("за период") + 11, content.indexOf("за период") + 36).trim();
        String startDate = pagePath.substring(0, pagePath.indexOf(" "));
        String endDate = pagePath.substring(pagePath.indexOf(" ") + 4);
        LocalDate date1;
        LocalDate date2;
        int year;
        try {
            date1 = LocalDate.parse(endDate, formatter);
            date2 = LocalDate.parse(startDate, formatter);

            // проверка на год. Если год совпадает в датах, то берем его,
            //  если переход через год с 12 по 1 месяц, то берем предыдущий год
            if ((date1.getYear() == date2.getYear()) ||
                    ((date2.getMonthValue() == 12) && (date1.getMonthValue() == 1) && ((date1.getYear() - date2.getYear()) == 1))) {
                year = date2.getYear();
            } else {
                throw new ParseException("parse year exception");
            }
        } catch (DateTimeParseException e) {
            throw new ParseException("date parse exception");
        }
        long days = ChronoUnit.DAYS.between(date2, date1);

        pagePath = content.substring(content.indexOf("за период") + 36,
                !content.contains("Время работы") ? content.indexOf("Интеграторы") : content.indexOf("Время работы")).trim();
        pagePath = pagePath.replaceAll("\n", " ");
        pagePath = pagePath.replaceAll(" {2}", " ");

        boolean error = true;
        String path1;
        String path2 = pagePath;
        ArrayList<String> param = new ArrayList<>();
        while (error) {
            path1 = path2.substring(0, path2.indexOf(" "));
            try {
                param.add(path1);
                new BigDecimal(path1);
                error = false;
            } catch (NumberFormatException ignored) {
            }
            if (error) {
                path2 = path2.substring(path2.indexOf(path1) + path1.length()).trim();
            }
        }
        param.removeAll(remove);
        for (String value: removeFirst) {
            param.remove(value);
        }
        for (int j = 0; j < param.size(); j++) {
            if (param.get(j).contains(",")) {
                param.set(j, param.get(j).replaceAll(",", ""));
            }
        }
        param.remove(param.size() - 1);
        int index;
        ArrayList<ParameterData> resultParam = new ArrayList<>();
        for (String value: param) {
            index = paramName.indexOf(value);
            if (index == -1) {
                throw new ParseException("unknown parameter: " + value);
            } else {
                resultParam.add(new ParameterData(dbParamName.get(index)));
            }
        }

        ArrayList<String> data = new ArrayList<>();
        while (path2.contains(" ")) {
            data.add(path2.substring(0, path2.indexOf(" ")));
            path2 = path2.substring(path2.indexOf(" ") + 1);
        }
        data.add(path2);
        for (Iterator<String> i = data.iterator(); i.hasNext();) {
            String item = i.next();
            for (String value: removeData) {
                if (item.equals(value)) {
                    i.remove();
                    break;
                }
            }
        }
        ArrayList<ArrayList<String>> paramData = new ArrayList<>();
        int size = data.size() / resultParam.size();
        for (int j = 0; j < resultParam.size(); j++) {
            ArrayList<String> line = new ArrayList<>(data.subList(j * size, j * size + size));
            for (int i = 0; i < line.size(); i++) {
                if (line.get(i).equals("-")) {
                    line.set(i, "");
                }
            }
            paramData.add(line);
        }
        if (paramData.get(0).get(0).equals("1")) {
            for (List<String> value: paramData) {
                value.remove(0);
            }
        }
        if (paramData.get(0).get(paramData.get(0).size() - 1).equals("Итого")) {
            for (List<String> value: paramData) {
                value.remove(value.size() - 1);
            }
        }

        int counter = 0;
        for (ArrayList<String> value: paramData) {
            counter = counter + value.size();
        }
        if (param.size() * days != counter) {
            throw new ParseException("error data size: " + (param.size() * days) + " " + counter);
        }

        ArrayList<String> dataLine = new ArrayList<>();
        for (String obj : paramData.get(0)) {
            if (obj.matches("\\d\\d\\.\\d\\d")) {
                dataLine.add(obj + "." + year);
            } else {
                throw new ParseException("parse error");
            }
        }

        paramData.set(0, dataLine);

        for (int i = 0; i < resultParam.size(); i++) {
            resultParam.get(i).setData(paramData.get(i));
        }

        //Убираем строки где только дата, а остальные значения пустые
        ParserUtils.removeNullRows(resultParam);

        pagePath = content.substring(!content.contains("Разность") ? content.indexOf("Результат за период") + 19 : content.indexOf("Разность") + 8,
                content.indexOf("Подписи")).trim();
        pagePath = pagePath.replaceAll("\n", " ");
        pagePath = pagePath.replaceAll(" {2}", " ");

        boolean errorIntegr = true;
        String path1Integr;
        String path2Integr = pagePath;
        ArrayList<String> paramIntegr = new ArrayList<>();
        while (errorIntegr) {
            path1Integr = path2Integr.substring(0, path2Integr.indexOf(" "));
            try {
                paramIntegr.add(path1Integr);
                new BigDecimal(path1Integr);
                errorIntegr = false;
            } catch (NumberFormatException ignored) {
            }
            if (errorIntegr) {
                path2Integr = path2Integr.substring(path2Integr.indexOf(path1Integr) + path1Integr.length()).trim();
            }
        }
        for (int j = 0; j < paramIntegr.size(); j++) {
            if (paramIntegr.get(j).contains(",")) {
                paramIntegr.set(j, paramIntegr.get(j).replaceAll(",", ""));
            }
        }
        paramIntegr.removeAll(remove);
        paramIntegr.remove(paramIntegr.size() - 1);
        int indexIntegr;
        ArrayList<ParameterData> resultParamIntegr = new ArrayList<>();
        for (String value: paramIntegr) {
            indexIntegr = paramName.indexOf(value);
            if (indexIntegr == -1) {
                throw new ParseException("unknown parameter: " + value);
            } else {
                resultParamIntegr.add(new ParameterData(dbParamNameIntegr.get(indexIntegr)));
            }
        }

        ArrayList<String> dataIntegr = new ArrayList<>();
        while (path2Integr.contains(" ")) {
            dataIntegr.add(path2Integr.substring(0, path2Integr.indexOf(" ")));
            path2Integr = path2Integr.substring(path2Integr.indexOf(" ") + 1);
        }
        dataIntegr.add(path2Integr);
        ArrayList<ArrayList<String>> paramDataIntegr = new ArrayList<>();
        int sizeIntegr = resultParamIntegr.size();
        for (int j = 0; j < resultParamIntegr.size(); j++) {
            ArrayList<String> line = new ArrayList<>();
            line.add(dataIntegr.get(j));
            line.add(dataIntegr.get(j + sizeIntegr));
            line.add(dataIntegr.get(j + 2 * sizeIntegr));
            paramDataIntegr.add(line);
        }
        for (ArrayList<String> value: paramDataIntegr) {
            value.remove(value.size() - 1);
        }

        int counterIntegr = 0;
        for (ArrayList<String> value: paramDataIntegr) {
            counterIntegr = counterIntegr + value.size();
        }
        if (paramIntegr.size() * 2 != counterIntegr) {
            throw new ParseException("error integr data size: " + (paramIntegr.size() * 2) + " " + counterIntegr);
        }

        for (int i = 0; i < resultParamIntegr.size(); i++) {
            resultParamIntegr.get(i).setData(paramDataIntegr.get(i));
        }

        resultParamIntegr.add(0, new ParameterData("Дата"));
        resultParamIntegr.get(0).setData(new ArrayList<>(Arrays.asList(formatter.format(date2), formatter.format(date1))));

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
        ParserUtils.updateValue("Time", reportData.getParamIntegr(), 3600);
        ParserUtils.updateValue("p1", reportData.getParam(), 0.101325f);
        ParserUtils.updateValue("p2", reportData.getParam(), 0.101325f);
        ParserUtils.updateValue("p3", reportData.getParam(), 0.101325f);

        return reportData;
    }
}
