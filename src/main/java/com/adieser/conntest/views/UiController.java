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
    private Button button_close;
    @FXML
    private Button button_minimize;
    @FXML
    private Button button_maximizeRestore;
    @FXML
    private Button button_resize;
    @FXML
    public Button button_start;
    @FXML
    public Button button_stop;
    @FXML
    private ComboBox<Integer> timechoicebox;
    @FXML
    private Label labelLostAvg1;
    @FXML
    private Label labelLostAvg2;
    @FXML
    private Label labelLostAvg3;
    @FXML
    public TableView tv_local;
    @FXML
    private TableColumn<PingLog, String> cl_date_local;
    @FXML
    private TableColumn<PingLog, Long> cl_ping_local;
    @FXML
    public TableView tv_isp;
    @FXML
    private TableColumn<PingLog, String> cl_date_isp;
    @FXML
    private TableColumn<PingLog, Long> cl_ping_isp;
    @FXML
    public TableView tv_cloud;
    @FXML
    private TableColumn<PingLog, String> cl_date_cloud;
    @FXML
    private TableColumn<PingLog, Long> cl_ping_cloud;
    @FXML
    public Button button_save_local;
    @FXML
    public Button button_save_isp;
    @FXML
    public Button button_save_cloud;

    private final ConnTestService connTestService;
    private ScheduledExecutorService executorService;

    @FXML
    private ProgressIndicator progressIndicator;

    private double xOffset = 0;
    private double yOffset = 0;


    //@Value("${conntest.pinglogs.path}" + "/ping.log")
    @Value("ping.log")
    private Resource resource;

    @Autowired
    public UiController(ConnTestService connTestService) {
        this.connTestService = connTestService;
    }

    @FXML
    public void initialize() {
        button_stop.setDisable(true);
        timechoicebox.getItems().addAll(1, 5, 10, 15);
        this.button_start.setOnAction(actionEvent -> {

            button_start.setDisable(true);
            progressIndicator.setVisible(true);
            timechoicebox.setDisable(true);
            button_stop.setDisable(false);
            Integer timechoice;
            if(timechoicebox.getValue() == null){
                timechoice = 5;
            }else {
                timechoice = timechoicebox.getValue();
            }

            Thread startThread = new Thread(() -> start(timechoice));
            startThread.start();

        });
        this.button_stop.setOnAction(actionEvent -> {
            button_stop.setDisable(true);
            progressIndicator.setVisible(false);
            this.stop();
            button_start.setDisable(false);
            timechoicebox.setDisable(false);
        });
        this.button_save_local.setOnAction(actionEvent -> saveLogs(tv_local, cl_date_local, cl_ping_local ));
        this.button_save_isp.setOnAction(actionEvent -> saveLogs(tv_isp, cl_date_isp, cl_ping_isp));
        this.button_save_cloud.setOnAction(actionEvent -> saveLogs(tv_cloud, cl_date_cloud, cl_ping_cloud));
        this.button_close.setOnAction(event -> handleClose());
        this.button_minimize.setOnAction(event -> handleMinimize());
        this.button_maximizeRestore.setOnAction(event -> handleMaximizeRestore());
        this.button_resize.setOnAction(event -> handleResize());
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

    public void readLog(List<String> ipAddress){
        String logFileName  = "src/main/resources/pingLogs/ping.log";

        LinkedList<PingLog> pingLogsIpLocal = new LinkedList<>();
        LinkedList<PingLog> pingLogsIpIsp = new LinkedList<>();
        LinkedList<PingLog> pingLogsIpCloud = new LinkedList<>();

        cl_date_local.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        cl_date_local.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        cl_ping_local.setCellValueFactory(new PropertyValueFactory<>("pingTime"));
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
        cl_date_isp.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        cl_date_isp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        cl_ping_isp.setCellValueFactory(new PropertyValueFactory<>("pingTime"));
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
        cl_date_cloud.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        cl_date_cloud.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        cl_ping_cloud.setCellValueFactory(new PropertyValueFactory<>("pingTime"));
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


        try (BufferedReader br = new BufferedReader(new FileReader(logFileName))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts  = line.split(",");

                PingLog pingLog = new PingLog();

                pingLog.setDateTime(LocalDateTime.parse(parts[0].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                pingLog.setIpAddress(parts[1].trim());
                pingLog.setPingTime(Long.parseLong(parts[2].trim()));


                if (Objects.equals(ipAddress.size() > 0 ? ipAddress.get(0) : null, pingLog.getIpAddress())) {
                    pingLogsIpLocal.addFirst(pingLog);
                } else if (Objects.equals(ipAddress.size() > 1 ? ipAddress.get(1) : null, pingLog.getIpAddress())) {
                    pingLogsIpIsp.addFirst(pingLog);
                } else if (Objects.equals("8.8.8.8", pingLog.getIpAddress())) {
                    pingLogsIpCloud.addFirst(pingLog);
                } else {}
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        tv_local.getItems().clear();
        tv_isp.getItems().clear();
        tv_cloud.getItems().clear();

        for (PingLog pl : pingLogsIpLocal){ if (pl != null) { tv_local.getItems().add(pl);}}
        for (PingLog pl : pingLogsIpIsp){ if (pl != null) { tv_isp.getItems().add(pl);}}
        for (PingLog pl : pingLogsIpCloud){ if (pl != null) { tv_cloud.getItems().add(pl);}}
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
                    writer.write(log.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    writer.write("\t");
                    writer.write(String.valueOf(log.getPingTime()));
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void setAverageLost(List<String> ipAddress){
        Platform.runLater(() -> {
            if (ipAddress.size() > 0 && ipAddress.get(0) != null) {
                labelLostAvg1.setText("Average lost pings: " + connTestService.getPingsLostAvgByIp(ipAddress.get(0)));
            }
            if (ipAddress.size() > 1 && ipAddress.get(1) != null) {
                labelLostAvg2.setText("Average lost pings: " + connTestService.getPingsLostAvgByIp(ipAddress.get(1)));
            }
            labelLostAvg3.setText("Average lost pings: " + connTestService.getPingsLostAvgByIp("8.8.8.8"));
        });
    }

    private void handleClose() {
        Stage stage = (Stage) button_close.getScene().getWindow();
        stage.close();
    }

    private void handleMinimize() {
        Stage stage = (Stage) button_minimize.getScene().getWindow();
        stage.setIconified(true);
    }

    private void handleMaximizeRestore() {
        Stage stage = (Stage) button_maximizeRestore.getScene().getWindow();
        if (stage.isMaximized()) {
            stage.setMaximized(false);
            ImageView maximizeRestorebutton2 = new ImageView(new Image(getClass().getResourceAsStream("/image/maximizeRestorebutton1.png")));
            maximizeRestorebutton2.setFitHeight(18);
            maximizeRestorebutton2.setFitWidth(18);
            button_maximizeRestore.setGraphic(maximizeRestorebutton2);
        } else {
            stage.setMaximized(true);
            ImageView maximizeRestorebutton1 = new ImageView(new Image(getClass().getResourceAsStream("/image/maximizeRestorebutton2.png")));
            maximizeRestorebutton1.setFitHeight(18);
            maximizeRestorebutton1.setFitWidth(18);
            button_maximizeRestore.setGraphic(maximizeRestorebutton1);
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
