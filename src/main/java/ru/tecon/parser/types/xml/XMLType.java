package ru.tecon.parser.types.xml;

import ru.tecon.parser.ParseException;
import ru.tecon.parser.model.ParameterData;
import ru.tecon.parser.model.ReportData;
import ru.tecon.parser.types.html.StaxStreamProcessor;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XMLType {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm");

    private static List<String> paramName = Arrays.asList("Q", "M1", "M2", "Mutechka", "t1", "P2", "t2", "Tnar",
            "Terr", "td", "date", "QAgg", "M1Agg", "M2Agg", "V1Agg", "V2Agg", "TnarAgg", "Дата", "Mpodmes", "V1", "V2",
            "P1", "Vrazbor", "Vutechka", "tCW", "PpodpitAvg", "Mpodpit", "MpodpitAgg", "Vpodpit", "VpodpitAgg",
            "Vpodmes");

    private static List<String> dbParamName = Arrays.asList("Q", "G1", "G2", "нет", "T1", "p2", "T2", "Time", "нет",
            "нет", "Дата", "нет", "нет", "нет", "нет", "нет", "нет", "нет", "нет", "V1", "V2", "p1", "нет", "нет", "T3",
            "нет", "нет", "нет", "нет", "нет", "нет");

    private static List<String> dbParamNameIntegr = Arrays.asList("нет", "нет", "нет", "нет", "нет", "нет", "нет", "нет", "нет",
            "нет", "нет", "Qi", "G1i", "G2i", "V1i", "V2i", "Timei", "Дата", "нет", "V1i", "V2i", "нет", "нет", "нет",
            "нет", "нет", "нет", "нет", "нет", "нет", "нет");

    private XMLType() {
    }

    public static void main(String[] args) throws ParseException {
        getData("C:\\Programs\\800000000000259337_D0011311OT_202004_+_Бабаевская ул., д.1дробь8, стр.3.xml");
    }

    public static ReportData getData(String filePath) throws ParseException {
        String address = "";
        String counterType = "";
        String counterNumber = "";
        LocalDate date2 = null;
        LocalDate date1 = null;
        String reportName = "";
        List<ParameterData> resultParam = new ArrayList<>();
        List<ParameterData> resultParamIntegr = new ArrayList<>();

        try (StaxStreamProcessor processor = new StaxStreamProcessor(
                Files.newInputStream(Paths.get(filePath)))) {
            try {
                if (processor.getElement("DTstart")) {
                    date2 = LocalDate.parse(processor.getText(), formatter);
                }

                if (processor.getElement("DTend")) {
                    date1 = LocalDate.parse(processor.getText(), formatter);
                }
            } catch (DateTimeParseException e) {
                throw new ParseException("can't parse date");
            }

            if (processor.getElement("circuit_code")) {
                reportName = processor.getText();
            }

            if (processor.getElement("address_full")) {
                address = processor.getText();
            }

            if (processor.getElement("model")) {
                counterType = processor.getText();
            }

            if (processor.getElement("serial_number")) {
                counterNumber = processor.getText();
            }

            boolean head = true;
            List<String> data;
            while (processor.startElement("daily_data", "daily_data_list")) {
                data = Arrays.asList(processor.checkElement("daily_data").split("[|]"));

                if (head) {
                    for (String item: data) {
                        resultParam.add(new ParameterData(item.split("=")[0]));
                    }

                    resultParam.removeIf(parameterData -> parameterData.getName().equals("Error"));
                }

                for (int i = 0; i < resultParam.size(); i++) {
                    String value = "";
                    String[] split = data.get(i).split("=");
                    if (split.length == 2) {
                        value = split[1];
                    }
                    if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        value = formatter1.format(LocalDate.parse(value, formatter));
                    }
                    resultParam.get(i).getData().add(value);
                }
                head = false;
            }


            String startDateIntegrate = "";
            if (processor.getElement("period_start_date")) {
                startDateIntegrate = processor.getText();
                if (startDateIntegrate.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
                    startDateIntegrate = formatter1.format(LocalDate.parse(startDateIntegrate.replaceAll("T", ""), formatter2));
                }
            }

            if (processor.getElement("period_start_meter_data")) {
                data = Arrays.asList(processor.checkElement("period_start_meter_data").split("[|]"));

                for (int i = 0; i < data.size(); i++) {
                    String value = "";
                    String[] split = data.get(i).split("=");
                    resultParamIntegr.add(new ParameterData(split[0]));
                    if (split.length == 2) {
                        value = split[1];
                    }
                    resultParamIntegr.get(i).getData().add(value);
                }
            }

            String endDateIntegrate = "";
            if (processor.getElement("period_end_date_24")) {
                endDateIntegrate = processor.getText();
                if (endDateIntegrate.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
                    endDateIntegrate = formatter1.format(LocalDate.parse(endDateIntegrate.replaceAll("T", ""), formatter2));
                }
            }

            if (processor.getElement("period_end_meter_data")) {
                data = Arrays.asList(processor.checkElement("period_end_meter_data").split("[|]"));

                for (int i = 0; i < data.size(); i++) {
                    String value = "";
                    String[] split = data.get(i).split("=");
                    if (split.length == 2) {
                        value = split[1];
                    }
                    resultParamIntegr.get(i).getData().add(value);
                }
            }

            resultParamIntegr.add(new ParameterData("Дата"));
            resultParamIntegr.get(resultParamIntegr.size() - 1).getData().addAll(Arrays.asList(startDateIntegrate, endDateIntegrate));
        } catch (XMLStreamException | IOException e) {
            throw new ParseException("parse error");
        }

        removeNullParameters(resultParam);
        removeNullParameters(resultParamIntegr);

        updateParamNames(resultParam, dbParamName);
        updateParamNames(resultParamIntegr, dbParamNameIntegr);

        changePositionDateDataRow(resultParam);
        changePositionDateDataRow(resultParamIntegr);

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

        return reportData;
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

    private static void removeNullParameters(List<ParameterData> resultParam) {
        resultParam.removeIf(parameterData -> {
            for (String value: parameterData.getData()) {
                if (!value.equals("")) {
                    return false;
                }
            }
            return true;
        });
    }

    private static void changePositionDateDataRow(List<ParameterData> resultParam) {
        ParameterData dateData = resultParam.stream().filter(parameterData -> parameterData.getName().equals("Дата")).findFirst().orElse(null);
        if (dateData != null) {
            resultParam.remove(dateData);
            resultParam.add(0, dateData);
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
