package com.spectrumeditor.aftaab.spectrum;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class Utility {

    enum DevicePerf {DEVICE_PERF_HIGHRES_NOT_COMPATIBLE, DEVICE_PERF_COMPATIBLE_CPU,
            DEVICE_PERF_COMPATIBLE_GPU};

    private static int getOpenGLVersion(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configInfo = activityManager.getDeviceConfigurationInfo();
        if (configInfo.reqGlEsVersion != ConfigurationInfo.GL_ES_VERSION_UNDEFINED) {
            return configInfo.reqGlEsVersion;
        } else {
            return 1 << 16; // Lack of property means OpenGL ES version 1
        }
    }


    static DevicePerf getDevicePerformance(Context context) {
        int nProcessors = Runtime.getRuntime().availableProcessors();
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;

        int OPEN_GL_COMPATIBLE_VERSION = 196609;
        if (nProcessors < 4 || availableMegs < 1250)
            return DevicePerf.DEVICE_PERF_HIGHRES_NOT_COMPATIBLE;
        else if (getOpenGLVersion(context) < OPEN_GL_COMPATIBLE_VERSION)
            return DevicePerf.DEVICE_PERF_COMPATIBLE_CPU;
        else
            return DevicePerf.DEVICE_PERF_COMPATIBLE_GPU;
    }


    // Source: https://stackoverflow.com/questions/15662258/how-to-save-a-bitmap-on-internal-storage
    static void saveToInternalStorage(Bitmap bitmap, Context ctx) {
        File imageFile = getOutputMediaFile(ctx);
        if (imageFile == null) {
            Log.d("ImageSave",
                    "Error creating media file, check storage permissions: ");// e.getMessage());
        }
        try {
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("ImageSave", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("ImageSave", "Error accessing file: " + e.getMessage());
        }
        assert imageFile != null;
        System.out.println("Saved Image at" + imageFile.getAbsolutePath());
        addImageToGallery(imageFile.getAbsolutePath(), ctx);
        Toast.makeText(ctx, "Saved image to gallery", Toast.LENGTH_LONG).show();

    }

    // Source: https://stackoverflow.com/questions/20859584/how-to-save-image-in-android-gallery
    private static void addImageToGallery(final String filePath, final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    static Bitmap addWaterMark(Bitmap src, Context mContext) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

        Bitmap waterMark = getBitmap(mContext);
        float aspectRatio = (float) waterMark.getHeight() / waterMark.getWidth();
        int waterMarkWidth = w / 10;
        int waterMarkHeight = (int) (waterMarkWidth * aspectRatio);
        waterMark = Bitmap.createScaledBitmap(waterMark, waterMarkWidth, waterMarkHeight, true);
        canvas.drawBitmap(waterMark, w - waterMarkWidth - 10, h - waterMarkHeight - 10, null);
        return result;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        Log.e(TAG, "getBitmap: 1");
        return bitmap;
    }

    private static Bitmap getBitmap(Context context) {
        Log.e(TAG, "getBitmap: 2");
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_spectrum_icon_watermark_plain);
        if (drawable instanceof BitmapDrawable) {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_spectrum_icon_watermark_plain);
        } else if (drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            throw new IllegalArgumentException("Unsupported drawable type");
        }
    }

    private static File getOutputMediaFile(Context ctx) {

        File mediaStorageDir = new File(ctx.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), "Spectrum");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(new Date());
        File mediaFile;
        String mImageName = "IMG_" + timeStamp + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    static float scaleOpacity(int x) {
        return (255f - 0f) * (x - 0f) / 100f;
    }

    static int clip(float x) {
        if (x > 255) {
            return 255;
        } else if (x < 0) return 0;
        else {
            return (int) x;
        }

    }
}
