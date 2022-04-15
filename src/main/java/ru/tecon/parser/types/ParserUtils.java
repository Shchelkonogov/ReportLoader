package ru.tecon.parser.types;

import ru.tecon.parser.ParseException;
import ru.tecon.parser.model.ParameterData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ParserUtils {

    private ParserUtils() {
    }

    public static void updateValue(String name, List<ParameterData> data, float increment) {
        for (ParameterData item: data) {
            if (item.getName().equals(name)) {
                try {
                    for (int i = 0; i < item.getData().size(); i++) {
                        BigDecimal value = new BigDecimal(item.getData().get(i)).multiply(new BigDecimal(increment)).setScale(2, RoundingMode.HALF_EVEN);
                        item.getData().set(i, value.toString());
                    }
                } catch (NumberFormatException ignore) {
                }
            }
        }
    }

    public static void removeNullRows(List<ParameterData> resultParam) {
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

    /**
     * Убираем параметры, где все значения пустые
     * @param resultParam список данных для удаления параметров со всеми пустыми значениями
     */
    public static void removeNullParameters(List<ParameterData> resultParam) {
        resultParam.removeIf(parameterData -> {
            for (String value: parameterData.getData()) {
                if (!value.equals("")) {
                    return false;
                }
            }
            return true;
        });
    }

    public static void updateParamNames(List<ParameterData> resultParam, List<String> newNames, List<String> paramName) throws ParseException {
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
