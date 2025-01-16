package com.adieser.conntest.views.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public class ModalController {
    @FXML
    private Button closeButton;
    @FXML
    private Button maximizeRestoreButton;

    @Setter
    private Runnable action;

    private double xOffset = 0;
    private double yOffset = 0;

    /**
     * Opens a new modal window displaying the specified FXML file.
     * @param ownerStage The main application stage.
     * @param fxmlFile The FXML file to load for the modal content.
     * @param title The title of the modal window.
     * @throws IOException if the specified FXML file cannot be loaded.
     */
    public void showModal(Stage ownerStage, String fxmlFile, String title, int width, int height) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();
        ModalController modalController = loader.getController();

        modalController.setAction(this.action);

        setStage(root, ownerStage, title, width, height);
    }

    public void setStage(Parent root, Stage ownerStage, String title, int width, int height){
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle(title);

        // Handle drag event
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        stage.setMinWidth(width);
        stage.setMinHeight(height);

        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(ownerStage);

        stage.show();
    }

    /*
     * Executes the action associated with the modal and closes the modal window.
     */
    @FXML
    public void handleAction() {
        if (action != null) {
            action.run();
        }

        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }


    /**
     * Closes the current stage.
     */
    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Maximizes or restores the current stage and updates the button image.
     */
    @FXML
    private void handleMaximizeRestore(ActionEvent event) {
        Stage stage = (Stage) maximizeRestoreButton.getScene().getWindow();
        if (stage.isMaximized()) {
            stage.setMaximized(false);
            ImageView maximizeRestoreButtonImage = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/maximizeRestorebutton1.png"))));
            maximizeRestoreButtonImage.setFitHeight(18);
            maximizeRestoreButtonImage.setFitWidth(18);
            maximizeRestoreButton.setGraphic(maximizeRestoreButtonImage);
        } else {
            stage.setMaximized(true);
            ImageView maximizeRestoreButtonImage = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/maximizeRestorebutton2.png"))));
            maximizeRestoreButtonImage.setFitHeight(18);
            maximizeRestoreButtonImage.setFitWidth(18);
            maximizeRestoreButton.setGraphic(maximizeRestoreButtonImage);
        }
    }
}