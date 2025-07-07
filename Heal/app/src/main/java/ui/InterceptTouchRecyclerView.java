package ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class InterceptTouchRecyclerView extends RecyclerView {

    private int lastX, lastY;

    public InterceptTouchRecyclerView(@NonNull Context context) {
        super(context);
    }

    public InterceptTouchRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptTouchRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        int x = (int) e.getX();
        int y = (int) e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Disallow the parent (NavigationView) to intercept touch events
                // when a touch starts on this RecyclerView.
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = Math.abs(x - lastX);
                int deltaY = Math.abs(y - lastY);

                // Check if the RecyclerView can scroll vertically
                boolean canScrollVertically = canScrollVertically(deltaY > 0 ? 1 : -1);

                // If the RecyclerView can scroll vertically and the primary movement is vertical,
                // disallow parent interception. Otherwise, allow parent interception.
                if (canScrollVertically && deltaY > deltaX) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Allow the parent to intercept touch events again when the touch ends.
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }

        lastX = x;
        lastY = y;
        return super.onInterceptTouchEvent(e);
    }
}
