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

import static android.R.attr.content;
import static android.R.attr.path;
import static com.e2esp.fcmagazine.activities.ReaderActivity.magazines;

/**
 * Created by Ali on 7/18/2017.
 */

public class DownloadFileTask extends AsyncTask<FileMetadata ,Integer, File> {

    private static final String TAG = "DownLoadFile";
    private static ProgressDialog downloadProgress = null;
    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    //private Magazines magazines;
    private Exception mException;
    //private String magazineName;

    public interface Callback {
        void onDownloadComplete(File result);

        void onError(Exception e);
    }

    DownloadFileTask(Context context, DbxClientV2 dbxClient, Callback callback) {
        mContext = context;
        //this.magazineName = magazineName;
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        //Log.i("Async-Example", "onPreExecute Called");
        downloadProgress = new ProgressDialog(mContext);
        //downloadProgress = ProgressDialog.show(mContext, "Wait", "Downloading Magazine",true);
        downloadProgress.setIndeterminate(false);
        downloadProgress.setMessage("Downloading Magazine");
        downloadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadProgress.show();
        super.onPreExecute();

    }
    @Override
    protected void onPostExecute(File result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDownloadComplete(result);
        }
        downloadProgress.dismiss();

    }

    @Override
    protected File doInBackground(FileMetadata... params) {

        File dropboxDir = new File(Environment.getExternalStorageDirectory(), "Dropbox");
        if (!dropboxDir.isDirectory()) {
            dropboxDir.mkdir();
        }

        String magazinesName = magazines.getName();

        File magazinesDir = new File(dropboxDir, magazinesName);
        if (!magazinesDir.isDirectory()) {
            magazinesDir.mkdir();
        }

        int i=0;
        Log.d("Magazines Name"," Magazines Name " + magazinesName);

        String folder = "/" +magazinesName+ "/";

        ListFolderResult result = null;
        int total = 0;
        //int total = result.getEntries().size();
        try {
            result = mDbxClient.files().listFolder(folder);
             total = result.getEntries().size();
            publishProgress(i,total);
        } catch (DbxException e) {
            e.printStackTrace();
        }
        while (true) {
            for (Metadata metadata : result.getEntries()) {

                File path = new File(magazinesDir,metadata.getName());
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
                        publishProgress(i, total);
                        downloadProgress.setProgress(i);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }

                Log.d("Path ","File Name " +metadata.getPathLower());
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

        return null;

    }//function do in background end

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        downloadProgress.setProgress(values[0]);
        downloadProgress.setMax(values[1]);
    }
}
