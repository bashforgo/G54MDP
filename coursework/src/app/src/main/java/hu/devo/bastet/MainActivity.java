package hu.devo.bastet;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.FormatException;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.orhanobut.logger.Logger;
import com.rey.material.app.DialogFragment;
import com.rey.material.app.SimpleDialog;
import com.squareup.picasso.Picasso;
import com.wnafee.vector.MorphButton;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.File;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import hu.devo.bastet.common.Music;
import hu.devo.bastet.common.Util;
import hu.devo.bastet.database.DatabaseAdapter;
import hu.devo.bastet.database.MusicResolver;
import hu.devo.bastet.database.PlaylistAdapter;
import hu.devo.bastet.database.PlaylistManager;
import hu.devo.bastet.dialog.AddOptionsDialog;
import hu.devo.bastet.dialog.BaseDialog;
import hu.devo.bastet.dialog.MusicSelectorDialog;
import hu.devo.bastet.dialog.PlaylistSelectorDialog;
import hu.devo.bastet.dialog.SaveOptionsDialog;
import hu.devo.bastet.service.MusicService;
import hu.devo.bastet.ui.IconRippleButton;
import hu.devo.bastet.ui.MorphingRippleButton;
import hu.devo.bastet.ui.QueueAdapter;
import hugo.weaving.DebugLog;

public class MainActivity extends AppCompatActivity {

    public static final int EXTERNAL_SELECTOR_REQUEST_CODE = 100;

    ///////////////////////////////////////////////////////////////////////////
    // elements that are bound in onCreate
    ///////////////////////////////////////////////////////////////////////////

    //slider containing everything
    @Bind(R.id.slidingMenu) protected SlidingMenu menu;

    //dialog containing all the play controls
    @Bind(R.id.controls) protected View controls;

    //seekbar
    @Bind(R.id.seeker) protected DiscreteSeekBar seeker;

    //buttons
    @Bind(R.id.menuButton) protected MorphingRippleButton menuButton;
    @Bind(R.id.play) protected MorphingRippleButton playButton;
    @Bind(R.id.secondaryPlay) protected MorphingRippleButton secondaryPlayButton;
    @Bind(R.id.prev) protected IconRippleButton prevButton;
    @Bind(R.id.next) protected IconRippleButton nextButton;
    @Bind(R.id.share) protected IconRippleButton shareButton;
    @Bind(R.id.save) protected IconRippleButton saveButton;
    @Bind(R.id.shuffle) protected IconRippleButton shuffleButton;

    //playlist and its empty dialog
    @Bind(R.id.listView) protected DynamicListView listView;
    @Bind(R.id.empty_view) protected View emptyView;

    //views containing the track title and artist
    @Bind(R.id.info) protected LinearLayout info;
    @Bind(R.id.secondaryInfo) protected LinearLayout secondaryInfo;

    //bindings for each info component
    @Bind(R.id.albumArtBg) protected ImageView art;
    @Bind(R.id.secondaryArtBg) protected ImageView secondaryArt;

    @Bind(R.id.artist) protected TextView artist;
    @Bind(R.id.secondaryArtist) protected TextView secondaryArtist;

    @Bind(R.id.trackTitle) protected TextView title;
    @Bind(R.id.secondaryTitle) protected TextView secondaryTitle;

    @Bind(R.id.currentTime) protected TextView currentTime;
    @Bind(R.id.trackLength) protected TextView trackLength;

    ///////////////////////////////////////////////////////////////////////////
    // other fields
    ///////////////////////////////////////////////////////////////////////////

    protected boolean activateControlsOnPlay;


    protected MusicService ms;
    protected ArrayList<Music> q;
    protected QueueAdapter qAdapter;
    protected DatabaseAdapter dbAdapter;

    ///////////////////////////////////////////////////////////////////////////
    // service communication
    ///////////////////////////////////////////////////////////////////////////
    /////downstream

    protected MusicService.MusicEventListener foregroundMEL =
            new MusicService.MusicEventListener() {

                @Override
                public void onError() {
                    //if there is an error create a dialog to let the user deal with it
                    SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.PopupDialog) {
                        @Override
                        public void onPositiveActionClicked(DialogFragment fragment) {
                            qAdapter.remove(getMusicService().getQueueManager().getNowPlaying());
                            if (q.isEmpty()) {
                                //show the empty playlist
                                menu.showMenu();
                            } else {
                                //or go to the next track
                                getMusicService().next();
                            }
                            super.onPositiveActionClicked(fragment);
                        }

                        @Override
                        public void onNegativeActionClicked(DialogFragment fragment) {
                            getMusicService().next();
                            super.onNegativeActionClicked(fragment);
                        }

                        @Override
                        public void onNeutralActionClicked(DialogFragment fragment) {
                            getMusicService().play(getMusicService().getQueueManager().getNowPlaying());
                            super.onNeutralActionClicked(fragment);
                        }
                    };

                    builder
                            .message("The track " + getMusicService().getQueueManager().getNowPlaying() +
                                    " couldn't be opened. Would you like to delete it form your playlist?")
                            .title("ERROR")
                            .positiveAction("YES")
                            .neutralAction("TRY AGAIN")
                            .negativeAction("NO");

                    DialogFragment fragment = DialogFragment.newInstance(builder);
                    fragment.setCancelable(false);
                    fragment.show(getSupportFragmentManager(), "k");

                }

                @Override
                public void onProgress(final int progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //update the seeker and the current time
                            seeker.setProgress(progress);
                            currentTime.setText(Util.formatDuration(progress));
                        }
                    });
                }

            };

    protected MusicService.MusicEventListener backgroundMEL =
            new MusicService.MusicEventListener() {
                @DebugLog
                @Override
                public void onTerminate() {
                    finish();
                }

                @Override
                public void onTrackChange(Music m) {
                    loadMusicInfo(m);
                }

                @Override
                public void onForcedStateChange(boolean isPlaying) {
                    //reflect play states on button states
                    if (isPlaying) {
                        Util.setMorphButtonState(MorphButton.MorphState.END, playButton);
                        Util.setMorphButtonState(MorphButton.MorphState.END, secondaryPlayButton);
                    } else {
                        Util.setMorphButtonState(MorphButton.MorphState.START, playButton);
                        Util.setMorphButtonState(MorphButton.MorphState.START, secondaryPlayButton);
                    }
                }

                @Override
                protected void onError() {
                    if (foregroundMEL != null && !foregroundMEL.isListening()) {
                        //don't show error dialog, silently skip
                        getMusicService().next();
                    }
                }
            };
    protected MusicService.QueueEventListener qEventListener =
            new MusicService.QueueEventListener() {
                @Override
                public void onFirstAdded() {
                    activateListControls();
                }

                @Override
                public void onLastDeleted() {
                    deactivatePlayControls();
                    deactivateListControls();
                }

                @Override
                public void onListChanged() {
                    qAdapter.notifyDataSetChanged();
                }

                @Override
                public void onNowPlayingDeleted() {
                    deactivatePlayControls();
                }
            };


    /////upstream
    protected ServiceConnection sc = new ServiceConnection() {
        @DebugLog
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //store the service instance for easy communication
            ms = ((MusicService.MusicServiceBinder) service).getServiceInstance();
            q = ms.getQ();


            //setup the playlist
            qAdapter = new QueueAdapter(getApplicationContext(), getMusicService(), listView, menu);

            //add downstream communication with event listeners
            getMusicService().addMusicEventListener(foregroundMEL);
            getMusicService().addMusicEventListener(backgroundMEL);
            getMusicService().addQueueEventListener(qEventListener);

            //initialise the adapter for the internal music selector
            dbAdapter = new DatabaseAdapter(getApplicationContext(), MusicResolver.getEmptyList(), getMusicService(), qAdapter);
            MusicSelectorDialog.init(dbAdapter);
            SaveOptionsDialog.init(q);

            MusicResolver.init(dbAdapter);
            MusicResolver.run();


            boolean showMenu = true;

            //disable some buttons if the playlist is empty
            if (q.isEmpty()) {
                deactivateListControls();
            }

            //load the activity state from the MusicService
            if (ms.isPlaying()) {
                loadMusicInfo(ms.getQueueManager().getNowPlaying());
                showMenu = false;
            } else {
                deactivatePlayControls();
            }

            shuffleButton.setActivated(getMusicService().getQueueManager().isShuffling());

            ms.setActivityVisible(true);

            //if the activity was opened with a VIEW intent then parse it
            Intent intent = getIntent();
            if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri openUri = intent.getData();
                if (openUri != null) {
                    Music m = parseSingleUri(openUri);
                    showMenu = false;
                    if (m != null) {
                        ms.play(m);
                    }
                }
            }

            if (showMenu) {
                menu.showMenu();
            }
        }

        @DebugLog
        @Override
        public void onServiceDisconnected(ComponentName name) {
            ms = null;
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // lifecycle methods
    ///////////////////////////////////////////////////////////////////////////

    @DebugLog
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //start and bind to the MusicService
        //need both so that it keeps running even after unbind
        Intent binderIntent = new Intent(this, MusicService.class);
        startService(binderIntent);
        bindService(binderIntent, sc, Context.BIND_AUTO_CREATE);

        //without butterknife there would be 50 lines of findByIds here
        ButterKnife.bind(this);

        //force marquee on textviews
        title.setSelected(true);
        secondaryTitle.setSelected(true);

        artist.setSelected(true);
        secondaryArtist.setSelected(true);

        //you can slide out the playlist dialog anywhere except the playcontrols
        //the library had a bug where it wouldn't properly ignore the dialog
        //that's why it is imported as an extra module instead of compiled as a dependency
        menu.addIgnoredView(controls);

        //the menu button morphs as the playlist is shown/hidden
        menu.setOnOpenListener(new SlidingMenu.OnOpenListener() {
            @Override
            public void onOpen() {
                Util.setMorphButtonState(MorphButton.MorphState.END, menuButton);
            }
        });

        menu.setOnCloseListener(new SlidingMenu.OnCloseListener() {
            @Override
            public void onClose() {
                Util.setMorphButtonState(MorphButton.MorphState.START, menuButton);
            }
        });

        //set the seekbar's popup indicator to show the time
        seeker.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
            @Override
            public String transformToString(int value) {
                return Util.formatDuration(value);
            }

            @Override
            public boolean useStringTransform() {
                return true;
            }

            @Override
            public int transform(int value) {
                return 0;
            }
        });

        //make the seekbar seek the medaiplayer
        seeker.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) {
                    getMusicService().seek(value);
                    getMusicService().resume();
                    getMusicService().notifyForcedResume();
                }
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
            }
        });


        //show a pretty picture when the playlist is empty
        listView.setEmptyView(emptyView);

        //initialize the BottomSheet dialogs
        BaseDialog.init(this);

        MusicResolver.registerObserver();

    }

    @DebugLog
    @Override
    protected void onResume() {
        //add appropriate listeners and load activity state
        if (ms != null) {
            ms.setActivityVisible(true);
            if (!foregroundMEL.isListening()) {
                ms.addMusicEventListener(foregroundMEL);
            }
            if (!qEventListener.isListening()) {
                ms.addQueueEventListener(qEventListener);
            }
            if (ms.isPlaying()) {
                loadMusicInfo(ms.getQueueManager().getNowPlaying());
            }
        }
        PlaylistSelectorDialog.listen();
        super.onResume();
    }

    @DebugLog
    @Override
    protected void onPause() {
        //remove appropriate listeners
        if (ms != null) {
            ms.setActivityVisible(false);
            if (foregroundMEL.isListening()) {
                ms.removeMusicEventListener(foregroundMEL);
            }
            if (qEventListener.isListening()) {
                ms.removeQueueEventListener(qEventListener);
            }
            PlaylistManager.retainQ(q);
        }
        PlaylistSelectorDialog.unlisten();
        super.onPause();
    }

    @DebugLog
    @Override
    protected void onDestroy() {
        //remove everything
        if (ms != null) {
            if (backgroundMEL.isListening()) {
                ms.removeMusicEventListener(backgroundMEL);
            }
            if (!ms.isPlaying()) {
                stopService(new Intent(this, MusicService.class));
            }
        }
        unbindService(sc);
        ms = null;
        MusicResolver.unregisterObserver();
        super.onDestroy();
    }

    ///////////////////////////////////////////////////////////////////////////
    // react to queue changes
    ///////////////////////////////////////////////////////////////////////////

    protected void activatePlayControls() {
        Logger.i("activating");
        info.setVisibility(View.VISIBLE);
        secondaryInfo.setVisibility(View.VISIBLE);

        playButton.setEnabled(true);
        secondaryPlayButton.setEnabled(true);
        prevButton.setEnabled(true);
        nextButton.setEnabled(true);
        shareButton.setEnabled(true);

        seeker.setEnabled(true);
    }

    protected void deactivatePlayControls() {
        Logger.i("deactivating");
        info.setVisibility(View.GONE);
        secondaryInfo.setVisibility(View.GONE);
        Picasso.with(this).load(R.drawable.default_art).into(art);
        Picasso.with(this).load(R.drawable.default_art).into(secondaryArt);

        //        menu.showMenu();
        playButton.setEnabled(false);
        secondaryPlayButton.setEnabled(false);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
        shareButton.setEnabled(false);

        seeker.setEnabled(false);
        seeker.setProgress(0);

        currentTime.setText("0:00");
        trackLength.setText("0:00");

        activateControlsOnPlay = true;
    }

    protected void activateListControls() {
        saveButton.setEnabled(true);
        shuffleButton.setEnabled(true);
    }

    protected void deactivateListControls() {
        saveButton.setEnabled(false);
        shuffleButton.setEnabled(false);
    }

    ///////////////////////////////////////////////////////////////////////////
    // basic button functionality
    ///////////////////////////////////////////////////////////////////////////

    @OnClick(R.id.menuButton)
    protected void onMenuButtonClick() {
        menu.toggle(true);
    }


    @OnClick({R.id.play, R.id.secondaryPlay})
    protected void onPlayClick(MorphingRippleButton button) {
        MorphingRippleButton other = (button == playButton) ? secondaryPlayButton : playButton;
        if (button.getState() == MorphButton.MorphState.END) {
            getMusicService().resume();
            Util.setMorphButtonState(MorphButton.MorphState.END, other);
        } else {
            getMusicService().pause();
            Util.setMorphButtonState(MorphButton.MorphState.START, other);
        }
    }

    @OnClick(R.id.prev)
    protected void onPrevClick() {
        getMusicService().prev();
    }

    @OnClick(R.id.next)
    protected void onNextClick() {
        getMusicService().next();
    }

    @OnClick(R.id.share)
    protected void onShareClick() {
        //create a new intent with the track's uri
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        Uri uri = Uri.fromFile(new File(getMusicService().getQueueManager().getNowPlaying().getPath()));
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this track!");
        startActivity(Intent.createChooser(shareIntent, "Share this track using:"));
    }


    @OnClick(R.id.shuffle)
    protected void onShuffleClick() {
        getMusicService().getQueueManager().toggleShuffling();
        shuffleButton.setActivated(getMusicService().getQueueManager().isShuffling());
    }

    ///////////////////////////////////////////////////////////////////////////
    // filling the playlist
    ///////////////////////////////////////////////////////////////////////////

    @OnClick(R.id.add)
    protected void onAddClicked() {
        AddOptionsDialog.show();

        //prefetch data
        MusicResolver.run();
    }

    @DebugLog
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //handle external file selection
        onResume();
        if (requestCode == EXTERNAL_SELECTOR_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                ClipData uris = data.getClipData();

                if (uris == null) {
                    parseSingleUri(data.getData());
                } else {
                    parseMultiUri(uris);
                }
            }
            BaseDialog.hide();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Parses a single uri to Music.
     *
     * @param uri the uri
     * @return the music
     */
    protected Music parseSingleUri(Uri uri) {
        try {
            Music m = new Music(uri);
            getMusicService().getQueueManager().add(m);
            Util.makeToast("The file was added successfully");
            qAdapter.notifyDataSetChanged();
            return m;
        } catch (FormatException e) {
            Util.makeToast("Couldn't add that file");
            return null;
        }
    }

    /**
     * Parses multiple uris to Music and adds them to the Q.
     *
     * @param uris the uris
     */
    protected void parseMultiUri(ClipData uris) {
        boolean err = false;
        boolean singleError = true;

        boolean success = false;
        boolean singleSuccess = true;

        for (int i = 0; i < uris.getItemCount(); i++) {
            try {
                Music m = new Music(uris.getItemAt(i).getUri());
                getMusicService().getQueueManager().add(m);
                if (success) {
                    singleSuccess = false;
                }
                success = true;
            } catch (FormatException e) {
                if (err) {
                    singleError = false;
                }
                err = true;
            }
        }

        if (err) {
            if (success) {
                Util.makeToast("Some files were added, others weren't");
            } else if (singleError) {
                Util.makeToast("There was an error adding that file");
            } else {
                Util.makeToast("Couldn't add any of those files");
            }
        } else {
            if (singleSuccess) {
                Util.makeToast("The file was added successfully");
            } else {
                Util.makeToast("All files were added successfully");
            }
        }

        if (success) {
            qAdapter.notifyDataSetChanged();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // playlist persisting
    ///////////////////////////////////////////////////////////////////////////
    @OnClick(R.id.save)
    protected void onSaveClick(){
        SaveOptionsDialog.show();
    }

    @OnClick(R.id.load)
    protected void onLoadClick() {
        PlaylistSelectorDialog.show(PlaylistAdapter.LOAD);
    }

    @OnLongClick({R.id.add,R.id.save,R.id.load,R.id.shuffle})
    protected boolean onListButtonsHint(IconRippleButton button) {
        String text = "";
        switch (button.getId()) {
            case R.id.add:
                text = "Add files to the playlist";
                break;
            case R.id.save:
                text = "Save the playlist";
                break;
            case R.id.load:
                text = "Load a previously saved playlist";
                break;
            case R.id.shuffle:
                text = "Toggle shuffle mode";
                break;
            default:
                Logger.e("this really shouldn't happen");
                break;
        }
        Util.makeToast(text);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (menu.isMenuShowing()) {
            menu.toggle();
        } else {
            super.onBackPressed();
        }
    }

    public MusicService getMusicService() {
        return ms;
    }

    /**
     * Loads music metadata into the appropriate views.
     *
     * @param m the Music
     */
    protected void loadMusicInfo(Music m) {
        //activate buttons if they were disabled
        if (activateControlsOnPlay) {
            activatePlayControls();
            activateControlsOnPlay = false;
        }

        //load views with track information
        m.loadIntoViewAsync(art, false);
        m.loadIntoViewAsync(secondaryArt, false);

        title.setText(m.getTitle());
        secondaryTitle.setText(m.getTitle());

        artist.setText(m.getArtist());
        secondaryArtist.setText(m.getArtist());

        seeker.setProgress(0);
        currentTime.setText("0:00");

        trackLength.setText(m.getReadableDuration());

        seeker.setMax((int) m.getDuration());

        Util.setMorphButtonState(MorphButton.MorphState.END, playButton);
        Util.setMorphButtonState(MorphButton.MorphState.END, secondaryPlayButton);

        //notify the playlist that the dialog has to be updated
        qAdapter.notifyDataSetChanged();
    }

}
