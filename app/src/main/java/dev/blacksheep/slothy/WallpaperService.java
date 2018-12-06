package dev.blacksheep.slothy;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class WallpaperService extends IntentService {
    private final static String TAG = "WallpaperService";
    FirebaseStorage storage;

    public WallpaperService() {
        super("WallpaperService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String filePath = intent.getStringExtra("filename");
        // Log.e("onStartJob", filePath);
        StorageReference islandRef = storageRef.child(filePath);
        try {
            final String extension = filePath.substring(filePath.lastIndexOf(".")); // Extension with dot .jpg, .png
            String filename = Utils.randomString(10);
            final File localFile = File.createTempFile(filename, extension.replace(".", ""), getCacheDir());
            islandRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    // Log.e("SUCCESS", "FILEDOWNLOADED!!!");
                    Utils.setWallpaper(getApplicationContext(), localFile);
                    createNotificationChannel();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    exception.printStackTrace();
                    Toast.makeText(WallpaperService.this, "Failed to set wallpaper: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        Log.e("createNotification", "Showing Notification");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notification Channel";
            String description = "Notify when new wallpaper arrives";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("default", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New wallpaper")
                .setContentText("New wallpaper from your partner!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(123456, mBuilder.build());
    }
}
