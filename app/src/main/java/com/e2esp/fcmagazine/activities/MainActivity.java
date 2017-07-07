package com.e2esp.fcmagazine.activities;

import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.adapters.MagazineRecyclerAdapter;
import com.e2esp.fcmagazine.interfaces.OnMagazineClickListener;
import com.e2esp.fcmagazine.models.Magazine;
import com.e2esp.fcmagazine.utils.Consts;

import java.util.ArrayList;

/**
 * Created by Zain on 2/10/2017.
 */

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMagazines;
    private ArrayList<Magazine> magazinesListLatest;
    private ArrayList<Magazine> magazinesListRecent;
    private ArrayList<Magazine> magazinesListDownloaded;
    private MagazineRecyclerAdapter magazineRecyclerAdapter;

    private TextView textViewTitle;
    private View viewOverlayWaker;

    private Animation animationTitleIn;
    private Animation animationTitleOut;

    private boolean overlayVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //startActivity(new Intent(this, SplashActivity.class));

        setContentView(R.layout.activity_main);
        setupView();

        loadMagazines();

        createOverlayAnimations();
        showOverlay();
    }

    private void setupView() {
        recyclerViewMagazines = (RecyclerView) findViewById(R.id.recyclerViewMagazines);
        magazinesListLatest = new ArrayList<>();
        magazinesListRecent = new ArrayList<>();
        magazinesListDownloaded = new ArrayList<>();
        magazineRecyclerAdapter = new MagazineRecyclerAdapter(this, magazinesListLatest, magazinesListRecent, magazinesListDownloaded, new OnMagazineClickListener() {
            @Override
            public void onMagazineClick(Magazine magazine) {
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

    private void loadMagazines() {
        magazinesListLatest.clear();
        magazinesListLatest.add(new Magazine("Latest Issue JUNE 2017", "issue42/%s.jpg", R.drawable.latest_issue_cover, 60));

        magazinesListRecent.clear();
        magazinesListRecent.add(new Magazine("MAR 2017", "issue41/%s.jpg", R.drawable.magazine_cover_1, 76));
        magazinesListRecent.add(new Magazine("FEB 2017", "issue40/%s.jpg", R.drawable.magazine_cover_2, 58));

        magazinesListDownloaded.clear();
        //magazinesListDownloaded.add(new Magazine("AUG 2016", "issue39/%s.jpg", R.drawable.magazine_cover_5, 54));
        //magazinesListDownloaded.add(new Magazine("MAY 2016", "issue39/%s.jpg", R.drawable.magazine_cover_6, 54));

        adjustListSize();

        magazineRecyclerAdapter.notifyDataSetChanged();
    }

    private void adjustListSize() {
        if (magazinesListRecent.size() % 2 == 1) {
            magazinesListRecent.add(new Magazine(true));
        }
        if (magazinesListDownloaded.size() % 2 == 1) {
            magazinesListDownloaded.add(new Magazine(true));
        }
    }

    private void magazineClicked(Magazine magazine) {
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra(Consts.EXTRA_MAGAZINE, magazine);
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
