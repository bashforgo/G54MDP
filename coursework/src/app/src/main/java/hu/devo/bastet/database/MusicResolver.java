package hu.devo.bastet.database;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;

import hu.devo.bastet.Bastet;
import hu.devo.bastet.common.Music;

/**
 * Static class that contains utility methods for reading the built in music database.
 * Created by Barnabas on 19/11/2015.
 */
public class MusicResolver {
    protected static boolean fresh;
    protected static ArrayList<Music> list;
    protected static Cursor observedCursor;
    protected static DatabaseAdapter adapter;

    //keeps track of whether a refresh is needed
    protected static ContentObserver observer = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            Logger.i("music database changed, needs refresh");
            fresh = false;
        }
    };


    /**
     * store a reference to the DatabaseAdapter, so that it can be notified about a data change
     *
     * @param adptr the adapter
     */
    public static void init(DatabaseAdapter adptr) {
        adapter = adptr;
    }

    /**
     * Makes a new cursor for the database
     *
     * @return that cursor
     */
    public static
    @Nullable
    Cursor getCursor() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] cols = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";

        return Bastet.getContext().getContentResolver().query(uri, cols, selection, null, null);
    }

    /**
     * Makes a new empty list or returns one that contains all the music in the database.
     *
     * @return that list
     */
    public static ArrayList<Music> getEmptyList() {
        if (list == null) {
            Cursor c = getCursor();
            if (c != null) {
                list = new ArrayList<>(c.getCount());
                c.close();
            }
        }

        return list;
    }

    /**
     * Fill the list if necessary
     */
    public static void run() {
        if (!fresh) {
            new AsyncTask<Void,Void,Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Cursor c = getCursor();
                    if (c != null && c.moveToFirst()) {
                        while (!c.isAfterLast()) {
                            list.add(new Music(c));
                            if (adapter != null) {
                                //as soon as any music is available notify the adapter
                                //this makes for a really fluid user experience
                                publishProgress();
                            }

                            c.moveToNext();
                        }
                        fresh = true;
                        c.close();
                    }


                    return null;
                }

                @Override
                protected void onProgressUpdate(Void... values) {
                    super.onProgressUpdate(values);
                    adapter.notifyDataSetChanged();
                }

            }.execute();
        }
    }


    /**
     * Registers the observer to listen for data changes. This should be called as soon as the
     * activity becomes active.
     */
    public static void registerObserver() {
        observedCursor = getCursor();
        if (observedCursor != null) {
            observedCursor.registerContentObserver(observer);
        }
    }

    /**
     * Unregister the observer. This should be called only when the activity gets destroyed since
     * the database can change when the user puts the app in the background.
     */
    public static void unregisterObserver() {
        observedCursor.unregisterContentObserver(observer);
        observedCursor.close();
        observedCursor = null;
    }
}
