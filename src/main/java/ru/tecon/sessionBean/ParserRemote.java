package ru.tecon.sessionBean;

import ru.tecon.model.DataModel;
import ru.tecon.model.ParserResult;
import ru.tecon.parser.model.ReportData;

import javax.ejb.Remote;
import java.util.List;
import java.util.concurrent.Future;

@Remote
public interface ParserRemote {

    ParserResult parse(ReportData data);

    List<String> getObjectNames(String searchPath);

    List<String> getHeatSystemNames(String objectId);

    void saveAssociation(String associateName, String reportAddress, String counterType, String counterNumber);

    Future<Void> uploadSecondaryData(List<DataModel> histData);
}
