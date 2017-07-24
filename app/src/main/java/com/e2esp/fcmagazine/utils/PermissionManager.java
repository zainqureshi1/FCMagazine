package com.e2esp.fcmagazine.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;

/**
 * Created by Ali on 7/24/2017.
 */

public class PermissionManager {

    private static PermissionManager instance;

    public static PermissionManager getInstance() {
        if (instance == null) {
            instance = new PermissionManager();
        }
        return instance;
    }

    private ArrayList<Request> requests;

    private PermissionManager() {
        requests = new ArrayList<>();
    }

    public boolean hasPermission(Activity activity, String permission) {
        int result = ContextCompat.checkSelfPermission(activity, permission);
        if (result == PackageManager.PERMISSION_DENIED) {
            return false;
        }
        return true;
    }

    private boolean alreadyRequested(int requestCode) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).code == requestCode) {
                return true;
            }
        }
        return false;
    }

    public void checkPermissionRequest(final Activity activity, final String permission, final int requestCode, String rationale, final Callback callback) {
        if (hasPermission(activity, permission)) {
            callback.onGranted();
            return;
        }

        if (alreadyRequested(requestCode)) {
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            new AlertDialog.Builder(activity)
                    .setMessage(rationale)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission(activity, permission, requestCode, callback);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
            return;
        }

        requestPermission(activity, permission, requestCode, callback);
    }

    private void requestPermission(Activity activity, String permission, int requestCode, Callback callback) {
        requests.add(new Request(requestCode, callback));
        ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
    }

    public void onResult(int requestCode, String [] permissions, int [] grantResults) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).code == requestCode) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requests.get(i).callback.onGranted();
                } else {
                    requests.get(i).callback.onDenied();
                }
                requests.remove(i);
                return;
            }
        }
    }

    public interface Callback {
        void onGranted ();
        void onDenied ();
    }

    private class Request {
        private int code;
        private Callback callback;

        public Request(int code, Callback callback) {
            this.code = code;
            this.callback = callback;
        }
    }

}
