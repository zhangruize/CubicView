package com.example.library;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;


/**
 * 一个支持3D翻转效果的ViewGroup
 */
public class CubicView extends ViewGroup {

    private final int mTouchSlop;
    private Camera mCamera = new Camera();
    private Matrix mMatrix = new Matrix();
    private float mFraction = 0;
    private boolean mTouchEnabled = true;
    private float mOldX;
    private float mOldY;
    private Style mStyle = Style.ROTATE_LEFT;
    private int mFirstPosition = 0;
    private int mOldFristPosition = 0;
    private long mDuration = 300;
    private TimeInterpolator mInterpolator = new DecelerateInterpolator();
    private ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private Animator.AnimatorListener mAnimatorListener;
    private boolean isEnd = false;
    private boolean mHandled = false;

    public enum Style {
        ROTATE_UP, ROTATE_DOWN, ROTATE_LEFT, ROTATE_RIGHT
    }

    public CubicView(Context context) {
        this(context, null);
    }

    public CubicView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public CubicView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mFraction = (float) animation.getAnimatedValue();
                invalidate();
            }
        };
        mAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isEnd = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            final View view = getChildAt(i);
            view.layout(0, 0, r - l, b - t);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
//        System.out.println("mFirstPosition " + mFirstPosition);
        drawChild(getChildAt(mFirstPosition), canvas, mStyle, mFraction, false);
        drawChild(getChildAt(getNextPosition()), canvas, mStyle, mFraction, true);
        if (isEnd) {
            if (Math.round(mFraction) == 1) {
                mFirstPosition = getNextPosition();
            }
            isEnd = false;
        }
    }


    private int getNextPosition() {
        if (mStyle == Style.ROTATE_UP || mStyle == Style.ROTATE_LEFT)
            return (mFirstPosition + 1) % getChildCount();
        else
            return (mFirstPosition - 1 + getChildCount()) % getChildCount();
    }

    private void drawChild(View child, Canvas canvas, Style style, float fraction, boolean isSecond) {
        canvas.save();
        Matrix matrix = getMatrix(child, style, fraction, isSecond);
        canvas.concat(matrix);
        drawChild(canvas, child, getDrawingTime());
        canvas.restore();
    }

    private Matrix getMatrix(View child, Style style, float fraction, boolean isSecond) {
        float centerX;
        float centerY;
        float height = child.getHeight();
        float width = child.getWidth();
        mCamera.save();
        switch (style) {
            case ROTATE_UP:
                mCamera.rotateX(isSecond ? -90 + 90 * fraction : 90 * fraction);
                mCamera.getMatrix(mMatrix);
                centerX = width / 2;
                centerY = isSecond ? 0 : height;
                mMatrix.preTranslate(-centerX, -centerY);
                mMatrix.postTranslate(centerX, centerY + (isSecond ? height - fraction * height : -fraction * height));
                break;
            case ROTATE_LEFT:
                mCamera.rotateY(isSecond ? 90 - 90 * fraction : -90 * fraction);
                mCamera.getMatrix(mMatrix);
                centerX = isSecond ? 0 : width;
                centerY = height / 2;
                mMatrix.preTranslate(-centerX, -centerY);
                mMatrix.postTranslate(centerX + (isSecond ? width - fraction * width : -fraction * width), centerY);
                break;
            case ROTATE_DOWN:
                mCamera.rotateX(isSecond ? 90 * (1 - fraction) : -90 * fraction);
                mCamera.getMatrix(mMatrix);
                centerX = width / 2;
                centerY = isSecond ? height : 0;
                mMatrix.preTranslate(-centerX, -centerY);
                mMatrix.postTranslate(centerX, centerY + (isSecond ? -(1 - fraction) * height : fraction * height));
                break;
            case ROTATE_RIGHT:
                mCamera.rotateY(isSecond ? -90 * (1 - fraction) : 90 * fraction);
                mCamera.getMatrix(mMatrix);
                centerX = isSecond ? width : 0;
                centerY = height / 2;
                mMatrix.preTranslate(-centerX, -centerY);
                mMatrix.postTranslate(centerX + (isSecond ? -(1 - fraction) * width : fraction * width), centerY);
                break;
        }
        mCamera.restore();
        return mMatrix;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mTouchEnabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case ACTION_DOWN:
                mOldX = event.getX();
                mOldY = event.getY();
                mOldFristPosition = mFirstPosition;
                mHandled = false;
                break;
            case ACTION_MOVE:
                float tempFraction = 0;
                float currentY = event.getY();
                float currentX = event.getX();
                if (!mHandled && Math.abs(event.getX() - mOldX) >= mTouchSlop) {
                    mHandled = true;
                    mStyle = event.getX() <= mOldX ? Style.ROTATE_LEFT : Style.ROTATE_RIGHT;
                } else if (!mHandled && Math.abs(event.getY() - mOldY) >= mTouchSlop) {
                    mHandled = true;
                    mStyle = event.getY() <= mOldY ? Style.ROTATE_UP : Style.ROTATE_DOWN;
                }
                if (mHandled) {
                    if (mStyle == Style.ROTATE_DOWN || mStyle == Style.ROTATE_UP) {
                        tempFraction = Math.abs(currentY - mOldY) / getHeight();
                        if (currentY <= mOldY) {
                            mStyle = Style.ROTATE_UP;
                            mFirstPosition = (mOldFristPosition + (int) tempFraction) % getChildCount();
                        } else {
                            mStyle = Style.ROTATE_DOWN;
                            mFirstPosition = (mOldFristPosition - (int) tempFraction + getChildCount()) % getChildCount();
                        }

                    } else {
                        tempFraction = Math.abs(currentX - mOldX) / getWidth();
                        if (currentX <= mOldX) {
                            mStyle = Style.ROTATE_LEFT;
                            mFirstPosition = (mOldFristPosition + (int) tempFraction) % getChildCount();
                        } else {
                            mStyle = Style.ROTATE_RIGHT;
                            mFirstPosition = (mOldFristPosition - (int) tempFraction + getChildCount()) % getChildCount();
                        }
                    }
                    mFraction = tempFraction - (int) tempFraction;
                    invalidate();
                }
                break;
            case ACTION_UP:
                if (Math.round(mFraction) == 1) {
                    getValueAnimator(mFraction, 1).start();
                } else {
                    getValueAnimator(mFraction, 0).start();
                }
                break;
        }
        return true;
    }

    private ValueAnimator getValueAnimator(float from, float to) {
        ValueAnimator animator = new ValueAnimator();
        animator.setDuration(mDuration);
        animator.setInterpolator(mInterpolator);
        animator.setFloatValues(from, to);
        animator.addUpdateListener(mUpdateListener);
        animator.addListener(mAnimatorListener);
        return animator;
    }
}
