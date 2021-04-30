package ru.tecon;

import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import ru.tecon.model.treeTable.Document;
import ru.tecon.model.treeTable.DocumentParsStatus;
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
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named("uploadService")
@ViewScoped
public class ServiceMB implements Serializable {

    private static Logger log = Logger.getLogger(ServiceMB.class.getName());

    private static final Map<String, String> DOCUMENT_TYPES = Stream.of(new String[][] {
            {"html", "Единый формат МОЭК"},
            {"pdf", "PDF файлы"},
            {"xml", "XML файлы"}
    }).collect(Collectors.toMap(key -> key[0], value -> value[1]));

    private static final Map<String, String> CONTENT_TYPES = Stream.of(new String[][] {
            {"html", "text/html"},
            {"pdf", "application/pdf"},
            {"xml", "application/xml"}
    }).collect(Collectors.toMap(key -> key[0], value -> value[1]));

    private String rootPath;

    private ExecutorService service = Executors.newSingleThreadExecutor();

    private UUID uuid;

    private TreeNode root;
    private TreeNode rootFilter;
    private List<Document> treeData = new ArrayList<>();
    private Map<String, DefaultTreeNode> foldersMap = new HashMap<>();

    private boolean parseStatus = false;
    private Set<String> updateItems = new CopyOnWriteArraySet<>();

    private Map<String, ParserResult> parserResults = new HashMap<>();

    private String reportName;
    private String searchText;
    private List<String> searchList = new ArrayList<>();
    private String searchSelectItem;

    private List<String> heatSystemList = new ArrayList<>();
    private String selectHeatSystem;

    private boolean sessionActive = false;

    @EJB(name = "ParserBean")
    private ParserLocal ejbParser;

    @PostConstruct
    private void init() {
        uuid = UUID.randomUUID();
        root = new DefaultTreeNode(new Document("Files", "-"), null);

        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();

        HttpServletRequest request = (HttpServletRequest) ec.getRequest();
        String sessionID = request.getParameter("sessionID");

        if ((sessionID != null) && ejbParser.checkSession(sessionID)) {
            sessionActive = true;
        }

        ServletContext sc = (ServletContext) ec.getContext();
        rootPath = System.getProperty("user.dir") + sc.getInitParameter("upload.location") + "/" + uuid + "/";
    }

    /**
     * Метод для обновления модели данных treeTable
     */
    public void updateTreeData() {
        treeData.clear();
        root.getChildren().clear();
        foldersMap.clear();

        Path folder = Paths.get(rootPath);

        if (Files.exists(folder) && Files.isDirectory(folder)) {
            try (Stream<Path> pathStream = Files.walk(folder)) {
                List<Path> paths = pathStream
                        .filter(path -> Files.isRegularFile(path))
                        .collect(Collectors.toList());

                Set<String> extensions = paths.stream()
                        .map(Utils::getExtension)
                        .collect(Collectors.toSet());

                for (String extension: extensions) {
                    foldersMap.put(extension, new DefaultTreeNode(new Document(DOCUMENT_TYPES.get(extension), "0/0/0/0"), root));
                }

                for (Path path: paths) {
                    DefaultTreeNode parent = foldersMap.get(Utils.getExtension(path));
                    Document doc = new Document(path.getFileName().toString(),
                            Utils.humanReadableByteCountBin(FileChannel.open(path).size()));

                    treeData.add(doc);
                    new DefaultTreeNode(doc, parent);
                }

                for (Map.Entry<String, DefaultTreeNode> entry: foldersMap.entrySet()) {
                    ((Document) entry.getValue().getData()).setSize("0/0/0/" + entry.getValue().getChildCount());
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "read files error", e);
            }
        }
    }

    /**
     * Метод для скачивания файла с сервера приложений
     * @param fileName имя файла
     * @throws IOException если файл не найден
     */
    public void download(String fileName) throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        Path path = Paths.get(rootPath + fileName);

        ec.responseReset();
        ec.setResponseContentType(CONTENT_TYPES.get(Utils.getExtension(fileName)) + "; charset=UTF-8");
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
        ec.setResponseCharacterEncoding("UTF-8");

        try (InputStream inputStream = Files.newInputStream(path);
             OutputStream output = ec.getResponseOutputStream()) {
            byte[] bytesBuffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputStream.read(bytesBuffer)) > 0) {
                output.write(bytesBuffer, 0, bytesRead);
            }

            output.flush();
            fc.responseComplete();
        }
    }

    /**
     * Метод проверяет, требуется ли рисовать кнопку "просмотр файла"
     * @param name имя файла
     * @return true - если кнопку отображать и наоборот
     */
    public boolean checkRender(String name) {
        return !DOCUMENT_TYPES.containsValue(name);
    }

    /**
     * Мотод проверяет, требуется ли отображать кнопку "связать"
     * @param status статус разбора файла
     * @param name имя файла
     * @return true - если кнопку отображать и наоборот
     */
    public boolean checkControlRender(int status, String name) {
        return ((checkRender(name)) && ((status == DocumentParsStatus.ERROR) || (status == DocumentParsStatus.NOTICE)));
    }

    /**
     * Метод запускает диалоговое окно по нажатию кнопки "связать"
     * @param name имя файла
     */
    public void initAssociateDialog(String name) {
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
                    ejbParser.getHeatSystemNames(parserResults.get(reportName).getObjectId());
                    break;
                }
            }
        }

        PrimeFaces.current().ajax().update("dialog");
        PrimeFaces.current().executeScript("PF('" + dialogName + "').show()");
    }

    /**
     * Метод ассоциирует выбранный объект с данными в бд
     */
    public void associate() {
        ReportData data = parserResults.get(reportName).getReportData();

        ejbParser.saveAssociation(searchSelectItem, data.getAddress(), data.getCounterType(), data.getCounterNumber());

        data.setAddress(searchSelectItem);

        checkDBAssociate(data);
    }

    /**
     * Метод ассоциирует выбранную теплосистему с данными в бд
     */
    public void associateHeatSystem() {
        ReportData data = parserResults.get(reportName).getReportData();
        data.setReportType(selectHeatSystem + "!important");

        checkDBAssociate(data);
    }

    /**
     * Метод пытается ассоциировать разобранные данные с бд
     * @param data разобранные данные
     */
    private void checkDBAssociate(ReportData data) {
        for (Document doc: treeData) {
            if (doc.getName().equals(reportName)) {
                try {
                    ParserResult result = ejbParser.parse(data).get(1, TimeUnit.MINUTES);

                    if (result.getStatus() == 2) {
                        createTooltip(doc, result);
                        doc.setStatus(DocumentParsStatus.OK);
                        parserResults.remove(reportName);
                    } else {
                        result.setReportData(data);
                        parserResults.put(doc.getName(), result);
                        doc.setStatus(DocumentParsStatus.NOTICE);
                    }
                } catch (InterruptedException ignore) {
                } catch (ExecutionException e) {
                    parserResults.put(doc.getName(), new ParserResult(4, "", "Неизвестная ошибка", ""));
                    doc.setStatus(DocumentParsStatus.ERROR);
                    log.warning("Неизвестная ошибка");
                } catch (TimeoutException e) {
                    parserResults.put(doc.getName(), new ParserResult(4, "", "Превышенно время ожидания результата", ""));
                    doc.setStatus(DocumentParsStatus.ERROR);
                    log.warning("Превышенно время ожидания результата");
                } catch (NullPointerException e) {
                    parserResults.put(doc.getName(), new ParserResult(4, "", "Серверная ошибка", ""));
                    doc.setStatus(DocumentParsStatus.ERROR);
                    log.warning("Серверная ошибка");
                }

                updateParentSizeAndStatus(foldersMap.get(Utils.getExtension(doc.getName())), doc.getStatus(), true);

                updateItems.add("treeTable:treeTableData:" + getNodeId(doc.getName(), 0));
                updateItems.add("treeTable:treeTableData:" + getNodeId(doc.getName(), 1));
                checkUpdate(false);

                break;
            }
        }
    }

    /**
     * Метод начинает разбор всех файлов
     */
    public void parse() {
        log.info("Start parsing files");
        parseStatus = true;
        parserResults.clear();
        PrimeFaces.current().executeScript("start();");

        new Thread(() -> {
            for (Document doc: treeData) {
                TreeNode parent = foldersMap.get(Utils.getExtension(doc.getName()));

                ((Document) parent.getData()).setStatus(DocumentParsStatus.WORKING);
                doc.setStatus(DocumentParsStatus.WORKING);

                String parentId = getNodeId(doc.getName(), 0);
                String nodeId = getNodeId(doc.getName(), 1);

                updateItems.add("treeTable:treeTableData:" + parentId);
                updateItems.add("treeTable:treeTableData:" + nodeId);

                try {
                    Future<ReportData> future = service.submit(() -> ParseFile.parseFile(rootPath + doc.getName()));

                    ReportData data = future.get(1, TimeUnit.MINUTES);

                    ParserResult result = ejbParser.parse(data).get(1, TimeUnit.MINUTES);

                    if (result.getStatus() == 2) {
                        createTooltip(doc, result);
                        doc.setStatus(DocumentParsStatus.OK);
                    } else {
                        result.setReportData(data);
                        parserResults.put(doc.getName(), result);
                        doc.setStatus(DocumentParsStatus.NOTICE);
                    }
                } catch (InterruptedException ignore) {
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof ParseException) {
                        ParseException parseException = (ParseException) e.getCause();
                        parserResults.put(doc.getName(), new ParserResult(4, "", parseException.getMessage(), ""));
                        doc.setStatus(DocumentParsStatus.ERROR);
                        log.warning(parseException.getMessage());
                    } else {
                        parserResults.put(doc.getName(), new ParserResult(4, "", "Неизвестная ошибка", ""));
                        doc.setStatus(DocumentParsStatus.ERROR);
                        log.warning("Неизвестная ошибка");
                    }
                } catch (TimeoutException e) {
                    parserResults.put(doc.getName(), new ParserResult(4, "", "Превышенно время ожидания результата", ""));
                    doc.setStatus(DocumentParsStatus.ERROR);
                    log.warning("Превышенно время ожидания результата");
                } catch (NullPointerException e) {
                    parserResults.put(doc.getName(), new ParserResult(4, "", "Серверная ошибка", ""));
                    doc.setStatus(DocumentParsStatus.ERROR);
                    log.warning("Серверная ошибка");
                }

                updateParentSizeAndStatus(parent, doc.getStatus(), false);

                updateItems.add("treeTable:treeTableData:" + parentId);
                updateItems.add("treeTable:treeTableData:" + nodeId);
            }
            log.info("parse files is finish");
            parseStatus = false;
        }).start();
    }

    /**
     * Метод формирует tooltip для разобранного элемента
     * @param doc документ
     * @param result результат разбора
     */
    private void createTooltip(Document doc, ParserResult result) {
        String addTooltip = "";
        if ((result.getSystem() != null) && !result.getSystem().equals("")) {
            addTooltip = " (" + result.getSystem() + ")";
        }
        doc.setTooltip(ejbParser.getObjectName(Integer.valueOf(result.getObjectId())) + addTooltip);
    }

    /**
     * Метод обновляет статус и размер (статус разбора) родительского узла
     * @param parent родительский узел
     * @param currentNodeStatus статус разбора элемента
     * @param update когда первый раз разбираем (true -> ok++; bad--;)
     *               когда доразбираем не разобранные (false -> ok++; bad++;)
     */
    private void updateParentSizeAndStatus(TreeNode parent, int currentNodeStatus, boolean update) {
        Set<Integer> status = parent.getChildren().stream()
                .map(treeNode -> ((Document) treeNode.getData()).getStatus())
                .collect(Collectors.toSet());

        if (!status.contains(DocumentParsStatus.WORKING) && !status.contains(DocumentParsStatus.NEW)) {
            if ((status.size() == 1) && (!status.contains(DocumentParsStatus.NOTICE))) {
                ((Document) parent.getData()).setStatus(status.iterator().next());
            } else {
                ((Document) parent.getData()).setStatus(DocumentParsStatus.NOTICE_NODE);
            }
        }

        int count = Integer.parseInt(((Document) parent.getData()).getSize().split("/")[3]);
        int ok = Integer.parseInt(((Document) parent.getData()).getSize().split("/")[0]);
        int bad = Integer.parseInt(((Document) parent.getData()).getSize().split("/")[1]);
        int manual = Integer.parseInt(((Document) parent.getData()).getSize().split("/")[2]);

        if (currentNodeStatus == DocumentParsStatus.OK) {
            ok++;
            if (update) {
                manual--;
            }
        }
        if (!update) {
            if (currentNodeStatus == DocumentParsStatus.ERROR) {
                bad++;
            }
            if (currentNodeStatus == DocumentParsStatus.NOTICE) {
                manual++;
            }
        }
        ((Document) parent.getData()).setSize(ok + "/" + bad + "/" + manual + "/" + count);
    }

    /**
     * Метод определяет id элемента treeTable
     * @param name имя элемента
     * @param level уровень элемента 0 - узел; 1 - лепесток
     * @return возвращает id
     */
    private String getNodeId(String name, int level) {
        TreeNode rootNode = Objects.nonNull(rootFilter) ? rootFilter : root;

        for (TreeNode parent: rootNode.getChildren()) {
            for (TreeNode node: parent.getChildren()) {
                if (((Document) node.getData()).getName().equals(name)) {
                    switch (level) {
                        case 0: return node.getParent().getRowKey();
                        case 1: return node.getRowKey();
                        default: return null;
                    }
                }
            }
        }
        return null;
    }

    public String getStatusImage(int status) {
        switch (status) {
            case DocumentParsStatus.WORKING: {
                return "pi pi-spin pi-spinner";
            }
            case DocumentParsStatus.OK: {
                return "pi pi-check";
            }
            case DocumentParsStatus.ERROR: {
                return "pi pi-times";
            }
            case DocumentParsStatus.NOTICE:
            case DocumentParsStatus.NOTICE_NODE: {
                return "pi pi-info";
            }
            default: return "";
        }
    }

    public String getColor(int status) {
        switch (status) {
            case DocumentParsStatus.OK: {
                return "green";
            }
            case DocumentParsStatus.ERROR: {
                return "red";
            }
            case DocumentParsStatus.NOTICE:
            case DocumentParsStatus.NOTICE_NODE: {
                return "salmon";
            }
            default: return "black";
        }
    }

    /**
     * Метод обновляет элементы treeTable
     * @param checkTimer true - если требуется проверить js таймер, для постоянного обновления
     */
    public void checkUpdate(boolean checkTimer) {
        if (!updateItems.isEmpty()) {
            Set<String> copy = new HashSet<>(updateItems);
            updateItems.removeAll(copy);

            copy.forEach(s -> {
                PrimeFaces.current().ajax().update(s + ":sizeColumn");
                PrimeFaces.current().ajax().update(s + ":iconColumn");
                PrimeFaces.current().ajax().update(s + ":controlColumn");
            });

//            PrimeFaces.current().executeScript("unsetWidth();");
        }
        if (checkTimer && !parseStatus) {
            PrimeFaces.current().executeScript("stop(); PF('parseButton').enable();");
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

    public boolean isSizeRender(String text) {
        return text.contains("/");
    }

    public String getOkParse(String text) {
        return text.split("/")[0];
    }

    public String getBadParse(String text) {
        return text.split("/")[1];
    }

    public String getManualParse(String text) {
        return text.split("/")[2];
    }

    public String getAll(String text) {
        return  "(" + (Integer.parseInt(text.split("/")[0]) + Integer.parseInt(text.split("/")[1]) + Integer.parseInt(text.split("/")[2])) +
                " из " + text.split("/")[3] + ")";
    }

    public String getButtonName(int status) {
        switch (status) {
            case DocumentParsStatus.ERROR: return "Статус";
            case DocumentParsStatus.NOTICE:
                default: return "Связать";
        }
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

    public TreeNode getRootFilter() {
        return rootFilter;
    }

    public void setRootFilter(TreeNode rootFilter) {
        PrimeFaces.current().executeScript("addListener()");
        this.rootFilter = rootFilter;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }
}
