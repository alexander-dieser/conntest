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

    public static final String INVALIDIP = "Invalid IP address";

    @Getter
    private final List<String> ipList = new ArrayList<>();

    private final ApplicationContext applicationContext;
    @Autowired
    public CustomIPModalController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @FXML
    public void handleModalAction(ActionEvent event) {
        String ip1 = ipInput1.getText();
        String ip2 = ipInput2.getText();
        String ip3 = ipInput3.getText();

        if (validateIP(ip1) && validateIP(ip2) && validateIP(ip3)) {
            ipList.add(ip1);
            ipList.add(ip2);
            ipList.add(ip3);

            if (action != null) {
                action.run();
            }

            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
        } else {
            errorInput.setText("One or more IP addresses are invalid. Please check and try again.");
        }
    }



    @Override
    public void showModal(Stage ownerStage, String fxmlFile, String title, int width, int height) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        loader.setControllerFactory(applicationContext::getBean); // Usa el ApplicationContext de Spring
        Parent root = loader.load();
        setStage(root, ownerStage, title, width, height);
    }


    /**
     * Validates an IP address using a regex pattern.
     * @param ip The IP address to validate.
     * @return True if valid or empty, false otherwise.
     */
    private boolean validateIP(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        return ip.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");
    }
}