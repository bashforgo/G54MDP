package hu.devo.bastet.dialog;

import hu.devo.bastet.database.DatabaseAdapter;

/**
 * Dialog that shows a list for selecting new tracks for the Q.
 * Created by Barnabas on 01/12/2015.
 */
public class MusicSelectorDialog extends BaseDialog {
    protected static SelectorDialog content;
    protected static DatabaseAdapter adapter;


    /**
     * Initialize with an adapter to apply on the DynamicListView in the dialog.
     *
     * @param adptr the adapter
     */
    public static void init(DatabaseAdapter adptr) {
        adapter = adptr;
    }

    /**
     * Show the dialog. Content is only loaded once.
     */
    public static void show() {
        if (content == null) {
            content = new SelectorDialog("Select some files from the list below",
                    "{md-done} Done", adapter);
        }
        show(content, false);
    }

}
