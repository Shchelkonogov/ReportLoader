package ru.tecon;

import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import ru.tecon.model.Document;
import ru.tecon.model.ParserResult;
import ru.tecon.parser.ParseException;
import ru.tecon.parser.ParseFile;
import ru.tecon.parser.model.ReportData;
import ru.tecon.sessionBean.ParserLocal;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.ServletContext;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named("uploadService")
@ViewScoped
public class ServiceMB implements Serializable {

    private static Logger log = Logger.getLogger(ServiceMB.class.getName());

    private static final Map<String, String> DOCUMENT_TYPES = Stream.of(new String[][] {
            {".html", "Единый формат МОЭК"},
            {".pdf", "PDF файлы"},
            {".xml", "XML файлы"}
    }).collect(Collectors.toMap(key -> key[0], value -> value[1]));

    private UUID uuid;

    private TreeNode root;
    private List<Document> treeData = new ArrayList<>();
    private Map<String, DefaultTreeNode> foldersMap = new HashMap<>();

    private boolean update = false;
    private boolean parseStatus = false;
    private Set<String> updateItems = new CopyOnWriteArraySet<>();

//    private boolean showData = false;
//    private Map<String, ParseData> parseDataMap = new HashMap<>();
//    private ParseData showParseData;

    private Map<String, ParserResult> parserResults = new HashMap<>();

    private String reportName;
    private String searchText;
    private List<String> searchList = new ArrayList<>();
    private String searchSelectItem;

    private List<String> heatSystemList = new ArrayList<>();
    private String selectHeatSystem;

    @EJB(name = "ParserBean")
    private ParserLocal ejbParser;

    @PostConstruct
    private void init() {
        uuid = UUID.randomUUID();
        root = new DefaultTreeNode(new Document("Files", "-", -1), null);
    }

    public void updateTreeData() {
        treeData.clear();
        root.getChildren().clear();
        foldersMap.clear();
        ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        File folder = new File(System.getProperty("user.dir") + context.getInitParameter("upload.location") + "/" + uuid);
        try {
            if (folder.exists()) {
                int i = 0;
                File[] files = Objects.requireNonNull(folder.listFiles());
                Set<String> extensions = Arrays.stream(files).map(file -> getExtension(file.getName())).collect(Collectors.toSet());
                for (String extension: extensions) {
                    foldersMap.put(extension, new DefaultTreeNode(new Document(extension, "0/0/0", i), root));
                    i++;
                }
                for (File file: files) {
                    DefaultTreeNode parent = foldersMap.get(getExtension(file.getName()));
                    Document doc = new Document(file.getName(), file.length() + " Байт", parent.getChildCount());
                    treeData.add(doc);
                    int newSize = Integer.parseInt(((Document) parent.getData()).getSize().split("/")[2]) + 1;
                    ((Document) parent.getData()).setSize("0/0/" + newSize);
                    new DefaultTreeNode(doc, parent);
                }
            }
        } catch (NullPointerException e) {
            log.info("No files in folder: " + folder);
        }
    }

    private String getExtension(String name) {
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return DOCUMENT_TYPES.get(name.substring(lastIndexOf).toLowerCase());
    }

    public void download(String fileName) throws IOException {
//        if (parseDataMap.containsKey(fileName) && !showData) {
//            showData = true;
//        }
//        showParseData = parseDataMap.get(fileName);

        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        File file = new File(System.getProperty("user.dir") + context.getInitParameter("upload.location") + "/" + uuid + "/" + fileName);

        ec.responseReset();
        ec.setResponseContentType("application/pdf; charset=UTF-8");
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
        ec.setResponseCharacterEncoding("UTF-8");

        try (InputStream fileInputStream = new FileInputStream(file);
             OutputStream output = ec.getResponseOutputStream()) {
            byte[] bytesBuffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(bytesBuffer)) > 0) {
                output.write(bytesBuffer, 0, bytesRead);
            }

            output.flush();
            fc.responseComplete();
        }
    }

    public boolean checkRender(String name) {
        return !foldersMap.containsKey(name);
    }

    public boolean checkControlRender(int status, String name) {
        if (foldersMap.containsKey(name)) {
            return false;
        }
        return ((status != 2) && (status != -1) && (status != 1));
    }

    private void updateHeatSystemList(String objectId) {
        heatSystemList = ejbParser.getHeatSystemNames(objectId);
    }

    public void save(String name) {
        reportName = name;
        searchList.clear();
        searchText = "";
        searchSelectItem = "";
        selectHeatSystem = "";
        String dialogName = "dlg2";
        if (parserResults.containsKey(reportName)) {
            switch (parserResults.get(reportName).getStatus()) {
                case 0: {
                    dialogName = "dlg1";
                    break;
                }
                case 1: {
                    dialogName = "dlg3";
                    updateHeatSystemList(parserResults.get(reportName).getObjectId());
                    break;
                }
            }
        }
        PrimeFaces.current().ajax().update("dialog");
        PrimeFaces.current().executeScript("PF('" + dialogName + "').show()");
    }

    public void associate() {
        ReportData data = parserResults.get(reportName).getReportData();

        ejbParser.saveAssociation(searchSelectItem, data.getAddress(), data.getCounterType(), data.getCounterNumber());

        data.setAddress(searchSelectItem);

        checkDBAssociate(data);
    }

    public void associateHeatSystem() {
        ReportData data = parserResults.get(reportName).getReportData();
        data.setReportType(selectHeatSystem);

        checkDBAssociate(data);
    }

    private void checkDBAssociate(ReportData data) {
        ParserResult result = ejbParser.parse(data);

        for (Document doc: treeData) {
            if (doc.getName().equals(reportName)) {
                if (result.getStatus() == 2) {
                    doc.setStatus(2);
                    parserResults.remove(reportName);
                    System.gc();
                } else {
                    result.setReportData(data);
                    parserResults.put(doc.getName(), result);
                    doc.setStatus(4);
                }

                int id = ((Document) foldersMap.get(getExtension(doc.getName())).getData()).getId();
                PrimeFaces.current().ajax().update("treeTable:treeTableData:" + id + "_" + doc.getId() + ":sizeColumn");
                PrimeFaces.current().ajax().update("treeTable:treeTableData:" + id + "_" + doc.getId() + ":iconColumn");
                PrimeFaces.current().ajax().update("treeTable:treeTableData:" + id + "_" + doc.getId() + ":controlColumn");
                break;
            }
        }
    }

    public String getMessage() {
        if (parserResults.containsKey(reportName)) {
            return parserResults.get(reportName).getMessage();
        } else {
            return "";
        }
    }

    public String getFileName() {
        if (parserResults.containsKey(reportName)) {
            return parserResults.get(reportName).getReportData().getFileName();
        } else {
            return "";
        }
    }

    public String getAddress() {
        if (parserResults.containsKey(reportName)) {
            return parserResults.get(reportName).getReportData().getAddress();
        } else {
            return "";
        }
    }

    public String getReportHeader() {
        if (parserResults.containsKey(reportName)) {
            return parserResults.get(reportName).getReportData().getReportType();
        } else {
            return "";
        }
    }

    public String getCounterType() {
        if (parserResults.containsKey(reportName)) {
            return parserResults.get(reportName).getReportData().getCounterType();
        } else {
            return "";
        }
    }

    public void loadNames() {
        if (searchText.replaceAll("%", "").length() >= 4) {
            searchList = ejbParser.getObjectNames(searchText.replaceAll("%", ""));
        }
    }

    public void clear() {
        searchList.clear();
    }

    public void parse() {
        log.info("Start parsing files");
        parseStatus = true;
//        parseDataMap.clear();
        parserResults.clear();
        PrimeFaces.current().executeScript("start();");

        ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        String location = context.getInitParameter("upload.location");

        new Thread(() -> {
            for (Document doc: treeData) {
                doc.setStatus(1);
                TreeNode parent = foldersMap.get(getExtension(doc.getName()));
                ((Document) parent.getData()).setStatus(1);

                updateItems.add("treeTable:treeTableData:" + ((Document) parent.getData()).getId());
                updateItems.add("treeTable:treeTableData:" + ((Document) parent.getData()).getId() + "_" + doc.getId());
                update = true;

                try {
                    ReportData data = ParseFile.parseFile(System.getProperty("user.dir") + location + "/" + uuid + "/" + doc.getName());
                    ParserResult result = ejbParser.parse(data);
                    if (result.getStatus() == 2) {
                        doc.setStatus(2);
                        System.gc();
                    } else {
                        result.setReportData(data);
                        parserResults.put(doc.getName(), result);
                        doc.setStatus(4);
                    }
                } catch (ParseException e) {
                    parserResults.put(doc.getName(), new ParserResult(4, "", e.getMessage(), ""));
                    doc.setStatus(3);
                    log.warning(e.getMessage());
                }

//                try {
//                    Thread.sleep(ThreadLocalRandom.current().nextInt(2000));
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }


//                parseDataMap.put(doc.getName(), new ParseData(doc.getName(), "1", "1", "1", "1", "1", "1"));
//                doc.setStatus(2);
                boolean check = true;
                for (TreeNode node: parent.getChildren()) {
                    if ((((Document) node.getData()).getStatus() != 2) && (((Document) node.getData()).getStatus() != 3) &&
                            (((Document) node.getData()).getStatus() != 4)) {
                        check = false;
                        break;
                    }
                }
                if (check) {
                    Set<Integer> status = new HashSet<>();
                    parent.getChildren().forEach(treeNode -> status.add(((Document) treeNode.getData()).getStatus()));

                    if (status.size() == 1) {
                        ((Document) parent.getData()).setStatus(status.iterator().next());
                    } else {
                        ((Document) parent.getData()).setStatus(4);
                    }
                }

                int count = Integer.parseInt(((Document) parent.getData()).getSize().split("/")[2]);
                int ok = Integer.parseInt(((Document) parent.getData()).getSize().split("/")[0]);
                int bad = Integer.parseInt(((Document) parent.getData()).getSize().split("/")[1]);
                if (doc.getStatus() == 2) {
                    ok++;
                }
                if ((doc.getStatus() == 3) || (doc.getStatus() == 4)) {
                    bad++;
                }
                ((Document) parent.getData()).setSize(ok + "/" + bad + "/" + count);

                updateItems.add("treeTable:treeTableData:" + ((Document) parent.getData()).getId());
                updateItems.add("treeTable:treeTableData:" + ((Document) parent.getData()).getId() + "_" + doc.getId());
                update = true;
            }
            log.info("parse files is finish");
            parseStatus = false;
        }).start();
    }

    public String getStatusImage(int status) {
        switch (status) {
            case 1: {
                return "pi pi-spin pi-spinner";
            }
            case 2: {
                return "pi pi-check";
            }
            case 3: {
                return "pi pi-times";
            }
            case 4: {
                return "pi pi-info";
            }
            default: return "";
        }
    }

    public String getColor(int status) {
        switch (status) {
            case 2: {
                return "green";
            }
            case 3: {
                return "red";
            }
            case 4: {
                return "salmon";
            }
            default: return "black";
        }
    }

    public void checkUpdate() {
        if (update) {
            update = false;
            Set<String> copy = new HashSet<>(updateItems);
            updateItems.removeAll(copy);

            copy.forEach(s -> {
                PrimeFaces.current().ajax().update(s + ":sizeColumn");
                PrimeFaces.current().ajax().update(s + ":iconColumn");
                PrimeFaces.current().ajax().update(s + ":controlColumn");
            });

//            PrimeFaces.current().executeScript("unsetWidth();");
        }
        if (!parseStatus) {
            PrimeFaces.current().executeScript("stop(); PF('parseButton').enable();");
        }
    }

    public boolean isSizeRender(String text) {
        return text.contains("/");
    }

    public String getOkParse(String text) {
        return text.split("/")[0];
    }

    public String getBadParse(String text) {
        return text.split("/")[1];
    }

    public String getAll(String text) {
        return  "(" + (Integer.parseInt(text.split("/")[0]) + Integer.parseInt(text.split("/")[1])) + " из " + text.split("/")[2] + ")";
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public List<String> getSearchList() {
        return searchList;
    }

    public void setSearchList(List<String> searchList) {
        this.searchList = searchList;
    }

    public void onSelect(SelectEvent<String> event) {
        searchSelectItem = event.getObject();
    }

    public void onSelectHeatSystem(SelectEvent<String> event) {
        selectHeatSystem = event.getObject();
    }

    public List<String> getHeatSystemList() {
        return heatSystemList;
    }

    public void setHeatSystemList(List<String> heatSystemList) {
        this.heatSystemList = heatSystemList;
    }

    //    public boolean isShowData() {
//        return showData;
//    }
//
//    public ParseData getShowParseData() {
//        return showParseData;
//    }
}
