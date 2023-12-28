package com.adieser.conntest.views;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
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
    private Button resizeButton;
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

    //private double xOffset = 0;
    //private double yOffset = 0;

    public static final String COLUMN_NAME_DATE = "dateTime";
    public static final String COLUMN_NAME_TIME = "pingTime";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final Logger logger;

    @Value("${conntest.pinglogs.path}")
    Resource logFileName;

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
        this.resizeButton.setOnAction(event -> handleResize());
    }

    public void start(Integer timechoice) {
        /*Thread connectionThread = new Thread(() -> connTestService.testLocalISPInternet());
        connectionThread.start();

        try {
            connectionThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

         */
        connTestService.testLocalISPInternet();

        List<String> ipAddress = getIps();
        progressIndicator.setVisible(false);

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            readLog(ipAddress);
            setAverageLost(ipAddress);
        }, 0, timechoice, TimeUnit.SECONDS);
    }

    public void stop() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        connTestService.stopTests();
    }

    public void buildTable(){
        dateLocalColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_DATE));
        dateLocalColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN))));
        pingLocalColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_TIME));
        /*
        Platform.runLater(() -> {
            try {
                cl_ping_local.setCellFactory(column -> { return new TableCell<PingLog, Long>() {
                        @Override
                        protected void updateItem(Long item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setText(null);
                                setStyle("");
                            } else {
                                setText(String.valueOf(item));
                                if (item == -1) {
                                    setStyle("-fx-text-fill: red;");
                                } else {
                                    setStyle("");
                                }
                            }
                        }
                    };
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        */
        dateIspColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_DATE));
        dateIspColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN))));
        pingIspColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_TIME));
        /*
        Platform.runLater(() ->{
             try {
                cl_ping_isp.setCellFactory(column -> { return new TableCell<PingLog, Long>() {
                        @Override
                        protected void updateItem(Long item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setText(null);
                                setStyle("");
                            } else {
                                setText(String.valueOf(item));
                                if (item == -1) {
                                    setStyle("-fx-text-fill: red;");
                                } else {
                                    setStyle("");
                                }
                            }
                        }
                };});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        */
        dateCloudColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_DATE));
        dateCloudColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN))));
        pingCloudColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME_TIME));
        /*
        Platform.runLater(() -> {
            try {
                cl_ping_cloud.setCellFactory(column -> { return new TableCell<PingLog, Long>() {
                    @Override
                    protected void updateItem(Long item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(String.valueOf(item));
                            if (item == -1) {
                                setStyle("-fx-text-fill: red;");
                            } else {
                                setStyle("");
                            }
                        }
                    }
                };
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        */


    }

    public void readLog(List<String> ipAddress){
        LinkedList<PingLog> pingLogsIpLocal = new LinkedList<>();
        LinkedList<PingLog> pingLogsIpIsp = new LinkedList<>();
        LinkedList<PingLog> pingLogsIpCloud = new LinkedList<>();

        buildTable();

        try (BufferedReader br = new BufferedReader(new FileReader(logFileName + "/ping.log"))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts  = line.split(",");

                PingLog pingLog = new PingLog();

                pingLog.setDateTime(LocalDateTime.parse(parts[0].trim(), DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
                pingLog.setIpAddress(parts[1].trim());
                pingLog.setPingTime(Long.parseLong(parts[2].trim()));


                if (Objects.equals(!ipAddress.isEmpty() ? ipAddress.get(0) : null, pingLog.getIpAddress())) {
                    pingLogsIpLocal.addFirst(pingLog);
                } else if (Objects.equals(ipAddress.size() > 1 ? ipAddress.get(1) : null, pingLog.getIpAddress())) {
                    pingLogsIpIsp.addFirst(pingLog);
                } else if (Objects.equals("8.8.8.8", pingLog.getIpAddress())) {
                    pingLogsIpCloud.addFirst(pingLog);
                }
            }

        } catch (IOException e) {
            logger.error("Error reading logs", e);
        }

        localTableView.getItems().clear();
        ispTableView.getItems().clear();
        cloudTableView.getItems().clear();

        loadData(pingLogsIpLocal, pingLogsIpIsp, pingLogsIpCloud);

    }

    void loadData(LinkedList<PingLog> pingLogsIpLocal, LinkedList<PingLog> pingLogsIpIsp, LinkedList<PingLog> pingLogsIpCloud){
        for (PingLog pl : pingLogsIpLocal)
            if (pl != null) localTableView.getItems().add(pl);
        for (PingLog pl : pingLogsIpIsp){ if (pl != null) { ispTableView.getItems().add(pl);}}
        for (PingLog pl : pingLogsIpCloud){ if (pl != null) { cloudTableView.getItems().add(pl);}}
    }

    public List<String> getIps() {
        return connTestService.getLocalAndISPIpAddresses();
    }

    public void saveLogs(TableView<PingLog> tableView, TableColumn<PingLog, String> dateColumn, TableColumn<PingLog, Long> pingColumn) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar como");
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
        final String text = "Average lost pings:";
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

    private void handleResize() {
        /* Stage stage = (Stage) button_resize.getScene().getWindow();

        // Crear el nodo de redimensionamiento (en este caso, usaremos un rectángulo)
        Rectangle resizeHandle = new Rectangle(10, 10);
        resizeHandle.setCursor(Cursor.SE_RESIZE);

        // Agregar el nodo de redimensionamiento al contenedor principal
        StackPane stackPane = (StackPane) stage.getScene().getRoot();
        stackPane.getChildren().add(resizeHandle);

        // Manejar eventos de arrastre para redimensionar la ventana
        resizeHandle.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        resizeHandle.setOnMouseDragged(event -> {
            double deltaX = event.getScreenX() - xOffset;
            double deltaY = event.getScreenY() - yOffset;

            stage.setWidth(stage.getWidth() + deltaX);
            stage.setHeight(stage.getHeight() + deltaY);

            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

         */
    }

}
