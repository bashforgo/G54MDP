package hu.devo.bastet.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.joanzapata.iconify.Iconify;

/**
 * Created by Barnabas on 16/11/2015.
 */
public class IconRippleButton extends com.rey.material.widget.Button {

    public IconRippleButton(Context context) {
        this(context, null);
    }

    public IconRippleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconRippleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IconRippleButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setTransformationMethod(null);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(Iconify.compute(getContext(), text, this), type);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return isEnabled() && super.onTouchEvent(event);
    }
}
