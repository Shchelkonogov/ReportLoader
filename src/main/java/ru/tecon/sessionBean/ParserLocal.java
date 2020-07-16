package ru.tecon.sessionBean;

import ru.tecon.model.DataModel;
import ru.tecon.model.ParserResult;
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
     * Асинхронный метод, который обновляет статус пересчета данных в базе
     * (внутренние особенности базы)
     * @param histData значения параметров
     * @return null
     */
    Future<Void> updateDataBaseCalculation(List<DataModel> histData);
}
