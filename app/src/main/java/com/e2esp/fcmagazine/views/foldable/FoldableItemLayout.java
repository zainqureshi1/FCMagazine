package com.e2esp.fcmagazine.views.foldable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.alexvasilkov.foldablelayout.shading.FoldShading;

/**
 * Created by Zain on 2/13/2017.
 */

class FoldableItemLayout extends FrameLayout {

    private static final int CAMERA_DISTANCE = 48;
    private static final float CAMERA_DISTANCE_MAGIC_FACTOR = 8f / CAMERA_DISTANCE;

    private boolean isAutoScaleEnabled;

    private final BaseLayout baseLayout;
    private final PartView coverPart;
    private final PartView coveredPart;

    private int width;
    private int height;
    private Bitmap cacheBitmap;

    private boolean isInTransformation;

    private float foldRotation;
    private float scale = 1f;
    private float scaleFactor = 1f;
    private float scaleFactorY = 1f;
    private float scaleFactorX = 1f;

    int orientation;

    FoldableItemLayout(Context context, int orientation) {
        super(context);

        this.orientation = orientation;

        baseLayout = new BaseLayout(this);

        if (orientation == FoldableListLayout.VERTICAL) {
            coverPart = new PartView(this, Gravity.TOP, orientation);
            coveredPart = new PartView(this, Gravity.BOTTOM, orientation);
        } else {
            coverPart = new PartView(this, Gravity.START, orientation);
            coveredPart = new PartView(this, Gravity.END, orientation);
        }

        setInTransformation(false);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return !isInTransformation && super.dispatchTouchEvent(ev);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (foldRotation != 0f) {
            ensureCacheBitmap();
        }

        super.dispatchDraw(canvas);
    }

    private void ensureCacheBitmap() {
        width = getWidth();
        height = getHeight();

        // Check if correct cache bitmap is already created
        if (cacheBitmap != null && cacheBitmap.getWidth() == width
                && cacheBitmap.getHeight() == height) {
            return;
        }

        if (cacheBitmap != null) {
            cacheBitmap.recycle();
            cacheBitmap = null;
        }

        if (width != 0 && height != 0) {
            try {
                cacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError outOfMemoryError) {
                cacheBitmap = null;
            }
        }

        applyCacheBitmap(cacheBitmap);
    }

    private void applyCacheBitmap(Bitmap bitmap) {
        baseLayout.setCacheCanvas(bitmap == null ? null : new Canvas(bitmap));
        coverPart.setCacheBitmap(bitmap);
        coveredPart.setCacheBitmap(bitmap);
    }

    /**
     * Fold rotation value in degrees.
     */
    public void setFoldRotation(float rotation) {
        foldRotation = rotation;

        coverPart.applyFoldRotation(rotation);
        coveredPart.applyFoldRotation(rotation);

        setInTransformation(rotation != 0f);

        scaleFactor = 1f;

        if (orientation == FoldableListLayout.VERTICAL) {
            if (isAutoScaleEnabled && width > 0) {
                double sin = Math.abs(Math.sin(Math.toRadians(rotation)));
                float dw = (float) (height * sin) * CAMERA_DISTANCE_MAGIC_FACTOR;
                scaleFactor = width / (width + dw);

                setScale(scale);
            }
        } else {
            if (isAutoScaleEnabled && height > 0) {
                double sin = Math.abs(Math.sin(Math.toRadians(rotation)));
                float dy = (float) (width * sin) * CAMERA_DISTANCE_MAGIC_FACTOR;
                scaleFactor = height / (height + dy);

                setScale(scale);
            }
        }


    }

    public void setScale(float scale) {
        this.scale = scale;

        final float scaleX = scale * scaleFactor * scaleFactorX;
        final float scaleY = scale * scaleFactor * scaleFactorY;

        if (orientation == FoldableListLayout.VERTICAL) {
            baseLayout.setScaleY(scaleFactorY);
        } else {
            baseLayout.setScaleX(scaleFactorX);
        }
        coverPart.setScaleX(scaleX);
        coverPart.setScaleY(scaleY);
        coveredPart.setScaleX(scaleX);
        coveredPart.setScaleY(scaleY);
    }

    public void setScaleFactorY(float scaleFactorY) {
        this.scaleFactorY = scaleFactorY;
        setScale(scale);
    }

    public void setScaleFactorX(float scaleFactorX) {
        this.scaleFactorX = scaleFactorX;
        setScale(scale);
    }

    /**
     * Translation preserving middle line splitting.
     */
    public void setRollingDistance(float distance) {
        final float tempScale;
        if (orientation == FoldableListLayout.VERTICAL) {
            tempScale = scale * scaleFactor * scaleFactorY;
        } else {
            tempScale = scale * scaleFactor * scaleFactorX;
        }
        coverPart.applyRollingDistance(distance, tempScale);
        coveredPart.applyRollingDistance(distance, tempScale);
    }

    private void setInTransformation(boolean isInTransformation) {
        if (this.isInTransformation == isInTransformation) {
            return;
        }
        this.isInTransformation = isInTransformation;

        baseLayout.setDrawToCache(isInTransformation);
        coverPart.setVisibility(isInTransformation ? VISIBLE : INVISIBLE);
        coveredPart.setVisibility(isInTransformation ? VISIBLE : INVISIBLE);
    }

    public void setAutoScaleEnabled(boolean isAutoScaleEnabled) {
        this.isAutoScaleEnabled = isAutoScaleEnabled;
    }

    public FrameLayout getBaseLayout() {
        return baseLayout;
    }

    public void setLayoutVisibleBounds(Rect visibleBounds) {
        coverPart.setVisibleBounds(visibleBounds);
        coveredPart.setVisibleBounds(visibleBounds);
    }

    public void setFoldShading(FoldShading shading) {
        coverPart.setFoldShading(shading);
        coveredPart.setFoldShading(shading);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Helping GC to faster clean up bitmap memory.
        // See issue #10: https://github.com/alexvasilkov/FoldableLayout/issues/10.
        if (cacheBitmap != null) {
            cacheBitmap.recycle();
            applyCacheBitmap(cacheBitmap = null);
        }
    }

    /**
     * View holder layout that can draw itself into given canvas.
     */
    @SuppressLint("ViewConstructor")
    private static class BaseLayout extends FrameLayout {

        private Canvas cacheCanvas;
        private boolean isDrawToCache;

        BaseLayout(FoldableItemLayout layout) {
            super(layout.getContext());

            final int matchParent = ViewGroup.LayoutParams.MATCH_PARENT;
            LayoutParams params = new LayoutParams(matchParent, matchParent);
            layout.addView(this, params);
            setWillNotDraw(false);
        }

        @Override
        public void draw(Canvas canvas) {
            if (isDrawToCache) {
                if (cacheCanvas != null) {
                    cacheCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    super.draw(cacheCanvas);
                }
            } else {
                super.draw(canvas);
            }
        }

        void setCacheCanvas(Canvas cacheCanvas) {
            this.cacheCanvas = cacheCanvas;
        }

        void setDrawToCache(boolean drawToCache) {
            if (isDrawToCache != drawToCache) {
                isDrawToCache = drawToCache;
                invalidate();
            }
        }

    }

    /**
     * Splat part view. It will draw top or bottom part of cached bitmap and overlay shadows.
     * Also it contains main logic for all transformations (fold rotation, scale, "rolling
     * distance").
     */
    @SuppressLint("ViewConstructor")
    private static class PartView extends View {

        private final int gravity;

        private Bitmap bitmap;
        private final Rect bitmapBounds = new Rect();

        private float clippingFactor = 0.5f;

        private final Paint bitmapPaint;

        private Rect visibleBounds;

        private int intVisibility;
        private int extVisibility;

        private float localFoldRotation;
        private FoldShading shading;

        private int orientation;

        PartView(FoldableItemLayout parent, int gravity, int orientation) {
            super(parent.getContext());
            this.gravity = gravity;
            this.orientation = orientation;

            final int matchParent = LayoutParams.MATCH_PARENT;
            parent.addView(this, new LayoutParams(matchParent, matchParent));
            setCameraDistance(CAMERA_DISTANCE * getResources().getDisplayMetrics().densityDpi);

            bitmapPaint = new Paint();
            bitmapPaint.setDither(true);
            bitmapPaint.setFilterBitmap(true);

            setWillNotDraw(false);
        }

        void setCacheBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
            calculateBitmapBounds();
        }

        void setVisibleBounds(Rect visibleBounds) {
            this.visibleBounds = visibleBounds;
            calculateBitmapBounds();
        }

        void setFoldShading(FoldShading shading) {
            this.shading = shading;
        }

        private void calculateBitmapBounds() {
            if (bitmap == null) {
                bitmapBounds.set(0, 0, 0, 0);
            } else {
                int bh = bitmap.getHeight();
                int bw = bitmap.getWidth();

                if (orientation == FoldableListLayout.VERTICAL) {
                    int top = gravity == Gravity.TOP ? 0 : (int)
                            (bh * (1f - clippingFactor) - 0.5f);
                    int bottom = gravity == Gravity.TOP ? (int) (bh * clippingFactor + 0.5f) : bh;
                    bitmapBounds.set(0, top, bw, bottom);
                } else {
                    int start = gravity == Gravity.START ? 0
                            : (int) (bw * (1f - clippingFactor) - 0.5f);
                    int end = gravity == Gravity.START ? (int) (bw * clippingFactor + 0.5f) : bw;
                    bitmapBounds.set(start, 0, end, bh);
                }

                if (visibleBounds != null) {
                    if (!bitmapBounds.intersect(visibleBounds)) {
                        bitmapBounds.set(0, 0, 0, 0); // No intersection
                    }
                }
            }

            invalidate();
        }

        void applyFoldRotation(float rotation) {
            float position = rotation;
            while (position < 0f) {
                position += 360f;
            }
            position %= 360f;
            if (position > 180f) {
                position -= 360f; // Now position is within (-180; 180]
            }

            float tempRotation = 0f;
            boolean isVisible = true;

            if (gravity == Gravity.TOP || gravity == Gravity.START) {
                if (position <= -90f || position == 180f) { // (-180; -90] || {180} - Will not show
                    isVisible = false;
                } else if (position < 0f) { // (-90; 0) - Applying rotation
                    tempRotation = position;
                }
                // [0; 180) - Holding still
            } else {
                if (position >= 90f) { // [90; 180] - Will not show
                    isVisible = false;
                } else if (position > 0f) { // (0; 90) - Applying rotation
                    tempRotation = position;
                }
                // (-180; 0] - Holding still
            }

            if (orientation == FoldableListLayout.VERTICAL) {
                setRotationX(tempRotation);
            } else {
                setRotationY(-tempRotation);
            }

            intVisibility = isVisible ? VISIBLE : INVISIBLE;
            applyVisibility();

            localFoldRotation = position;

            invalidate(); // Needed to draw shadow overlay
        }

        void applyRollingDistance(float distance, float scale) {
            if (orientation == FoldableListLayout.VERTICAL) {
                // Applying translation
                setTranslationY((int) (distance * scale + 0.5f));

                // Computing clipping for top view (bottom clipping will be 1 - topClipping)
                final int h = getHeight() / 2;
                final float topClipping = h == 0 ? 0.5f : 0.5f * (h - distance) / h;

                clippingFactor = gravity == Gravity.TOP ? topClipping : 1f - topClipping;
            } else {
                // Applying translation
                setTranslationX((int) (distance * scale + 0.5f));

                // Computing clipping for top view (bottom clipping will be 1 - topClipping)
                final int w = getWidth() / 2;
                final float startClipping = w == 0 ? 0.5f : 0.5f * (w - distance) / w;

                clippingFactor = gravity == Gravity.START ? startClipping : 1f - startClipping;
            }

            calculateBitmapBounds();
        }

        @Override
        public void setVisibility(int visibility) {
            extVisibility = visibility;
            applyVisibility();
        }

        @SuppressLint("WrongConstant")
        private void applyVisibility() {
            super.setVisibility(extVisibility == VISIBLE ? intVisibility : extVisibility);
        }

        @SuppressLint("MissingSuperCall")
        @Override
        public void draw(Canvas canvas) {
            if (shading != null) {
                shading.onPreDraw(canvas, bitmapBounds, localFoldRotation, gravity);
            }
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, bitmapBounds, bitmapBounds, bitmapPaint);
            }
            if (shading != null) {
                shading.onPostDraw(canvas, bitmapBounds, localFoldRotation, gravity);
            }
        }

    }

}