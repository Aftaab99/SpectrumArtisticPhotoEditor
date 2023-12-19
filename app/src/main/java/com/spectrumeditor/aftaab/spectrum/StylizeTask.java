package com.spectrumeditor.aftaab.spectrum;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;

import java.lang.ref.WeakReference;

class StylizeTask extends AsyncTask<String, Void, Void> {

    private WeakReference<MainActivity> activityWeakReference;

    private int position;

    StylizeTask(MainActivity context) {
        activityWeakReference = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        MainActivity activity = activityWeakReference.get();
        if (activity == null || activity.isFinishing()) return;

        activity.progressBar.setVisibility(View.VISIBLE);
        activity.isStyleBeingApplied = true;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        MainActivity activity = activityWeakReference.get();
        if (activity == null || activity.isFinishing()) return;

        activity.opacitySeekbar.setVisibility(View.VISIBLE);
        activity.opacitySeekbar.setProgress(activity.currentOpacity);
        activity.selectedImageView.setImageBitmap(activity.styleImageWithOpacity);
        activity.isStyleApplied = true;
        activity.toolbar.getMenu().removeItem(R.id.toolbar_menu_crop);
        activity.toolbar.invalidate();
        activity.isStyleBeingApplied = false;
        activity.progressBar.setVisibility(View.INVISIBLE);
        activity.resetStyleStatus(position);
    }


    @Override
    protected Void doInBackground(String... strings) {

        MainActivity activity = activityWeakReference.get();
        if (activity == null || activity.isFinishing()) return null;

        Stylize stylize = new Stylize(strings[0], activity);
        this.position = Integer.parseInt(strings[2]);
        activity.styleImage = stylize.stylizeImage(activity, activity.originalImage);
        activity.addBitmapToMemoryCache(strings[0], activity.styleImage);
        activity.styleImage = Bitmap.createScaledBitmap(activity.styleImage, activity.originalImage.getWidth(), activity.originalImage.getHeight(), true);
        activity.styleImageWithOpacity = UpdateOpacityTask.getBitmapWithAlpha(activity.currentOpacity, activity.originalImage, activity.styleImage);
        activity.currentOpacity = activity.styleOpacities.get(strings[1]);
        activity.currentStyle = strings[1];
        return null;
    }

}
