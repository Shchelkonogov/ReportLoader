package ru.tecon.model.association;

/**
 * Абстрактный класс для ассоциаций
 * @author Maksim Shchelkonogov
 */
public abstract class AssociationModel {

    private String rowID;

    AssociationModel(String rowID) {
        this.rowID = rowID;
    }

    public String getRowID() {
        return rowID;
    }
}
