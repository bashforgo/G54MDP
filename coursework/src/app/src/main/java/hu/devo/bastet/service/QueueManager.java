package hu.devo.bastet.service;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import hu.devo.bastet.common.Music;

/**
 * Handles the Q.
 * Created by Barnabas on 19/11/2015.
 */
public class QueueManager {
    protected final MusicService ms;

    protected int nowPlaying = 0;
    protected boolean isShuffling = false;

    protected ArrayList<Music> q;
    protected ArrayList<Music> pseudoQ;
    protected Random gen;

    /**
     * create empty Q
     */
    public QueueManager(MusicService ms) {
        this.ms = ms;
        q = new ArrayList<>(25);
    }

    /**
     * add a track
     */
    public void add(Music m) {
        Logger.i("adding " + m);
        if (q.isEmpty()) {
            ms.notifyFirstAdded();
        } else if (qContains(m)){
            m = new Music(m);
        }
        q.add(m);
        //if shuffling add a track in a random location
        if (isShuffling) {
            int len = pseudoQ.size();
            int loc = randBetween(0, len);
            Music now = getNowPlaying();
            pseudoQ.add(loc, m);
            nowPlaying = pseudoQ.indexOf(now);
        }
    }

    /**
     * Built in add all doesn't work as shuffling and other stuff needs to be handled above.
     *
     * @param tracks the tracks
     */
    public void addAll(List<Music> tracks) {
        for (Music m: tracks) {
            add(m);
        }
    }

    private boolean qContains(Music newMusic) {
        for (Music m: q) {
            if (m.getUniq() == newMusic.getUniq()) {
                return true;
            }
        }
        return false;
    }

    /**
     * remove a track
     */
    public void remove(Music m) {
        Logger.i("removing " + m);
        if (isShuffling) {
            Music playing = getNowPlaying();
            if (playing == m) {
                ms.notifyNowPlayingDeleted();
            }
            pseudoQ.remove(m);
            nowPlaying = pseudoQ.indexOf(playing);
        } else {
            if (0 <= nowPlaying && nowPlaying < q.size() &&
                    q.get(nowPlaying) == m) {
                ms.notifyNowPlayingDeleted();
            }
            int index = q.indexOf(m);
            if (0 <= index && index <= nowPlaying) {
                --nowPlaying;
            }
        }
        q.remove(m);
        if (q.isEmpty()) {
            ms.notifyLastDeleted();
        }
    }


    /**
     * get the item currently playing from the current Q
     */
    public Music getNowPlaying() {
        try {
            if (isShuffling) {
                return pseudoQ.get(nowPlaying);
            } else {
                return q.get(nowPlaying);
            }
        } catch (IndexOutOfBoundsException e) {
            Logger.i("this really shouldn't happen");
            return q.get(0);
        }
    }

    public void play(Music m) {
        play(q.indexOf(m));
    }

    /**
     * set now playing track <br/>
     * changes data
     */
    public Music play(int pos) {
        pos = normalizePos(pos);

        if (0 <= nowPlaying) {
            getFromCurrentQ(this.nowPlaying).setIsPlaying(false);
        }

        Music playing = q.get(pos);
        playing.setIsPlaying(true);

        //set nowPlaying to the place in the current Q
        if (isShuffling) {
            nowPlaying = pseudoQ.indexOf(playing);
        } else {
            nowPlaying = pos;
        }

        return playing;
    }

    public Music getNext() {
        int pos = normalizePos(nowPlaying + 1);
        return getFromCurrentQ(pos);
    }

    public Music getPrev() {
        int pos = normalizePos(nowPlaying - 1);
        return getFromCurrentQ(pos);
    }

    public ArrayList<Music> getQ() {
        return q;
    }

    /**
     * Sets the whole Q to this list.
     *
     * @param tracks the tracks
     */
    public void setQ(List<Music> tracks) {
        if (ms.isPlaying()) {
            ms.pause();
            ms.notifyForcedPause();
        }
        q.clear();
        ms.notifyNowPlayingDeleted();
        ms.notifyLastDeleted();

        addAll(tracks);
        if (isShuffling) {
            pseudoQ.clear();
            generatePseudoQ();
        }
        ms.notifyListChanged();
    }

    public boolean isShuffling() {
        return isShuffling;
    }

    public void setIsShuffling(boolean setShuffle) {
        if (setShuffle) {
            Music m = getNowPlaying();
            generatePseudoQ();
            nowPlaying = pseudoQ.indexOf(m);
        } else {
            Music m = getNowPlaying();
            pseudoQ.clear();
            nowPlaying = q.indexOf(m);
        }
        this.isShuffling = setShuffle;
    }

    public void toggleShuffling() {
        setIsShuffling(!isShuffling);
    }

    /**
     * generates a random Q that is hidden from the user
     */
    private void generatePseudoQ() {
        ArrayList<Music> deQ = new ArrayList<>(q);
        if (pseudoQ == null) {
            pseudoQ = new ArrayList<>(q.size());
        }
        int j = deQ.size();
        for (int i = deQ.size()-1; i >= 0; i--) {
            int loc = randBetween(0, i);
            pseudoQ.add(deQ.get(loc));
            deQ.remove(loc);
        }
        //        assert deQ.isEmpty();
    }

    /**
     * needs normalized pos
     **/
    protected Music getFromCurrentQ(int m) {
        m = normalizePos(m);
        if (isShuffling) {
            return pseudoQ.get(m);
        } else {
            return q.get(m);
        }
    }

    /**
     * round robin the list
     */
    protected int normalizePos(int pos) {
        if (0 <= pos) {
            return pos % q.size();
        } else {
            return (q.size() - Math.abs(pos)) % q.size();
        }
    }

    protected int randBetween(int min, int max) {
        if (gen == null) {
            gen = new Random();
        }

        return gen.nextInt((max - min) + 1) + min;
    }

    /**
     * Swaps two tracks in the Q.
     *
     * @param positionOne the position one
     * @param positionTwo the position two
     */
    public void swap(int positionOne, int positionTwo) {
        Logger.i("swapping " + positionOne + " and " + positionTwo);
        Collections.swap(q, positionOne, positionTwo);
        if (!isShuffling()) {
            if (nowPlaying == positionOne) {
                nowPlaying = positionTwo;
            } else if (nowPlaying == positionTwo) {
                nowPlaying = positionOne;
            }
        }
    }
}
