package com.e2esp.fcmagazine.activities;

import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.adapters.PageAdapter;
import com.e2esp.fcmagazine.models.Magazines;
import com.e2esp.fcmagazine.views.foldable.FoldableListLayout;
import com.e2esp.fcmagazine.models.Page;
import com.e2esp.fcmagazine.utils.Consts;
import com.e2esp.fcmagazine.utils.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Zain on 2/10/2017.
 */

public class ReaderActivity extends AppCompatActivity {

    public static Magazines magazines;

    private FoldableListLayout foldableListLayout;
    private ArrayList<Page> pages;
    private PageAdapter pageAdapter;

    private TextView textViewTitle;
    private HorizontalScrollView scrollViewThumbnailsContainer;
    private LinearLayout linearLayoutThumbnailsContainer;

    //private ProgressBar progressBarLoading;
    private View viewOverlayWaker;

    private Animation animationTitleIn;
    private Animation animationTitleOut;
    private Animation animationThumbnailsIn;
    private Animation animationThumbnailsOut;

    private int selectedPage = 1;
    private int pendingPageRotation;
    private Button downloadMagazine;
    private int screenWidth;
    private boolean overlayVisible = true;

    private static final String ACCESS_TOKEN = "t3HP7BPiD2AAAAAAAAAAHzZCvsP_y-pkY1kv0PCAPSdxi13bKay5dwS0xQbRsWqE";

    private DbxRequestConfig config = null;
    DbxClientV2 client = null;
    private ImageView selectedMagazine;
    public static int magazinePagesInDropbox;
    public static int magazinePagesInDirectory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_reader);

        downloadMagazine = (Button) findViewById(R.id.downloadMagazine);
        config = DbxRequestConfig.newBuilder("FC Magazine").build();
        client = new DbxClientV2(config, ACCESS_TOKEN);

        //check magazine already downloaded or not
        magazineDownloadedOrNot();

        if (magazines == null) {
            finish();
            return;
        }

        screenWidth = Utility.getScreenSize(this).x;
        setupView();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                createOverlayAnimations();
                if (pendingPageRotation > 0) {
                    foldableListLayout.scrollToPosition(pendingPageRotation, 50);
                }
                //progressBarLoading.setVisibility(View.GONE);
                overlaySleepTimer.start();
            }
        }, 100);
    }

    public void magazineDownloadedOrNot(){

        File dir = new File(Environment.getExternalStorageDirectory(), "Dropbox/" + magazines.getName());
        if(dir.isDirectory()){
            magazinePagesInDirectory = countFilesInDirectory(dir);

            //get list of selected magazine from dropbox
            GetMagazineList magazineList = new GetMagazineList(ReaderActivity.this, client, new GetMagazineList.Callback() {

                @Override
                public void onDownloadComplete(Integer result) {

                    magazinePagesInDropbox = result;
                    if(magazinePagesInDirectory==magazinePagesInDropbox){

                        selectedMagazine.setVisibility(View.GONE);
                        loadMagazine();
                        loadThumbnails();
                        downloadMagazine.setVisibility(View.GONE);

                    }
                    else {
                        downloadMagazine.setVisibility(View.VISIBLE);
                    }

                }

                @Override
                public void onError(Exception e) {

                    Toast.makeText(ReaderActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            });

            magazineList.execute();

        }else {
            downloadMagazine.setVisibility(View.VISIBLE);
        }

    }

    public void downloadClick(View view){

        config = DbxRequestConfig.newBuilder("FC Magazine").build();
        //DbxRequestConfig Config = DbxRequestConfig.newBuilder("MyApp/1.0").build();
        client = new DbxClientV2(config, ACCESS_TOKEN);

        DownloadFileTask downloadFile = new DownloadFileTask(ReaderActivity.this, client, new DownloadFileTask.Callback() {

            @Override
            public void onDownloadComplete(File result) {

                downloadMagazine.setVisibility(View.GONE);
                selectedMagazine.setVisibility(View.GONE);
                loadMagazine();
                loadThumbnails();
                //Toast.makeText(ReaderActivity.this, "Downloaded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {

                Toast.makeText(ReaderActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
        downloadFile.execute();

    }

    private void setupView() {
        foldableListLayout = (FoldableListLayout) findViewById(R.id.foldableListLayout);
        foldableListLayout.setOrientation(FoldableListLayout.HORIZONTAL);
        pages = new ArrayList<>();
        pageAdapter = new PageAdapter(this, pages, screenWidth);
        foldableListLayout.setAdapter(pageAdapter);

        textViewTitle = (TextView) findViewById(R.id.textViewTitle);
        selectedMagazine = (ImageView) findViewById(R.id.selectedMagazine);

        selectedMagazine.setImageBitmap(magazines.getCover());
        textViewTitle.setText(magazines.getName());

        scrollViewThumbnailsContainer = (HorizontalScrollView) findViewById(R.id.scrollViewThumbnailsContainer);
        linearLayoutThumbnailsContainer = (LinearLayout) findViewById(R.id.linearLayoutThumbnailsContainer);

        //progressBarLoading = (ProgressBar) findViewById(R.id.progressBarLoading);
        viewOverlayWaker = findViewById(R.id.viewOverlayWaker);
        viewOverlayWaker.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                showOverlay();
                return false;
            }
        });

        foldableListLayout.setOnFoldRotationListener(new FoldableListLayout.OnFoldRotationListener() {
            @Override
            public void onFoldRotation(float rotation, boolean isFromUser) {
                if (rotation % 180 == 0) {
                    int page = (int) (rotation / 180F);
                    if (page != selectedPage) {
                        onPageFlipped(page);
                    }
                }
            }
        });
    }

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

    private void loadMagazine(){
        pages.clear();

        File dropboxDir = new File(Environment.getExternalStorageDirectory(), "Dropbox");
        File magDir = new File(dropboxDir, magazines.getName());

        //Toast.makeText(this, "Show Magazine", Toast.LENGTH_SHORT).show();
        File[] files = magDir.listFiles();
        Arrays.sort(files);
        int i = 0;
        for (File file : files){

            String imagePath = file.getAbsolutePath();
            i++;
            pages.add(new Page(imagePath, i == selectedPage));
        }

        pageAdapter.notifyDataSetChanged();

    }

    private void loadThumbnails() {
        linearLayoutThumbnailsContainer.removeAllViews();
        int margin = getResources().getDimensionPixelSize(R.dimen.margin_tiny);

        int pageCount = pages.size();
        for (int i = 0; i < pageCount; i++) {
            Page page = pages.get(i);

            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setAdjustViewBounds(true);
            imageView.setImageBitmap(page.getThumbnail(this));
            imageView.setTag(i);
            imageView.setPadding(margin, margin, margin, margin);
            imageView.setOnClickListener(onThumbnailClickListener);

            LinearLayout.LayoutParams imageViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            linearLayoutThumbnailsContainer.addView(imageView, imageViewParams);
        }
        updateThumbnailsSelection();
    }

    private void updateThumbnailsSelection() {
        int colorSelected = getResources().getColor(R.color.soft_blue);
        int colorUnselected = getResources().getColor(R.color.transparent);
        int childCount = linearLayoutThumbnailsContainer.getChildCount();
        int pageCount = pages.size();
        for (int i = 0; i < childCount && i < pageCount; i++) {
            Page page = pages.get(i);
            linearLayoutThumbnailsContainer.getChildAt(i).setBackgroundColor(page.isSelected() ? colorSelected : colorUnselected);
        }
    }

    private void createOverlayAnimations() {
        animationTitleIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_downwards);
        animationTitleOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_upwards);
        animationThumbnailsIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_upwards);
        animationThumbnailsOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_downwards);
    }

    private void hideOverlay() {
        if (overlayVisible) {
            textViewTitle.startAnimation(animationTitleOut);
            linearLayoutThumbnailsContainer.startAnimation(animationThumbnailsOut);
            overlayVisible = false;
        }
    }

    private void showOverlay() {
        if (!overlayVisible) {
            textViewTitle.startAnimation(animationTitleIn);
            linearLayoutThumbnailsContainer.startAnimation(animationThumbnailsIn);
            overlayVisible = true;
        }
        overlaySleepTimer.start();
    }

    private void onPageFlipped(int pageNo) {
        Log.i("onPageFlipped", "pageNo: "+pageNo);
        selectPage(pageNo);
        if (linearLayoutThumbnailsContainer.getChildCount() > 0) {
            int width = linearLayoutThumbnailsContainer.getChildAt(0).getWidth();
            int dx = screenWidth / 2 - width - 2;
            scrollViewThumbnailsContainer.smoothScrollTo(pageNo * width - dx, 0);
        }
    }

    private void selectPage(int pageNo) {
        selectedPage = pageNo;
        int pageCount = pages.size();
        for (int i = 0; i < pageCount; i++) {
            pages.get(i).setSelected(selectedPage == i);
        }
        updateThumbnailsSelection();
    }

    private View.OnClickListener onThumbnailClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (int) v.getTag();
            foldableListLayout.scrollToPosition(index, 1000);
            selectPage(index);
            showOverlay();
        }
    };

    private CountDownTimer overlaySleepTimer = new CountDownTimer(2000, 2000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }
        @Override
        public void onFinish() {
            hideOverlay();
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int pageNo = (int) (foldableListLayout.getFoldRotation() / 180f);
        outState.putInt(Consts.EXTRA_PAGE_NO, pageNo);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int pageNo = savedInstanceState.getInt(Consts.EXTRA_PAGE_NO, 0);
        if (pageNo > 0) {
            if (foldableListLayout.getCount() > 0) {
                foldableListLayout.scrollToPosition(pageNo, 50);
            } else {
                pendingPageRotation = pageNo;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pages != null) {
            for (int i = 0; i < pages.size(); i++) {
                pages.get(i).recycle();
            }
        }
        System.gc();
    }

}
