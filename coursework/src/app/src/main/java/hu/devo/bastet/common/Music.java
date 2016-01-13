package hu.devo.bastet.common;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.nfc.FormatException;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyReference;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.container.ForeignKeyContainer;

import hu.devo.bastet.Bastet;
import hu.devo.bastet.database.BastetDB;
import hu.devo.bastet.database.Playlist;
import hu.devo.bastet.database.Playlist$Table;
import hu.devo.bastet.dialog.BaseDialog;

/**
 * Stores one track, and also acts as a Model for the DBFlow database.
 * Created by Barnabas on 19/11/2015.
 */
@Table(databaseName = BastetDB.NAME)
public class Music extends BaseModel{
    protected boolean isPlaying = false;
    @Column
    @PrimaryKey(autoincrement = true)
    //a unique id for storing this in the db
    //only used by DBFlow
    protected long dbID;
    @Column
    @ForeignKey(
            references = {@ForeignKeyReference(columnName = "playlistId",
                    columnType = Long.class, foreignColumnName = "id")},
            saveForeignKeyModel = false
    )
    //needed to link this with a playlist
    protected ForeignKeyContainer<Playlist> playlistContainer;


    @Column
    protected long id;
    @Column
    //needed to differentiate between tracks in the Q
    protected long uniq;
    @Column
    protected long albumId;
    @Column
    protected long duration;
    @Column
    protected String path;
    @Column
    protected String title;
    @Column
    protected String artist;

    protected String readableDuration;
    protected AlbumArtStorage aas;


    /**
     * default constructor for DBFLow, don't use this.
     */
    public Music() {}

    /**
     * Copy constructor, for adding a new copy of a track in the Q.
     * Uniq will be different for the copy, obviously
     *
     * @param m the Music to copy
     */
    public Music(Music m) {
        id = m.id;
        this.uniq = hashCode();
        setArtist(m.getArtist());
        setTitle(m.getTitle());
        path = m.getPath();
        setDuration(m.getDuration());

        readableDuration = Util.formatDuration(getDuration());

        aas = m.getAas();
    }

    /**
     * Constructs Music from a proper cursor, doesn't move it.
     * <p/>
     * needs the following columns: <br/>
     * _ID <br/>
     * ARTIST <br/>
     * TITLE <br/>
     * DATA <br/>
     * DURATION <br/>
     * ALBUM_ID <br/>
     */
    public Music(Cursor cur) {
        try {
            id = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            uniq = id;
            setArtist(cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
            setTitle(cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)));
            path = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            setDuration(cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));

            readableDuration = Util.formatDuration(getDuration());

            albumId = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));

            //the AAS is constructed when necessary
        } catch (IllegalArgumentException e) {
            Logger.e(e, "couldn't get music data from cursor");
        }
    }

    /**
     * Instantiates a new Music using a Uri. Involves quite a bit of MediaMetadataRetriever magic
     * and file name parsing heuristics.
     *
     * @param data the uri to parse
     * @throws FormatException if there was any error parsing this uri
     */
    public Music(Uri data) throws FormatException {
        try {
            String resolvedPath = "";
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();

            //if it's not a file then
            if (!data.getScheme().equals("file")) {

                ContentResolver cr = Bastet.getContext().getContentResolver();
                String mime = cr.getType(data);
                //it's not music, fail
                if (mime != null) {
                    if (!mime.contains("audio")) {
                        throw new FormatException("the file selected wasn't music");
                    }
                }

                String id;

                //if it's a document ie. document://...
                if (DocumentsContract.isDocumentUri(Bastet.getContext(), data)) {
                    //get the content id
                    String wholeID = DocumentsContract.getDocumentId(data);
                    id = wholeID.split(":")[1];
                } else {
                    //if it's anything else, hopefully content://...
                    id = data.getLastPathSegment();
                }

                //try to get the file path
                String[] column = {MediaStore.MediaColumns.DATA};
                String sel = MediaStore.MediaColumns._ID + "=?";

                //query the media storage for a file with this id
                Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        column, sel, new String[]{id}, null);

                if (cursor != null && cursor.moveToFirst()) {
                    resolvedPath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                    cursor.close();
                }

            } else {
                //it's only a file, use the path
                resolvedPath = data.getPath();
            }

            metaRetriever.setDataSource(resolvedPath);

            //look for embedded data and build the object... finally
            this.path = resolvedPath;
            this.id = uniq = this.hashCode();
            this.setTitle(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            this.setArtist(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            this.setDuration(Long.parseLong(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
            readableDuration = Util.formatDuration(getDuration());

            //if there was no data found do some filename parsing magic
            if (getTitle() == null || getTitle().isEmpty()) {
                //get the filename
                String[] pathSegments = getPath().split("/");
                String fileName = pathSegments[pathSegments.length-1];

                //replace _ with " "; this is a common substitution
                fileName = fileName.replaceAll("_", " ");

                //the file conatins dashes, hopefully it has an artist
                if (fileName.contains("-")) {
                    int dashIndex = fileName.lastIndexOf('-');

                    //set the artist to the left of the last dash
                    setArtist(fileName.substring(0, dashIndex).trim());

                    //set the track title to the right of the last dash to the end of the file name
                    if (fileName.contains(".")) {
                        int dotIndex = fileName.lastIndexOf('.');
                        setTitle(fileName.substring(dashIndex + 1, dotIndex).trim());
                    } else {
                        setTitle(fileName.substring(dashIndex + 1).trim());
                    }
                } else {
                    //no artist, just use the filename as title
                    setTitle(fileName.split("\\.")[0]);
                }
            }

            //set the artist to the general <unknown> form
            if (getArtist() == null || getArtist().isEmpty()) {
                setArtist("<unknown>");
            }

            //the AAS is constructed when necessary
        } catch (Exception e) {
            throw new FormatException("couldn't parse metadata, maybe it isn't music: " + e.getMessage());
        }
    }

    /**
     * Convenience method to handle the loading into an ImageView. This task is cancelled if
     * inDialog is set to true and then a dialog is closed. This way always the currently shown
     * ImageViews are loaded first.
     *
     * @param view     the ImageView to load the art into
     * @param inDialog true if the loading is done in a dialog
     */
    public void loadIntoViewAsync(final ImageView view, final boolean inDialog) {
        //AAS is not loaded make a new one async and load it into the view
        if (aas == null) {
            new AsyncTask<LoaderInput, Void, AlbumArtStorage>() {
                protected LoaderInput input;

                @Override
                protected void onPreExecute() {
                    //load the default as a placeholder
                    AlbumArtStorage.loadDefault(view);
                    if (inDialog) {
                        //add this task so that it can be cancelled
                        BaseDialog.addLoader(this);
                    }
                }

                @Override
                protected AlbumArtStorage doInBackground(LoaderInput... params) {
                    input = params[0];
                    if (isCancelled()) {
                        return null;
                    }
                    //create the AAS async
                    return new AlbumArtStorage(input.m);
                }

                @Override
                protected void onPostExecute(AlbumArtStorage albumArtStorage) {
                    input.m.aas = albumArtStorage;
                    //then load it on the main thread
                    if (albumArtStorage != null) {
                        albumArtStorage.loadIntoView(input.iv);
                    }
                    //loading is done, no need to cancel this anymore
                    if (input.inDialog) {
                        BaseDialog.removeLoader(this);
                    }
                }
            }.execute(new LoaderInput(this, view, inDialog));
        } else {
            //AAS is loaded, load it into the ImageView on the main thread
            view.post(new Runnable() {
                @Override
                public void run() {
                    aas.loadIntoView(view);
                }
            });
        }
    }

    /**
     * Sets the playlist this track belongs to. Used for saving the track into the database.
     *
     * @param pl the playlist
     */
    public void setPlaylistMember(Playlist pl) {
        playlistContainer = new ForeignKeyContainer<>(Playlist.class);
        playlistContainer.put(Playlist$Table.TITLE, pl.getTitle());
        playlistContainer.put(Playlist$Table.ID, pl.getId());
    }

    ///////////////////////////////////////////////////////////////////////////
    // accessors
    ///////////////////////////////////////////////////////////////////////////

    public String toString() {
        return getArtist() + " - " + getTitle();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getReadableDuration() {
        if (readableDuration == null || readableDuration.isEmpty()) {
            readableDuration = Util.formatDuration(duration);
        }
        return readableDuration;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public long getUniq() {
        return uniq;
    }

    public String getPath() {
        return path;
    }

    public AlbumArtStorage getAas() {
        return aas;
    }

    /**
     * Type safe data storage for the AsyncTask loader.
     */
    public class LoaderInput {
        protected Music m;
        protected ImageView iv;
        protected boolean inDialog;

        public LoaderInput(Music m, ImageView view, boolean inDialog) {
            this.m = m;
            this.iv = view;
            this.inDialog = inDialog;
        }
    }
}
