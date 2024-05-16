package com.adieser.conntest.views;

import com.adieser.conntest.controllers.responses.PingSessionExtract;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.service.ConnTestService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class UiController {
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
    private Label labelTable1;
    @FXML
    private Label labelTable2;
    @FXML
    private Label labelTable3;
    @FXML
    private Label labelLostAvg1;
    @FXML
    private Label labelLostAvg2;
    @FXML
    private Label labelLostAvg3;
    @FXML
    private TableView<PingLog> localTableView;
    @FXML
    private TableColumn<PingLog, String> dateLocalColumn;
    @FXML
    private TableColumn<PingLog, Long> pingLocalColumn;
    @FXML
    private TableView<PingLog> ispTableView;
    @FXML
    private TableColumn<PingLog, String> dateIspColumn;
    @FXML
    private TableColumn<PingLog, Long> pingIspColumn;
    @FXML
    private TableView<PingLog> cloudTableView;
    @FXML
    private TableColumn<PingLog, String> dateCloudColumn;
    @FXML
    private TableColumn<PingLog, Long> pingCloudColumn;
    private static final String COLUMN_NAME_DATE = "dateTime";
    private static final String COLUMN_NAME_TIME = "pingTime";
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    @FXML
    private Button saveLocalButton;
    @FXML
    private Button saveIspButton;
    @FXML
    private Button saveCloudButton;
    public final ConnTestService connTestService;
    private ScheduledExecutorService executorService;
    public final Logger logger;
    List<String> ipAddress;

    @Autowired
    public UiController(ConnTestService connTestService, Logger logger, ScheduledExecutorService executorService) {
        this.connTestService = connTestService;
        this.logger = logger;
        this.executorService = executorService;
    }

    /**
     * Initializes UI components and sets up event handlers.
     */
    @FXML
    private void initialize() {
        stopButton.setDisable(true);
        timeChoiceBox.getItems().addAll(1, 5, 10, 15);
        this.startButton.setOnAction(actionEvent -> {
            startButton.setDisable(true);
            progressIndicator.setVisible(true);
            dayFilterBox.setDisable(true);
            if(timeChoiceBox.getValue() != null){
                timechoice = timeChoiceBox.getValue();
            }
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
                if (executorService != null && !executorService.isShutdown()) {
                    executorService.shutdownNow();
                }
                timechoice = timeChoiceBox.getValue();
                createExecutorService();
                startExecutorService(timechoice);
            }
        });
        this.dayFilterBox.setOnAction(actionEvent -> {
            loadLogs();
            setAverageLost();
        });
        this.saveLocalButton.setOnAction(actionEvent -> saveLogs(localTableView, dateLocalColumn, pingLocalColumn));
        this.saveIspButton.setOnAction(actionEvent -> saveLogs(ispTableView, dateIspColumn, pingIspColumn));
        this.saveCloudButton.setOnAction(actionEvent -> saveLogs(cloudTableView, dateCloudColumn, pingCloudColumn));
        this.closeButton.setOnAction(event -> handleClose());
        this.minimizeButton.setOnAction(event -> handleMinimize());
        this.maximizeRestoreButton.setOnAction(event -> handleMaximizeRestore());
    }

    /**
     * Initiates a ping session and starts Scheduled Executor to load logs and set the average of lost pings in a loop.
     */
    public void start() {
        connTestService.testLocalISPInternet();
        ipAddress = connTestService.getIpAddressesFromActiveTests();
        updateVisualControls();
        setIPLabels();
        createExecutorService();
        startExecutorService(timechoice);
    }

    /**
     * Creates a single-threaded scheduled executor.
     */
    void createExecutorService() {
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts the executor service to load logs and set the average of lost pings.
     * */
    void startExecutorService(Integer timechoice){
        executorService.scheduleAtFixedRate(() -> {
            loadLogs();
            setAverageLost();
        }, 0, timechoice, TimeUnit.SECONDS);
    }

    /**
     * Updates the visual controls by hiding the progress indicator and enabling the stop button.
     */
    public void updateVisualControls(){
        progressIndicator.setVisible(false);
        stopButton.setDisable(false);
    }

    /**
     * Shuts down the scheduled executor service and stops all tests.
     * The first one includes log loading and the set of average lost
     */
    public void stop() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        connTestService.stopTests();
    }

    /**
     * Builds tables and loads ping logs for each table view
     */
    public void loadLogs() {
        buildTable(dateLocalColumn, pingLocalColumn);
        buildTable(dateIspColumn, pingIspColumn);
        buildTable(dateCloudColumn, pingCloudColumn);

        LocalDateTime currentDayStartTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime currentDayEndTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        for (int i = 0; i < Math.min(ipAddress.size(), 3); i++) {
            String ip = ipAddress.get(i);
            TableView<PingLog> tableView = switch (i) {
                case 0 -> localTableView;
                case 1 -> ispTableView;
                case 2 -> cloudTableView;
                default -> null;
            };
            if (tableView != null && ip != null) {
                if (dayFilterBox.isSelected()) {
                    addPingLogsToTableView(connTestService.getPingsByDateTimeRangeByIp(currentDayStartTime, currentDayEndTime, ip), tableView);
                } else {
                    addPingLogsToTableView(connTestService.getPingsByIp(ip), tableView);
                }
            }
        }
    }

    /**
     * Configures the date and ping time columns in each TableView.
     */
    private void buildTable(TableColumn<PingLog, String> dateColumn, TableColumn<PingLog, Long> pingColumn){
        dateColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_DATE));
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN))));
        pingColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_TIME));
        setCellValueFactory(pingColumn);
    }

    /**
     * Sets up a custom cell value factory for a TableColumn representing ping times.
     * If the ping time is -1, the text color is set to red
     * @param pingColumn updated table column
     */
    private void setCellValueFactory(TableColumn<PingLog, Long> pingColumn) {
        pingColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setTextFill(getColorForItem(item));
                setText(empty ? null : getString());
                setFont(Font.font("", FontWeight.BOLD, 12));
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
     * Adds the ping logs to the specified TableView.
     * @param pingLogsIp list of pings
     * @param tableView the TableView where the ping logs will be displayed.
     */
    private void addPingLogsToTableView(PingSessionExtract pingLogsIp, TableView<PingLog> tableView) {
        if (pingLogsIp != null) {
            List<PingLog> reversedPingLogs = new ArrayList<>(pingLogsIp.getPingLogs());
            Collections.reverse(reversedPingLogs);
            ObservableList<PingLog> tableViewItems = tableView.getItems();

            Platform.runLater(() -> tableViewItems.setAll(reversedPingLogs));
        }
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
                    writer.write(log.getDateTime().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
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
     * Sets the average of lost pings for each IP address to corresponding labels.
     */
    public void setAverageLost() {
        final String text = "Average lost pings: ";
        LocalDateTime currentDayStartTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime currentDayEndTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        Platform.runLater(() -> {
            for (int i = 0; i < Math.min(ipAddress.size(), 3); i++) {
                if (ipAddress.get(i) != null) {
                    Label label = switch (i) {
                        case 0 -> labelLostAvg1;
                        case 1 -> labelLostAvg2;
                        case 2 -> labelLostAvg3;
                        default -> null;
                    };
                    if (label != null) {
                        if (dayFilterBox.isSelected()) {
                            label.setText(text + connTestService.getPingsLostAvgByDateTimeRangeByIp(currentDayStartTime, currentDayEndTime, ipAddress.get(i)) + "%");
                        } else {
                            label.setText(text + connTestService.getPingsLostAvgByIp(ipAddress.get(i)) + "%");
                        }
                    }
                }
            }
        });
    }

    /**
     * Sets the average of lost pings for each IP address to corresponding labels.
     */
    public void setIPLabels(){
        Platform.runLater(() -> {
            if (!ipAddress.isEmpty() && ipAddress.get(0) != null) {
                labelTable1.setText("Pings to Local (" + ipAddress.get(0) + ")");
            }
            if (ipAddress.size() > 1 && ipAddress.get(1) != null) {
                labelTable2.setText("Pings to Isp (" + ipAddress.get(1) + ")");
            }
            labelTable3.setText("Pings to the Internet (8.8.8.8)");
        });
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
