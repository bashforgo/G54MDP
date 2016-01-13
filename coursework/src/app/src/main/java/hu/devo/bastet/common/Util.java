package hu.devo.bastet.common;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.wnafee.vector.MorphButton;

import java.util.concurrent.TimeUnit;

import hu.devo.bastet.Bastet;

/**
 * Utility static class
 * Created by Barnabas on 24/11/2015.
 */
public class Util {

    private static Toast toast;

    /**
     * App-wide toaster, always cancels the previous before making the new one.
     *
     * @param text the text to make the toast with.
     */
    public static void makeToast(String text) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(Bastet.getContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Sets a MorphButton's state to another one if it's not already in that state
     *
     * @param s      the state to set it to
     * @param button the button whose state needs changing
     */
    public static void setMorphButtonState(MorphButton.MorphState s, MorphButton button) {
        if (button.getState() != s) {
            button.setState(s, true);
        }
    }

    /**
     * detaches a dialog hierarchy from its parent
     *
     * @param v the view to detach
     */
    public static void clearParentView(View v) {
        ViewGroup parent = (ViewGroup) v.getParent();
        if (parent != null) {
            parent.removeView(v);
        }
    }

    /**
     * Formats a long to the app-wide track duration format.
     *
     * @param duration the duration
     * @return the formatted duration
     */
    public static String formatDuration(long duration) {
        //built in formatters don't provide this kind of formatting.
        return String.format("%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
    }

}
