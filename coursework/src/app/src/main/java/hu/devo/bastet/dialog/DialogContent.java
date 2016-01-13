package hu.devo.bastet.dialog;

import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import butterknife.ButterKnife;
import hu.devo.bastet.Bastet;
import hu.devo.bastet.R;

/**
 * Baseclass for describing the content of the bottomSheet.
 * Created by Barnabas on 01/12/2015.
 */
public class DialogContent {
    protected int height;
    protected View view;

    public DialogContent(@LayoutRes int layout, String instruction, int height) {
        view = LayoutInflater.from(Bastet.getContext()).inflate(layout,null);
        ((TextView) ButterKnife.findById(view, R.id.dialogInstruction)).setText(instruction);
        this.height = height;
    }

}
