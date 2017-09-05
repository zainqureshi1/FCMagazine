package com.e2esp.fcmagazine.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.tasks.DownloadMagazineTask;
import com.e2esp.fcmagazine.utils.DbClient;
import com.e2esp.fcmagazine.utils.Utility;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FirebaseMsgService extends FirebaseMessagingService {
    private final String TAG = "FirebaseMsgService";

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.e(TAG, "From: " + remoteMessage.getFrom());
        Map<String, String> data = remoteMessage.getData();
        String title = data.get("title");
        sendNotification(title);
    }

    private void sendNotification(String title) {
        if (!Utility.isInternetConnected(this, false)) {
            return;
        }

        createNotification(title);

        DownloadMagazineTask downloadMagazineTask = new DownloadMagazineTask(this, DbClient.getDbClient(), title, new DownloadMagazineTask.Callback() {
            @Override
            public void onDownloadComplete() {
                notificationManager.cancelAll();
            }
            @Override
            public void updateProgress(int downloaded, int total) {
                updateNotificationProgress(total, downloaded);
            }
            @Override
            public void onError(Exception e) {
                notificationManager.cancelAll();
            }
        });
        downloadMagazineTask.execute();
    }

    private void createNotification(String title) {
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationBuilder.setOngoing(true)
                .setContentTitle(getString(R.string.downloading_magazine))
                .setContentText(title)
                .setSmallIcon(R.drawable.ic_notification)
                .setProgress(0, 0, false)
                .setPriority(Notification.PRIORITY_MAX);

        notificationManager.notify(0, notificationBuilder.build());
    }

    public void updateNotificationProgress(int max, int progress) {
        notificationBuilder.setProgress(max, progress, false);
        notificationManager.notify(0, notificationBuilder.build());
    }

}
