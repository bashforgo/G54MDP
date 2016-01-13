package hu.devo.bastet.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat.Builder;
import android.support.v7.app.NotificationCompat.MediaStyle;

import com.orhanobut.logger.Logger;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

import hu.devo.bastet.Bastet;
import hu.devo.bastet.MainActivity;
import hu.devo.bastet.R;
import hu.devo.bastet.common.AlbumArtStorage;
import hu.devo.bastet.common.Music;
import hugo.weaving.DebugLog;

/**
 * Manages the notification.
 * Created by Barnabas on 03/12/2015.
 */
public class PlaybackNotificationManager {
    protected static final int NOTIFICATION_ID = 42;
    protected static final int REQUEST_CODE = 42;
    protected static final String ACTION_STOP = "stop";
    protected static final String ACTION_PAUSE = "pause";
    protected static final String ACTION_RESUME = "resume";
    protected static final String ACTION_PREV = "prev";
    protected static final String ACTION_NEXT = "next";


    protected PendingIntent stopIntent;
    protected PendingIntent pauseIntent;
    protected PendingIntent resumeIntent;
    protected PendingIntent nextIntent;
    protected PendingIntent prevIntent;
    protected PendingIntent openIntent;

    protected Context ctx;
    protected MusicService ms;
    protected Bitmap defaultIcon;
    protected IntentFilter filter;
    protected MediaSessionCompat.Token sess;
    protected BastetNotificationBuilder builder;
    protected NotificationManager notificationManager;
    protected AsyncTask<Void, Void, Void> prevCreation;
    protected NotificationActionReceiver intentReceiver;

    /**
     * Instantiates a new Playback notification manager.
     *
     * @param ms   the MusicService
     * @param sess the MediaSessionCompat.Token needed for the
     *             NotificationCompat.MediaStyle notification
     */
    public PlaybackNotificationManager(MusicService ms, MediaSessionCompat.Token sess) {
        this.ms = ms;
        this.sess = sess;
        this.ctx = Bastet.getContext();

        notificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        //pending intents for handling notification actions
        String pkg = ctx.getPackageName();
        stopIntent = PendingIntent.getBroadcast(ctx, REQUEST_CODE,
                new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        pauseIntent = PendingIntent.getBroadcast(ctx, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        resumeIntent = PendingIntent.getBroadcast(ctx, REQUEST_CODE,
                new Intent(ACTION_RESUME).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        prevIntent = PendingIntent.getBroadcast(ctx, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(ctx, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intent = new Intent(ms, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent = PendingIntent.getActivity(ctx, REQUEST_CODE, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        //receiver and filter for actually receiving these
        intentReceiver = new NotificationActionReceiver();

        filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREV);
        filter.addAction(ACTION_STOP);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_RESUME);

        builder = new BastetNotificationBuilder();
    }

    /**
     * Create notification. isPlaying is required so that we can prepare the notification before
     * the MediaPlayer gets ready.
     *
     * @param m         the Music whose metadata needs to be displayed
     * @param isPlaying determines which actions to show
     */
    @DebugLog
    public void createNotification(final Music m, final boolean isPlaying) {
        //creating a nice notification like this takes long, so if a new notification is queued up
        //we stop creating the old one
        if (prevCreation != null) {
            prevCreation.cancel(true);
        }
        prevCreation = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Logger.i("create new notification for " + m);

                Notification notif = builder
                        .setActions(isPlaying)
                        .setMusic(m)
                        .build();

                //in foreground even if paused as going into background could kill the service
                ms.startForeground(NOTIFICATION_ID, notif);
                notificationManager.notify(NOTIFICATION_ID, notif);
                ctx.registerReceiver(intentReceiver, filter);
                prevCreation = null;
                return null;
            }
        }.execute();
    }

    public void removeNotification() {
        Logger.i("removing notification");
        if (prevCreation != null) {
            //a build might still be running, stop that
            prevCreation.cancel(true);
        }
        ms.stopForeground(true);
    }

    /**
     * Lazy load the default icon, before a cover is ready.
     *
     * @return the default icon
     */
    @WorkerThread
    protected Bitmap getDefaultIcon() {
        if (defaultIcon == null) {
            try {
                defaultIcon = Picasso.with(ctx).load("android.resource://hu.devo.bastet/" + R.drawable.cd).get();
                Logger.i("defaultIcon loaded from Picasso");
            } catch (IOException e) {
                defaultIcon = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.cd);
                Logger.i("defaultIcon loaded from system method");
            }
        }
        return defaultIcon;
    }


    /**
     * Streamlined notification creation that is reusable.
     */
    protected class BastetNotificationBuilder {
        protected Action pauseAction;
        protected Action resumeAction;
        protected ArrayList<Action> actions;
        protected Builder builder = new Builder(ctx);

        /**
         * Instantiates a new notification builder, that is reusable.
         */
        public BastetNotificationBuilder() {
            //target API is 19 the method is not yet deprecated
            //noinspection deprecation
            builder
                    .setColor(ctx.getResources().getColor(R.color.primary))
                    .setShowWhen(false)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(openIntent)
                    .setOnlyAlertOnce(true)
                    .setStyle(
                            new MediaStyle()
                                    .setMediaSession(sess)
                                    //show close and pause even when the notification is collapsed
                                    .setShowActionsInCompactView(1, 3)
                    );

            //nice LOLLIPOP behaviour
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .setVisibility(Notification.VISIBILITY_PUBLIC);
            }

            pauseAction = new Action(R.drawable.ic_pause_white_24dp, "pause", pauseIntent);
            resumeAction = new Action(R.drawable.ic_play_white_24dp, "resume", resumeIntent);

            //initially populate the actions
            actions = new ArrayList<>(4);
            actions.add(new Action(R.drawable.ic_rewind_white_24dp, "previous", prevIntent));
            actions.add(pauseAction);
            actions.add(new Action(R.drawable.ic_fast_forward_white_24dp, "next", nextIntent));
            actions.add(new Action(R.drawable.ic_close_white_24dp, "close", stopIntent));
        }

        /**
         * Sets the music for the notification. Fills the AAS if necessary,
         * should be run in a background thread.
         *
         * @param m the Music
         * @return this
         */
        @WorkerThread
        public BastetNotificationBuilder setMusic(final Music m) {
            Bitmap icon;
            AlbumArtStorage aas = m.getAas();
            if (aas != null) {
                //load the stored cover or fail with the default
                try {
                    if (aas.isUri()) {
                        icon = Picasso.with(ctx).load(aas.getUri()).get();
                    } else {
                        icon = Picasso.with(ctx).load(aas.getFile()).get();
                    }
                } catch (IOException e) {
                    icon = getDefaultIcon();
                }
            } else {
                //the AAS is not ready yet, fill it and then recreate the notification
                new AlbumArtStorage(m, new Runnable() {
                    @Override
                    public void run() {
                        Logger.i("recreating notification for " + m);
                        setMusic(m);
                        notificationManager.notify(NOTIFICATION_ID, build());
                    }
                });
                icon = getDefaultIcon();
            }

            builder
                    .setLargeIcon(icon)
                    .setContentTitle(m.getTitle())
                    .setContentText(m.getArtist());

            return this;
        }


        /**
         * Sets actions based on the play state.
         *
         * @param isPlaying whether the track is playing
         * @return this
         */
        public BastetNotificationBuilder setActions(boolean isPlaying) {
            actions.remove(1);
            if (isPlaying) {
                actions.add(1, pauseAction);
            } else {
                actions.add(1, resumeAction);
            }
            builder.mActions = actions;
            return this;
        }

        public Notification build() {
            return builder.build();
        }
    }

    /**
     * Receiver for the notification actions.
     */
    protected class NotificationActionReceiver extends BroadcastReceiver {

        @DebugLog
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_STOP:
                    ms.terminateMaybe();
                    ms.notifyForcedPause();
                    break;
                case ACTION_PAUSE:
                    ms.pause();
                    ms.notifyForcedPause();
                    break;
                case ACTION_RESUME:
                    ms.resume();
                    ms.notifyForcedResume();
                    break;
                case ACTION_PREV:
                    ms.prev();
                    break;
                case ACTION_NEXT:
                    ms.next();
                    break;
                default:
                    Logger.i("Unknown action ignored.");
                    break;
            }
        }
    }
}
