package it.icron.icronium.live;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RREndpoint {

    @JsonProperty("Disabled")
    public boolean disabled;

    @JsonProperty("Key")
    public String key;

    @JsonProperty("URL")
    public String url;

    @JsonProperty("Label")
    public String label;
}