package com.e2esp.fcmagazine.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPaint;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.refs.HardReference;

/**
 * Created by Zain on 2/10/2017.
 */

public class LoadPdfPagesTask extends AsyncTask<Void, Void, Bitmap[]> {

    private byte[] pdfData;
    private int[] pages;

    public LoadPdfPagesTask(byte[] pdfData, int[] pages) {
        this.pdfData = pdfData;
        this.pages = pages;
    }

    @Override
    protected void onPreExecute() {
        //Settings
        PDFImage.sShowImages = true; // show images
        PDFPaint.s_doAntiAlias = true; // make text smooth
        HardReference.sKeepCaches = true; // save images in cache
    }

    @Override
    protected Bitmap[] doInBackground(Void... params) {
        Bitmap[] bitmaps = new Bitmap[pages.length];
        try {
            ByteBuffer byteBuffer = ByteBuffer.NEW(pdfData);
            PDFFile pdfFile = new PDFFile(byteBuffer);
            int imageWidth = 200;
            int pageCount = pdfFile.getNumPages();
            for (int i = 0; i < pages.length; i++) {
                try {
                    int pageNo = pages[i];
                    if (pageNo < pageCount) {
                        PDFPage pdfPage = pdfFile.getPage(pageNo, true);
                        float pageWidth = pdfPage.getWidth();
                        float pageHeight = pdfPage.getHeight();
                        float scale = imageWidth / pageWidth;
                        Bitmap bitmap = pdfPage.getImage((int) (pageWidth * scale), (int) (pageHeight * scale), null, true, true);
                        bitmaps[i] = bitmap;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.gc();
        return bitmaps;
    }

}
