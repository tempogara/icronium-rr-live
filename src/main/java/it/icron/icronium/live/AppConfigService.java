package it.icron.icronium.live;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce persistenza configurazione LIVE
 */
public class AppConfigService {

    private static final Path CONFIG_FILE =
        MainApp.WORK_DIR.resolve("config.json");

    // =================================================
    // LOAD
    // =================================================

    public static LoadedConfig load() {

        LoadedConfig result = new LoadedConfig();

        if (!Files.exists(CONFIG_FILE))
            return result;

        try {

            String json = Files.readString(CONFIG_FILE);

            AppConfig cfg = AppConfig.fromJson(json);

            for (AppConfig.Row r : cfg.rows) {

                // ricrea IpRow LIVE
                IpRow row = new IpRow(
                    r.name,
                    r.url
                );

                // sempre STOP all'avvio
                row.stop();

                row.statusProperty()
                   .set("Caricato");

                result.rows.add(row);
            }

            AppLogger.log("Config caricata: " +
                    result.rows.size() + " righe");

        } catch (Exception e) {

            AppLogger.log(
                "Errore load config: " + e.getMessage()
            );

            e.printStackTrace();
        }

        return result;
    }

    // =================================================
    // SAVE
    // =================================================

    public static void save(List<IpRow> rows) {

        AppConfig cfg = new AppConfig();

        for (IpRow r : rows) {

            AppConfig.Row row =
                new AppConfig.Row();

            row.name =
                r.nameProperty().get();

            row.url =
                r.urlProperty().get();

            cfg.rows.add(row);
        }

        try {

            Files.writeString(
                CONFIG_FILE,
                cfg.toJson()
            );

            AppLogger.log(
                "Config salvata: " +
                rows.size() + " righe");

        } catch (IOException e) {

            AppLogger.log(
                "Errore save config: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // =================================================
    // DTO
    // =================================================

    public static class LoadedConfig {

        public final List<IpRow> rows =
            new ArrayList<>();
    }
}
