package hu.devo.bastet.common;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.orhanobut.logger.Logger;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hu.devo.bastet.Bastet;
import hu.devo.bastet.R;
import hu.devo.bastet.spotify.SpotifyResponse;
import hugo.weaving.DebugLog;

/**
 * Stores and manages either a cached album art file or uri
 */
public class AlbumArtStorage {

    //deserializes json
    protected static Gson gson = new Gson();
    protected static Context ctx = Bastet.getContext();
    //requests to spotify using okhttp
    protected static OkHttpClient client = new OkHttpClient();
    //cache for embedded album art files
    protected static Map<String, File> fileCache = new HashMap<>(10);
    //location for default image
    protected static String defaultUri = "android.resource://hu.devo.bastet/" + R.drawable.cd;

    protected File file;
    protected String uri;
    protected boolean isUri;
    protected boolean fillComplete;
    protected ArrayList<Runnable> afterFillComplete = new ArrayList<>(5);

    protected Music m;

    /**
     * Instantiates a new Album art storage synchronously, therefore should be called from a worker
     * thread.
     *
     * @param m the Music this AAS is associated with
     */
    @WorkerThread
    @DebugLog
    AlbumArtStorage(Music m) {
        Logger.i("creating new AAS for " + m);
        this.m = m;
        fill();
    }

    /**
     * Instantiates a new Album art storage asynchronously. May be called from the main thread.
     * Also runs a Runnable after the storage has been filled.
     *
     * @param m    the Music this AAS is associated with
     * @param then after this has been filled run this
     */
    @DebugLog
    public AlbumArtStorage(Music m, @NonNull final Runnable then) {
        this.m = m;
        m.aas = this;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                afterFillComplete.add(then);
                fill();
                return null;
            }
        }.execute();
    }

    /**
     * Convenience method to load the default art into an ImageView
     *
     * @param iv the ImageView
     */
    public static void loadDefault(ImageView iv) {
        Picasso.with(ctx).load(defaultUri).into(iv);
    }

    /**
     * Load the stored image into an ImageView. If the filling is not et complete then it loads the
     * default album art and completes the task asynchronously.
     *
     * @param iv the ImageView
     */
    @DebugLog
    public void loadIntoView(final ImageView iv) {
        if (fillComplete) {
            Logger.i("loading the %s, %s for %s", isUri ? "uri" : "file", isUri ? uri : file, m);
            if (isUri()) {
                Picasso.with(ctx).load(getUri()).into(iv);
            } else {
                Picasso.with(ctx).load(getFile()).into(iv);
            }
        } else {
            Logger.i("loading the default for now");
            Picasso.with(ctx).load(defaultUri).into(iv);
            afterFillComplete.add(new Runnable() {
                @Override
                public void run() {
                    iv.post(new Runnable() {
                        @Override
                        public void run() {
                            loadIntoView(iv);
                        }
                    });
                }
            });
        }
    }

    /**
     * Tries to fill the AAS from 3 different locations.
     */
    @DebugLog
    protected void fill() {
        Logger.i("filling AAS for " + m);
        boolean success = false;
        int i = 0;
        if (m.albumId == -1) {
            //skip content resolver search
            i = 1;
        }
        //loop through all possible album art locations
        for (; i < 4 && !success; i++) {
            switch (i) {
                case 0:
                    Logger.i("trying to fill from content resolver");
                    success = fillFromContentResolver(m.albumId);
                    break;
                case 1:
                    Logger.i("trying to fill from file");
                    success = fillFromMetaRetriever();
                    break;
                case 2:
                    Logger.i("no art found, filling from spotify");
                    success = fillFromSpotify();
                    break;
                case 3:
                    Logger.i("still nope, defaulting");
                    success = fillDefault();
                    break;
                default:
                    Logger.e("this really shouldn't happen");
                    break;
            }
        }

        //run callbacks
        fillComplete = true;
        for (Runnable r : afterFillComplete) {
            r.run();
        }
        afterFillComplete.clear();
    }

    /**
     * Tries to look for an art in the built in album art store.
     *
     * @param albumId the album id to look for
     * @return true if an art was found
     */
    protected boolean fillFromContentResolver(long albumId) {
        try {
            final Uri ALBUM_ART_STORE = Uri.parse("content://media/external/audio/albumart");
            Uri albumArtUri = ContentUris.withAppendedId(ALBUM_ART_STORE, albumId);
            MediaStore.Images.Media.getBitmap(ctx.getContentResolver(), albumArtUri).recycle();
            //if we can get the image then save the uri
            isUri = true;
            uri = albumArtUri.toString();
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    /**
     * Tries to look for an embedded album art in the file using the MediaMetadataRetriever.
     *
     * @return true if an art was found
     */
    protected boolean fillFromMetaRetriever() {
        //look in the cache first
        File cached = fileCache.get(m.getPath());
        if (cached == null) {
            try {
                //safely encodes the path, so that it can be a filename
                String name = URLEncoder.encode(m.getPath(), Charset.defaultCharset().name());
                //if this already exists from a previous run then use that
                File tmp = new File(ctx.getCacheDir(), name + ".art");

                //otherwise look for the art in the file
                if (!tmp.exists()) {
                    MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                    metaRetriever.setDataSource(m.getPath());
                    //get the bytes
                    byte[] art = metaRetriever.getEmbeddedPicture();
                    //decode it to a bitmap
                    Bitmap image = BitmapFactory.decodeByteArray(art, 0, art.length);

                    FileOutputStream fos = new FileOutputStream(tmp);

                    //and compress it to a jpeg
                    image.compress(Bitmap.CompressFormat.JPEG, 90, fos);

                    //we don't immediately need the bitmap so recycle it
                    image.recycle();
                    fos.close();
                }

                file = tmp;
                fileCache.put(m.getPath(), file);

                return true;
            } catch (Exception e) {
                file = null;
                return false;
            }
        } else {
            //found a cached picture, use that
            file = cached;
            return true;
        }
    }

    /**
     * Tries to get an art from spotify, with two tries. Once specifically looking for the artist,
     * and once just looking for the filename.
     *
     * @return true if an art was found
     */
    protected boolean fillFromSpotify() {
        if (m.getArtist().equals("<unknown>") || m.getArtist().isEmpty()) {
            //query with the filename only, as there is no artist
            return querySpotify(m.getTitle());
        } else {
            String query = "artist:" + m.getArtist() + "+track:" + m.getTitle();
            //query specifically for the specified artist and track name
            //retry with the above method if this fails
            return querySpotify(query) || querySpotify(m.getArtist());
        }
    }

    /**
     * Queries spotify for an album art url using a string.
     *
     * @param query what to look for
     * @return true if an art was found
     */
    protected boolean querySpotify(String query) {
        try {

            String queryString = Uri.encode(query);
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/search?type=track&q=" + queryString)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            //deserialize the received JSON
            SpotifyResponse res = gson.fromJson(response.body().charStream(), SpotifyResponse.class);
            if (res.error != null) {
                return false;
            } else {
                if (res.tracks.items.size() > 0 &&
                        res.tracks.items.get(0).album.images.size() > 0
                        ) {
                    isUri = true;
                    uri = res.tracks.items.get(0).album.images.get(0).url;
                    return true;
                } else {
                    return false;
                }
            }
        } catch (IOException e) {
            //also fails here when there is no data connection
            return false;
        }
    }

    /**
     * All other methods failed use the default art.
     *
     * @return always true
     */
    protected boolean fillDefault() {
        isUri = true;
        uri = defaultUri;
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // getters
    ///////////////////////////////////////////////////////////////////////////

    public boolean isUri() {
        return isUri;
    }

    public File getFile() {
        return file;
    }

    public String getUri() {
        return uri;
    }
}
