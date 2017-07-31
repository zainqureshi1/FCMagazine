package com.e2esp.fcmagazine.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.DbxClientV2Base;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.e2esp.fcmagazine.models.Magazines;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;

import static android.R.attr.content;
import static android.R.attr.path;
import static com.e2esp.fcmagazine.activities.ReaderActivity.magazines;

/**
 * Created by Ali on 7/18/2017.
 */

public class DownloadFileTask extends AsyncTask<FileMetadata ,Integer, Void> {

    private static ProgressDialog downloadProgress = null;
    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private Magazines magazines;

    public interface Callback {
        void onDownloadComplete();
        void updateProgress();
        void onError(Exception e);
    }

    DownloadFileTask(Context context, DbxClientV2 dbxClient,Magazines magazines, Callback callback) {
        mContext = context;
        this.magazines = magazines;
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        //Log.i("Async-Example", "onPreExecute Called");
        /*downloadProgress = new ProgressDialog(mContext);
        //downloadProgress = ProgressDialog.show(mContext, "Wait", "Downloading Magazine",true);
        downloadProgress.setIndeterminate(false);
        downloadProgress.setMessage("Downloading Magazine");
        downloadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadProgress.show();
        super.onPreExecute();*/

    }
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            magazines.setDownloaded(true);
            mCallback.onDownloadComplete();
        }
        //downloadProgress.dismiss();

    }

    @Override
    protected Void doInBackground(FileMetadata... params) {

        File dropboxDir = new File(Environment.getExternalStorageDirectory(), "FC Magazine");
        if (!dropboxDir.isDirectory()) {
            dropboxDir.mkdir();
        }


        File magazinesDir = new File(dropboxDir, magazines.getName());
        if (!magazinesDir.isDirectory()) {
            magazinesDir.mkdir();
        }

        String magazineName= magazines.getName();

        int i=0;
        Log.d("Magazines Name"," Magazines Name " + magazineName);

        String folder = "/" +magazineName+ "/";

        ListFolderResult result = null;
        int total = 0;
        //int total = result.getEntries().size();
        try {
            result = mDbxClient.files().listFolder(folder);
             total = result.getEntries().size();
            magazines.setCurrentMagazinePages(i);
            magazines.setCurrentMagazinePages(total);

        } catch (DbxException e) {
            e.printStackTrace();
        }

        if(result != null) {

            while (true) {

                Collections.sort(result.getEntries(), new Comparator<Metadata>() {
                    @Override
                    public int compare(Metadata o1, Metadata o2) {
                        if (o1.getName() == null || o2.getName() == null)
                            return 0;
                        return o1.getName().compareTo(o2.getName());

                    }
                });
                for (Metadata metadata : result.getEntries()) {

                    File path = new File(magazinesDir, metadata.getName());
                    i++;

                    OutputStream downloadFile = null;

                    try {
                        downloadFile = new FileOutputStream(path);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    try {
                        //DbxClientV2 client = null;
                        FileMetadata filemetadata = mDbxClient.files().downloadBuilder(metadata.getPathLower())
                                .download(downloadFile);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } catch (DbxException e1) {
                        e1.printStackTrace();
                    } finally {
                        try {
                            downloadFile.close();
                            magazines.setCurrentMagazinePages(i);
                            publishProgress(i, total);
                            //publishProgress(i, total);
                            //downloadProgress.setProgress(i);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                    }

                    Log.d("Path ", "File Name " + metadata.getPathLower());
                    //System.out.println(metadata.getPathLower());
                }

                if (!result.getHasMore()) {
                    break;
                }

                try {
                    result = mDbxClient.files().listFolderContinue(result.getCursor());
                } catch (DbxException e) {
                    e.printStackTrace();
                }
            }//while loop end

        }

        return null;
    }//function do in background end

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        mCallback.updateProgress();
        //downloadProgress.setProgress(values[0]);
        //downloadProgress.setMax(values[1]);
    }
}
