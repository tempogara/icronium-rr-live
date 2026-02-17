package it.icron.icronium.live;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

import javafx.beans.property.*;

/**
 * Rappresenta una sorgente LIVE
 */
public class IpRow {

    // =================================================
    // BASIC INFO
    // =================================================

    private final StringProperty name =
            new SimpleStringProperty("");

    private final StringProperty url =
            new SimpleStringProperty("");

    private final StringProperty status =
            new SimpleStringProperty("Idle");

    // =================================================
    // PROGRESS / INFO
    // =================================================

    private final DoubleProperty progress =
            new SimpleDoubleProperty(0);

    private final LongProperty receivedBytes =
            new SimpleLongProperty(0);

    private final ObjectProperty<LocalDateTime> lastUpdate =
            new SimpleObjectProperty<>(null);

    // =================================================
    // CONTROL
    // =================================================

    private final BooleanProperty active =
            new SimpleBooleanProperty(false);

    // =================================================
    // LIVE CACHE
    // =================================================

    private volatile byte[] lastBytes;

    private transient ScheduledFuture<?> future;

    // =================================================
    // CTOR
    // =================================================

    public IpRow(String name, String url) {

        this.name.set(name);
        this.url.set(url);
    }

    // =================================================
    // PROPERTIES
    // =================================================

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty urlProperty() {
        return url;
    }
    
    public String getUrl() {
        return url.getValue();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public LongProperty receivedBytesProperty() {
        return receivedBytes;
    }

    public ObjectProperty<LocalDateTime> lastUpdateProperty() {
        return lastUpdate;
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    // =================================================
    // CONTROL
    // =================================================

    public boolean isActive() {
        return active.get();
    }

    /** ▶ PLAY */
    public void start() {
        active.set(true);
    }

    /** ⏹ STOP */
    public void stop() {
        active.set(false);
    }

    /** 🔄 RESET */
    public void reset() {

        active.set(false);

        progress.set(0);
        receivedBytes.set(0);
        lastBytes = null;

        status.set("Reset");
    }

    // =================================================
    // LIVE
    // =================================================

    public byte[] getLastBytes() {
        return lastBytes;
    }

    public void setLastBytes(byte[] b) {
        this.lastBytes = b;
    }

    // =================================================
    // SCHEDULER CONTROL
    // =================================================

    public ScheduledFuture<?> getFuture() {
        return future;
    }

    public void setFuture(ScheduledFuture<?> f) {
        this.future = f;
    }

    public void clearFuture() {

        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }
}
