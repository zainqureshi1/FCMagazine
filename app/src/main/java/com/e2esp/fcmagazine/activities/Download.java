package com.e2esp.fcmagazine.activities;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v1.DbxAccountInfo;
import com.dropbox.core.v1.DbxClientV1;
import com.dropbox.core.v1.DbxEntry;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.users.FullAccount;
import com.e2esp.fcmagazine.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.io.IOException;

import static android.R.attr.path;


/**
 * Created by Ali on 7/17/2017.
 */

public class Download extends AppCompatActivity{

    private final static String DROPBOX_FILE_DIR = "/FC Magazine/";

    private static final String ACCESS_TOKEN = "t3HP7BPiD2AAAAAAAAAAHzZCvsP_y-pkY1kv0PCAPSdxi13bKay5dwS0xQbRsWqE";
    private FileMetadata mSelectedFile;

    private DbxRequestConfig config = null;
    DbxClientV2 client = null;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.download);

        /*uploadFileBtn = (Button) findViewById(R.id.uploadFileBtn);
        uploadFileBtn.setOnClickListener(this);*/
        config = DbxRequestConfig.newBuilder("FC Magazine").build();
        //DbxRequestConfig Config = DbxRequestConfig.newBuilder("MyApp/1.0").build();
        client = new DbxClientV2(config, ACCESS_TOKEN);

        DownloadFileTask downloadFile = new DownloadFileTask(Download.this, client, new DownloadFileTask.Callback() {

            @Override
            public void onDownloadComplete(File result) {
                //Toast.makeText(Download.this, "Downloaded" + result, Toast.LENGTH_SHORT).show();
                Log.d("Download Complete", "onDownloadComplete: " +result);
            }

            @Override
            public void onError(Exception e) {

                Toast.makeText(Download.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
        downloadFile.execute();


    }

}


