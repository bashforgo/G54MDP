package hu.devo.bastet.database;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ModelContainer;
import com.raizlabs.android.dbflow.annotation.OneToMany;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.list.FlowQueryList;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;
import java.util.List;

import hu.devo.bastet.Bastet;
import hu.devo.bastet.common.Music;
import hu.devo.bastet.common.Music$Table;

/**
 * Model for DBFlow. Used to store playlists in a database.
 * Created by Barnabas on 30/11/2015.
 */
@ModelContainer
@Table(databaseName = BastetDB.NAME)
public class Playlist extends BaseModel {
    @Column
    @PrimaryKey(autoincrement = true)
    protected long id;
    @Column
    protected long trackCount;
    @Column
    protected long duration;
    @Column
    protected String title;

    protected Music first;
    protected List<Music> firstFour;
    protected FlowQueryList<Music> tracks;

    /**
     * Lazy load the tracks contained in the playlist.
     *
     * @return the tracks
     */
    @OneToMany(methods = {OneToMany.Method.SAVE, OneToMany.Method.DELETE}, variableName = "tracks")
    public FlowQueryList<Music> getTracks() {
        if (tracks == null) {
            tracks = new Select()
                    .from(Music.class)
                    .where(Condition.column(Music$Table.PLAYLISTCONTAINER_PLAYLISTID).is(getId()))
                    .queryTableList();

            //register a self observer
            tracks.enableSelfRefreshes(Bastet.getContext());
        }
        return tracks;
    }

    /**
     * Lazy load the first four tracks of the playlist.
     * Used for displaying the covers in the playlist.
     *
     * @return the first four tracks in the playlist
     */
    public List<Music> getFirstFour() {
        if (firstFour == null) {
            if (tracks != null) {
                firstFour = new ArrayList<>(4);
                for (int i = 0; i < 4; i++) {
                    firstFour.add(tracks.get(i));
                }
            } else {
                firstFour = new Select()
                        .from(Music.class)
                        .where(Condition.column(Music$Table.PLAYLISTCONTAINER_PLAYLISTID).is(getId()))
                        .limit(4)
                        .queryList();
            }
        }
        return firstFour;
    }

    /**
     * Gets the first track.
     * Used for displaying the album cover when there are less than four tracks in the playlist.
     *
     * @return the first track
     */
    public Music getFirst() {
        if (first == null) {
            if (tracks != null) {
                first = tracks.get(0);
            } else {
                first = new Select()
                        .from(Music.class)
                        .where(Condition.column(Music$Table.PLAYLISTCONTAINER_PLAYLISTID).is(getId()))
                        .querySingle();
            }
        }
        return first;
    }

    ///////////////////////////////////////////////////////////////////////////
    // accessors
    ///////////////////////////////////////////////////////////////////////////

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
