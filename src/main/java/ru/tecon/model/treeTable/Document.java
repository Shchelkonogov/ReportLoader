package ru.tecon.model.treeTable;

import java.io.Serializable;
import java.util.StringJoiner;

/**
 * Модель данных для отображения в treeTable.
 * Состаит из имени файла, размера файла и статуса разбора.
 * В узлах treeTable в поле размер файла заношу сводку по разбору файлов.
 */
public class Document implements Serializable {

    private String name;
    private String size;
    private int status = DocumentParsStatus.NEW;

    public Document(String name, String size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Document.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("size='" + size + "'")
                .add("status=" + status)
                .toString();
    }
}
