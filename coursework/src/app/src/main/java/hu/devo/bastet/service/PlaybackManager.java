package hu.devo.bastet.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import hu.devo.bastet.Bastet;
import hu.devo.bastet.common.Music;
import hugo.weaving.DebugLog;

/**
 * Handles the MediaPlayer.
 * Created by Barnabas on 20/11/2015.
 */
public class PlaybackManager implements AudioManager.OnAudioFocusChangeListener {
    protected final MusicService ms;

    protected boolean updaterEnabled;
    protected boolean noisyRegistered;
    protected boolean unNoisyRegistered;
    protected boolean wasPlaying;

    protected Timer progressUpdater;
    protected TimerTask updaterTask;
    protected FocusStates focusState;
    protected MediaPlayer mediaPlayer;

    protected PlayerEventHandler playerEventHandler = new PlayerEventHandler();
    protected AudioManager audioManager = (AudioManager)
            Bastet.getContext().getSystemService(Context.AUDIO_SERVICE);
    protected IntentFilter noisyFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    ///////////////////////////////////////////////////////////////////////////
    // playback controls
    ///////////////////////////////////////////////////////////////////////////
    protected IntentFilter unNoisyFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
    /**
     * Resume when the headset is reattached.
     */
    protected BroadcastReceiver unNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.i(String.valueOf(intent.getIntExtra("state", -1)) + " state " + isInitialStickyBroadcast());
            if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())
                    && intent.getIntExtra("state", -1) == 1
                    && !isInitialStickyBroadcast()) {
                resume();
                ms.notifyForcedResume();
                unregisterUnNoisy();
            }
        }
    };
    /**
     * Pause when a headset is unplugged.
     */
    protected BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @DebugLog
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (isPlaying()) {
                    pause();
                    ms.notifyForcedPause();
                    //listen when the headset is replugged
                    registerUnNoisy();
                }
            }
        }
    };

    /**
     * Instantiates a new Playback manager.
     *
     * @param ms the ms
     */
    public PlaybackManager(MusicService ms) {
        this.ms = ms;
    }

    public void play(Music m) {
        //get audio focus
        getFocus();

        //listen for headset unplug
        registerNoisy();

        //maybe initialize the MediaPlayer
        mediaPlayer = getMediaPlayer();

        //reset it
        mediaPlayer.reset();

        //prepare playback
        Uri uri = Uri.parse(m.getPath());
        try {
            mediaPlayer.setDataSource(Bastet.getContext(), uri);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Logger.e(e,"couldn't play music");
            ms.notifyError();
            e.printStackTrace();
        }
    }

    /**
     * Stop listeners and playback.
     */
    public void pause() {
        if (mediaPlayer != null && isPlaying()) {
            mediaPlayer.pause();
            unregisterNoisy();
            disableProgressUpdater();
        }
    }

    /**
     * Resume playback and attach listeners.
     */
    public void resume() {
        mediaPlayer.start();
        getFocus();
        registerNoisy();
        enableProgressUpdater();
    }


    ///////////////////////////////////////////////////////////////////////////
    // listeners
    ///////////////////////////////////////////////////////////////////////////

    public void seek(int to) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(to);
        }
    }

    /**
     * the progress updater is a timer task ran every 500 ms that is enabled/disabled
     * with these methods
     */
    protected void enableProgressUpdater() {
        if (!updaterEnabled) {
            progressUpdater = new Timer();
            TimerTask newUpdaterTask = new TimerTask() {
                @Override
                public void run() {
                    ms.notifyProgress(mediaPlayer.getCurrentPosition());
                }
            };
            progressUpdater.scheduleAtFixedRate(newUpdaterTask, 0, 500);
            updaterEnabled = true;
            updaterTask = newUpdaterTask;
        }
    }

    protected void disableProgressUpdater() {
        if (updaterEnabled) {
            progressUpdater.cancel();
            updaterTask.cancel();
            updaterEnabled = false;
        }
    }

    protected void registerNoisy() {
        if (!noisyRegistered) {
            Bastet.getContext().registerReceiver(noisyReceiver, noisyFilter);
            noisyRegistered = true;
        }
    }

    protected void unregisterNoisy() {
        if (noisyRegistered) {
            Bastet.getContext().unregisterReceiver(noisyReceiver);
            noisyRegistered = false;
        }
    }

    protected void registerUnNoisy() {
        if (!unNoisyRegistered) {
            Bastet.getContext().registerReceiver(unNoisyReceiver, unNoisyFilter);
            unNoisyRegistered = true;
        }
    }

    protected void unregisterUnNoisy() {
        if (unNoisyRegistered) {
            Bastet.getContext().unregisterReceiver(unNoisyReceiver);
            unNoisyRegistered = false;
        }
    }

    /**
     * Try to get the audio focus.
     */
    protected void getFocus() {
        if (focusState != FocusStates.focus) {
            int res = audioManager.requestAudioFocus
                    (this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                focusState = FocusStates.focus;
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // focus
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Release the audio focus.
     */
    protected void releaseFocus() {
        if (focusState == FocusStates.focus) {
            if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                focusState = FocusStates.noFocus;
            }
        }
    }

    @DebugLog
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (wasPlaying && !isPlaying()) {
                    ms.resume();
                    ms.notifyForcedResume();
                    wasPlaying = false;
                }
                if (focusState == FocusStates.ducking) {
                    unduck();
                }
                focusState = FocusStates.focus;
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                focusState = FocusStates.noFocus;
                if (isPlaying()) {
                    //                    stop();
                    ms.pause();
                    releaseFocus();
                    ms.notifyForcedPause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                focusState = FocusStates.noFocus;
                if (isPlaying()) {
                    ms.pause();
                    ms.notifyForcedPause();
                    wasPlaying = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                focusState = FocusStates.ducking;
                if (isPlaying()) {
                    duck();
                }
                break;
            default:
                Logger.e("this really shouldn't happen");
                break;
        }
    }

    protected void duck() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(0.2f, 0.2f);
        }
    }

    protected void unduck() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(1.0f, 1.0f);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    protected MediaPlayer getMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnErrorListener(playerEventHandler);
            mediaPlayer.setOnPreparedListener(playerEventHandler);
            mediaPlayer.setOnCompletionListener(playerEventHandler);

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setWakeMode(Bastet.getContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }
        return mediaPlayer;
    }

    ///////////////////////////////////////////////////////////////////////////
    // accessors
    ///////////////////////////////////////////////////////////////////////////

    enum FocusStates {
        noFocus, ducking, focus
    }

    protected class PlayerEventHandler implements
            MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Logger.e("mediaplayer error: %d %d", what, extra);
            if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN
                    || what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                ms.notifyError();
            }
            return true;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            //player is ready, start playback
            mp.start();
            enableProgressUpdater();
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            ms.next();
        }
    }

}
