package hu.devo.bastet.dialog;

import hu.devo.bastet.database.PlaylistAdapter;

/**
 * Manges the playlist selector dialogs.
 * Created by Barnabas on 02/12/2015.
 */
public class PlaylistSelectorDialog extends BaseDialog {
    protected static SelectorDialog load;
    protected static SelectorDialog saveAs;
    protected static PlaylistAdapter adapter;


    /**
     * Show one of the two playlist dialogs. Possible values are {@link PlaylistAdapter#SAVE_AS} or
     * {@link PlaylistAdapter#LOAD}. The content is lazy loaded.
     *
     * @param type the type
     */
    public static void show(int type) {
        switch (type) {
            case PlaylistAdapter.SAVE_AS:
                if (saveAs == null) {
                    saveAs = new SelectorDialog("Select the playlist you want to overwrite",
                            "{md-cancel} Cancel", getAdapter());
                }
                getAdapter().setType(PlaylistAdapter.SAVE_AS, saveAs.listView);
                show(saveAs,false);
                break;
            case PlaylistAdapter.LOAD:
                if (load == null) {
                    load = new SelectorDialog("Select the playlist you want to load",
                            "{md-cancel} Cancel", getAdapter());
                }
                getAdapter().setType(PlaylistAdapter.LOAD, load.listView);
                show(load, true);
                break;
        }
    }


    /**
     * Lazy load the adapter
     *
     * @return the adapter
     */
    public static PlaylistAdapter getAdapter() {
        if (adapter == null) {
            adapter = new PlaylistAdapter(activity.getMusicService());
        }
        return adapter;
    }

    /**
     * Reflects {@link PlaylistAdapter#listen()}
     */
    public static void listen() {
        if (adapter != null) {
            adapter.listen();
        }
    }

    /**
     * Reflects {@link PlaylistAdapter#unlisten()}
     */
    public static void unlisten() {
        if (adapter != null) {
            adapter.unlisten();
        }
    }
}
