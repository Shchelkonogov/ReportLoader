package ru.tecon.sessionBean;

import ru.tecon.beanInterface.LoadOPCRemote;
import ru.tecon.model.DataModel;
import ru.tecon.model.ParserResult;
import ru.tecon.model.ValueModel;
import ru.tecon.parser.model.ParameterData;
import ru.tecon.parser.model.ReportData;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless бин для разбора файлов тепловых отчетов,
 * реализует локальный интерфейс {@link ParserLocal}
 */
@Stateless(name = "ParserBean", mappedName = "ejb/ParserBean")
@Local(ParserLocal.class)
public class ParserSB implements ParserLocal {

    private static final Logger LOG = Logger.getLogger(ParserSB.class.getName());

    private static final String ALTER_SQL = "alter session set NLS_NUMERIC_CHARACTERS='.,'";

    private static final String SQL = "call fix_obj_heat_sys(?, ?, ?, ?, ?, ?, ?, ?, ?, to_date(?, 'dd.mm.yyyy'), " +
            "to_date(?, 'dd.mm.yyyy'), ?, ?, ?, ?, ?, ?)";

    private static final String GET_OBJECT_NAMES = "select obj_name from obj_object where obj_type = 303 and upper(obj_name) like ? " +
            "order by obj_name";

    private static final String GET_HEAT_SYSTEM_NAMES = "select distinct b.techproc from (select * from dz_obj_param0_1 where stat_agr_id <> 4) d, " +
            "(select x.id, c.techproc_type_char || replace(x.zone, '0', '') techproc from dz_param x, dev_techproc_type c " +
            "where  x.techproc_type_id = c.techproc_type_id) b " +
            "where d.obj_id = ? " +
            "and d.id = b.id " +
            "and b.techproc in (select distinct techproc from opc_uu_config_type)";

    private static final String INSERT_SAVE_ASSOCIATION_NAME = "insert into dic_names values " +
            "((select obj_id from obj_object where obj_name = ?), ?, 'Y')";

    private static final String INSERT_SAVE_ASSOCIATION_COUNTER = "insert into dic_counters values " +
            "((select obj_id from obj_object where obj_name = ?), ?, ?)";

    private static final String IDENTIFY_OBJECT_PARAMETERS = "select item_name, par_id, stat_aggr from opc_uu_config_type " +
            "where techproc = ? and base_par_code = ?";

    private static final String INSERT_DAY_DATA = "insert into dz_hist_day_data values (?, ?, ?, to_date(?, 'dd.mm.yyyy hh24:mi:ss'), ?, null)";

    private static final String INSERT_DB_CALCULATION = "insert into dz_last_calc_day (obj_id, par_id, stat_aggr, time_stamp) " +
            "values(?, ?, ?, to_date(?, 'dd.mm.yyyy'))";

    private static final String UPDATE_DB_CALCULATION = "update dz_last_calc_day set time_stamp = to_date(?, 'dd.mm.yyyy') " +
            "where obj_id = ? and par_id = ? and stat_aggr = ?";

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    @Resource(name = "jdbc/DataSourceUpload")
    private DataSource dsUpload;

    @EJB(name = "LoadOPC", mappedName = "ejb/LoadOPC")
    private LoadOPCRemote loadOPCRemote;

    @EJB(name = "ParserBean")
    private ParserLocal ejbLocal;

    @Override
    public ParserResult parse(ReportData data) {
        try (Connection connection = ds.getConnection();
             PreparedStatement alterStm = connection.prepareStatement(ALTER_SQL);
             CallableStatement stm = connection.prepareCall(SQL)) {
            alterStm.execute();

            String reportType = data.getReportType();
            String reportTypeImportant = "";
            if (reportType.contains("!important")) {
                reportTypeImportant = reportType.replace("!important", "");
                reportType = reportTypeImportant;
            }

            stm.setString(1, data.getFileName());
            stm.setString(2, reportType);
            stm.setString(3, reportType);
            stm.setString(4, data.getAddress());
            stm.setString(5, data.getCounterType());
            stm.setString(6, data.getCounterNumber());
            stm.setString(7, reportType);

            String qi = "";
            String timei = "";
            String date = "";
            if (!data.getParamIntegr().isEmpty()) {
                date = data.getParamIntegr().get(0).getData().get(0);
            }
            for (ParameterData item: data.getParamIntegr()) {
                if (item.getName().equals("Qi")) {
                    qi = item.getData().get(0);
                }
                if (item.getName().equals("Timei")) {
                    timei = item.getData().get(0);
                }
            }

            stm.setString(8, qi);
            stm.setString(9, timei);
            stm.setString(10, date);
            stm.setString(11, date);
            stm.setString(12, reportTypeImportant);

            stm.registerOutParameter(13, Types.INTEGER);
            stm.registerOutParameter(14, Types.INTEGER);
            stm.registerOutParameter(15, Types.VARCHAR);
            stm.registerOutParameter(16, Types.VARCHAR);
            stm.registerOutParameter(17, Types.VARCHAR);

            stm.executeUpdate();

            if (stm.getInt(13) == 2) {
                uploadData(data, stm.getString(16), stm.getInt(14));
            }

            return new ParserResult(stm.getInt(13), data.getFileName(), stm.getString(17), stm.getString(14));
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "fix_obj_heat_sys error: for file " + data.getFileName(), e);
        }
        return null;
    }

    private void uploadData(ReportData data, String system, int objectId) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(IDENTIFY_OBJECT_PARAMETERS)) {
            List<DataModel> integrateData = new ArrayList<>();
            List<DataModel> histData = new ArrayList<>();

            generateData(integrateData, stm, data.getParamIntegr(), system, objectId, -3);
            generateData(histData, stm, data.getParam(), system, objectId, 0);

            loadOPCRemote.putData(integrateData);

            ejbLocal.uploadSecondaryData(histData);

            ejbLocal.updateDataBaseCalculation(histData);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "identify object parameters error: ", e);
        }
    }

    private void generateData(List<DataModel> generateData, PreparedStatement stm, List<ParameterData> data, String system,
                              int objectId, int changeDate) throws SQLException {
        outer:
        for (int i = 1; i < data.size(); i++) {
            ParameterData parameterData = data.get(i);
            stm.setString(1, system);
            stm.setString(2, parameterData.getName());

            ResultSet res = stm.executeQuery();
            if (res.next()) {
                DataModel dataModel = new DataModel(res.getString(1), objectId, res.getInt(2), res.getInt(3), null, null);

                for (int j = 0; j < parameterData.getData().size(); j++) {
                    String item = parameterData.getData().get(j);

                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(data.get(0).getData().get(j) + " 00:00:00",
                                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")).plusHours(changeDate);

                        dataModel.addData(new ValueModel(item, dateTime));
                    } catch (DateTimeParseException ignore) {
                        continue outer;
                    }
                }

                generateData.add(dataModel);
            }
        }
    }

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Future<Void> updateDataBaseCalculation(List<DataModel> histData) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stmInsert = connect.prepareStatement(INSERT_DB_CALCULATION);
             PreparedStatement stmUpdate = connect.prepareStatement(UPDATE_DB_CALCULATION)) {
            for (DataModel model: histData) {
                String date = model.getData().get(model.getData().size() - 1).getTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                try {
                    stmInsert.setInt(1, model.getObjectId());
                    stmInsert.setInt(2, model.getParamId());
                    stmInsert.setInt(3, model.getAggregateId());
                    stmInsert.setString(4, date);

                    stmInsert.executeUpdate();
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1) {
                        try {
                            stmUpdate.setString(1, date);
                            stmUpdate.setInt(2, model.getObjectId());
                            stmUpdate.setInt(3, model.getParamId());
                            stmUpdate.setInt(4, model.getAggregateId());

                            stmUpdate.executeUpdate();
                        } catch (SQLException ignore) {
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "error update data base calculation", e);
        }
        return null;
    }

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Future<Void> uploadSecondaryData(List<DataModel> histData) {
        try (Connection connect = dsUpload.getConnection();
             PreparedStatement stmUpload = connect.prepareStatement(INSERT_DAY_DATA)) {
            int counter = 0;
            int objectId = -1;
            for (DataModel model: histData) {
                for (ValueModel dataModel: model.getData()) {
                    try {
                        if (objectId == -1) {
                            objectId = model.getObjectId();
                        }

                        stmUpload.setInt(1, model.getObjectId());
                        stmUpload.setInt(2, model.getParamId());
                        stmUpload.setInt(3, model.getAggregateId());
                        stmUpload.setString(4, dataModel.getTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                        stmUpload.setString(5, dataModel.getValue());

                        stmUpload.executeUpdate();
                        counter++;
                    } catch (SQLException ignore) {
                    }
                }
            }
            if (counter != 0) {
                LOG.info("insert " + counter + " secondary data for object " + objectId);
            } else {
                LOG.info("no secondary data inserted for object " + objectId);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "upload secondary data error: ", e);
        }
        return null;
    }

    @Override
    public List<String> getObjectNames(String searchPath) {
        List<String> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(GET_OBJECT_NAMES)) {
            stm.setString(1, "%" + searchPath.toUpperCase() + "%");

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(res.getString(1));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "find names error:", e);
        }
        return result;
    }

    @Override
    public List<String> getHeatSystemNames(String objectId) {
        List<String> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(GET_HEAT_SYSTEM_NAMES)) {
            stm.setInt(1, Integer.parseInt(objectId));

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(res.getString(1));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "load heat system error:", e);
        }
        return result;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void saveAssociation(String associateName, String reportAddress, String counterType, String counterNumber) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stmAssociateName = connect.prepareStatement(INSERT_SAVE_ASSOCIATION_NAME);
             PreparedStatement stmAssociateCounter = connect.prepareStatement(INSERT_SAVE_ASSOCIATION_COUNTER)) {
            if (Objects.nonNull(reportAddress) && !reportAddress.trim().equals("")) {
                stmAssociateName.setString(1, associateName);
                stmAssociateName.setString(2, reportAddress);
                stmAssociateName.executeUpdate();

                LOG.info("insert association: " + reportAddress + " with " + associateName);

                if (Objects.nonNull(counterType) && Objects.nonNull(counterNumber) &&
                        !counterType.trim().equals("") && !counterNumber.trim().equals("")) {
                    stmAssociateCounter.setString(1, associateName);
                    stmAssociateCounter.setString(2, counterType);
                    stmAssociateCounter.setString(3, counterNumber);
                    stmAssociateCounter.executeUpdate();

                    LOG.info("insert association: " + counterType + " " + counterNumber + " with " + associateName);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "save name association error:", e);
        }
    }
}
