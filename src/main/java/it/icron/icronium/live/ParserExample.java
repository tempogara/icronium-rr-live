package it.icron.icronium.live;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * Parser JSON → Lista RREndpoint
 */
public class ParserExample {

    private static final ObjectMapper MAPPER =
        new ObjectMapper()
            // ignora campi sconosciuti
            .configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
            );

    private ParserExample() {
        // utility class
    }

    // =================================================
    // PUBLIC API
    // =================================================

    /**
     * Converte JSON array → List<RREndpoint>
     */
    public static List<RREndpoint> parse(String json) {

        if (json == null || json.isBlank())
            return Collections.emptyList();

        try {

            return MAPPER.readValue(
                json,
                new TypeReference<List<RREndpoint>>() {}
            );

        } catch (Exception e) {

            System.err.println(
                "Errore parsing JSON: " + e.getMessage()
            );

            e.printStackTrace();

            return Collections.emptyList();
        }
    }
}
