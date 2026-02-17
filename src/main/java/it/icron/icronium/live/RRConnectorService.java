package it.icron.icronium.live;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RRConnectorService {

    public static ConnectorContext connect(String rrId) throws Exception {

        ConnectorContext ctx = new ConnectorContext();
        ctx.setRrId(rrId);

        String apis = httpGet(ctx.getBaseUrl() +
                "simpleapi/get?lang=en&pw=0");

        ctx.setApis(parseApis(apis));
        return ctx;
    }

    private static String httpGet(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        return read(c);
    }

    private static String read(HttpURLConnection c) throws IOException {
        try (InputStream is = c.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int r;
            while ((r = is.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            return baos.toString("UTF-8");
        }
    }

    private static List<RREndpoint> parseApis(String json) {
        
        List<RREndpoint> list =
            ParserExample.parse(json);

        StatusBus.set("Caricati " + list.size() + " endpoint");
        
        return list;
    }

    
}
