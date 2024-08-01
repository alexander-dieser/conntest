package com.adieser.conntest.views;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ScrollManager {
    private final List<ScrollBar> verticalScrollBars = new ArrayList<>();
    private final ChangeListener<Number> scrollListener = (observable, oldValue, newValue) -> syncVerticalScroll(newValue.doubleValue());

    public void addScrollListener(TableView<?> tableView) {
        ScrollBar scrollBar = findScrollBar(tableView);
        if (scrollBar != null && !verticalScrollBars.contains(scrollBar)) {
            verticalScrollBars.add(scrollBar);
            scrollBar.valueProperty().addListener(scrollListener);
        }
    }

    public void removeScrollListener(TableView<?> tableView) {
        ScrollBar scrollBar = findScrollBar(tableView);
        if (scrollBar != null && verticalScrollBars.contains(scrollBar)) {
            scrollBar.valueProperty().removeListener(scrollListener);
            verticalScrollBars.remove(scrollBar);
        }
    }

    private ScrollBar findScrollBar(TableView<?> tableView) {
        Set<Node> nodes = tableView.lookupAll(".scroll-bar");
        for (Node node : nodes) {
            if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == Orientation.VERTICAL) {
                return scrollBar;
            }
        }
        return null;
    }

    void syncVerticalScroll(double newValue) {
        for (ScrollBar scrollBar : verticalScrollBars) {
            scrollBar.setValue(newValue);
        }
    }

    public void removeAllScrollListeners() {
        for (ScrollBar scrollBar : verticalScrollBars) {
            scrollBar.valueProperty().removeListener(scrollListener);
        }
        verticalScrollBars.clear();
    }
}
