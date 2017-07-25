package com.e2esp.fcmagazine.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.widget.ImageView;

import com.e2esp.fcmagazine.utils.FileLoader;
import com.e2esp.fcmagazine.utils.Utility;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by Zain on 2/14/2017.
 */

public class Page {

    private String imagePath;
    private boolean selected;

    private Bitmap bitmap;
    private Bitmap thumbnail;

    private ImageView imageView;
    private PhotoViewAttacher photoViewAttacher;

    public Page(String imagePath, boolean selected) {
        this.imagePath = imagePath;
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Bitmap getBitmap(Context context) {
        if (bitmap == null && imagePath != null) {
            bitmap = FileLoader.loadFromFile(imagePath);
        }
        return bitmap;
    }

    public Bitmap getThumbnail(Context context) {
        if (thumbnail == null) {
            if (bitmap == null) {
                getBitmap(context);
            }
            if (bitmap != null) {
                thumbnail = Utility.getResizedBitmap(bitmap, 100);
            }
        }
        return thumbnail;
    }

    public PhotoViewAttacher attachView(ImageView imageView, final int screenWidth) {
        if (photoViewAttacher == null) {
            photoViewAttacher = new PhotoViewAttacher(imageView);
        }
        if (this.imageView == null) {
            this.imageView = imageView;
        }
        photoViewAttacher.update();
        this.imageView.post(new Runnable() {
            @Override
            public void run() {
                float[] f = new float[9];
                Page.this.imageView.getImageMatrix().getValues(f);
                float scaleX = f[Matrix.MSCALE_X];
                float drawableWidth = Page.this.imageView.getDrawable().getIntrinsicWidth();
                float imageWidth = drawableWidth * scaleX;
                photoViewAttacher.setScale(screenWidth / imageWidth);

                Matrix displayMatrix = new Matrix();
                photoViewAttacher.getSuppMatrix(displayMatrix);
                displayMatrix.getValues(f);
                photoViewAttacher.onDrag(0, -f[Matrix.MTRANS_Y]);
            }
        });
        return photoViewAttacher;
    }

    public void recycle() {
        if (bitmap != null) bitmap.recycle();
        if (thumbnail != null) thumbnail.recycle();
    }

}
