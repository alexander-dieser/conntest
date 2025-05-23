package com.adieser.conntest.views;

import com.adieser.conntest.ConntestApplication;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public class JavafxApplication extends Application {
    private ConfigurableApplicationContext context;

    /**
     * Initializes the Spring application context
     */
    @Override
    public void init() {
        ApplicationContextInitializer<GenericApplicationContext> initializer = genericApplicationContext -> {
            genericApplicationContext.registerBean(Application.class, () -> JavafxApplication.this);
            genericApplicationContext.registerBean(Parameters.class, this::getParameters);
            genericApplicationContext.registerBean(HostServices.class, this::getHostServices);
        };

        this.context = new SpringApplicationBuilder().sources(ConntestApplication.class)
                .initializers(initializer)
                .build().run(getParameters().getRaw().toArray(new String[0]));
    }

    /**
     * Initializes the primary stage of the JavaFX application.
     * Configures the stage to have a transparent style, allowing custom styling.
     * @param primaryStage primary stage of the JavaFX application.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        this.context.publishEvent(new StageReadyEvent(primaryStage));
    }

    /**
     * Closes the Spring application context and terminates the JavaFX application.
     */
    @Override
    public void stop() throws Exception {
        this.context.close();
        Platform.exit();
    }

    /**
     * Custom event indicating that the primary Stage is ready.
     */
    public static class StageReadyEvent extends ApplicationEvent {
        /**
         * Retrieve the primary Stage associated with this event
         * @return the primary Stage
         */
        public Stage getStage() {
            return (Stage) getSource();
        }

        /**
         * Constructs a StageReadyEvent with the specified source
         * @param source source of the event
         */
        public StageReadyEvent(Object source) {
            super(source);
        }
    }
}
