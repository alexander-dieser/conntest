package com.adieser.conntest.views;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableView;

import java.util.Set;

public class ScrollManager {
    private ScrollBar localVerticalScrollBar;
    private ScrollBar ispVerticalScrollBar;
    private ScrollBar cloudVerticalScrollBar;

    public void addScrollListener(TableView<?> localTableView, TableView<?> ispTableView, TableView<?> cloudTableView) {
        this.localVerticalScrollBar = findScrollBar(localTableView);
        this.ispVerticalScrollBar = findScrollBar(ispTableView);
        this.cloudVerticalScrollBar = findScrollBar(cloudTableView);

        localVerticalScrollBar.valueProperty().addListener((observable, oldValue, newValue) -> syncVerticalScroll(localVerticalScrollBar.getValue()));
        ispVerticalScrollBar.valueProperty().addListener((observable, oldValue, newValue) -> syncVerticalScroll(ispVerticalScrollBar.getValue()));
        cloudVerticalScrollBar.valueProperty().addListener((observable, oldValue, newValue) -> syncVerticalScroll(cloudVerticalScrollBar.getValue()));
    }

    private ScrollBar findScrollBar(TableView<?> tableView) {
        ScrollBar result = null;
        Set<Node> nodes = tableView.lookupAll(".scroll-bar");
        for (Node node : nodes) {
            if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == Orientation.VERTICAL && !tableView.getItems().isEmpty()) {
                    result = scrollBar;
                    break;
                }

        }
        return result;
    }

    private void syncVerticalScroll(double newValue) {
        localVerticalScrollBar.setValue(newValue);
        ispVerticalScrollBar.setValue(newValue);
        cloudVerticalScrollBar.setValue(newValue);
    }

    public void removeScrollListener() {
        localVerticalScrollBar.valueProperty().removeListener((observable, oldValue, newValue) -> syncVerticalScroll(localVerticalScrollBar.getValue()));
        ispVerticalScrollBar.valueProperty().removeListener((observable, oldValue, newValue) -> syncVerticalScroll(ispVerticalScrollBar.getValue()));
        cloudVerticalScrollBar.valueProperty().removeListener((observable, oldValue, newValue) -> syncVerticalScroll(cloudVerticalScrollBar.getValue()));
    }

}