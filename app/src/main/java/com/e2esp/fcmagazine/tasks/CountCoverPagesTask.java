package com.e2esp.fcmagazine.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.e2esp.fcmagazine.utils.Consts;

/**
 * Created by Ali on 7/20/2017.
 */

public class CountCoverPagesTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = "CountCoverPagesTask";

    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;

    public CountCoverPagesTask(DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int count = 0;
        ListFolderResult result = null;
        try {
            result = mDbxClient.files().listFolder(Consts.DB_PATH_COVER_PAGES);
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
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onComplete(result);
        }
    }

    public interface Callback {
        void onComplete(Integer result);

        void onError(Exception e);
    }

}