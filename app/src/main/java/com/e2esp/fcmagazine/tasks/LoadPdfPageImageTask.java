package com.e2esp.fcmagazine.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPaint;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.refs.HardReference;

/**
 * Created by Zain on 2/10/2017.
 */

public class LoadPdfPageImageTask extends AsyncTask<Void, Void, Bitmap> {

    private byte[] pdfData;
    private int page;
    private ImageView imageView;

    public LoadPdfPageImageTask(byte[] pdfData, int page, ImageView imageView) {
        this.pdfData = pdfData;
        this.page = page;
        this.imageView = imageView;
    }

    @Override
    protected void onPreExecute() {
        //Settings
        PDFImage.sShowImages = true; // show images
        PDFPaint.s_doAntiAlias = true; // make text smooth
        HardReference.sKeepCaches = true; // save images in cache
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.NEW(pdfData);
            PDFFile pdfFile = new PDFFile(byteBuffer);
            float scale = 1.0f;
            int pageCount = pdfFile.getNumPages();
            if (page < pageCount) {
                PDFPage pdfPage = pdfFile.getPage(1, true);
                Bitmap bitmap = pdfPage.getImage((int) (pdfPage.getWidth() * scale), (int) (pdfPage.getHeight() * scale), null, true, true);
                return bitmap;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageView != null && bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

}
