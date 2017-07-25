package com.e2esp.fcmagazine.activities;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
/*
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
*/

import static com.e2esp.fcmagazine.activities.ReaderActivity.magazines;

/**
 * Created by Ali on 7/25/2017.
 */

public class GetMagazineList extends AsyncTask<Object, Object, Integer> {

private static final String TAG = "DownLoadFile";
private static ProgressDialog downloadProgress = null;
private final Context mContext;
private final DbxClientV2 mDbxClient;
private final Callback mCallback;
private Exception mException;

public interface Callback {
    void onDownloadComplete(Integer result);

    void onError(Exception e);
}

    GetMagazineList(Context context, DbxClientV2 dbxClient, GetMagazineList.Callback callback) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        //Log.i("Async-Example", "onPreExecute Called");
        //downloadProgress = ProgressDialog.show(mContext, "Wait", "Downloading Image",true);

    }
    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDownloadComplete(result);
        }
        //downloadProgress.dismiss();

    }

    @Override
    protected Integer doInBackground(Object... params) {

        //String folder = "/Cover Pages/";

        String magazinesName = magazines.getName();
        //Toast.makeText(mContext, "Magazine Name " +magazinesName, Toast.LENGTH_SHORT).show();
        magazinesName = "/"+magazinesName+"/";

        int count = 0;
        ListFolderResult result = null;
        try {
            result = mDbxClient.files().listFolder(magazinesName);
        } catch (DbxException e) {
            e.printStackTrace();
        }
        while (true) {
            for (Metadata metadata : result.getEntries()) {

                count++;
                Log.d("Path ","File Name " +metadata);
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

        return count;

    }//function do in background end

}
