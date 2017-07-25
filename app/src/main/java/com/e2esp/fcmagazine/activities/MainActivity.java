package com.e2esp.fcmagazine.activities;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.adapters.MagazineRecyclerAdapter;
import com.e2esp.fcmagazine.adapters.MagazineRecyclerAdapters;
import com.e2esp.fcmagazine.interfaces.OnMagazineClickListener;
import com.e2esp.fcmagazine.models.Magazine;
import com.e2esp.fcmagazine.models.Magazines;
import com.e2esp.fcmagazine.utils.Consts;
import com.e2esp.fcmagazine.utils.PermissionManager;

import java.io.File;
import java.util.ArrayList;

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
                //int cover = coverPagesInDropbox;
                DownloadCoverPages(coverPagesInStorage,coverPagesInDropbox);
                //Toast.makeText(MainActivity.this, "Cover Pages " + coverPagesInDropbox, Toast.LENGTH_SHORT).show();

                magazineRecyclerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {

                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });

        folderList.execute();
        /*int length = new File("Dropbox").listFiles().length;*/

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
                Log.d("File: ", "Cover Page  " +file.getName());
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
                    loadMagazines();
                }

                @Override
                public void onError(Exception e) {

                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            });
            downloadCoverPage.execute();

        }
        else{
            loadMagazines();
        }
        //magazineRecyclerAdapter.notifyDataSetChanged();

    }

    private void loadMagazines() {

        File dropboxDir = new File(Environment.getExternalStorageDirectory(), "Dropbox");
        if (dropboxDir.isDirectory()) {
            //dropboxDir.mkdir();

            Log.d("Directory " , "Directory Found ");
            Toast.makeText(this, "Directory Found", Toast.LENGTH_SHORT).show();



            String path = Environment.getExternalStorageDirectory() + "/Dropbox/Cover Pages/Jun 2017.jpg";

            File imgFile = new File(path);

            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            //get name of file(Cover Pages)
            String magazineFilePath = imgFile.getName();

            //Toast.makeText(this, "Cover Page Name " +coverPageName, Toast.LENGTH_SHORT).show();

            //remove the dot jpg extension
            String magazineName = magazineFilePath.substring(0, magazineFilePath.lastIndexOf("."));

            //Toast.makeText(this, "Magazine Name " +magazineName, Toast.LENGTH_LONG).show();

            magazinesListLatest.clear();
            magazinesListLatest.add(new Magazines(magazineName, myBitmap));

            String pathRecent = Environment.getExternalStorageDirectory() + "/Dropbox/Cover Pages/Mar 2017.jpg";

            File imgFileRecent = new File(pathRecent);

            Bitmap myBitmapRecent = BitmapFactory.decodeFile(imgFileRecent.getAbsolutePath());
            //get name of file(Cover Pages)
            String magazineFilePathRecent = imgFileRecent.getName();

            //Toast.makeText(this, "Cover Page Name " +coverPageNameRecent, Toast.LENGTH_SHORT).show();

            //remove the dot jpg extension
            String magazineNameRecent = magazineFilePathRecent.substring(0, magazineFilePathRecent.lastIndexOf("."));

            //Toast.makeText(this, "Magazine Name " +magazineName, Toast.LENGTH_LONG).show();

            magazinesListRecent.clear();
            magazinesListRecent.add(new Magazines(magazineNameRecent, myBitmapRecent));


            String pathDownloaded = Environment.getExternalStorageDirectory() + "/Dropbox/Cover Pages/Feb 2017.jpg";

            File imgFileDownloaded = new File(pathDownloaded);

            Bitmap myBitmapDownloaded = BitmapFactory.decodeFile(imgFileDownloaded.getAbsolutePath());
            //get name of file(Cover Pages)
            String magazineFilePathDownloaded = imgFileDownloaded.getName();

            //Toast.makeText(this, "Cover Page Name " +coverPageNameRecent, Toast.LENGTH_SHORT).show();

            //remove the dot jpg extension
            String magazineNameDownloaded = magazineFilePathDownloaded.substring(0, magazineFilePathDownloaded.lastIndexOf("."));

            //Toast.makeText(this, "Magazine Name " +magazineName, Toast.LENGTH_LONG).show();

            magazinesListRecent.add(new Magazines(magazineNameDownloaded, myBitmapDownloaded));
            magazinesListDownloaded.clear();
            //magazinesListDownloaded.add(new Magazine("AUG 2016", "issue39/%s.jpg", R.drawable.magazine_cover_5, 54));
            //magazinesListDownloaded.add(new Magazine("MAY 2016", "issue39/%s.jpg", R.drawable.magazine_cover_6, 54));

            adjustListSize();

        }

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