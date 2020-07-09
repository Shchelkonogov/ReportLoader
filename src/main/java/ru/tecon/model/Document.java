package ru.tecon.model;

import java.io.Serializable;
import java.util.StringJoiner;

public class Document implements Serializable {

    private String name;
    private String size;
    private int status = -1;
    private int id;

    public Document(String name, String size, int id) {
        this.name = name;
        this.size = size;
        this.id = id;
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

    public int getId() {
        return id;
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
