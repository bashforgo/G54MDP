package hu.devo.bastet.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.rey.material.widget.RippleManager;
import com.wnafee.vector.MorphButton;

/**
 * Created by Barnabas on 16/11/2015.
 */
public class MorphingRippleButton extends MorphButton {

    protected RippleManager rippleManager;

    public MorphingRippleButton(Context context) {
        this(context, null);
    }

    public MorphingRippleButton(Context context, AttributeSet attrs) {
        this(context, attrs, com.wnafee.vector.R.attr.morphButtonStyle);
    }

    public MorphingRippleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getRippleManager().onCreate(this, context, attrs, defStyleAttr, 0);
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
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (isEnabled()) {
            result = getRippleManager().onTouchEvent(event) || result;
        }
        return result;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RippleManager.cancelRipple(this);
    }

}
