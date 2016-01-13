package hu.devo.bastet.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.support.v4.media.session.MediaSessionCompat;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import hu.devo.bastet.Bastet;
import hu.devo.bastet.common.Music;
import hu.devo.bastet.database.PlaylistManager;
import hugo.weaving.DebugLog;

/**
 * Class that acts as a mediator between all the background wizardry.
 */
public class MusicService extends Service {


    /**
     * I rolled my own broadcasts for downstream communication with the service as the built in one
     * seemed too heavy weight for what I'm trying to accomplish here.
     */
    protected List<MusicEventListener> musicEventListeners = new ArrayList<>(2);
    protected List<QueueEventListener> queueEventListeners = new ArrayList<>(2);

    protected boolean activityVisible;
    protected QueueManager queueManager;
    protected PlaybackManager playbackManager;
    protected PlaybackNotificationManager notifManager;
    protected MusicServiceBinder binder = new MusicServiceBinder();

    ///////////////////////////////////////////////////////////////////////////
    // lifecycle
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate() {
        playbackManager = new PlaybackManager(this);
        ComponentName cn = new ComponentName
                ("hu.devo.bastet.service", "PlaybackNotificationManager.NotificationActionReceiver");
        MediaSessionCompat sess = new MediaSessionCompat(Bastet.getContext(), "Bastet", cn, null);
        notifManager = new PlaybackNotificationManager(this, sess.getSessionToken());
        queueManager = new QueueManager(this);
        //try to load back the previous Q
        PlaylistManager.loadQ(queueManager);
    }

    @DebugLog
    @Override
    public void onDestroy() {
        notifManager.removeNotification();
        //save the Q
        PlaylistManager.retainQ(queueManager.getQ());
        //since the service is killed there is no need for the process anymore either
        //this should only happen when the user explicitly asks for this by pressing the x
        //in the notification
        Logger.i("killing Bastet, goodbye");
        Process.killProcess(Process.myPid());
    }

    @DebugLog
    @Override
    public IBinder onBind(Intent intent) {
        Logger.i("binding");
        return binder;
    }

    @DebugLog
    @Override
    public boolean onUnbind(Intent intent) {
        Logger.i("unbinding");
        return false;
    }

    /**
     * Play the passed in Music.
     *
     * @param m the Music to play
     */
    public void play(Music m) {
        Logger.i("playing " + m);
        //create a new notification
        notifManager.createNotification(m, true);
        //notify the QueueManager
        getQueueManager().play(m);
        //and start the playback
        playbackManager.play(m);
        //notify UI listeners
        notifyTrackChange(m);
    }

    ///////////////////////////////////////////////////////////////////////////
    // play controls
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Pause and create a notification
     */
    public void pause() {
        pause(true);
    }

    /**
     * Pause and maybe create a notification
     *
     * @param createNotif true if you want a notification to be created
     */
    private void pause(boolean createNotif) {
        Logger.i("pausing " + getQueueManager().getNowPlaying());
        playbackManager.pause();
        if (createNotif) {
            notifManager.createNotification(queueManager.getNowPlaying(), false);
        }
    }

    /**
     * Removes the notification and stops the process if the activity is in the background.
     */
    @DebugLog
    public void terminateMaybe() {
        pause(false);
        notifManager.removeNotification();
        if (!activityVisible) {
            Logger.i("terminating at " + getQueueManager().getNowPlaying());
            notifyTerminate();
        }
    }

    /**
     * Resume playback.
     */
    public void resume() {
        Logger.i("resuming " + getQueueManager().getNowPlaying());
        playbackManager.resume();
        notifManager.createNotification(queueManager.getNowPlaying(), true);
    }

    /**
     * Play the previous track.
     */
    public void prev() {
        Music m = getQueueManager().getPrev();
        play(m);
    }

    /**
     * Play the next track.
     */
    public void next() {
        Music m = getQueueManager().getNext();
        play(m);
    }

    /**
     * Seek to the position
     *
     * @param to the position to seek to
     */
    public void seek(int to) {
        playbackManager.seek(to);
    }

    public boolean isPlaying() {
        return playbackManager.isPlaying();
    }

    ///////////////////////////////////////////////////////////////////////////
    // accessors
    ///////////////////////////////////////////////////////////////////////////

    public void setPlaylist(List<Music> tracks) {
        getQueueManager().setQ(tracks);
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public ArrayList<Music> getQ() {
        return queueManager.getQ();
    }

    public void setActivityVisible(boolean activityVisible) {
        this.activityVisible = activityVisible;
    }

    public void addMusicEventListener(MusicEventListener l) {
        if (l != null) {
            musicEventListeners.add(l);
            l.listening = true;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // MusicEventListener
    ///////////////////////////////////////////////////////////////////////////

    public void removeMusicEventListener(MusicEventListener l) {
        musicEventListeners.remove(l);
        l.listening = false;
    }

    public void notifyTrackChange(Music m) {
        for (MusicEventListener l : musicEventListeners) {
            l.onTrackChange(m);
        }
    }

    public void notifyProgress(int progress) {
        for (MusicEventListener l : musicEventListeners) {
            l.onProgress(progress);
        }
    }

    public void notifyForcedPause() {
        for (MusicEventListener l : musicEventListeners) {
            l.onForcedStateChange(false);
        }
    }

    public void notifyForcedResume() {
        for (MusicEventListener l : musicEventListeners) {
            l.onForcedStateChange(true);
        }
    }

    public void notifyError() {
        for (MusicEventListener l : musicEventListeners) {
            l.onError();
        }
    }

    public void notifyTerminate() {
        for (MusicEventListener l : musicEventListeners) {
            l.onTerminate();
        }
    }

    public void addQueueEventListener(QueueEventListener l) {
        if (l != null) {
            queueEventListeners.add(l);
            l.listening = true;
        }
    }

    public void removeQueueEventListener(QueueEventListener l) {
        queueEventListeners.remove(l);
        l.listening = false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // QueueEventListener
    ///////////////////////////////////////////////////////////////////////////

    protected void notifyFirstAdded() {
        Logger.i("first added");
        for (QueueEventListener l : queueEventListeners) {
            l.onFirstAdded();
        }
    }

    protected void notifyLastDeleted() {
        Logger.i("last deleted");
        for (QueueEventListener l : queueEventListeners) {
            l.onLastDeleted();
        }
    }

    protected void notifyNowPlayingDeleted() {
        Logger.i("now playing deleted");
        notifManager.removeNotification();
        for (QueueEventListener l : queueEventListeners) {
            l.onNowPlayingDeleted();
        }
    }

    protected void notifyListChanged() {
        Logger.i("now playing deleted");
        for (QueueEventListener l : queueEventListeners) {
            l.onListChanged();
        }
    }

    public static abstract class MusicEventListener {
        protected boolean listening;

        /**
         * called every 0.5s when a track plays
         * NOT CALLED FROM THE UI THREAD
         */
        protected void onProgress(int progress) {
        }

        /**
         * called when the track changes
         */
        protected void onTrackChange(Music m) {
        }

        /**
         * called when play is forcibly started/stopped
         */
        protected void onForcedStateChange(boolean isPlaying) {
        }

        /**
         * called when an error happens
         */
        protected void onError() {
        }

        public void onTerminate() {
        }

        public boolean isListening() {
            return listening;
        }
    }

    public static abstract class QueueEventListener {
        protected boolean listening;

        /**
         * Called when the first element is added to the Q.
         */
        protected void onFirstAdded() {
        }

        /**
         * Called when the last element is deleted form the Q.
         */
        protected void onLastDeleted() {
        }

        /**
         * Called when anything in the list is changed.
         */
        protected void onListChanged() {
        }

        /**
         * Called when the track that was currently playing is deleted.
         */
        protected void onNowPlayingDeleted() {
        }

        public boolean isListening() {
            return listening;
        }
    }

    public class MusicServiceBinder extends Binder {

        public MusicService getServiceInstance() {
            return MusicService.this;
        }

    }
}
