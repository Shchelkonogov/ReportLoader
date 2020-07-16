package ru.tecon.model.treeTable;

/**
 * Стандартные наборы статусов разбора файлов
 */
public final class DocumentParsStatus {

    private DocumentParsStatus() {
    }

    /**
     * Объект без статуса
     */
    static final int NEW = 0;

    /**
     * Объект находится в состоянии разбора
     */
    public static final int WORKING = 1;

    /**
     * Объект разобран успешно
     */
    public static final int OK = 2;

    /**
     * Объект не разобран
     */
    public static final int ERROR = 3;

    /**
     * Объект разобран, но требуется участие пользователя
     */
    public static final int NOTICE = 4;

    /**
     * Статус узла, не все объекты разобраны
     */
    public static final int NOTICE_NODE = 5;
}
