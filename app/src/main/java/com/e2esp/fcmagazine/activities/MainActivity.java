package com.e2esp.fcmagazine.activities;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.adapters.MagazineRecyclerAdapters;
import com.e2esp.fcmagazine.interfaces.OnMagazineClickListener;
import com.e2esp.fcmagazine.models.Magazines;
import com.e2esp.fcmagazine.utils.PermissionManager;

import io.fabric.sdk.android.Fabric;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Zain on 2/10/2017.
 */

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMagazines;
    private ArrayList<Magazines> magazinesListLatest;
    private ArrayList<Magazines> magazinesListRecent;
    private ArrayList<Magazines> magazinesListDownloaded;
    private MagazineRecyclerAdapters magazineRecyclerAdapter;

    private TextView textViewTitle;
    private View viewOverlayWaker;

    private final static String DROPBOX_FILE_DIR = "/FC Magazine/";

    private static final String ACCESS_TOKEN = "t3HP7BPiD2AAAAAAAAAAHzZCvsP_y-pkY1kv0PCAPSdxi13bKay5dwS0xQbRsWqE";
    private FileMetadata mSelectedFile;

    private DbxRequestConfig config = null;
    DbxClientV2 client = null;

    private Animation animationTitleIn;
    private Animation animationTitleOut;
    public static int coverPagesInDropbox;
    public static int coverPagesInStorage;

    private boolean overlayVisible = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //startActivity(new Intent(this, SplashActivity.class));

        setContentView(R.layout.activity_main);

        PermissionManager.getInstance().checkPermissionRequest(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, 9, "Store Magazine Cover Pages", new PermissionManager.Callback() {
            @Override
            public void onGranted() {

                createDirs();
                setupView();
                //loadMagazines();
                createOverlayAnimations();
                showOverlay();

            }
            @Override
            public void onDenied() {

            }
        });

    }

    private void createDirs() {

        File dropboxDir = new File(Environment.getExternalStorageDirectory(), "Dropbox");
        if (!dropboxDir.isDirectory()) {
            dropboxDir.mkdir();
        }
        File coverPagesDir = new File(dropboxDir, "Cover Pages");
        if (!coverPagesDir.isDirectory()) {
            coverPagesDir.mkdir();
        }
    }



    private void setupView() {


        config = DbxRequestConfig.newBuilder("FC Magazine").build();
        //DbxRequestConfig Config = DbxRequestConfig.newBuilder("MyApp/1.0").build();
        client = new DbxClientV2(config, ACCESS_TOKEN);


        GetFolderList folderList = new GetFolderList(MainActivity.this, client, new GetFolderList.Callback() {

            @Override
            public void onDownloadComplete(Integer result) {

                coverPagesInDropbox = result;
                DownloadCoverPages(coverPagesInStorage,coverPagesInDropbox);
                magazineRecyclerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {

                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });

        folderList.execute();

        File dir = new File(Environment.getExternalStorageDirectory(), "Dropbox/Cover Pages");
        coverPagesInStorage = countFilesInDirectory(dir);
        //Toast.makeText(this, "Number of Cover Pages " +coverPagesInStorage, Toast.LENGTH_LONG).show();

        recyclerViewMagazines = (RecyclerView) findViewById(R.id.recyclerViewMagazines);
        magazinesListLatest = new ArrayList<>();
        magazinesListRecent = new ArrayList<>();
        magazinesListDownloaded = new ArrayList<>();
        magazineRecyclerAdapter = new MagazineRecyclerAdapters(this, magazinesListLatest, magazinesListRecent, magazinesListDownloaded, new OnMagazineClickListener() {
            @Override
            public void onMagazineClicked(Magazines magazine) {
                magazineClicked(magazine);
            }
        });

        GridLayoutManager layoutManagerMagazines = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        layoutManagerMagazines.setSpanSizeLookup(magazineRecyclerAdapter.getSpanSizeLookup());
        recyclerViewMagazines.setLayoutManager(layoutManagerMagazines);
        recyclerViewMagazines.setItemAnimator(new DefaultItemAnimator());
        recyclerViewMagazines.setAdapter(magazineRecyclerAdapter);

        textViewTitle = (TextView) findViewById(R.id.textViewTitle);
        textViewTitle.setText(R.string.app_name);

        viewOverlayWaker = findViewById(R.id.viewOverlayWaker);
        viewOverlayWaker.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                showOverlay();
                return false;
            }
        });
    }

    //count number of cover pages
    public static int countFilesInDirectory(File directory) {
        int count = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                //Log.d("File: ", "Cover Page  " +file.getName());
                count++;
            }
            if (file.isDirectory()) {
                count += countFilesInDirectory(file);
            }
        }
        return count;
    }

    //download cover pages
    public void DownloadCoverPages(int coverPagesInStorage, int coverPagesInDropbox){

        if(coverPagesInStorage != coverPagesInDropbox){

            DownloadCoverPage downloadCoverPage = new DownloadCoverPage(MainActivity.this, client, new DownloadCoverPage.Callback() {

                @Override
                public void onDownloadComplete(File result) {
                    Log.d("Download Complete", "onDownloadComplete: " +result);
                    loadCoverPages();
                }

                @Override
                public void onError(Exception e) {

                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            });
            downloadCoverPage.execute();

        }
        else{
            loadCoverPages();
        }
        //magazineRecyclerAdapter.notifyDataSetChanged();

    }

    private void loadCoverPages() {

        File dropboxDir = new File(Environment.getExternalStorageDirectory(), "Dropbox");
        File magDir = new File(dropboxDir, "Cover Pages");
        magazinesListRecent.clear();

        for (File file:magDir.listFiles()){

            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            //get name of file(Cover Pages)
            String magazineFilePath = file.getName();
            //remove the dot jpg extension
            String magazineName = magazineFilePath.substring(0, magazineFilePath.lastIndexOf("."));

            String convertToDate = magazineName;
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy");

            Date date = null;
            try {
                date = dateFormat.parse(convertToDate);
                //Toast.makeText(this, "Covert to date" +date, Toast.LENGTH_LONG).show();
                //return dateFormat.format(date);
            }
            catch(ParseException pe) {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                //return "Date";
            }

            magazinesListRecent.add(new Magazines(magazineName, myBitmap,date));


        }//for loop end


        Collections.sort(magazinesListRecent, new Comparator<Magazines>() {
            @Override
            public int compare(Magazines o2, Magazines o1) {
                if (o1.getDate() == null || o2.getDate() == null)
                    return 0;
                return o1.getDate().compareTo(o2.getDate());

            }
        });

        Magazines latestMagazine = magazinesListRecent.remove(0);
        magazinesListLatest.clear();
        magazinesListLatest.add(latestMagazine);

        magazineRecyclerAdapter.notifyDataSetChanged();
    }

    private void adjustListSize() {
        if (magazinesListRecent.size() % 2 == 1) {
            magazinesListRecent.add(new Magazines(true));
        }
        if (magazinesListDownloaded.size() % 2 == 1) {
            magazinesListDownloaded.add(new Magazines(true));
        }
    }

    private void magazineClicked(Magazines magazine) {
        ReaderActivity.magazines = magazine;
        Intent intent = new Intent(this, ReaderActivity.class);
        startActivity(intent);
    }

    private void createOverlayAnimations() {
        animationTitleIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_downwards);
        animationTitleOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_upwards);
    }

    private void hideOverlay() {
        if (overlayVisible) {
            textViewTitle.startAnimation(animationTitleOut);
            overlayVisible = false;
        }
    }

    private void showOverlay() {
        if (!overlayVisible) {
            textViewTitle.startAnimation(animationTitleIn);
            overlayVisible = true;
        }
        overlaySleepTimer.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.getInstance().onResult(requestCode,permissions,grantResults);
    }

    private boolean backPressed = false;
    @Override
    public void onBackPressed() {
        if (backPressed) {
            super.onBackPressed();
            return;
        }

        backPressed = true;
        Toast.makeText(this, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                backPressed = false;
            }
        }, 2000);
    }

    private CountDownTimer overlaySleepTimer = new CountDownTimer(5000, 5000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }
        @Override
        public void onFinish() {
            hideOverlay();
        }
    };

}