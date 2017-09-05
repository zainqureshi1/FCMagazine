package com.e2esp.fcmagazine.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

/**
 * Created by Ali on 7/25/2017.
 */

public class CountMagazinePagesTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = "CountMagazinePagesTask";

    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private final String magazineName;
    private Exception mException;
    private final int pagesInDirectory;

    public CountMagazinePagesTask(DbxClientV2 dbxClient, String magazineName, int pagesInDirectory, CountMagazinePagesTask.Callback callback) {
        this.mDbxClient = dbxClient;
        this.mCallback = callback;
        this.magazineName = magazineName;
        this.pagesInDirectory = pagesInDirectory;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int count = 0;
        ListFolderResult result = null;
        try {
            result = mDbxClient.files().listFolder("/" + magazineName + "/");
        } catch (DbxException e) {
            e.printStackTrace();
            mException = e;
        }

        while (true) {
            if (result == null) {
                break;
            }
            for (Metadata metadata : result.getEntries()) {
                count++;
                Log.d(TAG, "File Meta: " + metadata);
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
        return count;
    }

    @Override
    protected void onPostExecute(Integer pagesInDropbox) {
        super.onPostExecute(pagesInDropbox);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDownloadComplete(pagesInDropbox, pagesInDirectory);
        }
    }

    public interface Callback {
        void onDownloadComplete(Integer pagesInDropbox, int pagesInDirectory);
        void onError(Exception e);
    }

}
