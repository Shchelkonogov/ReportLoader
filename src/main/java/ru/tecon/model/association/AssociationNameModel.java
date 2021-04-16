package ru.tecon.model.association;

import java.util.StringJoiner;

/**
 * Класс расширение для ассоциаций адресов
 * @author Maksim Shchelkonogov
 */
public class AssociationNameModel extends AssociationModel {

    private String name;

    public AssociationNameModel(String rowID, String name) {
        super(rowID);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AssociationNameModel.class.getSimpleName() + "[", "]")
                .add("rowID='" + super.getRowID() + "'")
                .add("name='" + name + "'")
                .toString();
    }
}
