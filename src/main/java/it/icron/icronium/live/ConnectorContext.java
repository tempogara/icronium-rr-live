package it.icron.icronium.live;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ConnectorContext {

    private String rrId;
    private String baseUrl;

    private List<RREndpoint> apis = new ArrayList<>();

    public boolean isConnected() {
        return rrId != null;
    }

    public String getBaseUrl() { return baseUrl; }
    public List<RREndpoint> getApis() { return apis; }

    public void setRrId(String rrId) {
        this.rrId = rrId;
        this.baseUrl = rrId;
        if (!rrId.startsWith("http")) {
        	this.baseUrl = "http://localhost/" + rrId+"/";
        } else {
        	this.rrId = extractRrId(rrId);
        }
        if (this.baseUrl.indexOf("?") != -1) {
        	this.baseUrl = this.baseUrl.substring(0, this.baseUrl.indexOf("?") );
        }
        this.baseUrl = this.baseUrl + "api/";
    }
    

    public void setApis(List<RREndpoint> apis) {
        this.apis = apis;
    }

	public MainApp getMainApp() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getMainApp'");
	}
	
	public static String extractRrId(String url) {
	    try {
	        URI uri = URI.create(url);
	        String path = uri.getPath();          // es: "/_IBGEL/"
	        if (path == null) return null;

	        // rimuove slash iniziali/finali
	        path = path.replaceAll("^/+", "").replaceAll("/+$", "");

	        // primo segmento
	        int slash = path.indexOf('/');
	        return (slash >= 0) ? path.substring(0, slash) : path;

	    } catch (Exception e) {
	        return null;
	    }
	}

	public String getRrId() {
		return rrId;
	}
}
