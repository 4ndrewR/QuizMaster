package com.quizmaster.views;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

/**
 * Shared factory for the clickable dashboard tiles. Extracted so the Teacher
 * and Student dashboards don't duplicate the same builder method.
 */
final class Tiles {
    private Tiles() {}

    static VBox tile(String heading, String subtext, EventHandler<MouseEvent> onClick) {
        VBox box = new VBox(6);
        box.getStyleClass().add("tile");
        box.setPrefSize(240, 130);
        box.setAlignment(Pos.CENTER);
        Label h = new Label(heading);
        h.getStyleClass().add("tile-heading");
        Label s = new Label(subtext);
        s.getStyleClass().add("tile-sub");
        s.setWrapText(true);
        s.setMaxWidth(220);
        s.setAlignment(Pos.CENTER);
        box.getChildren().addAll(h, s);
        box.setOnMouseClicked(onClick);
        return box;
    }
}
