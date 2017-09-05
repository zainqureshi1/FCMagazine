package com.e2esp.fcmagazine.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Created by Zain on 2/10/2017.
 */

public class FileLoader {

    public static byte[] loadPDFFromAssets(Context context, String fileName) {
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            int size = inputStream.available();
            byte[] data = new byte[size];
            inputStream.read(data);
            inputStream.close();
            return data;
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] loadPDFFromStorage(String fileName) {
        try {

            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + fileName);
            RandomAccessFile f = new RandomAccessFile(file, "r");
            byte[] data = new byte[(int)f.length()];
            f.readFully(data);
            return data;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static Bitmap loadImageFromAssets(Context context, String fileName) {
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap loadFromFile(String filename) {
        try {
            File f = new File(filename);
            //if (!f.exists()) { return null; }
            Bitmap tmp = BitmapFactory.decodeFile(filename);
            return tmp;
        } catch (Exception e) {
            return null;
        }
    }

}
