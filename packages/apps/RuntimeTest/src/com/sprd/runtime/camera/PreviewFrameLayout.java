package com.sprd.runtime.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.widget.RelativeLayout;


/**
 * Created by hefeng on 18-5-30.
 */

public class PreviewFrameLayout extends RelativeLayout {
    public interface OnSizeChangedListener {
        public void onSizeChanged(int width, int height);
    }

    private boolean mScreenState;
    public static final double VAL_WIDTH_RATIO = 4D;
    public static final double VAL_HEIGHT_RATIO = 3D;
    public static final double VAL_CAMERA_PREVIEW_RATIO = (1 / (VAL_WIDTH_RATIO / VAL_HEIGHT_RATIO));
    private double mAspectRatio;
    private PreviewFrameLayout.OnSizeChangedListener mListener;

    public PreviewFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAspectRatio(VAL_WIDTH_RATIO / VAL_HEIGHT_RATIO);
    }

    @Override
    protected void onFinishInflate() {
    }

    public void setAspectRatio(double ratio) {
        if (ratio <= 0.0)
            throw new IllegalArgumentException();
        if (Configuration.ORIENTATION_PORTRAIT == getResources().getConfiguration().orientation) {
            ratio = 1 / ratio;
        }
        if (mAspectRatio != ratio) {
            mAspectRatio = ratio;
            requestLayout();
        }
    }

    public void setAspectRatio(double ratio, boolean state) {
        if (mScreenState != state) {
            mScreenState = state;
            requestLayout();
        }
        setAspectRatio(ratio);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int previewWidth = MeasureSpec.getSize(widthSpec);
        int previewHeight = MeasureSpec.getSize(heightSpec);

        int hPadding = getPaddingLeft() + getPaddingRight();
        int vPadding = getPaddingTop() + getPaddingBottom();

        previewWidth -= hPadding;
        previewHeight -= vPadding;
        Tuple<Integer, Integer> tuple = Util.getOptimalSize(
                previewWidth, previewHeight, mAspectRatio, mScreenState);
        previewWidth = tuple.first;
        previewHeight = tuple.second;
        previewWidth += hPadding;
        previewHeight += vPadding;

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY));
    }

    public void setOnSizeChangedListener(PreviewFrameLayout.OnSizeChangedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mListener != null)
            mListener.onSizeChanged(w, h);
    }
}
