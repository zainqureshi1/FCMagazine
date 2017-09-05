package com.e2esp.fcmagazine.activities;

import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.adapters.PageAdapter;
import com.e2esp.fcmagazine.models.Magazine;
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

    public static Magazine magazine;

    private FoldableListLayout foldableListLayout;
    private ArrayList<Page> pages;
    private PageAdapter pageAdapter;

    private HorizontalScrollView scrollViewThumbnailsContainer;
    private LinearLayout linearLayoutThumbnailsContainer;

    private Animation animationThumbnailsIn;
    private Animation animationThumbnailsOut;

    private int selectedPage = 1;
    private int pendingPageRotation;
    private int screenWidth;
    private boolean overlayVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_reader);

        if (magazine == null) {
            finish();
            return;
        }

        screenWidth = Utility.getScreenSize(this).x;
        setActionBar();
        setupView();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadMagazine();
                loadThumbnails();
                createOverlayAnimations();
                if (pendingPageRotation > 0) {
                    foldableListLayout.scrollToPosition(pendingPageRotation, 50);
                }
                //progressBarLoading.setVisibility(View.GONE);
                overlaySleepTimer.start();
            }
        }, 100);
    }

    public void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            TextView magazineName = new TextView(ReaderActivity.this);
            magazineName.setText(magazine.getName());
            magazineName.setTextColor(ContextCompat.getColor(this, R.color.black));
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(magazineName);
            actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.gray)));
            actionBar.show();
        }
    }

    private void setupView() {
        foldableListLayout = (FoldableListLayout) findViewById(R.id.foldableListLayout);
        foldableListLayout.setOrientation(FoldableListLayout.HORIZONTAL);
        pages = new ArrayList<>();
        pageAdapter = new PageAdapter(this, pages, screenWidth);
        foldableListLayout.setAdapter(pageAdapter);

        scrollViewThumbnailsContainer = (HorizontalScrollView) findViewById(R.id.scrollViewThumbnailsContainer);
        linearLayoutThumbnailsContainer = (LinearLayout) findViewById(R.id.linearLayoutThumbnailsContainer);

        findViewById(R.id.viewOverlayWaker).setOnTouchListener(new View.OnTouchListener() {
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

    private void loadMagazine(){
        pages.clear();

        File magDir = new File(getFilesDir(), magazine.getName());
        File[] files = magDir.listFiles();
        Arrays.sort(files);
        int i = 0;
        for (File file : files) {
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
        int colorSelected = ContextCompat.getColor(this, R.color.soft_blue);
        int colorUnselected = ContextCompat.getColor(this, R.color.transparent);
        int childCount = linearLayoutThumbnailsContainer.getChildCount();
        int pageCount = pages.size();
        for (int i = 0; i < childCount && i < pageCount; i++) {
            Page page = pages.get(i);
            linearLayoutThumbnailsContainer.getChildAt(i).setBackgroundColor(page.isSelected() ? colorSelected : colorUnselected);
        }
    }

    private void createOverlayAnimations() {
        animationThumbnailsIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_upwards);
        animationThumbnailsOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_downwards);
    }

    private void hideOverlay() {
        if (overlayVisible) {
            linearLayoutThumbnailsContainer.startAnimation(animationThumbnailsOut);
            overlayVisible = false;
        }
    }

    private void showOverlay() {
        if (!overlayVisible) {
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

    public void deleteMagazine() {
        File magazineDir = new File(getFilesDir(), magazine.getName());
        deleteFile(magazineDir);
        magazine.setDownloaded(false);

        setResult(RESULT_FIRST_USER);
        finish();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteFile(child);
            }
        }
        file.delete();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.magazine_menu, menu);
        setActionBar();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Take appropriate action for each action item click
        switch (item.getItemId()) {
            case R.id.delete:
                deleteMagazine();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
