package com.adieser.conntest.views.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CustomIPModalController extends ModalController {

    @FXML
    private Button closeButton;
    @FXML
    private TextField ipInput1;
    @FXML
    private TextField ipInput2;
    @FXML
    private TextField ipInput3;
    @FXML
    private Label errorInput;
    @Setter
    private Runnable action;
    @Getter
    private final List<String> ipList = new ArrayList<>();
    private final ApplicationContext applicationContext;

    @Autowired
    public CustomIPModalController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Handles the "Save" action in the modal.
     * If the IPs are valid, adds them to the list of IPs and closes the modal window.
     * @param event the action event triggered by the Save button.
     */
    @FXML
    public void handleModalAction(ActionEvent event) {
        String ip1 = ipInput1.getText().trim();
        String ip2 = ipInput2.getText().trim();
        String ip3 = ipInput3.getText().trim();

        List<String> validIPs = new ArrayList<>();

        if (!ip1.isEmpty() && validateIP(ip1)) validIPs.add(ip1);
        if (!ip2.isEmpty() && validateIP(ip2)) validIPs.add(ip2);
        if (!ip3.isEmpty() && validateIP(ip3)) validIPs.add(ip3);

        if (!validIPs.isEmpty()) {
            ipList.clear();
            ipList.addAll(validIPs);

            if (action != null) {
                action.run();
            }

            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
        } else {
            errorInput.setText("At least one valid IP address must be provided.");
        }
    }

    /**
     * Displays a modal window using the specified parameters.
     * Configures the controller for the modal and sets up the stage.
     * @param ownerStage the parent stage of the modal window.
     * @param fxmlFile the FXML file path for the modal layout.
     * @param title the title of the modal window.
     * @param width the width of the modal window.
     * @param height the height of the modal window.
     * @throws IOException if the FXML file cannot be loaded.
     */
    @Override
    public void showModal(Stage ownerStage, String fxmlFile, String title, int width, int height) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();
        setStage(root, ownerStage, title, width, height);
    }

    /**
     * Validates an IP address using a regex pattern
     * @param ip the IP address to validate
     * @return true if the IP address is valid, false otherwise
     */
    private boolean validateIP(String ip) {
        if (ip == null || ip.isEmpty()) return true;
        return ip.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");
    }

    /**
     * Populates the input fields with the current IPs from ipList.
     */
    public void populateIpFields() {
        if (!ipList.isEmpty()) {
            ipInput1.setText(ipList.get(0));
            ipInput2.setText(ipList.size() > 1 ? ipList.get(1) : "");
            ipInput3.setText(ipList.size() > 2 ? ipList.get(2) : "");
        }
    }

    /**
     * Closes the current stage.
     */
    @FXML
    @Override
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
        if (action != null) {
            action.run();
        }
    }

}