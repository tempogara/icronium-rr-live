package it.icron.icronium.live;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StatusBus {

    private static final StringProperty status =
            new SimpleStringProperty("Pronto");

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public static StringProperty statusProperty() {
        return status;
    }

    public static void set(String msg) {

        String stamped = "[" + LocalTime.now().format(TIME_FMT) + "] " + msg;

        if (Platform.isFxApplicationThread()) {
            status.set(stamped);
        } else {
            Platform.runLater(() -> status.set(stamped));
        }
    }
}
