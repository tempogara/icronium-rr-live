package it.icron.icronium.live;

import java.util.ArrayList;
import java.util.List;

/**
 * Configurazione applicazione (LIVE only)
 */
public class AppConfig {

    public List<Row> rows = new ArrayList<>();

    // =================================================
    // DTO
    // =================================================

    public static class Row {

        public String name;
        public String url;
    }

    // =================================================
    // SERIALIZE → JSON
    // =================================================

    public String toJson() {

        StringBuilder sb = new StringBuilder();

        sb.append("{\n");
        sb.append("  \"rows\": [\n");

        for (int i = 0; i < rows.size(); i++) {

            Row r = rows.get(i);

            sb.append("    {\n");
            sb.append("      \"name\": \"")
              .append(escape(r.name)).append("\",\n");

            sb.append("      \"url\": \"")
              .append(escape(r.url)).append("\"\n");

            sb.append("    }");

            if (i < rows.size() - 1)
                sb.append(",");

            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }

    // =================================================
    // PARSE ← JSON
    // =================================================

    public static AppConfig fromJson(String json) {

        AppConfig cfg = new AppConfig();

        if (json == null || json.isBlank())
            return cfg;

        json = json.replace("\r", "");

        String rowsBlock = extractArray(json, "\"rows\"");

        if (rowsBlock == null)
            return cfg;

        int pos = 0;

        while (true) {

            int start = rowsBlock.indexOf('{', pos);
            if (start < 0) break;

            int end = rowsBlock.indexOf('}', start);
            if (end < 0) break;

            String obj =
                rowsBlock.substring(start + 1, end);

            Row r = new Row();

            r.name = extractString(obj, "\"name\"");
            r.url  = extractString(obj, "\"url\"");

            if (valid(r)) {
                cfg.rows.add(r);
            }

            pos = end + 1;
        }

        return cfg;
    }

    // =================================================
    // HELPERS
    // =================================================

    private static boolean valid(Row r) {

        return r != null &&
               r.name != null &&
               !r.name.isBlank() &&
               r.url != null &&
               !r.url.isBlank();
    }


    private static String extractArray(
            String src,
            String key) {

        int i = src.indexOf(key);

        if (i < 0)
            return null;

        int start = src.indexOf('[', i);
        int end   = src.indexOf(']', start);

        if (start < 0 || end < 0)
            return null;

        return src.substring(start + 1, end);
    }


    private static String extractString(
            String src,
            String key) {

        int i = src.indexOf(key);

        if (i < 0)
            return "";

        int start =
            src.indexOf('"', i + key.length()) + 1;

        int end =
            src.indexOf('"', start);

        if (start < 0 || end < 0)
            return "";

        return unescape(src.substring(start, end));
    }


    private static String escape(String s) {

        if (s == null)
            return "";

        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }


    private static String unescape(String s) {

        if (s == null)
            return "";

        return s
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
}
