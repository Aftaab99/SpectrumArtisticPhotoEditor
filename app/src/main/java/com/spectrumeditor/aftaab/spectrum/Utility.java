package com.spectrumeditor.aftaab.spectrum;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import static androidx.constraintlayout.widget.Constraints.TAG;
import static java.lang.System.out;

public class Utility {

    static Integer DEVICE_PERF_NOT_COMPATABLE = 0, DEVICE_PERF_COMPATABLE_CPU = 1,
            DEVICE_PERF_COMPATABLE_GPU = 2;
    static int OPEN_GL_COMPATABLE_VERSION = 196609;
    public static final String getSHA3_Hash(final String s) {
        final String SHA3 = "SHA512";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(SHA3);
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return s;
    }

    public static String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e("Utility", "Error process asset " + assetName + " to file path");
        }
        return null;
    }


    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public static void setPostRequestContent(HttpURLConnection conn,
                                             JSONObject jsonObject) throws IOException {

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(jsonObject.toString());
        Log.i(MainActivity.class.toString(), jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }

    public static Bitmap base64ToBitmap(String b64) {
        byte[] imageAsBytes = Base64.decode(b64.getBytes(), Base64.DEFAULT);
        BitmapFactory.Options opts = new
                BitmapFactory.Options();
        opts.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length, opts);
    }

    public static String fetchResponseHttps(HttpsURLConnection connection) {

        try {
            connection.setConnectTimeout(60000);
            connection.connect();
            InputStream in = new BufferedInputStream(connection.getInputStream());
            BufferedReader read = new BufferedReader(new InputStreamReader(in));

            StringBuffer res = new StringBuffer();
            String line;

            while ((line = read.readLine()) != null) {
                res.append(line);
            }
            return res.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String fetchResponseHttp(HttpURLConnection connection) {

        try {
            connection.setConnectTimeout(60000);
            connection.connect();
            InputStream in = new BufferedInputStream(connection.getInputStream());
            BufferedReader read = new BufferedReader(new InputStreamReader(in));

            StringBuffer res = new StringBuffer();
            String line;

            while ((line = read.readLine()) != null) {
                res.append(line);
            }
            out.println("RESPONSE:\n" + res);
            return res.toString();
        } catch (Exception e) {
            out.println("Failed..");
            e.printStackTrace();
        }

        return "";
    }

    public static int getOpenGLVersion(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configInfo = activityManager.getDeviceConfigurationInfo();
        if (configInfo.reqGlEsVersion != ConfigurationInfo.GL_ES_VERSION_UNDEFINED) {
            return configInfo.reqGlEsVersion;
        } else {
            return 1 << 16; // Lack of property means OpenGL ES version 1
        }
    }


    public static Integer getDevicePerformance(Context context) {
        int nProcessors = Runtime.getRuntime().availableProcessors();
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;

        System.out.println("processors=" + nProcessors + ", and ram=" + availableMegs);
        if (nProcessors < 4 || availableMegs < 1250)
            return DEVICE_PERF_NOT_COMPATABLE;
        else if (getOpenGLVersion(context) < OPEN_GL_COMPATABLE_VERSION)
            return DEVICE_PERF_COMPATABLE_CPU;
        else
            return DEVICE_PERF_COMPATABLE_GPU;
    }


    public static Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    // Source: https://stackoverflow.com/questions/15662258/how-to-save-a-bitmap-on-internal-storage
    public static void saveToInternalStorage(Bitmap bitmap, Context ctx) {
        File imageFile = getOutputMediaFile(ctx, "Spectrum");
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
        System.out.println("Saved Image at" + imageFile.getAbsolutePath());
        addImageToGallery(imageFile.getAbsolutePath(), ctx);
        Toast.makeText(ctx, "Saved image to gallery", Toast.LENGTH_LONG).show();

    }

    // Source: https://stackoverflow.com/questions/20859584/how-to-save-image-in-android-gallery
    public static void addImageToGallery(final String filePath, final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static Bitmap addWaterMark(Bitmap src, Context mContext) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

        Bitmap waterMark = getBitmap(mContext, R.drawable.ic_spectrum_icon_watermark_plain);
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

    private static Bitmap getBitmap(Context context, int drawableId) {
        Log.e(TAG, "getBitmap: 2");
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return BitmapFactory.decodeResource(context.getResources(), drawableId);
        } else if (drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    private static File getOutputMediaFile(Context ctx, String albumName) {

        File mediaStorageDir = new File(ctx.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName = "IMG_" + timeStamp + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    public static float scaleOpacity(int x) {
        return (255f - 0f) * (x - 0f) / 100f;
    }

    public static boolean isOnline() {
        try {
            int timeoutMs = 1500;
            Socket sock = new Socket();
            SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);

            sock.connect(sockaddr, timeoutMs);
            sock.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }


    public static float normalise(float x) {
        return x - 127.5f;
    }

    public static int clip(float x) {
        if (x > 255) {
            return 255;
        } else if (x < 0) return 0;
        else {
            return (int) x;
        }

    }
}
