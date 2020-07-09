package ru.tecon.parser.types.html;

import ru.tecon.parser.ParseException;
import ru.tecon.parser.model.ParameterData;
import ru.tecon.parser.model.ReportData;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HTMLType {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static List<String> paramName = Arrays.asList("Time", "Q", "V1", "V2", "dVminus", "dVplus", "G1", "G2",
            "dGminus", "dGplus", "T1", "T2", "Thv", "P1", "P2", "Tnar", "Q2", "G3", "V3", "P3", "TIME", "V3|G3");

    private static List<String> dbParamName = Arrays.asList("Дата", "Q", "V1", "V2", "нет", "нет", "G1", "G2",
            "нет", "нет", "T1", "T2", "T3", "p1", "p2", "Time", "нет", "нет", "нет", "p3", "Дата", "нет");

    private static List<String> dbParamNameIntegr = Arrays.asList("Дата", "Qi", "V1i", "V2i", "нет", "нет", "G1i", "G2i",
            "нет", "нет", "T1", "T2", "T3", "p1", "p2", "Timei", "нет", "нет", "нет", "p3", "Дата", "нет");

    private HTMLType() {
    }

    public static void main(String[] args) throws ParseException {
        getData("C:\\Programs\\1-я прядильная 6 цо.html");
}

    public static ReportData getData(String filePath) throws ParseException {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(filePath), Charset.forName("Cp1251"));
        } catch (UnmappableCharacterException ignore) {
            try {
                lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ParseException("can't read file");
            }
        } catch (IOException e) {
            throw new ParseException("can't read file");
        }

        lines.removeIf(s -> s.startsWith("function"));

        String address = "";
        String counterType = "";
        String counterNumber = "";
        LocalDate date2 = null;
        LocalDate date1 = null;
        String reportName = "";
        List<ParameterData> resultParam = new ArrayList<>();
        List<ParameterData> resultParamIntegr = new ArrayList<>();


        try (StaxStreamProcessor processor = new StaxStreamProcessor(
                new ByteArrayInputStream(String.join("", lines).getBytes(StandardCharsets.UTF_8)))) {

            List<String> items = new ArrayList<>(Arrays.asList("SERIAL", "DATAFROM", "DATATO", "SYSTEM", "ADDRESS", "MODEL"));
            for (int i = 0; i < items.size(); i++) {
                if (processor.getElement(items)) {
                    switch (processor.getLocalName()) {
                        case "SERIAL":
                            counterNumber = processor.getText();
                            break;
                        case "DATAFROM":
                            try {
                                date2 = LocalDate.parse(processor.getText(), formatter);
                            } catch (DateTimeParseException e) {
                                throw new ParseException("can't parse date");
                            }
                            break;
                        case "DATATO":
                            try {
                                date1 = LocalDate.parse(processor.getText(), formatter).plusDays(1);
                            } catch (DateTimeParseException e) {
                                throw new ParseException("can't parse date");
                            }
                            break;
                        case "SYSTEM":
                            reportName = processor.getText();
                            break;
                        case "ADDRESS":
                            address = processor.getText();
                            break;
                        case "MODEL":
                            counterType = processor.getText();
                            break;
                    }
                }
            }

            while (processor.startElement("ID", "RECORDS_CAP")) {
                String parameter = processor.getText();
                resultParam.add(new ParameterData(parameter));

                if (processor.checkElement("TOTAL", "PARAMETER")) {
                    resultParamIntegr.add(new ParameterData(parameter));
                }
            }

            List<String> parameterNames = resultParam.stream().map(ParameterData::getName).collect(Collectors.toList());

            while (processor.startElement("RECORD", "RECORDS")) {
                resultParam.forEach(parameterData -> parameterData.getData().add(""));
                for (int i = 0; i < parameterNames.size(); i++) {
                    String result = processor.checkElement(parameterNames, "RECORD");
                    if (result != null) {
                        for (ParameterData item: resultParam) {
                            if (item.getName().equals(result)) {
                                item.getData().set(item.getData().size() - 1, updateValue(processor.getValue()));
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            parameterNames = resultParamIntegr.stream().map(ParameterData::getName).collect(Collectors.toList());

            while (processor.startElement("TOTAL", "TOTALS")) {
                resultParamIntegr.forEach(parameterData -> parameterData.getData().add(""));
                for (int i = 0; i < parameterNames.size(); i++) {
                    String result = processor.checkElement(parameterNames, "TOTAL");
                    if (result != null) {
                        for (ParameterData item : resultParamIntegr) {
                            if (item.getName().equals(result)) {
                                item.getData().set(item.getData().size() - 1, updateValue(processor.getText()));
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
            throw new ParseException("parse error");
        }

        updateParamNames(resultParam, dbParamName);
        updateParamNames(resultParamIntegr, dbParamNameIntegr);

        removeNullRows(resultParam);
        removeNullRows(resultParamIntegr);

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

        try {
            String oldValue = reportData.getParamIntegr().get(0).getData().get(1);
            String newValue = formatter.format(LocalDate.parse(oldValue, formatter).plusDays(1));
            reportData.getParamIntegr().get(0).getData().set(1, newValue);
        } catch (Exception ignore) {
        }

        return reportData;
    }

    private static String updateValue(String value) {
        if (Objects.isNull(value) || value.trim().equals("-")) {
            return "";
        }
        return value.replace(",", ".").trim();
    }

    private static void removeNullRows(List<ParameterData> resultParam) {
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
    }

    private static void updateParamNames(List<ParameterData> resultParam, List<String> newNames) throws ParseException {
        List<String> unknownParamNames = new ArrayList<>();
        for (ParameterData data: resultParam) {
            int index = paramName.indexOf(data.getName());
            if (index != -1) {
                data.setName(newNames.get(index));
            } else {
                unknownParamNames.add(data.getName());
            }
        }
        if (!unknownParamNames.isEmpty()) {
            throw new ParseException("don't no parameter " + unknownParamNames);
        }
    }
}
