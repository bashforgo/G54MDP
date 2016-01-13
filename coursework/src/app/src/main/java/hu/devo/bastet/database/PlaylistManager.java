package hu.devo.bastet.database;

import android.os.AsyncTask;
import android.support.annotation.WorkerThread;

import com.raizlabs.android.dbflow.list.FlowQueryList;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.List;

import hu.devo.bastet.common.Music;
import hu.devo.bastet.common.Util;
import hu.devo.bastet.service.QueueManager;
import hugo.weaving.DebugLog;

/**
 * Static class that manages the playlists in the database.
 * Created by Barnabas on 30/11/2015.
 */
public class PlaylistManager {
    public static final String DATABASE_QUEUE_IDENTIFIER = "%%%%%RETAINED_QUEUE%%%%%";

    /**
     * Makes a new playlist synchronously. The name should be set in the passed in Playlist.
     *
     * @param pl   the playlist to be saved with the name already set
     * @param list the list of tracks the playlist has
     */
    @WorkerThread
    public static void make(Playlist pl, List<Music> list) {
        pl.duration = 0;
        pl.trackCount = 0;
        pl.tracks = new FlowQueryList<>(Music.class);
        //premature save, otherwise the music couldn't be associated with it
        pl.save();

        FlowQueryList<Music> entries = new FlowQueryList<>(Music.class);
        for(Music m: list) {
            // creating copies so that the playlist association doesn't have to be stored in
            // tracks that are in the Q.
            Music entry = new Music(m);
            entry.setPlaylistMember(pl);
            entries.add(entry);
            pl.duration += entry.getDuration();
            pl.trackCount++;
        }
        pl.tracks = entries;
        //save this time with the tracks
        pl.save();
    }


    /**
     * Save a list and toast when done. Async.
     *
     * @param q    the list
     * @param name the name for the playlist
     */
    public static void save(final List<Music> q, final String name) {
        save(q, name, true);
    }


    /**
     * Save a list and toast maybe when done. Async.
     *
     * @param q     the list
     * @param name  the name for the playlist
     * @param toast false for silent
     */
    private static void save(final List<Music> q, final String name, final boolean toast) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Playlist pl = new Playlist();
                pl.setTitle(name);

                make(pl, q);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (toast) {
                    Util.makeToast("Playlist saved");
                }
            }
        }.execute();
    }

    /**
     * Overwrite a playlist and toast when done.
     *
     * @param list the new list of tracks to save
     * @param pl   the playlist to overwrite
     */
    public static void overwrite(final List<Music> list, final Playlist pl) {
        overwrite(list, pl, true);
    }

    /**
     * Overwrite a playlist and toast when done.
     *
     * @param list  the new list of tracks
     * @param pl    the playlist to overwrite
     * @param toast false for silent
     */
    public static void overwrite(final List<Music> list, final Playlist pl, final boolean toast) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String title = pl.getTitle();
                pl.delete();

                Playlist newPl = new Playlist();
                newPl.setTitle(title);

                make(pl, list);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (toast) {
                    Util.makeToast("Playlist saved");
                }
            }
        }.execute();
    }

    /**
     * Gets playlists in the database, excludes retained Qs.
     *
     * @return the playlists
     */
    @DebugLog
    public static List<Playlist> getPlaylists() {
        return new Select()
                .from(Playlist.class)
                .where(Condition.column
                        (Playlist$Table.TITLE)
                        .isNot(DATABASE_QUEUE_IDENTIFIER)
                ).queryList();
    }

    /**
     * Saves or overwrites the retained Q.
     *
     * @param q the q to retain
     */
    @DebugLog
    public static void retainQ(final List<Music> q) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (!q.isEmpty()) {
                    Playlist pl = new Select()
                            .from(Playlist.class)
                            .where(Condition.column
                                    (Playlist$Table.TITLE)
                                    .is(DATABASE_QUEUE_IDENTIFIER)
                            ).querySingle();
                    if (pl != null) {
                        overwrite(q, pl, false);
                    } else {
                        save(q, DATABASE_QUEUE_IDENTIFIER, false);
                    }
                }
                return null;
            }
        }.execute();
    }

    /**
     * Reload the retained Q.
     *
     * @param qm the QueueManager
     */
    @DebugLog
    public static void loadQ(final QueueManager qm) {
        Playlist pl = new Select()
                .from(Playlist.class)
                .where(Condition.column(Playlist$Table.TITLE).is(DATABASE_QUEUE_IDENTIFIER))
                .querySingle();

        if (pl != null) {
            qm.setQ(pl.getTracks());
            pl.delete();
        }
    }
}
