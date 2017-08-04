package com.e2esp.fcmagazine.activities;

/**
 * Created by Ali on 7/20/2017.
 */


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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static android.R.attr.content;
import static android.R.attr.path;

/**
 * Created by Ali on 7/18/2017.
 */

public class DownloadCoverPage extends AsyncTask<FileMetadata, Void, File> {

    private static final String TAG = "DownLoadFile";
    private static ProgressDialog downloadProgress = null;
    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;

    public interface Callback {
        void onDownloadComplete(File result);

        void onError(Exception e);
    }

    DownloadCoverPage(Context context, DbxClientV2 dbxClient, Callback callback) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        //Log.i("Async-Example", "onPreExecute Called");
        downloadProgress = ProgressDialog.show(mContext, "Wait", "Downloading Cover Pages",true);

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


        String folder_main = "FC Magazine/Cover Pages";
        File dir = new File(mContext.getFilesDir(), "Cover Pages");
        dir.mkdir();
        int i=1;

        //folderSize(dir);

        String folder = "/Cover Pages/";

        ListFolderResult result = null;
        try {
            result = mDbxClient.files().listFolder(folder);
        } catch (DbxException e) {
            e.printStackTrace();
        }
        while (true) {
            for (Metadata metadata : result.getEntries()) {

                File path = new File(dir, metadata.getName());
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
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }

                Log.d("Path ","File Name " +metadata.getName());
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


}
