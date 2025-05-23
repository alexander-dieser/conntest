package com.adieser.conntest.views.utils;

import com.adieser.conntest.views.JavafxApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class StageListener implements ApplicationListener<JavafxApplication.StageReadyEvent> {
    private final Resource fxml;
    private final ApplicationContext applicationContext;
    private final Logger logger;

    private double xOffset = 0;
    private double yOffset = 0;

    public StageListener(@Value("classpath:/fxml/ui.fxml") Resource fxml, ApplicationContext applicationContext, Logger logger) {
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
            stage.setMinHeight(700);
            stage.setTitle("ConnTest");
            stage.getIcons().add(new Image("image/logo.png"));

            stage.show();
        } catch (IOException e) {
            logger.error("Error loading Stage", e);
        }
    }
}
