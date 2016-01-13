package hu.devo.bastet.database;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * Database used for saving playlists. I used DBFlow for this as my SQL is a bit rusty.
 * Created by Barnabas on 30/11/2015.
 */
@Database(name = BastetDB.NAME, version = BastetDB.VERSION, foreignKeysSupported = true)
public class BastetDB {
    public static final int VERSION = 1;
    public static final String NAME = "BastetDB";
}
