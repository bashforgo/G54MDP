package hu.devo.bastet.dialog;

import android.support.v7.widget.AppCompatEditText;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.rey.material.app.Dialog;
import com.rey.material.app.DialogFragment;
import com.rey.material.app.SimpleDialog;

import java.util.ArrayList;

import butterknife.ButterKnife;
import hu.devo.bastet.R;
import hu.devo.bastet.common.Music;
import hu.devo.bastet.database.PlaylistAdapter;
import hu.devo.bastet.database.PlaylistManager;

/**
 * Manages the dialog that shows the two options where to save a playlist.
 * Created by Barnabas on 01/12/2015.
 */
public class SaveOptionsDialog extends BaseDialog {
    protected static OptionsDialog content;
    protected static ArrayList<Music> q;

    /**
     * Shows another popup dialog that lets you enter a name for the new playlist.
     */
    protected static View.OnClickListener saveNew = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.PopupDialog) {
                @Override
                protected void onBuildDone(final Dialog dialog) {
                    EditText editText = ButterKnife.findById(dialog, R.id.dialogEditText);

                    //attach an actionListener, so that the enter button on the keyboard
                    // will save the playlist
                    editText.setImeActionLabel("OK", EditorInfo.IME_ACTION_DONE);
                    editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                save(dialog);
                            }
                            return false;
                        }
                    });
                }

                @Override
                public void onPositiveActionClicked(DialogFragment fragment) {
                    //save it
                    save(fragment.getDialog());
                    super.onPositiveActionClicked(fragment);
                }

                /**
                 * Save the playlist and close the dialogs
                 *
                 * @param d the dialog with the EditText
                 */
                protected void save(android.app.Dialog d) {
                    AppCompatEditText editText = ButterKnife.findById(d, R.id.dialogEditText);
                    String name = editText.getText().toString().trim();
                    PlaylistManager.save(q, name);
                    hide();
                    d.dismiss();
                }
            };

            //build the dialog
            builder
                    .title("Name your new playlist")
                    .contentView(R.layout.dialog_edit_text)
                    .positiveAction("OK")
                    .negativeAction("CANCEL");

            final DialogFragment fragment = DialogFragment.newInstance(builder);
            fragment.show(activity.getSupportFragmentManager(), "edit text dialog");
        }
    };


    /**
     * Show the PlaylistSelectorDialog of type SAVE_AS
     */
    protected static View.OnClickListener saveExisting = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PlaylistSelectorDialog.show(PlaylistAdapter.SAVE_AS);
        }
    };

    /**
     * We need the reference to the Q so that it can be saved to a playlist.
     *
     * @param queue the queue
     */
    public static void init(ArrayList<Music> queue) {
        q = queue;
    }

    /**
     * Show the dialog. Lazy loaded.
     */
    public static void show() {
        if (content == null) {
            content = new OptionsDialog("Save the playlist as...",
                    "{md-playlist-add} a new playlist", saveNew,
                    "{md-loop} overwrite an existing one", saveExisting);
        }
        show(content, true);
    }


}
