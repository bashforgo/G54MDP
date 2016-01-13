package hu.devo.bastet.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.rey.material.widget.RippleManager;

/**
 * Created by Barnabas on 19/11/2015.
 */
public class RippleListItem extends RelativeLayout {
    protected Runnable lastCanceler;
    protected RippleManager rippleManager;
    private int cancelRippleAfter = -1;

    public RippleListItem(Context context) {
        this(context, null);
    }

    public RippleListItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleListItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getRippleManager().onCreate(this, context, attrs, defStyleAttr, 0);
    }

    public void setCancelRippleAfter(int delay) {
        cancelRippleAfter = delay;
    }

    protected RippleManager getRippleManager() {
        if (rippleManager == null) {
            synchronized (RippleManager.class) {
                if (rippleManager == null)
                    rippleManager = new RippleManager();
            }
        }

        return rippleManager;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        RippleManager rippleManager = getRippleManager();
        if (l == rippleManager)
            super.setOnClickListener(l);
        else {
            rippleManager.setOnClickListener(l);
            setOnClickListener(rippleManager);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull final MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (0 < cancelRippleAfter) {
            //hacky way of cancelling stuck ripples
            if (lastCanceler != null) {
                removeCallbacks(lastCanceler);
            }
            lastCanceler = new Runnable() {
                @Override
                public void run() {
                    MotionEvent ev = MotionEvent.obtain(event);
                    ev.setAction(MotionEvent.ACTION_CANCEL);
                    getRippleManager().onTouchEvent(ev);
                }
            };
            postDelayed(lastCanceler, cancelRippleAfter);
        }
        return getRippleManager().onTouchEvent(event) || result;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RippleManager.cancelRipple(this);
    }
}
