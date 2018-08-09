package com.example.kakao.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.ScrollView;


public class PanoImageView extends ImageView {
    // Image's scroll orientation

    // Enable panorama effect or not
    private boolean mEnablePanoramaMode;

    private boolean WrapAroundView;

    // Image's width and height
    private int mDrawableWidth;
    private int mDrawableHeight;

    // View's width and height
    private int mWidth;
    private int mHeight;

    // Image's offset from initial state(center in the view).
    private float mMaxOffset;

    // The scroll progress.
    private float mProgress = 0;

    // Show scroll bar or not
    private boolean mEnableScrollbar;

    // The paint to draw scrollbar
    private Paint mScrollbarPaint;

    // Observe scroll state
    private PanoImageView.OnPanoramaScrollListener mOnPanoramaScrollListener;

    public PanoImageView(Context context) {
        this(context, null);
    }

    public PanoImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanoImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setScaleType(ScaleType.CENTER_CROP);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PanoImageView);
        mEnablePanoramaMode = typedArray.getBoolean(R.styleable.PanoImageView_piv_enablePanoMode, true);
        mEnableScrollbar = typedArray.getBoolean(R.styleable.PanoImageView_piv_show_scrollbar, true);
        typedArray.recycle();


        if (mEnableScrollbar) {
            initScrollbarPaint();
        }
    }

    private void initScrollbarPaint() {
        mScrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScrollbarPaint.setColor(Color.YELLOW);
        mScrollbarPaint.setStrokeWidth(dp2px(10f));
    }

    public void setgyroObserver(GyroObserver observer) {
        if (observer != null) {
            observer.addPanoImageView(this);
        }
    }

    void updateProgress(float progress) {
        if (mEnablePanoramaMode) {

            //Log.d("progress update",String.valueOf(progress));
            if(WrapAroundView){
                mProgress += progress;
                if(mProgress <-1){
                    mProgress = (mMaxOffset - mWidth)/mMaxOffset;
                }
                if(mProgress >1){
                    mProgress = -(mMaxOffset - mWidth)/mMaxOffset;
                }
                invalidate();
                if (mOnPanoramaScrollListener != null) {
                    mOnPanoramaScrollListener.onScrolled(this, -mProgress);
                }
            }
            else{
                mProgress += progress;
                if(mProgress<-1){mProgress=-1;}
                if(mProgress>1){mProgress=1;}
                invalidate();
                if (mOnPanoramaScrollListener != null) {
                    mOnPanoramaScrollListener.onScrolled(this, -mProgress);
                }
            }
        }
    }


    /*
     * Layout이 변경될때마다 실행되는 함수
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 화면 픽셀 크기
        mWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        mHeight = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();

        if (getDrawable() != null) {
            // 이미지의 실제크기
            mDrawableWidth = getDrawable().getIntrinsicWidth();
            mDrawableHeight = getDrawable().getIntrinsicHeight();
            // 화면크기 - 실제크기 비율.
            float imgScale = (float) mHeight / (float) mDrawableHeight;

            if(WrapAroundView){
                mMaxOffset = Math.abs((mDrawableWidth * imgScale * (1.0f/3.0f) + mWidth) * 0.5f);
            }
            else{
                mMaxOffset = Math.abs((mDrawableWidth * imgScale - mWidth) * 0.5f);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mEnablePanoramaMode || getDrawable() == null || isInEditMode()) {
            super.onDraw(canvas);
            return;
        }

        // Draw image
        float currentOffsetX = mMaxOffset * mProgress;
        canvas.save();
        //Log.d("draw image",String.valueOf(mProgress));
        canvas.translate(currentOffsetX, 0);
        super.onDraw(canvas);
        canvas.restore();

        // Draw scrollbar
        if (mEnableScrollbar) {
            float barBgWidth = mWidth * 0.9f;
            float barWidth = barBgWidth * mWidth / mDrawableWidth;

            float barBgStartX = (mWidth - barBgWidth) / 2;
            float barBgEndX = barBgStartX + barBgWidth;
            float barStartX = barBgStartX + (barBgWidth - barWidth) / 2 * (1 - mProgress);
            float barEndX = barStartX + barWidth;
            float barY = mHeight * 0.95f;

            mScrollbarPaint.setAlpha(100);
            canvas.drawLine(barBgStartX, barY, barBgEndX, barY, mScrollbarPaint);
            mScrollbarPaint.setAlpha(255);
            canvas.drawLine(barStartX, barY, barEndX, barY, mScrollbarPaint);
            mScrollbarPaint.setAlpha(100);
            canvas.drawLine(barBgStartX, barY, barBgEndX, barY, mScrollbarPaint);
            mScrollbarPaint.setAlpha(255);
            canvas.drawLine(barStartX, barY, barEndX, barY, mScrollbarPaint);
        }
    }

    private float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public void setEnablePanoramaMode(boolean enable) {
        mEnablePanoramaMode = enable;
    }

    public boolean isPanoramaModeEnabled() {
        return mEnablePanoramaMode;
    }

    public void setWrapAroundView(boolean enable){
        WrapAroundView = enable;
    }

    public boolean isWrapAroundView(){
        return WrapAroundView;
    }

    public void setEnableScrollbar(boolean enable) {
        if (mEnableScrollbar != enable){
            mEnableScrollbar = enable;
            if (mEnableScrollbar) {
                initScrollbarPaint();
            } else {
                mScrollbarPaint = null;
            }
        }
    }

    public boolean isScrollbarEnabled() {
        return mEnableScrollbar;
    }

    @Override
    public void setScaleType(ImageView.ScaleType scaleType) {
        /**
         * Do nothing because PanoImageView only
         * supports {@link scaleType.CENTER_CROP}
         */
    }

    /**
     * Interface definition for a callback to be invoked when the image is scrolling
     */
    public interface OnPanoramaScrollListener {
        /**
         * Call when the image is scrolling
         *
         * @param view the PanoImageView shows the image
         *
         * @param offsetProgress value between (-1, 1) indicating the offset progress.
         *                 -1 means the image scrolls to show its left(top) bound,
         *                 1 means the image scrolls to show its right(bottom) bound.
         */
        void onScrolled(PanoImageView view, float offsetProgress);
    }

    public void setOnPanoramaScrollListener(PanoImageView.OnPanoramaScrollListener listener) {
        mOnPanoramaScrollListener = listener;
    }
}
