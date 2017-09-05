package com.e2esp.fcmagazine.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.utils.Consts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Ali on 7/18/2017.
 */

public class DownloadCoverPagesTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "DownloadCoverPagesTask";

    private static ProgressDialog downloadProgress = null;
    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;

    public DownloadCoverPagesTask(Context context, DbxClientV2 dbxClient, Callback callback) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        downloadProgress = ProgressDialog.show(mContext, mContext.getString(R.string.downloading_cover_pages), mContext.getString(R.string.please_wait), true);
    }

    @Override
    protected Void doInBackground(Void... params) {
        File dir = new File(mContext.getFilesDir(), Consts.DIR_COVER_PAGES);

        ListFolderResult result = null;
        try {
            result = mDbxClient.files().listFolder(Consts.DB_PATH_COVER_PAGES);
        } catch (DbxException e) {
            e.printStackTrace();
            mException = e;
        }

        if (result != null) {
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    OutputStream outputStream = null;
                    try {
                        File file = new File(dir, metadata.getName());
                        outputStream = new FileOutputStream(file);
                        mDbxClient.files().downloadBuilder(metadata.getPathLower())
                                .download(outputStream);
                        Log.d(TAG, "Downloaded File: " + file.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
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
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        downloadProgress.dismiss();
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDownloadComplete();
        }
    }

    public interface Callback {
        void onDownloadComplete();
        void onError(Exception e);
    }

}
