package hu.devo.bastet.dialog;

import android.content.Intent;
import android.view.View;

import hu.devo.bastet.MainActivity;
import hu.devo.bastet.common.Util;

/**
 * Dialog to display the two options for adding music to the Q.
 * Created by Barnabas on 01/12/2015.
 */
public class AddOptionsDialog extends BaseDialog {
    protected static OptionsDialog content;

    /**
     * Show the internal selector.
     */
    protected static View.OnClickListener onInternalSelectorClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MusicSelectorDialog.show();
        }
    };

    /**
     * Fire an intent that should show an external file selector.
     */
    protected static View.OnClickListener onExternalSelectorClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent getContentIntent = new Intent();
            getContentIntent.setType("audio/*");
            getContentIntent.setAction(Intent.ACTION_GET_CONTENT);
            getContentIntent.setType("audio/*");
            String[] extraMimeTypes = {"audio/*"};
            getContentIntent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes);
            getContentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            try {
                activity.startActivityForResult(getContentIntent, MainActivity.EXTERNAL_SELECTOR_REQUEST_CODE);
            } catch (android.content.ActivityNotFoundException ex) {
                Util.makeToast("No suitable File Manager was found; try installing ES File Explorer");
            }
        }
    };

    /**
     * Show the dialog. The content is only created once.
     */
    public static void show() {
        if (content == null) {
            content = new OptionsDialog("Add music from...",
                    "{md-view-list} your music database", onInternalSelectorClick,
                    "{md-folder} an external file selector", onExternalSelectorClick);
        }
        show(content, true);
    }


}
