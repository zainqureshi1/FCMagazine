package com.e2esp.fcmagazine.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.adapters.MagazineRecyclerAdapters;
import com.e2esp.fcmagazine.interfaces.OnMagazineClickListener;
import com.e2esp.fcmagazine.models.Magazine;
import com.e2esp.fcmagazine.tasks.CountCoverPagesTask;
import com.e2esp.fcmagazine.tasks.CountMagazinePagesTask;
import com.e2esp.fcmagazine.tasks.DownloadCoverPagesTask;
import com.e2esp.fcmagazine.tasks.DownloadMagazineTask;
import com.e2esp.fcmagazine.utils.Consts;
import com.e2esp.fcmagazine.utils.DbClient;
import com.e2esp.fcmagazine.utils.PermissionManager;
import com.e2esp.fcmagazine.utils.Utility;
import com.google.firebase.messaging.FirebaseMessaging;

import org.apache.commons.io.FileUtils;

import io.fabric.sdk.android.Fabric;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Zain on 2/10/2017.
 */

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    private final int REQUEST_CODE_READER = 1;

    private ArrayList<Magazine> magazineListLatest;
    private ArrayList<Magazine> magazineListRecent;
    private ArrayList<Magazine> magazineListDownloaded;
    private MagazineRecyclerAdapters magazineRecyclerAdapter;

    private File coverPagesDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        setActionBar();

        setupView();
        createDirs();
        moveFilesFromExternalStorage();
        countCoverPages();
        loadCoversFromStorage();
        checkDownloadedMagazines();
    }

    public void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            TextView magazineName = new TextView(MainActivity.this);
            magazineName.setText(getString(R.string.app_name));
            magazineName.setTextColor(ContextCompat.getColor(this, R.color.black));
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(magazineName);
            actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.gray)));
            actionBar.show();
        }
    }

    private void setupView() {
        RecyclerView recyclerViewMagazines = (RecyclerView) findViewById(R.id.recyclerViewMagazines);
        magazineListLatest = new ArrayList<>();
        magazineListRecent = new ArrayList<>();
        magazineListDownloaded = new ArrayList<>();
        magazineRecyclerAdapter = new MagazineRecyclerAdapters(this, magazineListLatest, magazineListRecent, magazineListDownloaded, new OnMagazineClickListener() {
            @Override
            public void onDownloadClicked(Magazine magazine) {
                downloadClicked(magazine);
            }
            @Override
            public void onCoverPageClicked(Magazine magazine) {
                coverPageClicked(magazine);
            }
        });

        GridLayoutManager layoutManagerMagazines = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        layoutManagerMagazines.setSpanSizeLookup(magazineRecyclerAdapter.getSpanSizeLookup());
        recyclerViewMagazines.setLayoutManager(layoutManagerMagazines);
        recyclerViewMagazines.setItemAnimator(new DefaultItemAnimator());
        recyclerViewMagazines.setAdapter(magazineRecyclerAdapter);
    }

    private void moveFilesFromExternalStorage() {
        boolean permissionCheck = PermissionManager.getInstance().hasPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck) {
            File externalStorage = new File(Environment.getExternalStorageDirectory(), "FC Magazine");
            if (externalStorage.isDirectory()) {
                File internalStorage = getFilesDir();
                for (File file : externalStorage.listFiles()) {
                    if (file.isDirectory()) {
                        try {
                            FileUtils.copyDirectoryToDirectory(file, internalStorage);
                            deleteFile(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            deleteFile(externalStorage);
        }
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createDirs() {
        coverPagesDir = new File(getFilesDir(), Consts.DIR_COVER_PAGES);
        if (!coverPagesDir.exists() || !coverPagesDir.isDirectory()) {
            coverPagesDir.mkdir();
        }
    }

    private int countFilesInDirectory(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        for (File file: files) {
            if (file.isFile()) {
                count++;
            } else if (file.isDirectory()) {
                count += countFilesInDirectory(file);
            }
        }
        return count;
    }

    private void countCoverPages() {
        CountCoverPagesTask countCoverPagesTask = new CountCoverPagesTask(DbClient.getDbClient(), new CountCoverPagesTask.Callback() {
            @Override
            public void onComplete(Integer result) {
                int coverPagesInStorage = countFilesInDirectory(coverPagesDir);
                if (result == 0) {
                    Log.d(TAG, "Error getting cover pages count");
                    if (coverPagesInStorage <= 0) {
                        Utility.showToast(MainActivity.this, "Internet connection required to check updates");
                    }
                } else {
                    downloadCoverPages(coverPagesInStorage, result);
                    magazineRecyclerAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onError(Exception e) {
                Utility.showToast(MainActivity.this, "Error: "+e.getMessage());
            }
        });
        countCoverPagesTask.execute();
    }

    private void downloadCoverPages(int coverPagesInStorage, int coverPagesInDropbox) {
        if (coverPagesInStorage != coverPagesInDropbox) {
            DownloadCoverPagesTask downloadCoverPagesTask = new DownloadCoverPagesTask(MainActivity.this, DbClient.getDbClient(), new DownloadCoverPagesTask.Callback() {
                @Override
                public void onDownloadComplete() {
                    Log.d(TAG, "DownloadCoverPagesTask :: onDownloadComplete");
                    loadCoversFromStorage();
                    checkDownloadedMagazines();
                }
                @Override
                public void onError(Exception e) {
                    Utility.showToast(MainActivity.this, "Error: "+e.getMessage());
                }
            });
            downloadCoverPagesTask.execute();

        } else {
            loadCoversFromStorage();
            checkDownloadedMagazines();
        }
    }

    private void loadCoversFromStorage() {
        magazineListLatest.clear();

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        for (File file : coverPagesDir.listFiles()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            String fileName = file.getName();
            String magazineName = fileName.substring(0, fileName.lastIndexOf("."));
            Date date = null;
            try {
                date = dateFormat.parse(magazineName);
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
            magazineListLatest.add(new Magazine(magazineName, bitmap, date, false, 0, 0));
        }

        Collections.sort(magazineListLatest, new Comparator<Magazine>() {
            @Override
            public int compare(Magazine o2, Magazine o1) {
                if (o1.getDate() == null || o2.getDate() == null)
                    return 0;
                return o1.getDate().compareTo(o2.getDate());
            }
        });

    }

    private void checkDownloadedMagazines() {
        File[] files = getFilesDir().listFiles();
        for (File magazineDir: files) {
            String magazineName = magazineDir.getName();
            if (!magazineName.equals(Consts.DIR_COVER_PAGES)) {
                if (magazineDir.isDirectory()) {
                    int magazinePagesInDirectory = countFilesInDirectory(magazineDir);
                    if (magazinePagesInDirectory > 0) {
                        for (final Magazine magazine: magazineListLatest) {
                            if (magazineName.equals(magazine.getName())) {
                                magazine.setDownloaded(true);
                                CountMagazinePagesTask countMagazinePagesTask = new CountMagazinePagesTask(DbClient.getDbClient(), magazineName, magazinePagesInDirectory, new CountMagazinePagesTask.Callback() {
                                    @Override
                                    public void onDownloadComplete(Integer dropboxPageCount, int dirPageCount) {
                                        if (dropboxPageCount == 0) {
                                            Log.e(TAG, "CountMagazinePagesTask :: got 0 pages from dropbox");
                                            return;
                                        }
                                        if (dirPageCount == dropboxPageCount) {
                                            magazine.setDownloaded(true);
                                        } else {
                                            magazine.setDownloaded(false);
                                        }
                                        magazineRecyclerAdapter.notifyDataSetChanged();
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        Utility.showToast(MainActivity.this, "Error: "+e.getMessage());
                                    }
                                });
                                countMagazinePagesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                break;
                            }
                        }
                    }
                }
            }
        }
        magazineRecyclerAdapter.notifyDataSetChanged();
    }

    private void downloadClicked(final Magazine magazine) {
        if (!Utility.isInternetConnected(this, true)) {
            return;
        }

        magazine.setDownloading(true);
        magazineRecyclerAdapter.notifyDataSetChanged();

        DownloadMagazineTask downloadMagazineTask = new DownloadMagazineTask(MainActivity.this, DbClient.getDbClient(), magazine.getName(), new DownloadMagazineTask.Callback() {
            @Override
            public void onDownloadComplete() {
                magazine.setDownloading(false);
                magazine.setDownloaded(true);
                magazineRecyclerAdapter.notifyDataSetChanged();
            }
            @Override
            public void updateProgress(int downloaded, int total) {
                magazine.setDownloadedMagazinePages(downloaded);
                magazine.setTotalMagazinePages(total);
                magazineRecyclerAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(Exception e) {
                Utility.showToast(MainActivity.this, "Error: "+e.getMessage());
            }
        });
        downloadMagazineTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void coverPageClicked(Magazine magazine) {
        if (magazine.isDownloaded()) {
            ReaderActivity.magazine = magazine;
            Intent intent = new Intent(this, ReaderActivity.class);
            startActivityForResult(intent, REQUEST_CODE_READER);
        } else {
            Utility.showToast(this, R.string.please_download_magazine_first);
        }
    }

    private void subscribeClicked() {
        getSharedPreferences("subscribeClick", Context.MODE_PRIVATE).edit().putBoolean(Consts.KEY_SUBSCRIBED, true).apply();

        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setMessage("Thank you for subscribing. We will download future issues for you as soon as they are published.");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog subscribeDialog = builder1.create();
        subscribeDialog.show();
    }

    private void unsubscribeClicked() {
        getSharedPreferences(Consts.PREFS_SUBSCRIPTION, Context.MODE_PRIVATE).edit().putBoolean(Consts.KEY_SUBSCRIBED, false).apply();

        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setMessage("You have unsubscribed from magazine updates. Magazine will no longer auto download when published.\u000BPlease subscribe again to get best experience. ");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog subscribeDialog = builder1.create();
        subscribeDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        boolean subscribed = getSharedPreferences(Consts.PREFS_SUBSCRIPTION, Context.MODE_PRIVATE).getBoolean(Consts.KEY_SUBSCRIBED, false);
        if (subscribed) {
            MenuItem subscribeItem = menu.findItem(R.id.subscribe);
            subscribeItem.setTitle(R.string.unsubscribe);
        } else {
            MenuItem subscribeItem = menu.findItem(R.id.subscribe);
            subscribeItem.setTitle(R.string.subscribe);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.subscribe:
                String subscribe = getString(R.string.subscribe);
                String unsubscribe = getString(R.string.unsubscribe);
                if (item.getTitle().equals(subscribe)) {
                    FirebaseMessaging.getInstance().subscribeToTopic(Consts.SUBSCRIPTION_TOPIC);
                    item.setTitle(unsubscribe);
                    subscribeClicked();
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(Consts.SUBSCRIPTION_TOPIC);
                    item.setTitle(subscribe);
                    unsubscribeClicked();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_READER && resultCode == RESULT_FIRST_USER) {
            loadCoversFromStorage();
            checkDownloadedMagazines();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.getInstance().onResult(requestCode, permissions, grantResults);
    }

    private boolean backPressed = false;

    @Override
    public void onBackPressed() {
        if (backPressed) {
            super.onBackPressed();
            return;
        }

        backPressed = true;
        Utility.showToast(this, R.string.press_back_again_to_exit);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                backPressed = false;
            }
        }, 2000);
    }

}