package hu.devo.bastet.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.joanzapata.iconify.widget.IconTextView;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.dragdrop.TouchViewDraggableManager;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.DismissableManager;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;
import com.nhaarman.listviewanimations.util.Swappable;
import com.orhanobut.logger.Logger;

import butterknife.Bind;
import butterknife.BindColor;
import hu.devo.bastet.R;
import hu.devo.bastet.common.Music;
import hu.devo.bastet.common.MusicAdapter;
import hu.devo.bastet.service.MusicService;

/**
 * Created by Barnabas on 19/11/2015.
 */
public class QueueAdapter extends MusicAdapter implements Swappable {

    @Bind(R.id.itemPlayingIndicator) protected IconTextView indicator;
    @BindColor(R.color.white_more_alpha) protected int playingBackground;

    protected SlidingMenu menu;

    /**
     * sets the this as the listView's qAdapter
     * and adds DynamicListView functionality
     */
    public QueueAdapter(Context context, final MusicService ms, DynamicListView listView, SlidingMenu menu) {
        super(context, ms.getQ(), ms);

        this.menu = menu;

        listView.enableDragAndDrop();
        listView.setDraggableManager(new TouchViewDraggableManager(R.id.itemArtContainer));

        listView.enableSwipeToDismiss(
                new OnDismissCallback() {
                    @Override
                    public void onDismiss(@NonNull final ViewGroup listView, @NonNull final int[] reverseSortedPositions) {
                        for (int position : reverseSortedPositions) {
                            try {
                                remove(getItem(position));
                            } catch (IndexOutOfBoundsException e) {
                                Logger.e(position + " was deleted already");
                            }
                        }
                    }
                }
        );

        listView.setDismissableManager(new DismissableManager() {
            @Override
            public boolean isDismissable(long id, int position) {
                return !getItem(position).isPlaying() || !ms.isPlaying();
            }
        });

        listView.setAdapter(this);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RippleListItem view = (RippleListItem) super.getView(position, convertView, parent);

        final Music m = list.get(position);

        //set the indicator
        if (m.isPlaying()) {
            indicator.setVisibility(View.VISIBLE);
        } else {
            indicator.setVisibility(View.GONE);
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ms.play(m);
                notifyDataSetChanged();
                menu.toggle();
            }
        });

        view.setCancelRippleAfter(800);

        return view;
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).getUniq();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void remove(Music m) {
        qm.remove(m);
        notifyDataSetChanged();
    }

    @Override
    public void swapItems(int positionOne, int positionTwo) {
        qm.swap(positionOne, positionTwo);
        notifyDataSetChanged();
    }
}
