package hu.devo.bastet.dialog;

import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import hu.devo.bastet.R;
import hu.devo.bastet.ui.IconRippleButton;

/**
 * Generic dialog that shows two options.
 * Created by Barnabas on 01/12/2015.
 */
public class OptionsDialog extends DialogContent {

    /**
     * Instantiates a new dialog that gives two options.
     *
     * @param instruction should provide some context to what to chose form
     * @param option1     the first option
     * @param onOption1   what happens when the first is clicked
     * @param option2     the second option
     * @param onOption2   what happens when the second is clicked
     */
    public OptionsDialog(String instruction,
                         String option1, View.OnClickListener onOption1,
                         String option2, View.OnClickListener onOption2) {
        super(R.layout.dialog_options, instruction, ViewGroup.LayoutParams.WRAP_CONTENT);

        IconRippleButton b1 = ButterKnife.findById(view, R.id.dialogButton1);
        b1.setOnClickListener(onOption1);
        b1.setText(option1);

        IconRippleButton b2 = ButterKnife.findById(view, R.id.dialogButton2);
        b2.setOnClickListener(onOption2);
        b2.setText(option2);
    }
}
