
package hu.devo.bastet.spotify;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;


public class Album {

    @SerializedName("album_type")
    @Expose
    public String albumType;
    @SerializedName("available_markets")
    @Expose
    public List<String> availableMarkets = new ArrayList<String>();
    @SerializedName("external_urls")
    @Expose
    public ExternalUrls externalUrls;
    @SerializedName("href")
    @Expose
    public String href;
    @SerializedName("id")
    @Expose
    public String id;
    @SerializedName("images")
    @Expose
    public List<Image> images = new ArrayList<Image>();
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("type")
    @Expose
    public String type;
    @SerializedName("uri")
    @Expose
    public String uri;

}
