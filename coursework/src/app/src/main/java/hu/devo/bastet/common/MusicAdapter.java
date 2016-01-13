package hu.devo.bastet.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import hu.devo.bastet.R;
import hu.devo.bastet.service.MusicService;
import hu.devo.bastet.service.QueueManager;
import hu.devo.bastet.ui.RippleListItem;

/**
 * Generic adapter subclassed by the Q display and the track selector.
 * Created by Barnabas on 22/11/2015.
 */
public class MusicAdapter extends ArrayAdapter<Music> {

    protected final ArrayList<Music> list;
    protected Context ctx;
    protected MusicService ms;
    protected QueueManager qm;
    protected LayoutInflater inflater;
    protected int row = R.layout.list_item_music;

    @Bind(R.id.itemArt) protected ImageView art;
    @Bind(R.id.itemTitle) protected TextView title;
    @Bind(R.id.itemArtist) protected TextView artist;
    @Bind(R.id.itemDuration) protected TextView duration;
    @Bind(R.id.itemContainer) protected RippleListItem container;

    public MusicAdapter(Context context, ArrayList<Music> list, MusicService ms) {
        super(context, R.layout.list_item_music, list);

        this.ms = ms;
        ctx = context;
        this.list = list;
        this.qm = ms.getQueueManager();
        //create an inflater so that it doesn't have to be created on every getView
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;

        //convert a dialog or inflate a new one
        if (convertView == null) {
            view = inflater.inflate(row, parent, false);
        } else {
            view = convertView;
        }

        //inject resources
        ButterKnife.bind(this, view);

        //get the item
        final Music m = list.get(position);

        //load the picture
        m.loadIntoViewAsync(art, true);

        //setSelected so that the text marquees if it's too long
        artist.setText(m.getArtist());
        artist.setSelected(true);

        title.setText(m.getTitle());
        title.setSelected(true);

        duration.setText(m.getReadableDuration());

        return view;
    }
}
