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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;

import java.io.*;
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
    public Button startButton;
    @FXML
    public Button stopButton;
    @FXML
    private ComboBox<Integer> timeChoiceBox;
    @FXML
    private Label labelLostAvg1;
    @FXML
    private Label labelLostAvg2;
    @FXML
    private Label labelLostAvg3;
    @FXML
    public TableView<PingLog> localTableView;
    @FXML
    private TableColumn<PingLog, String> dateLocalColumn;
    @FXML
    private TableColumn<PingLog, Long> pingLocalColumn;
    @FXML
    public TableView<PingLog> ispTableView;
    @FXML
    private TableColumn<PingLog, String> dateIspColumn;
    @FXML
    private TableColumn<PingLog, Long> pingIspColumn;
    @FXML
    public TableView<PingLog> cloudTableView;
    @FXML
    private TableColumn<PingLog, String> dateCloudColumn;
    @FXML
    private TableColumn<PingLog, Long> pingCloudColumn;
    @FXML
    public Button saveLocalButton;
    @FXML
    public Button saveIspButton;
    @FXML
    public Button saveCloudButton;

    private final ConnTestService connTestService;
    private ScheduledExecutorService executorService;

    @FXML
    private ProgressIndicator progressIndicator;

    public static final String COLUMN_NAME_DATE = "dateTime";
    public static final String COLUMN_NAME_TIME = "pingTime";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final Logger logger;

    List<String> ipAddress;

    @Autowired
    public UiController(ConnTestService connTestService, Logger logger) {
        this.connTestService = connTestService;
        this.logger = logger;
    }

    @FXML
    public void initialize() {
        stopButton.setDisable(true);
        timeChoiceBox.getItems().addAll(1, 5, 10, 15);
        this.startButton.setOnAction(actionEvent -> {

            startButton.setDisable(true);
            progressIndicator.setVisible(true);
            timeChoiceBox.setDisable(true);
            stopButton.setDisable(false);
            Integer timechoice;
            if(timeChoiceBox.getValue() == null){
                timechoice = 5;
            }else {
                timechoice = timeChoiceBox.getValue();
            }

            Thread startThread = new Thread(() -> start(timechoice));
            startThread.start();

        });
        this.stopButton.setOnAction(actionEvent -> {
            stopButton.setDisable(true);
            progressIndicator.setVisible(false);
            this.stop();
            startButton.setDisable(false);
            timeChoiceBox.setDisable(false);
        });
        this.saveLocalButton.setOnAction(actionEvent -> saveLogs(localTableView, dateLocalColumn, pingLocalColumn));
        this.saveIspButton.setOnAction(actionEvent -> saveLogs(ispTableView, dateIspColumn, pingIspColumn));
        this.saveCloudButton.setOnAction(actionEvent -> saveLogs(cloudTableView, dateCloudColumn, pingCloudColumn));
        this.closeButton.setOnAction(event -> handleClose());
        this.minimizeButton.setOnAction(event -> handleMinimize());
        this.maximizeRestoreButton.setOnAction(event -> handleMaximizeRestore());
    }

    public void start(Integer timechoice) {
        connTestService.testLocalISPInternet();
        ipAddress = connTestService.getIpAddressesFromActiveTests();
        progressIndicator.setVisible(false);

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            loadLogs(ipAddress);
            setAverageLost(ipAddress);
        }, 0, timechoice, TimeUnit.SECONDS);
    }

    public void stop() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        connTestService.stopTests();
    }

    public void loadLogs(List<String> ipAddress) {
        buildTable();

        localTableView.getItems().clear();
        addPingLogsToTableView(connTestService.getPingsByIp(ipAddress.get(0)), localTableView);
        ispTableView.getItems().clear();
        addPingLogsToTableView(connTestService.getPingsByIp(ipAddress.get(1)), ispTableView);
        cloudTableView.getItems().clear();
        addPingLogsToTableView(connTestService.getPingsByIp("8.8.8.8"), cloudTableView);
    }

    private void buildTable(){
        dateLocalColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_DATE));
        dateLocalColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN))));
        pingLocalColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_TIME));

        dateIspColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_DATE));
        dateIspColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN))));
        pingIspColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_TIME));

        dateCloudColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_DATE));
        dateCloudColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN))));
        pingCloudColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_TIME));
    }

    private void addPingLogsToTableView(PingSessionExtract pingLogsIp, TableView<PingLog> tableView) {
        if (pingLogsIp != null) {
            List<PingLog> reversedPingLogs = new ArrayList<>(pingLogsIp.getPingLogs());
            Collections.reverse(reversedPingLogs);

            for (PingLog pingLog : reversedPingLogs) {
                if (pingLog != null) {
                    tableView.getItems().add(pingLog);
                }
            }
        }
    }


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

    public void setAverageLost(List<String> ipAddress){
        final String text = "Average lost pings: ";
        Platform.runLater(() -> {
            if (!ipAddress.isEmpty() && ipAddress.get(0) != null) {
                labelLostAvg1.setText(text + connTestService.getPingsLostAvgByIp(ipAddress.get(0)));
            }
            if (ipAddress.size() > 1 && ipAddress.get(1) != null) {
                labelLostAvg2.setText(text + connTestService.getPingsLostAvgByIp(ipAddress.get(1)));
            }
            labelLostAvg3.setText(text + connTestService.getPingsLostAvgByIp("8.8.8.8"));
        });
    }

    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void handleMinimize() {
        Stage stage = (Stage) minimizeButton.getScene().getWindow();
        stage.setIconified(true);
    }

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
