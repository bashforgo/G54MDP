package hu.devo.bastet.database;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;
import com.orhanobut.logger.Logger;
import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.Model;
import com.rey.material.widget.RippleManager;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import hu.devo.bastet.Bastet;
import hu.devo.bastet.R;
import hu.devo.bastet.common.Music;
import hu.devo.bastet.common.Util;
import hu.devo.bastet.dialog.BaseDialog;
import hu.devo.bastet.service.MusicService;
import hu.devo.bastet.ui.RippleListItem;

/**
 * DynamicListView adapter used for displaying and managing the playlists in the database.
 * Created by Barnabas on 02/12/2015.
 */
public class PlaylistAdapter extends ArrayAdapter<Playlist> {
    /**
     * onClick overwrites a playlist
     */
    public static final int SAVE_AS = 0;
    /**
     * onClick loads a playlist
     */
    public static final int LOAD = 1;

    protected int type;
    protected Context ctx;
    protected MusicService ms;
    protected List<Playlist> list;
    protected boolean needsRefresh;
    protected LayoutInflater inflater;
    protected FlowContentObserver observer;
    protected int row = R.layout.list_item_playlist;

    @Bind(R.id.itemTitle) protected TextView title;
    @Bind(R.id.itemDuration) protected TextView duration;
    @Bind(R.id.itemContainer) protected RippleListItem item;
    @Bind(R.id.itemTrackCount) protected TextView trackCount;

    @Bind(R.id.itemArt) protected ImageView art;
    @Bind(R.id.itemArt0) protected ImageView art0;
    @Bind(R.id.itemArt1) protected ImageView art1;
    @Bind(R.id.itemArt2) protected ImageView art2;
    @Bind(R.id.itemArt3) protected ImageView art3;
    @Bind(R.id.itemArtGrid) protected GridLayout artGrid;

    /**
     * delete the playlist form the database when dismissed
     */
    protected OnDismissCallback onDismiss = new OnDismissCallback() {
        @Override
        public void onDismiss(@NonNull ViewGroup listView, @NonNull int[] reverseSortedPositions) {
            for (int position : reverseSortedPositions) {
                final Playlist pl = list.get(position);
                //commit only when the dialog is hidden
                //better user experience, and would interfere with the automatic playlist observer
                BaseDialog.afterHide(new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        pl.delete();
                        return null;
                    }
                });
                remove(pl);
            }
        }
    };


    /**
     * Instantiates a new Playlist adapter. Used an overloaded constructor because instantiating the
     * superclass is not possible after a method call, therefore getPlaylists would be called twice.
     *
     * @param ms the global MusicService
     */
    public PlaylistAdapter(MusicService ms) {
        this(PlaylistManager.getPlaylists());
        this.ms = ms;
    }

    private PlaylistAdapter(final List<Playlist> list) {
        super(Bastet.getContext(), R.layout.list_item_playlist, list);
        ctx = Bastet.getContext();
        inflater = LayoutInflater.from(ctx);
        this.list = list;
        listen();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v;

        //refresh the playlist if needed
        if (needsRefresh) {
            refresh();
        }
        //convert a dialog or inflate a new one
        if (convertView == null) {
            v = inflater.inflate(row, parent, false);
        } else {
            v = convertView;
            RippleManager.cancelRipple(v);
        }

        RippleListItem view = (RippleListItem) v;

        //inject resources
        ButterKnife.bind(this, view);

        //get the item
        Playlist pl = list.get(position);

        //load the picture(s)
        if (pl.trackCount >= 4) {
            art.setVisibility(View.GONE);
            artGrid.setVisibility(View.VISIBLE);

            List<Music> tracks = pl.getFirstFour();
            tracks.get(0).loadIntoViewAsync(art0, true);
            tracks.get(1).loadIntoViewAsync(art1, true);
            tracks.get(2).loadIntoViewAsync(art2, true);
            tracks.get(3).loadIntoViewAsync(art3, true);
        } else if (pl.trackCount > 0) {
            art.setVisibility(View.VISIBLE);
            artGrid.setVisibility(View.GONE);

            Music titleTrack = pl.getFirst();
            titleTrack.loadIntoViewAsync(art, true);
        } else {
            art.setVisibility(View.VISIBLE);
            artGrid.setVisibility(View.GONE);

            Picasso.with(ctx).load(R.drawable.cd).into(art);
        }

        //setSelected so that the text marquees if it's too long
        trackCount.setText(pl.trackCount + (pl.trackCount == 1 ? " track" : " tracks"));
        trackCount.setSelected(true);

        title.setText(pl.getTitle());
        title.setSelected(true);

        duration.setText(Util.formatDuration(pl.duration));

        //the only difference between the 2 types is the OnClickListener
        switch (type) {
            case LOAD:
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //set the current Q as this playlist
                        List<Music> tracks = list.get(position).getTracks();
                        ms.setPlaylist(tracks);
                        BaseDialog.hide();
                    }
                });
                //when an item is dismissed the ripple gets stuck because the CANCEL event is not
                //fired for the onTouch method
                //this gets rid of that, that doesn't look horribly bad
                view.setCancelRippleAfter(800);
                break;
            case SAVE_AS:
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //overwrite an existing playlist
                        PlaylistManager.overwrite(ms.getQueueManager().getQ(), list.get(position));
                        BaseDialog.hide();
                    }
                });
                break;
        }


        return view;
    }

    /**
     * Enables or disables the swipe functionality based on type.
     *
     * @param type     the type
     * @param listView the list view
     * @return this
     */
    public PlaylistAdapter setType(int type, DynamicListView listView) {
        this.type = type;
        switch (type) {
            case LOAD:
                listView.enableSwipeToDismiss(onDismiss);
                break;
            case SAVE_AS:
                listView.disableSwipeToDismiss();
                break;
        }
        return this;
    }


    /**
     * Refreshes the playlist list with new data.
     */
    protected void refresh() {
        Logger.i("playlist refresh");
        list.clear();
        list.addAll(PlaylistManager.getPlaylists());
        needsRefresh = false;
        notifyDataSetChanged();
    }


    /**
     * Listens for content changes. Since this should only happen when the activity is visible this
     * should only be called in onResume.
     */
    public void listen() {
        observer = new FlowContentObserver();
        observer.addModelChangeListener(new FlowContentObserver.OnModelStateChangedListener() {
            @Override
            public void onModelStateChanged(Class<? extends Model> table, BaseModel.Action action) {
                needsRefresh = true;
            }
        });
        observer.registerForContentChanges(ctx, Playlist.class);
    }

    /**
     * Unlistens for content changes. Call in onPause.
     */
    public void unlisten() {
        observer.unregisterForContentChanges(ctx);
        observer = null;
    }
}
