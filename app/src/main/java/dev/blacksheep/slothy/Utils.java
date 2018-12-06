package dev.blacksheep.slothy;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;

public class Utils {
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    public static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    public static void setWallpaper(Context context, File file) {
        WallpaperManager wpManager = WallpaperManager.getInstance(context);
        FileInputStream is;
        BufferedInputStream bis;
        try {
            is = new FileInputStream(file);
            bis = new BufferedInputStream(is);
            Bitmap bitmap = BitmapFactory.decodeStream(bis);
            Bitmap useThisBitmap = Bitmap.createBitmap(bitmap);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wpManager.setBitmap(useThisBitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                // wpManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
            } else {
                wpManager.setBitmap(useThisBitmap);
            }

            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
