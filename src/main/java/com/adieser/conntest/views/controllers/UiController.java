package com.adieser.conntest.views.controllers;

import com.adieser.conntest.controllers.responses.PingSessionExtract;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.service.ConnTestService;
import com.adieser.conntest.views.models.PingRow;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import javafx.scene.Node;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class UiController {
    public final ConnTestService connTestService;
    private ScheduledExecutorService executorService;
    public final Logger logger;
    private final ApplicationContext applicationContext;
    List<String> ipAddress;
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @FXML
    private Button closeButton;
    @FXML
    private Button minimizeButton;
    @FXML
    private Button maximizeRestoreButton;
    @FXML
    private VBox startButtonBox;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private ToggleGroup toggleGroup;
    @FXML
    private RadioButton customIpsButton;
    @FXML
    private RadioButton autoDiscoveryButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ComboBox<Integer> timeChoiceBox;
    private Integer timechoice = 5;
    @FXML
    private CheckBox dayFilterBox;
    @FXML
    private CheckBox lostPingsFilterBox;
    @FXML
    private VBox tableVBox;
    @FXML
    private TableView<PingRow> tableView;
    @FXML
    private TableView<String> footerTable;
    @FXML
    private Label oldestPingLabel;
    @FXML
    private Button saveButton;

    private final List<TableColumn<String, String>> averageLostColumnList = new ArrayList<>();
    private final List<TableColumn<String, String>> averageLatencyColumnList = new ArrayList<>();
    private final List<TableColumn<String, String>> lowestHighestColumnList = new ArrayList<>();
    private final List<TableColumn<String, String>> pingCountColumnList = new ArrayList<>();

    @Autowired
    public UiController(ConnTestService connTestService, Logger logger, ScheduledExecutorService executorService, ApplicationContext applicationContext) {
        this.connTestService = connTestService;
        this.logger = logger;
        this.executorService = executorService;
        this.applicationContext = applicationContext;
    }

    /**
     * Initializes UI components and sets up event handlers
     */
    @FXML
    private void initialize() {
        ipAddress = getAllIpAddresses();
        if (ipAddress == null) {
            ipAddress = new ArrayList<>();
            ipAddress.add("");
            setTables();
        }else{
            setTables();
            setOldestPing();
            Platform.runLater(this::updateTables);
        }
        timeChoiceBox.getItems().addAll(1, 5, 10, 15);
        this.startButton.setOnAction(actionEvent -> {
            startButtonBox.setDisable(true);
            progressIndicator.setVisible(true);
            dayFilterBox.setDisable(true);
            if(timeChoiceBox.getValue() != null) timechoice = timeChoiceBox.getValue();
            Thread startThread = new Thread(this::start);
            startThread.start();
        });
        this.stopButton.setOnAction(actionEvent -> this.stop());
        this.timeChoiceBox.setOnAction(actionEvent -> {
            if(startButtonBox.isDisable()){
                stopExecutorService();
                timechoice = timeChoiceBox.getValue();
                createExecutorService();
                startExecutorService(timechoice);
            }
        });
    }

    /**
     * Retrieve the last three IP addresses from the ping logs
     * @return List of IP addresses
     */
    public List<String> getAllIpAddresses() {
        PingSessionExtract pingSession;
        try {
            pingSession = connTestService.getPings();
        } catch (IOException e) {
            pingSession = null;
            logger.error("Unable to find IP addresses at the pinglogs file", e);
        }
        Set<String> ipAddressesSet = new LinkedHashSet<>();

        if (pingSession != null && pingSession.getPingLogs() != null) {
            List<PingLog> pingLogs = pingSession.getPingLogs();
            Collections.reverse(pingLogs);

            for (PingLog log : pingLogs) {
                ipAddressesSet.add(log.getIpAddress());
            }
        }
        List<String> uniqueIpAddresses = new ArrayList<>(ipAddressesSet);
        return uniqueIpAddresses.stream().limit(3).toList();
    }


    /**
     * Initializes a ping session, sets up the table view if is not already set, creates and starts a Scheduled Executor
     * to load logs and update average lost pings continuously, and updates visual controls afterward.
     */
    public void start() {
        if (!customIpsButton.isSelected()) {
            connTestService.testLocalISPInternet();
            ipAddress = connTestService.getIpAddressesFromActiveTests();
        }else{
            connTestService.testCustomIps(ipAddress);
        }
        setTables();

        createExecutorService();
        startExecutorService(timechoice);

        updateVisualControls();
    }

    /**
     * Updates the visual controls by hiding the progress indicator, the description scroll and enabling the stop button and table view
     */
    public void updateVisualControls(){
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            stopButton.setDisable(false);
        });
    }

    /**
     * Creates a single-threaded scheduled executor.
     */
    void createExecutorService() {
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts the executor service to update the table
     * */
    public void startExecutorService(Integer timechoice){
        executorService.scheduleAtFixedRate(() -> Platform.runLater(this::updateTables), 0, timechoice, TimeUnit.SECONDS);
    }

    /**
     * Load logs, set the average of lost pings and ping count for each ip.
     * */
    @FXML
    void updateTables(){
        try {
            long amountOfPings;
            List<List<PingLog>> allPingLogs = new ArrayList<>();

            for (int i = 0; i < Math.min(ipAddress.size(), 3); i++) {
                amountOfPings = loadLogs(ipAddress.get(i), allPingLogs);
                setAverageLost(ipAddress.get(i), averageLostColumnList.get(i));
                setAverageLatency(ipAddress.get(i), averageLatencyColumnList.get(i));
                setLowestHighest(ipAddress.get(i), lowestHighestColumnList.get(i));
                setPingCount(pingCountColumnList.get(i), amountOfPings);
            }

            setOldestPing();

            List<PingRow> pingRows = combinePingsByDateTime(allPingLogs);
            Collections.reverse(pingRows);
            tableView.getItems().setAll(pingRows);
        }catch(IOException e){
            stopExecutorService();
        }
    }

    /**
     * Shuts down the scheduled executor service and stops all tests.
     */
    public void stop() {
        stopButton.setDisable(true);
        progressIndicator.setVisible(false);
        stopExecutorService();
        connTestService.stopTests();
        startButtonBox.setDisable(false);
        dayFilterBox.setDisable(false);
    }

    /**
     * Shuts down the scheduled executor service
     */
    public void stopExecutorService() {
        if (executorService != null && !executorService.isShutdown()) executorService.shutdownNow();
    }

    /**
     * Sets up the TableView and Footer Table to display ping data for multiple IPs, including a save button
     */
    public void setTables() {
        Platform.runLater(() -> {
            tableView.getItems().clear();
            tableView.getColumns().clear();
            footerTable.getColumns().clear();
            averageLostColumnList.clear();
            averageLatencyColumnList.clear();
            lowestHighestColumnList.clear();
            pingCountColumnList.clear();
        });
        // Set pings table
        TableColumn<PingRow, String> dateTimeColumn = new TableColumn<>("Date");
        dateTimeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern(DATE_FORMAT))));
        dateTimeColumn.setPrefWidth(150);
        TableColumn<PingRow, String> allPingTimeColumn = new TableColumn<>("Ping Time (ms)");

        //Set footer table
        TableColumn<String, String> averageLostColumn = new TableColumn<>("Average Lost");
        TableColumn<String, String> averageLatencyColumn = new TableColumn<>("Average Latency");
        TableColumn<String, String> lowestHighestColumn = new TableColumn<>("Lowest / Highest");
        TableColumn<String, String> pingCountColumn = new TableColumn<>("PingCount");

        lowestHighestColumn.setPrefWidth(150);

        averageLatencyColumn.getColumns().add(lowestHighestColumn);
        averageLatencyColumn.getStyleClass().add("sub-column");
        averageLostColumn.getColumns().add(averageLatencyColumn);
        pingCountColumn.getColumns().add(averageLostColumn);

        Platform.runLater(() -> {
            tableView.getColumns().add(dateTimeColumn);
            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tableView.setFocusTraversable(false);

            footerTable.getColumns().add(pingCountColumn);
            footerTable.setSelectionModel(null);
            footerTable.setItems(FXCollections.observableList(new ArrayList<>(Collections.nCopies(Math.max(ipAddress.size(), 1), ""))));
            footerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        });

        // Set ping columns and labels
        for (int i = 0; i < Math.min(ipAddress.size(), 3); i++) {
            buildPingsColumn(ipAddress.get(i), allPingTimeColumn, i);
            setLabels();
        }

        saveButton.setOnAction(event -> saveLogs(dateTimeColumn, allPingTimeColumn));

        Platform.runLater(() -> tableView.getColumns().add(allPingTimeColumn));
    }

    /**
     * Builds and configures a new TableColumn for displaying ping times for a specific IP address.
     *
     * @param ip The IP address for which the ping times will be displayed.
     * @param allTimeColumn The parent column to which this IP-specific ping column will be added.
     * @param index The index of the ping time in the list of ping times for each row.
     */
    private void buildPingsColumn(String ip, TableColumn<PingRow, String> allTimeColumn, int index){
        TableColumn<PingRow, String> pingTimeColumn = new TableColumn<>(ip);
        pingTimeColumn.setCellValueFactory(cellData -> {
            List<String> pingTimes = cellData.getValue().getPingTimes();
            return new SimpleObjectProperty<>(pingTimes.size() > index ? pingTimes.get(index) : "...");
        });
        pingTimeColumn.setPrefWidth(150);
        setPingTimeCellColor(pingTimeColumn);

        allTimeColumn.getColumns().add(pingTimeColumn);
    }

    /**
     * Set average lost and ping count labels for a specified IP address at the footer table
     */
    private void setLabels() {
        TableColumn<String, String> resultAverageLostColumn = new TableColumn<>("");
        TableColumn<String, String> resultAverageLatencyColumn = new TableColumn<>("");
        TableColumn<String, String> resultlowestHighestColumn = new TableColumn<>("");
        TableColumn<String, String> resultPingCountColumn = new TableColumn<>("");

        resultlowestHighestColumn.setPrefWidth(150);
        resultAverageLatencyColumn.getColumns().add(resultlowestHighestColumn);
        resultAverageLatencyColumn.getStyleClass().add("sub-column");
        resultAverageLostColumn.getColumns().add(resultAverageLatencyColumn);
        resultPingCountColumn.getColumns().add(resultAverageLostColumn);

        Platform.runLater(() -> {
            averageLostColumnList.add(resultAverageLostColumn);
            averageLatencyColumnList.add(resultAverageLatencyColumn);
            lowestHighestColumnList.add(resultlowestHighestColumn);
            pingCountColumnList.add(resultPingCountColumn);
            footerTable.getColumns().add(resultPingCountColumn);
        });
    }

    /**
     * Sets up a custom cell value factory for a TableColumn representing ping times
     * If the ping time is -1, the text color is set to red
     * If the ping time is less than or equal to 20, the text color is set to green
     * If the ping time is between 300 and 1000, the text color is set to yellow
     * If the ping time is greater than or equal to 1000, the text color is set to orange
     * @param pingColumn updated table column
     */
    private void setPingTimeCellColor(TableColumn<PingRow, String> pingColumn) {
        pingColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else if (item.equals("...")) {
                    setText(item);
                    setTextFill(Color.GRAY);
                } else {
                    setText(item);
                    setTextFill(getColorForPingTime(Long.valueOf(item)));
                }
                setFont(Font.font("", FontWeight.BOLD, 12));
                setAlignment(Pos.CENTER);
            }

            private Color getColorForPingTime(Long pingTime) {
                if (pingTime == null || pingTime.equals(-1L)) {
                    return Color.RED;
                } else if (pingTime <= 20) {
                    return Color.web("#2E8B57");
                } else if (pingTime >= 300 && pingTime <= 1000) {
                    return Color.web("#FFD700");
                } else if (pingTime >= 1000) {
                    return Color.ORANGE;
                } else {
                    return Color.web("#4c4c4c");
                }
            }
        });
    }

    /**
     * Loads the ping logs for the specified IP address and adds them to the provided list.
     * If the day filter is selected, it retrieves logs within the current day's time range.
     * Returns the number of pings for the session.
     * @param ip The IP address for which to load logs.
     * @param allPingLogs The list where the loaded logs will be added.
     * @return The total number of pings for the session.
     */
    public long loadLogs(String ip, List<List<PingLog>> allPingLogs) throws IOException {
        PingSessionExtract pingLogs;

        if (!dayFilterBox.isSelected() && !lostPingsFilterBox.isSelected()) {
            pingLogs = connTestService.getPingsByIp(ip);
        } else if (dayFilterBox.isSelected() && !lostPingsFilterBox.isSelected()) {
            pingLogs = connTestService.getPingsByDateTimeRangeByIp(getStartOfDay(), getEndOfDay(), ip);
        } else if (lostPingsFilterBox.isSelected()) {
            pingLogs = connTestService.getLostPingsByIp(ip);
        } else { //Both filters selected
            pingLogs = connTestService.getLostPingsByDateTimeRangeByIp(getStartOfDay(), getEndOfDay(), ip);
        }

        allPingLogs.add(pingLogs.getPingLogs());
        return pingLogs.getAmountOfPings();
    }


    /**
     * Combines the ping logs from multiple IPs by their timestamp into a single list of PingRow objects.
     * Each PingRow contains the date and time, and the associated ping times for each IP.
     * @param pingLogsByIp The list of ping logs, grouped by IP address.
     * @return A list of PingRow objects, combining pings by timestamp.
     */
    public List<PingRow> combinePingsByDateTime(List<List<PingLog>> pingLogsByIp) {
        Map<LocalDateTime, List<String>> combinedMap = new TreeMap<>();

        for (int i = 0; i < pingLogsByIp.size(); i++) {
            List<PingLog> pingLogs = pingLogsByIp.get(i);

            for (PingLog pingLog : pingLogs) {
                LocalDateTime dateTime = pingLog.getDateTime();
                String pingTime = String.valueOf(pingLog.getPingTime());

                combinedMap.putIfAbsent(dateTime, new ArrayList<>(Collections.nCopies(pingLogsByIp.size(), "...")));
                combinedMap.get(dateTime).set(i, pingTime);
            }
        }

        List<PingRow> pingRows = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<String>> entry : combinedMap.entrySet()) {
            LocalDateTime dateTime = entry.getKey();
            List<String> pingTimes = entry.getValue();
            pingRows.add(new PingRow(dateTime, pingTimes));
        }

        return pingRows;
    }


    /**
     * Returns the start of the current day (00:00:00).
     * @return The start of the current day as a LocalDateTime.
     */
    private LocalDateTime getStartOfDay() {
        return LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Returns the end of the current day (23:59:59).
     * @return The end of the current day as a LocalDateTime.
     */
    private LocalDateTime getEndOfDay() {
        return LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }
    /**
     * Sets the average of lost pings for each IP address to corresponding labels.
     */
    public void setAverageLost(String ip, TableColumn<String, String> column) throws IOException {
        if (!dayFilterBox.isSelected() && !lostPingsFilterBox.isSelected()) {
            column.setText(connTestService.getPingsLostAvgByIp(ip) + "%");
        } else if (dayFilterBox.isSelected()  && !lostPingsFilterBox.isSelected() ) {
            column.setText(connTestService.getPingsLostAvgByDateTimeRangeByIp(getStartOfDay(), getEndOfDay(), ip) + "%");
        } else if (lostPingsFilterBox.isSelected()) {
            column.setText("100 %");
        }
    }

    /**
     * Sets the average latency for each IP address to corresponding labels.
     */
    public void setAverageLatency(String ip, TableColumn<String, String> column) throws IOException {
        if (!dayFilterBox.isSelected() && !lostPingsFilterBox.isSelected()) {
            column.setText(connTestService.getAvgLatencyByIp(ip) + " ms");
        } else if (dayFilterBox.isSelected()  && !lostPingsFilterBox.isSelected() ) {
            column.setText(connTestService.getAvgLatencyByDateTimeRangeByIp(getStartOfDay(), getEndOfDay(), ip) + " ms");
        } else if (lostPingsFilterBox.isSelected()) {
            column.setText("-1 ms");
        }
    }

    /**
     * Sets the lowest latency pinglog and the highest latency pinglog for each IP address to corresponding labels.
     */
    public void setLowestHighest(String ip, TableColumn<String, String> column) throws IOException {
        PingSessionExtract extract = connTestService.getMaxMinPingLog(ip);
        if (!dayFilterBox.isSelected() && !lostPingsFilterBox.isSelected()) {
            extract = connTestService.getMaxMinPingLog(ip);
        } else if (dayFilterBox.isSelected()  && !lostPingsFilterBox.isSelected() ) {
            extract = connTestService.getMaxMinPingLogByDateTimeRangeByIp(getStartOfDay(), getEndOfDay(), ip);
        } else if (lostPingsFilterBox.isSelected()) {
            column.setText("-1 / -1 ms");
            return;
        }
        if (extract.getPingLogs() != null && extract.getPingLogs().size() >= 2) {
            PingLog lowest = extract.getPingLogs().get(0);
            PingLog highest = extract.getPingLogs().get(1);
            column.setText(lowest.getPingTime() + " / " + highest.getPingTime() + " ms");
        }else{
            column.setText("0 / 0 ms");
        }
    }

    /**
     * Sets the ping count for each IP address to corresponding labels
     */
    public void setPingCount(TableColumn<String, String> column, long amountOfPings) {
        column.setText(String.valueOf(amountOfPings));
    }

    /**
     * Sets a label to display the oldest ping date and time
     */
    public void setOldestPing() {
        try {
            PingSessionExtract extract = connTestService.getPings();
            List<PingLog> pingLogs = extract.getPingLogs();

            if (pingLogs != null && !pingLogs.isEmpty()) {
                PingLog oldest = pingLogs.get(0);
                oldestPingLabel.setText("Oldest ping: " + oldest.getDateTime().format(DateTimeFormatter.ofPattern(DATE_FORMAT)));
            }else{
                oldestPingLabel.setText("Oldest ping: -");
            }

        } catch (IOException e) {
            logger.error("Error retrieving ping logs: {}", e.getMessage(), e);
        }

    }

    /**
     * Saves the ping logs displayed in the TableView to a .log file.
     * @param dateColumn the column representing the date of the ping logs.
     * @param pingParentColumn the columns representing the time of the ping logs.
     */
    private void saveLogs(TableColumn<PingRow, String> dateColumn, TableColumn<PingRow, String> pingParentColumn) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setInitialFileName("pingrecord.log");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log Files (*.log)", "*.log"));

        Stage stage = new Stage();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(dateColumn.getText());

                for (TableColumn<PingRow, ?> childColumn : pingParentColumn.getColumns()) {
                    writer.write(",");
                    writer.write(childColumn.getText());
                }
                writer.newLine();

                ObservableList<PingRow> items = tableView.getItems();
                for (PingRow log : items) {
                    writer.write(log.getDateTime().format(DateTimeFormatter.ofPattern(DATE_FORMAT)));

                    for (TableColumn<PingRow, ?> childColumn : pingParentColumn.getColumns()) {
                        writer.write(",");
                        Object value = childColumn.getCellData(log);
                        writer.write(value != null ? value.toString() : "");
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                logger.error("Error saving logs to file", e);
            }
        }
    }

    /**
     * Opens a confirmation dialog to clear the ping log file. If the user confirms,
     * this method triggers the `clearLogsAction` method, which calls the service
     * to clear the log file and updates the displayed tables.
     * @param event The ActionEvent triggered by the user interaction, used to obtain the current stage.
     * @throws IOException if there is an error loading the confirmation dialog.
     */
    @FXML
    private void clearLogs(ActionEvent event) throws IOException {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ModalController modalController = new ModalController();
        modalController.setAction(this::clearLogsAction);
        modalController.showModal(ownerStage, "/fxml/confirmClearLogDialog.fxml", "Clear Log Confirmation", 300,150);
    }

    private void clearLogsAction() {
        try {
            connTestService.clearPingLogFile();
            Platform.runLater(this::updateTables);
        } catch (InterruptedException e) {
            logger.error("Error clearing ping log file", e);
        }
    }


    /**
     * Opens the user manual in a new window
     */
    @FXML
    private void openUserManual(ActionEvent event) throws IOException {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ModalController modalController = new ModalController();
        modalController.showModal(ownerStage, "/fxml/usermanual.fxml", "User Manual", 300, 400);
    }

    /**
     * Opens a modal dialog to allow the user to set up to one, two or three custom IP addresses
     * @param event The ActionEvent triggered by the user interaction, used to obtain the current stage.
     * @throws IOException if there is an error loading the modal dialog.
     */
    @FXML
    private void setCustomIps(ActionEvent event) throws IOException {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        CustomIPModalController customIPModalController = applicationContext.getBean(CustomIPModalController.class);
        customIPModalController.setAction(() -> setCustomIpsAction(customIPModalController.getIpList()));
        customIPModalController.showModal(ownerStage, "/fxml/customIPsDialog.fxml", "Set Custom IPs", 400, 300);
        customIPModalController.populateIpFields();
    }

    private void setCustomIpsAction(List<String> ipList) {
        if(ipList.isEmpty()){
            autoDiscoveryButton.setSelected(true);
        }else {
            customIpsButton.setSelected(true);
            this.ipAddress = ipList;
            setTables();
            Platform.runLater(this::updateTables);
        }
    }

    @FXML
    private void setAutoDiscovery(){
        autoDiscoveryButton.setSelected(true);
    }


    /**
     * Closes the current stage.
     */
    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
        System.exit(0);
    }

    /**
     * Minimizes the current stage.
     */
    @FXML
    private void handleMinimize(ActionEvent event) {
        Stage stage = (Stage) minimizeButton.getScene().getWindow();
        stage.setIconified(true);
    }

    /**
     * Maximizes or restores the current stage and updates the button image.
     */
    @FXML
    private void handleMaximizeRestore(ActionEvent event) {
        Stage stage = (Stage) maximizeRestoreButton.getScene().getWindow();
        if (stage.isMaximized()) {
            stage.setMaximized(false);
            ImageView maximizeRestorebutton2 = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/maximizeRestorebutton1.png"))));
            maximizeRestorebutton2.setFitHeight(18);
            maximizeRestorebutton2.setFitWidth(18);
            maximizeRestoreButton.setGraphic(maximizeRestorebutton2);
        } else {
            stage.setMaximized(true);
            ImageView maximizeRestorebutton1 = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/maximizeRestorebutton2.png"))));
            maximizeRestorebutton1.setFitHeight(18);
            maximizeRestorebutton1.setFitWidth(18);
            maximizeRestoreButton.setGraphic(maximizeRestorebutton1);
        }
    }

}