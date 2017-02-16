package com.e2esp.fcmagazine.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.alexvasilkov.android.commons.adapters.ItemsAdapter;
import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.models.Page;

import java.util.ArrayList;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by Zain on 2/13/2017.
 */

public class PageAdapter extends ItemsAdapter<Page> {

    private Context context;
    private int screenWidth;
    private PhotoViewAttacher lastPhotoViewAttacher;

    public PageAdapter(Context context, ArrayList<Page> pages, int screenWidth) {
        super(context);
        this.context = context;
        this.screenWidth = screenWidth;
        setItemsList(pages);
    }

    @Override
    protected View createView(Page item, int pos, ViewGroup parent, LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.page_item_layout, parent, false);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.imageViewPage = (ImageView) view.findViewById(R.id.imageViewPage);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    protected void bindView(Page item, int pos, View convertView) {
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.bindView(item);
    }

    private Matrix getLastPageMatrix() {
        /*if (lastPhotoViewAttacher != null) {
            Matrix displayMatrix = new Matrix(lastPhotoViewAttacher.getImageMatrix());
            lastPhotoViewAttacher.getDisplayMatrix(displayMatrix);
            return displayMatrix;
        }*/
        return null;
    }

    private class ViewHolder {
        private ImageView imageViewPage;
        private void bindView(Page page) {
            if (imageViewPage != null) {
                Bitmap bitmap = page.getBitmap(context);
                if (bitmap != null) {
                    imageViewPage.setImageBitmap(bitmap);
                }
                lastPhotoViewAttacher = page.attachView(imageViewPage, screenWidth);
            }
        }
    }

}
