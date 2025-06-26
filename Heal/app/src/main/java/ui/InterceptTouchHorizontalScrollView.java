package ui; // Or your appropriate package

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class InterceptTouchHorizontalScrollView extends HorizontalScrollView {

    public InterceptTouchHorizontalScrollView(Context context) {
        super(context);
    }

    public InterceptTouchHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptTouchHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Request disallow intercept touch event from parent when a horizontal scroll is possible.
        // This is a simplified check. More sophisticated logic might be needed for specific cases.
        if (canScrollHorizontally(1) || canScrollHorizontally(-1)) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return super.onInterceptTouchEvent(ev);
    }
}