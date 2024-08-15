package com.adieser.conntest.views;

import com.adieser.conntest.controllers.responses.PingSessionExtract;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.service.ConnTestService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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
    private HBox tablesHBox;

    private final List<Label> pingCountLabelsList = new ArrayList<>();
    private final List<Label> averageLostLabelsList = new ArrayList<>();
    private final List<TableView<PingLog>> tablesViewList = new ArrayList<>();


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
     * Initializes a ping session, sets up table views if they are not already set, creates and starts a Scheduled Executor
     * to load logs and update average lost pings continuously, and updates visual controls afterward.
     */
    public void start() {
        connTestService.testLocalISPInternet();
        ipAddress = connTestService.getIpAddressesFromActiveTests();

        if (tablesViewList.isEmpty()) setTables();

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
     * Starts the executor service to update the tables
     * */
    void startExecutorService(Integer timechoice){
        executorService.scheduleAtFixedRate(() -> Platform.runLater(this::updateTables), 0, timechoice, TimeUnit.SECONDS);
    }

    /**
     * Load logs and set the average of lost pings for each table.
     * */
    void updateTables(){
        try {
            for (int i = 0; i < Math.min(ipAddress.size(), 3); i++) {
                loadLogs(ipAddress.get(i), tablesViewList.get(i));
                setAverageLost(ipAddress.get(i), averageLostLabelsList.get(i));
                setPingCount(tablesViewList.get(i), pingCountLabelsList.get(i));
            }
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
     * Clears existing table views and labels, then builds and displays new tables
     * This method calls buildTableWithLabels() for each table views
     */
    public void setTables() {
        Platform.runLater(() -> tablesHBox.getChildren().clear());
        tablesViewList.clear();
        averageLostLabelsList.clear();
        for (int i = 0; i < Math.min(ipAddress.size(), 3); i++) {
            buildTableWithLabels(ipAddress.get(i));
        }
    }

    /**
     * Builds a table view with labels for a specified IP address, including
     * date and ping time columns, and a save button
     * @param ip The IP address for which the table is built.
     */
    private void buildTableWithLabels(String ip) {
        // Title and average lost labels setup
        Label titleLabel = new Label("Pings to " + ip);
        titleLabel.getStyleClass().add("h2-label");

        Label pingCountLabel = new Label("Ping count: ");
        pingCountLabel.getStyleClass().add("h3-label");

        Label averageLostLabel = new Label("Average lost pings: ");
        averageLostLabel.getStyleClass().add("h3-label");

        // Table view setup
        TableView<PingLog> table = new TableView<>();
        TableColumn<PingLog, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        dateColumn.setPrefWidth(150);
        table.getColumns().add(dateColumn);

        TableColumn<PingLog, Long> pingColumn = new TableColumn<>("Ping Time (ms)");
        pingColumn.setCellValueFactory(new PropertyValueFactory<>("pingTime"));
        pingColumn.setPrefWidth(100);
        setPingTimeCellColor(pingColumn);
        table.getColumns().add(pingColumn);

        table.getStyleClass().add("table");
        table.setFocusTraversable(false);

        // Save button setup
        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> saveLogs(table, dateColumn, pingColumn));
        saveButton.getStyleClass().add("save-button");

        //VBox setup
        VBox tableVBox = new VBox(titleLabel, pingCountLabel, averageLostLabel, table, saveButton);
        tableVBox.getStyleClass().add("table-vbox");
        VBox.setMargin(saveButton, new Insets(10,0,0,0));

        pingCountLabelsList.add(pingCountLabel);
        averageLostLabelsList.add(averageLostLabel);
        tablesViewList.add(table);
        Platform.runLater(() -> tablesHBox.getChildren().add(tableVBox));
    }

    /**
     * Sets up a custom cell value factory for a TableColumn representing ping times
     * If the ping time is -1, the text color is set to red
     * If the ping time is less than or equal to 20, the text color is set to green
     * If the ping time is between 300 and 1000, the text color is set to yellow
     * If the ping time is greater than or equal to 1000, the text color is set to orange
     * @param pingColumn updated table column
     */
    private void setPingTimeCellColor(TableColumn<PingLog, Long> pingColumn) {
        pingColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setTextFill(getColorForItem(item));
                setText(empty ? null : getString());
                setFont(Font.font("", FontWeight.BOLD, 12));
                setAlignment(Pos.CENTER);
            }

            private String getString() {
                return getItem() == null ? "" : getItem().toString();
            }

            private Color getColorForItem(Long item) {
                if (item == null || item.equals(-1L)) {
                    return Color.RED;
                } else if (item <= 20) {
                    return Color.web("#2E8B57");
                } else if (item >= 300 && item <= 1000) {
                    return Color.web("#FFD700");
                } else if (item >= 1000) {
                    return Color.ORANGE;
                }else {
                    return Color.web("#4c4c4c");
                }
            }
        });
    }

    /**
     * Loads ping logs for each table view
     */
    public void loadLogs(String ip, TableView<PingLog> table) throws IOException {
        if (!dayFilterBox.isSelected()) {
            addPingLogsToTableView(connTestService.getPingsByIp(ip),table);
        } else {
            LocalDateTime currentDayStartTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime currentDayEndTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            addPingLogsToTableView(connTestService.getPingsByDateTimeRangeByIp(currentDayStartTime, currentDayEndTime, ip), table);
        }
    }

    /**
     * Adds the ping logs to the specified TableView.
     * @param pingLogsIp list of pings
     * @param tableView the TableView where the ping logs will be displayed.
     */
    private void addPingLogsToTableView(PingSessionExtract pingLogsIp, TableView<PingLog> tableView) {
        if (pingLogsIp != null) {
            List<PingLog> reversedPingLogs = new ArrayList<>(pingLogsIp.getPingLogs());
            Collections.reverse(reversedPingLogs);
            ObservableList<PingLog> tableViewItems = tableView.getItems();

           tableViewItems.setAll(reversedPingLogs);
        }
    }

    /**
     * Sets the average of lost pings for each IP address to corresponding labels.
     */
    public void setAverageLost(String ip, Label label) throws IOException {
        final String text = "Average lost pings: ";
            if (!dayFilterBox.isSelected()) {
                label.setText(text + connTestService.getPingsLostAvgByIp(ip) + "%");
            } else {
                LocalDateTime currentDayStartTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime currentDayEndTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
                label.setText(text + connTestService.getPingsLostAvgByDateTimeRangeByIp(currentDayStartTime, currentDayEndTime, ip) + "%");
            }
    }

    /**
     * Sets the ping count for each IP address to corresponding labels.
     */
    public void setPingCount(TableView<PingLog> tableView, Label label) {
        final String text = "Ping count: ";
        label.setText(text + tableView.getItems().size());
    }

    /**
     * Saves the ping logs displayed in the TableView to a .log file.
     * @param tableView  the TableView containing the ping logs to be saved.
     * @param dateColumn the column representing the date of the ping logs.
     * @param pingColumn the column representing the time of the ping logs.
     */
    private void saveLogs(TableView<PingLog> tableView, TableColumn<PingLog, String> dateColumn, TableColumn<PingLog, Long> pingColumn) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as");
        fileChooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        fileChooser.setInitialFileName("pingrecord.log");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de registro (*.log)", "*.log"));

        Stage stage = new Stage();
        java.io.File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(dateColumn.getText());
                writer.write("\t\t\t");
                writer.write(pingColumn.getText());
                writer.newLine();

                ObservableList<PingLog> items = tableView.getItems();
                for (PingLog log : items) {
                    writer.write(log.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    writer.write("\t");
                    writer.write(String.valueOf(log.getPingTime()));
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
