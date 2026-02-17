package it.icron.icronium.live;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppLogger {

    private static final Path LOG_FILE =
        MainApp.WORK_DIR.resolve("app.log");

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String msg) {

        try {

            String line =
                "[" + LocalDateTime.now().format(FMT) + "] "
                + msg + "\n";

            Files.writeString(
                LOG_FILE,
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

        } catch (IOException ignored) {}
    }
}
