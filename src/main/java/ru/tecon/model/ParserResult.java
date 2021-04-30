package ru.tecon.model;

import ru.tecon.parser.model.ReportData;

import java.io.Serializable;
import java.util.StringJoiner;

/**
 * Класс с результатами попытки ассоциации разобранных данных с объектами в базе.
 * Присутствует ссылка на разобранные данные
 */
public class ParserResult implements Serializable {

    private int status;
    private String name;
    private String objectId;
    private String message;
    private String system;
    private ReportData reportData = new ReportData();

    public ParserResult(int status, String name, String message, String objectId) {
        this.status = status;
        this.name = name;
        this.message = message;
        this.objectId = objectId;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public ReportData getReportData() {
        return reportData;
    }

    public void setReportData(ReportData reportData) {
        this.reportData = reportData;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getSystem() {
        return system;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ParserResult.class.getSimpleName() + "[", "]")
                .add("status=" + status)
                .add("name='" + name + "'")
                .add("objectId='" + objectId + "'")
                .add("message='" + message + "'")
                .add("system='" + system + "'")
                .add("reportData=" + reportData)
                .toString();
    }
}
