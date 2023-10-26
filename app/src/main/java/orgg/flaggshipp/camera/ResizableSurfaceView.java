package orgg.flaggshipp.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;

public class ResizableSurfaceView extends SurfaceView {
    public ResizableSurfaceView(Context context) {
        super(context);
    }

    public ResizableSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResizableSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = View.MeasureSpec.getSize(widthMeasureSpec);

        int desiredHeight = (int) (parentWidth / 9.0 * 16.0);

        setMeasuredDimension(parentWidth, desiredHeight);
    }
}
