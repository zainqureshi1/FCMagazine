package com.e2esp.fcmagazine.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Ali on 7/18/2017.
 */

public class DownloadMagazineTask extends AsyncTask<Void, Integer, Void> {
    private static final String TAG = "DownloadMagazineTask";

    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private String mMagazineName;

    public DownloadMagazineTask(Context context, DbxClientV2 dbxClient, String magazineName, Callback callback) {
        mContext = context;
        mMagazineName = magazineName;
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected Void doInBackground(Void... params) {
        File dir = new File(mContext.getFilesDir(), mMagazineName);
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        int i = 0;
        int total = 0;

        ListFolderResult result = null;
        try {
            result = mDbxClient.files().listFolder("/" + mMagazineName + "/");
            total = result.getEntries().size();
            publishProgress(i, total);
        } catch (DbxException e) {
            e.printStackTrace();
            mException = e;
        }

        if (result != null) {
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
                    OutputStream outputStream = null;
                    try {
                        File file = new File(dir, metadata.getName());
                        outputStream = new FileOutputStream(file);
                        mDbxClient.files().downloadBuilder(metadata.getPathLower())
                                .download(outputStream);
                        Log.d(TAG, "Downloaded File: " + file.getAbsolutePath());
                        i++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                                publishProgress(i, total);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                }

                if (!result.getHasMore()) {
                    break;
                }
                try {
                    result = mDbxClient.files().listFolderContinue(result.getCursor());
                } catch (DbxException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        mCallback.updateProgress(values[0], values[1]);
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDownloadComplete();
        }
    }

    public interface Callback {
        void onDownloadComplete();
        void updateProgress(int downloaded, int total);
        void onError(Exception e);
    }

}
