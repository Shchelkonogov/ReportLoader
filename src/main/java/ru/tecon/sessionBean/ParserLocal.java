package ru.tecon.sessionBean;

import ru.tecon.model.DataModel;
import ru.tecon.model.ParserResult;
import ru.tecon.parser.model.ReportData;

import javax.ejb.Local;
import java.util.List;
import java.util.concurrent.Future;

@Local
public interface ParserLocal {

    ParserResult parse(ReportData data);

    List<String> getObjectNames(String searchPath);

    List<String> getHeatSystemNames(String objectId);

    void saveAssociation(String associateName, String reportAddress, String counterType, String counterNumber);

    Future<Void> uploadSecondaryData(List<DataModel> histData);
}
