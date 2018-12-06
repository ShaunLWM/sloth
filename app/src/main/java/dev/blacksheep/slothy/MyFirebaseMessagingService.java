package dev.blacksheep.slothy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Log.e(TAG, "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().size() > 0) {
            // Log.e(TAG, "Message data payload: " + remoteMessage.getData());
            Map<String, String> data = remoteMessage.getData();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            // Log.e("DataEmail", data.get("email"));
            // Log.e("MyEmail", sharedPref.getString("email", ""));
            if (data.get("email").equals(sharedPref.getString("email", ""))) {
                // Log.e("Mine", "IS MINE, SCHEDULING JOB");
                // scheduleJob(data.get("message"));
                Intent intent = new Intent(this, WallpaperService.class);
                intent.putExtra("filename", data.get("message"));
                startService(intent);
            }
        }
    }
}