package ru.tecon.sessionBean;

import ru.tecon.model.DataModel;
import ru.tecon.model.ParserResult;
import ru.tecon.model.association.AssociationModel;
import ru.tecon.parser.model.ReportData;

import javax.ejb.Local;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Локальный интерфейс для разборщика тепловых отчетов
 */
@Local
public interface ParserLocal {

    /**
     * Метод пытается ассоциировать разобранные данные файла с объектом и теплосистемой в базе
     * @param data разобранные данные файла
     * @return результат разбора
     */
    Future<ParserResult> parse(ReportData data);

    /**
     * Метод выгружает из базы список имен объектов базы, в соответсвии с поисковой строкой
     * @param searchPath поисковая строка
     * @return список объектов
     */
    List<String> getObjectNames(String searchPath);

    /**
     * Метод выгружает из базы список теплосистем, которые доступны объекту
     * @param objectId id объекта
     * @return список теплосистем
     */
    List<String> getHeatSystemNames(String objectId);

    /**
     * Метод кладет ассоциацию имени из файла, типа и номера счетчика с объектом базы
     * @param associateName имя из файла
     * @param reportAddress адрес из базы, к которому ассоциируем
     * @param counterType тип счетчика
     * @param counterNumber номер счетчика
     */
    void saveAssociation(String associateName, String reportAddress, String counterType, String counterNumber);

    /**
     * Асинхронный метод, который загружает данные по параметрам в вторичную базу
     * @param histData знаечния параметров
     * @return null
     */
    Future<Void> uploadSecondaryData(List<DataModel> histData);

    /**
     * Метод который загружает данные по отчету и интеграторы в вторичную базу
     * @param data знаечния параметров
     * @param reportData знаечния разбора отчета
     * @param system теплосистема
     * @param objectID id объекта
     */
    void uploadSecondaryFileAndIntegrateData(List<DataModel> data, ReportData reportData, String system, int objectID);

    /**
     * Асинхронный метод, который обновляет статус пересчета данных в базе
     * (внутренние особенности базы)
     * @param histData значения параметров
     * @return null
     */
    Future<Void> updateDataBaseCalculation(List<DataModel> histData);

    /**
     * Метод проверяет активна ли сессия
     * @param sessionID id сессии
     * @return true если активна
     */
    boolean checkSession(String sessionID);

    /**
     * Выгрузка списка ассоциаций адресов
     * @param name имя для поиска
     * @return список асоциаций
     */
    List<AssociationModel> getAssociationNames(String name);

    /**
     * Выгрузка списка ассоциаций счетчиков
     * @param name имя для поиска
     * @return список асоциаций
     */
    List<AssociationModel> getAssociationCounters(String name);

    /**
     * Удаляем ассоциацию адресов
     * @param rowID id строки
     */
    void removeAssociationName(String rowID);

    /**
     * Удаляем ассоциацию счетчиков
     * @param rowID id строки
     */
    void removeAssociationCounter(String rowID);

    /**
     * Выгружаем имя объекта по его ID
     * @param objectID ID объекта
     * @return полученное имя
     */
    String getObjectName(int objectID);
}
