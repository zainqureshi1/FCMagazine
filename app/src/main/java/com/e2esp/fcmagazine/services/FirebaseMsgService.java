package com.e2esp.fcmagazine.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.activities.DownloadFileTask;
import com.e2esp.fcmagazine.activities.MainActivity;
import com.e2esp.fcmagazine.models.Magazines;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMsgService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMsgService";
    private Magazines magazine;
    NotificationCompat.Builder notificationBuilder;
    NotificationManager notificationManager;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        //It is optional
        Log.e(TAG, "From: " + remoteMessage.getFrom());
        Log.e(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());

        SharedPreferences subscribeCheck=getSharedPreferences("subscribeClick", Context.MODE_PRIVATE);
        boolean subscribed = subscribeCheck.getBoolean("isSubscribe", false);

        if (subscribed == true) {
            //Calling method to generate notification
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }else {

            Log.d("Subscribe "," Notttt");
        }
    }

    //This method is only generating push notification
    private void sendNotification(String title, String messageBody) {

        String ACCESS_TOKEN = "t3HP7BPiD2AAAAAAAAAAHzZCvsP_y-pkY1kv0PCAPSdxi13bKay5dwS0xQbRsWqE";

        DbxRequestConfig config = config = DbxRequestConfig.newBuilder("FC Magazine").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (!wifi.isConnected() && !mobile.isConnected()) {
            //Toast.makeText(getApplicationContext(), " Please make sure, your network connection is ON", Toast.LENGTH_LONG).show();
            return;
        }

        magazine = new Magazines(title, null, null);

        magazine.setName(title);
        magazine.setDownloading(true);

        notificationBuilder();

        Integer notificationID = 100;

        DownloadFileTask downloadFile = new DownloadFileTask(this, client, magazine, new DownloadFileTask.Callback() {

            @Override
            public void onDownloadComplete() {
                //magazine.setDownloading(false);
                //magazineRecyclerAdapter.notifyDataSetChanged();
                //loadCoverPages();
            }

            @Override
            public void updateProgress() {


                progressBar();


            }

            @Override
            public void onError(Exception e) {

                //Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
        downloadFile.execute();

    }


    public void progressBar(){

//Set notification information:


        notificationBuilder.setContentTitle("Magazine Downloading")
                .setProgress(magazine.getTotalMagazinePages(), magazine.getCurrentMagazinePages(), false);

//Send the notification:
        notificationManager.notify(0, notificationBuilder.build());



    }

    private void notificationBuilder(){

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationBuilder.setOngoing(true)
                .setContentTitle("Magazine Downloading")
                .setContentText("New Magazine avaialable").setSmallIcon(R.drawable.ic_notification)
                .setProgress(0, 0, false);

//Send the notification:
        Notification notification = notificationBuilder.build();
        notificationManager.notify(0, notification);

    }
}
