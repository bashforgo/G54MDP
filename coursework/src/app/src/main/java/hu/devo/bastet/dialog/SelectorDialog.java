package hu.devo.bastet.dialog;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;

import butterknife.ButterKnife;
import hu.devo.bastet.R;
import hu.devo.bastet.ui.IconRippleButton;

/**
 * Generic dialog that has a DynamicListView in it.
 * Created by Barnabas on 01/12/2015.
 */
public class SelectorDialog extends DialogContent {

    protected DynamicListView listView;

    /**
     * Hides the dialog
     */
    protected View.OnClickListener onCancel = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            BaseDialog.hide();
        }
    };

    /**
     * Instantiates a new Selector dialog.
     *
     * @param instruction should provide some context for what to choose from or what is displayed
     * @param cancelText  text for the bottom cancel button
     * @param adapter     the adapter to apply to the DynamicListView
     */
    public SelectorDialog(String instruction, String cancelText, ListAdapter adapter) {
        super(R.layout.dialog_selector, instruction, ViewGroup.LayoutParams.MATCH_PARENT);

        IconRippleButton cancelButton = ButterKnife.findById(view, R.id.selectorCancel);
        cancelButton.setOnClickListener(onCancel);
        cancelButton.setText(cancelText);

        listView = ButterKnife.findById(view, R.id.selectorList);
        listView.setAdapter(adapter);
    }
}
