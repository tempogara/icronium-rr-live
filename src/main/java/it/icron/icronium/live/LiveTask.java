package it.icron.icronium.live;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Task LIVE:
 * - scarica contenuto in RAM
 * - confronta con precedente
 * - se cambia → push esterno
 */
public class LiveTask implements Runnable {

    private final IpRow row;
    private final String eventID;

    public LiveTask(IpRow row, String eventID) {
        this.row = row;
        this.eventID = eventID;
    }

    @Override
    public void run() {

        // Sicurezza: STOP = niente lavoro
        if (row == null || !row.isActive())
            return;

        String url = row.urlProperty().get();

        if (url == null || url.isBlank())
            return;

        try {

            AppLogger.log("LIVE poll: " + row.nameProperty().get());

            // ===========================
            // 1) DOWNLOAD
            // ===========================

            byte[] data = download(url);

            if (data == null || data.length == 0) {
                AppLogger.log("LIVE empty response");
                return;
            }

            // ===========================
            // 2) CHECK CHANGE
            // ===========================

            byte[] prev = row.getLastBytes();

            if (prev != null && Arrays.equals(prev, data)) {

                AppLogger.log("LIVE unchanged: " +
                        row.nameProperty().get());

                return;
            }

            row.setLastBytes(data);

            // ===========================
            // 3) PUSH
            // ===========================

            LiveUploader.send(row, data, this.eventID);

            // ===========================
            // 4) UI UPDATE
            // ===========================

            MainApp.safeUi(() -> {

                row.statusProperty()
                   .set("LIVE inviato");

                row.receivedBytesProperty()
                   .set(data.length);

                row.lastUpdateProperty()
                   .set(LocalDateTime.now());

                row.progressProperty()
                   .set(1.0);
            });

            AppLogger.log("LIVE sent: " +
                    row.nameProperty().get());

        } catch (Exception ex) {

            AppLogger.log(
                "LIVE error (" +
                row.nameProperty().get() + "): " +
                ex.getMessage()
            );

            MainApp.safeUi(() ->
                row.statusProperty()
                   .set("Errore")
            );
        }
    }

    // =================================================
    // DOWNLOAD HTTP
    // =================================================

    private byte[] download(String urlStr) throws Exception {

        HttpURLConnection conn =
            (HttpURLConnection) new URL(urlStr).openConnection();

        conn.setRequestMethod("GET");

        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);

        conn.setUseCaches(false);

        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Pragma", "no-cache");

        int code = conn.getResponseCode();

        if (code != 200) {

            throw new RuntimeException(
                "HTTP " + code
            );
        }

        try (InputStream is = conn.getInputStream()) {

            return is.readAllBytes();

        } finally {

            conn.disconnect();
        }
    }
}
