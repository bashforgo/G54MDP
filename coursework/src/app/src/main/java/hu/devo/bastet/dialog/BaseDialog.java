package hu.devo.bastet.dialog;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.rey.material.app.BottomSheetDialog;

import java.util.ArrayList;

import hu.devo.bastet.MainActivity;
import hu.devo.bastet.common.Util;

/**
 * Class that manages the BottomSheet.
 * Created by Barnabas on 01/12/2015.
 */
public class BaseDialog {
    protected static MainActivity activity;
    protected static BottomSheetDialog bottomSheet;
    protected static ArrayList<AsyncTask> loaders = new ArrayList<>(10);
    protected static ArrayList<AsyncTask<Void, Void, Void>> afterHideTasks = new ArrayList<>(10);

    /**
     * Initialise the BottomSheet dialogs. Reference for activity needed as the AddOptionsDialog
     * uses it to call startActivityForResult.
     *
     * @param act the act
     */
    public static void init(MainActivity act) {
        activity = act;
    }

    /**
     * Lazy load the BottomSheet.
     *
     * @return the sheet
     */
    public static BottomSheetDialog getBottomSheet() {
        if (bottomSheet == null) {
            bottomSheet = new BottomSheetDialog(activity);
        }
        return bottomSheet;
    }

    /**
     * Show the bottomSheet.
     *
     * @param content   the content to display
     * @param newDialog whether to create a new bottomSheet or reuse the old; reusing it and
     *                  changing its height causes a slide-out animation
     */
    public static void show(@NonNull DialogContent content, boolean newDialog) {
        //clear from the parent so it can be reattached
        Util.clearParentView(content.view);
        if (newDialog) {
            bottomSheet = null;
        }
        getBottomSheet()
                .contentView(content.view)
                .heightParam(content.height)
                .show();
    }

    /**
     * Hide the sheet.
     */
    public static void hide() {
        getBottomSheet().cancel();
        //stops image loaders so that the Q has precedence.
        stopLoaders();
        //run some callbacks
        runAfterHideTasks();
    }


    /**
     * Add a cancelable image loader task.
     *
     * @param task the task
     */
    public static void addLoader(@NonNull AsyncTask task) {
        loaders.add(task);
    }

    /**
     * Remove an image loader task. Usually after it's done.
     *
     * @param task the task
     */
    public static void removeLoader(AsyncTask task) {
        loaders.remove(task);
    }

    /**
     * Stop the image loaders.
     */
    public static void stopLoaders() {
        Logger.i("cancelling " + loaders.size() + " loaders");
        for (AsyncTask t : loaders) {
            t.cancel(true);
        }
        loaders.clear();
    }


    /**
     * Add a callback after the dialog is hidden. Used for committing to delete playlists.
     *
     * @param t the callback to run
     */
    public static void afterHide(@NonNull AsyncTask<Void, Void, Void> t) {
        afterHideTasks.add(t);
    }

    /**
     * Run the callbacks. Async.
     */
    public static void runAfterHideTasks() {
        Logger.i("running " + afterHideTasks.size() + " tasks after hiding");
        for (AsyncTask<Void, Void, Void> t : afterHideTasks) {
            t.execute();
        }
        afterHideTasks.clear();
    }


}
