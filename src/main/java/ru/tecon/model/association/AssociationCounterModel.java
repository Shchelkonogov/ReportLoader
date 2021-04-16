package ru.tecon.model.association;

import java.util.StringJoiner;

/**
 * Класс расширение для ассоциаций счетчиков
 * @author Maksim Shchelkonogov
 */
public class AssociationCounterModel extends AssociationModel {

    private String counterType;
    private String counterNumber;

    public AssociationCounterModel(String rowID, String counterType, String counterNumber) {
        super(rowID);
        this.counterType = counterType;
        this.counterNumber = counterNumber;
    }

    public String getCounterNumber() {
        return counterNumber;
    }

    public String getCounterType() {
        return counterType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AssociationCounterModel.class.getSimpleName() + "[", "]")
                .add("rowID='" + super.getRowID() + "'")
                .add("counterType='" + counterType + "'")
                .add("counterNumber='" + counterNumber + "'")
                .toString();
    }
}
