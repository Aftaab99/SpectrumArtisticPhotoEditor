package com.spectrumeditor.aftaab.spectrum;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public class UpdateOpacityTask extends AsyncTask<UpdateOpacityTaskParams, Void, Void> {

    WeakReference<MainActivity> activityWeakReference;

    UpdateOpacityTask(MainActivity context) {
        activityWeakReference = new WeakReference<>(context);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        MainActivity activity = activityWeakReference.get();
        if (activity == null || activity.isFinishing()) return;

        activity.selectedImageView.setImageBitmap(activity.styleImageWithOpacity);
    }


    @Override
    protected Void doInBackground(UpdateOpacityTaskParams... params) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null || activity.isFinishing()) return null;


        Bitmap originalImage = params[0].getOriginalImage();
        Bitmap styleImage = params[0].getStyleImage();
        int opacity = params[0].getOpacity();
        activity.styleImageWithOpacity = getBitmapWithAlpha(opacity, originalImage, styleImage);
        return null;
    }

    static Bitmap getBitmapWithAlpha(int opacity, Bitmap originalImage, Bitmap styleImage) {
        float scaledOpacity = Utility.scaleOpacity(opacity);
        Bitmap bmOverlay = Bitmap.createBitmap(originalImage.getWidth(), originalImage.getHeight(), originalImage.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(originalImage, new Matrix(), null);
        Paint paint = new Paint();
        paint.setAlpha((int) scaledOpacity);
        canvas.drawBitmap(styleImage, new Matrix(), paint);
        return bmOverlay;
    }

}
