
package hu.devo.bastet.spotify;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class SpotifyResponse {

    @SerializedName("error")
    @Expose
    public String error;
    @SerializedName("error_description")
    @Expose
    public String errorDescription;
    @SerializedName("tracks")
    @Expose
    public Tracks tracks;

}
