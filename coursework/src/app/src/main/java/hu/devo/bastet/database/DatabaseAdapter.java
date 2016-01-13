package hu.devo.bastet.database;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import hu.devo.bastet.common.Music;
import hu.devo.bastet.common.MusicAdapter;
import hu.devo.bastet.common.Util;
import hu.devo.bastet.service.MusicService;
import hu.devo.bastet.ui.QueueAdapter;

/**
 * Used in the music selector dialog
 * Created by Barnabas on 22/11/2015.
 */
public class DatabaseAdapter extends MusicAdapter {
    protected final QueueAdapter qAdapter;

    public DatabaseAdapter(Context context, ArrayList<Music> list,
                           MusicService ms, QueueAdapter queueAdapter) {
        super(context, list, ms);

        qAdapter = queueAdapter;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        final Music m = list.get(position);

        //when an element is clicked in this list
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //add it to the Q
                qm.add(m);
                qAdapter.notifyDataSetChanged();
                //and make a toast
                Util.makeToast(m.getTitle() + " was successfully added to your playlist!");
            }
        });

        return view;
    }
}
