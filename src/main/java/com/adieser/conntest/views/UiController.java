package com.adieser.conntest.views;

import com.adieser.conntest.controllers.responses.PingSessionExtract;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.service.ConnTestService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.springframework.stereotype.Component;
import org.slf4j.Logger;

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
    List<String> ipAddress;
    @FXML
    private Button closeButton;
    @FXML
    private Button minimizeButton;
    @FXML
    private Button maximizeRestoreButton;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ComboBox<Integer> timeChoiceBox;
    Integer timechoice = 5;
    @FXML
    private CheckBox dayFilterBox;

    @FXML
    private VBox tablesVBox;

    TableView<PingRow> tableView = new TableView<>();
    TableView<String> footerTable = new TableView<>();

    private final List<TableColumn<String, String>> pingCountColumnList = new ArrayList<>();
    private final List<TableColumn<String, String>> averageLostColumnList = new ArrayList<>();


    @Autowired
    public UiController(ConnTestService connTestService, Logger logger, ScheduledExecutorService executorService) {
        this.connTestService = connTestService;
        this.logger = logger;
        this.executorService = executorService;
    }

    /**
     * Initializes UI components and sets up event handlers
     */
    @FXML
    private void initialize() {
        stopButton.setDisable(true);
        timeChoiceBox.getItems().addAll(1, 5, 10, 15);
        this.startButton.setOnAction(actionEvent -> {
            startButton.setDisable(true);
            progressIndicator.setVisible(true);
            dayFilterBox.setDisable(true);
            if(timeChoiceBox.getValue() != null) timechoice = timeChoiceBox.getValue();
            Thread startThread = new Thread(this::start);
            startThread.start();
        });
        this.stopButton.setOnAction(actionEvent -> {
            stopButton.setDisable(true);
            progressIndicator.setVisible(false);
            this.stop();
            startButton.setDisable(false);
            dayFilterBox.setDisable(false);
        });
        this.timeChoiceBox.setOnAction(actionEvent -> {
            if(startButton.isDisable()){
                stopExecutorService();
                timechoice = timeChoiceBox.getValue();
                createExecutorService();
                startExecutorService(timechoice);
            }
        });
        this.dayFilterBox.setOnAction(actionEvent -> { if(ipAddress != null) Platform.runLater(this::updateTables); });
        this.closeButton.setOnAction(event -> handleClose());
        this.minimizeButton.setOnAction(event -> handleMinimize());
        this.maximizeRestoreButton.setOnAction(event -> handleMaximizeRestore());
    }

    /**
     * Initializes a ping session, sets up the table view if is not already set, creates and starts a Scheduled Executor
     * to load logs and update average lost pings continuously, and updates visual controls afterward.
     */
    public void start() {
        connTestService.testLocalISPInternet();
        ipAddress = connTestService.getIpAddressesFromActiveTests();

        if (tableView.getColumns().isEmpty()) setTables();

        createExecutorService();
        startExecutorService(timechoice);

        updateVisualControls();
    }

    /**
     * Updates the visual controls by hiding the progress indicator and enabling the stop button.
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
    void startExecutorService(Integer timechoice){
        executorService.scheduleAtFixedRate(() -> Platform.runLater(this::updateTables), 0, timechoice, TimeUnit.SECONDS);
    }

    /**
     * Load logs, set the average of lost pings and ping count for each ip.
     * */
    void updateTables(){
        try {
            long amountOfPings;
            List<List<PingLog>> allPingLogs = new ArrayList<>();

            for (int i = 0; i < Math.min(ipAddress.size(), 3); i++) {
                amountOfPings = loadLogs(ipAddress.get(i), allPingLogs);
                setAverageLost(ipAddress.get(i), averageLostColumnList.get(i));
                setPingCount(pingCountColumnList.get(i), amountOfPings);
            }

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
        stopExecutorService();
        connTestService.stopTests();
    }

    /**
     * Shuts down the scheduled executor service
     */
    public void stopExecutorService() {
        if (executorService != null && !executorService.isShutdown()) executorService.shutdownNow();
    }

    /**
     * Sets up the TableView and Footer Table to display ping data for multiple IPs, including a save button,
     * and updates the UI.
     */
    public void setTables() {
        Platform.runLater(() -> tablesVBox.getChildren().clear());
        averageLostColumnList.clear();
        pingCountColumnList.clear();

        // Table setup
        TableColumn<PingRow, String> dateTimeColumn = new TableColumn<>("Date");
        dateTimeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        dateTimeColumn.setPrefWidth(150);
        TableColumn<PingRow, String> allPingTimeColumn = new TableColumn<>("Ping Time (ms)");

        tableView.getColumns().add(dateTimeColumn);
        tableView.getStyleClass().add("table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setFocusTraversable(false);

        // Footer table setup
        TableColumn<String, String> averageLostColumn = new TableColumn<>("Average Lost");
        averageLostColumn.setPrefWidth(150);
        TableColumn<String, String> pingCountColumn = new TableColumn<>("PingCount");
        pingCountColumn.getColumns().add(averageLostColumn);

        footerTable.getColumns().add(pingCountColumn);
        footerTable.setSelectionModel(null);
        footerTable.setItems(FXCollections.observableList(new ArrayList<>(Collections.nCopies(ipAddress.size(), ""))));
        footerTable.getStyleClass().add("footer_table");
        footerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Save button setup
        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> saveLogs(dateTimeColumn, allPingTimeColumn));
        saveButton.getStyleClass().add("save-button");

        //Ping columns and label columns setup
        for (int i = 0; i < Math.min(ipAddress.size(), 3); i++) {
            buildPingsColumn(ipAddress.get(i), allPingTimeColumn, i);
            setLabels();
        }
        tableView.getColumns().add(allPingTimeColumn);

        //Get all together
        VBox tableVBox = new VBox(tableView, footerTable,saveButton);
        tableVBox.getStyleClass().add("table-vbox");
        Platform.runLater(() -> tablesVBox.getChildren().add(tableVBox));
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
     * Builds a table view with labels for a specified IP address, including
     * date and ping time columns, and a save button
     */
    private void setLabels() {
        TableColumn<String, String> resultALColumn = new TableColumn<>("");
        averageLostColumnList.add(resultALColumn);
        resultALColumn.setPrefWidth(150);
        TableColumn<String, String> resultPCColumn = new TableColumn<>("");
        pingCountColumnList.add(resultPCColumn);
        resultPCColumn.getColumns().add(resultALColumn);

        footerTable.getColumns().add(resultPCColumn);
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
     * Carga los registros de ping para cada dirección IP y los agrega a la tabla unificada.
     */
    public long loadLogs(String ip, List<List<PingLog>> allPingLogs) throws IOException {
        PingSessionExtract pingLogs = !dayFilterBox.isSelected()
                ? connTestService.getPingsByIp(ip)
                : connTestService.getPingsByDateTimeRangeByIp(getStartOfDay(), getEndOfDay(), ip);

        allPingLogs.add(pingLogs.getPingLogs());
        return pingLogs.getAmountOfPings();
    }

    public List<PingRow> combinePingsByDateTime(List<List<PingLog>> pingLogsByIp) {
        Map<LocalDateTime, List<String>> combinedMap = new TreeMap<>();

        for (List<PingLog> pingLogs : pingLogsByIp) {
            for (PingLog pingLog : pingLogs) {
                LocalDateTime dateTime = pingLog.getDateTime();
                String pingTime = String.valueOf(pingLog.getPingTime());

                // Si el dateTime ya está en el mapa, agregamos el tiempo de ping
                combinedMap.putIfAbsent(dateTime, new ArrayList<>());
                combinedMap.get(dateTime).add(pingTime);
            }
        }

        List<PingRow> pingRows = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<String>> entry : combinedMap.entrySet()) {
            LocalDateTime dateTime = entry.getKey();
            List<String> pingTimes = entry.getValue();

            while (pingTimes.size() < pingLogsByIp.size()) {pingTimes.add("...");}

            pingRows.add(new PingRow(dateTime, pingTimes));
        }

        return pingRows;
    }

    /**
     * Obtiene el inicio del día actual.
     */
    private LocalDateTime getStartOfDay() {
        return LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Obtiene el final del día actual.
     */
    private LocalDateTime getEndOfDay() {
        return LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }
    /**
     * Sets the average of lost pings for each IP address to corresponding labels.
     */
    public void setAverageLost(String ip, TableColumn<String, String> column) throws IOException {
        if (!dayFilterBox.isSelected()) {
            column.setText(connTestService.getPingsLostAvgByIp(ip) + "%");
        } else {
            LocalDateTime currentDayStartTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime currentDayEndTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            column.setText(connTestService.getPingsLostAvgByDateTimeRangeByIp(currentDayStartTime, currentDayEndTime, ip) + "%");
        }
    }

    /**
     * Sets the ping count for each IP address to corresponding labels
     */
    public void setPingCount(TableColumn<String, String> column, long amountOfPings) {
        column.setText(String.valueOf(amountOfPings));
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
                writer.write("\t\t\t");

                for (TableColumn<PingRow, ?> childColumn : pingParentColumn.getColumns()) {
                    writer.write(childColumn.getText());
                    writer.write("\t");
                }
                writer.newLine();

                ObservableList<PingRow> items = tableView.getItems();
                for (PingRow log : items) {
                    writer.write(log.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    writer.write("\t\t");

                    for (TableColumn<PingRow, ?> childColumn : pingParentColumn.getColumns()) {
                        Object value = childColumn.getCellData(log);
                        writer.write(value != null ? value.toString() : "");
                        writer.write("\t");
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                logger.error("Error saving logs to file", e);
            }
        }
    }


    /**
     * Closes the current stage.
     */
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Minimizes the current stage.
     */
    private void handleMinimize() {
        Stage stage = (Stage) minimizeButton.getScene().getWindow();
        stage.setIconified(true);
    }

    /**
     * Maximizes or restores the current stage and updates the button image.
     */
    private void handleMaximizeRestore() {
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
