package com.adieser.conntest.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import java.io.IOException;
@SuppressWarnings("unused")
@Component
public class StageListener implements ApplicationListener<JavafxApplication.StageReadyEvent> {
    private final Resource fxml;
    private final ApplicationContext applicationContext;
    private final Logger logger;

    private double xOffset = 0;
    private double yOffset = 0;

    @SuppressWarnings("unused")
    public StageListener(@Value("classpath:/ui.fxml") Resource fxml, ApplicationContext applicationContext, Logger logger) {
        this.fxml = fxml;
        this.applicationContext = applicationContext;
        this.logger = logger;
    }

    /**
     * Handles the StageReadyEvent by initializing the JavaFX Stage with the specified FXML file.
     * @param stageReadyEvent StageReadyEvent indicating that the primary Stage is ready.
     */
    @Override
    public void onApplicationEvent(JavafxApplication.StageReadyEvent stageReadyEvent) {
        try {
            Stage stage = stageReadyEvent.getStage();
            FXMLLoader fxmlLoader = new FXMLLoader(fxml.getURL());
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent root = fxmlLoader.load();

            Scene scene = new Scene(root, 1000, 700);

            // Handle drag event
            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });

            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.setTitle("ConnTest");
            stage.getIcons().add(new Image("image/logo.png"));

            stage.show();
        } catch (IOException e) {
            logger.error("Error loading Stage", e);
        }
    }
}
