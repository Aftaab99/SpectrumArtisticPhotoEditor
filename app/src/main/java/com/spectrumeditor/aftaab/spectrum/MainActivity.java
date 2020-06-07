package com.spectrumeditor.aftaab.spectrum;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.collection.LruCache;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.theartofdev.edmodo.cropper.CropImage;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.listeners.IPickResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements ArtStyleAdapter.ArtListViewHolder.ItemSelectListener, IPickResult {

    ImageView selectedImageView;
    ProgressBar progressBar;
    RecyclerView stylesRecyclerView;
    FloatingActionButton fab;
    Integer currentOpacity = 100;

    Boolean isImageSelected = false, isStyleApplied = false, isStyleBeingApplied = false;
    Bitmap originalImage, styleImage;
    LruCache<String, Bitmap> memoryCache;
    BottomAppBar appBar;
    Toolbar toolbar;
    SeekBar opacitySeekbar;
    String currentStyle = null;
    List<ArtStyle> styles = new ArrayList<>();
    Map<String, Integer> styleOpacities = new HashMap<>();
    SharedPreferences.OnSharedPreferenceChangeListener listener;

    ArtStyleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        selectedImageView = findViewById(R.id.selectImage);
        stylesRecyclerView = findViewById(R.id.stylesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        if (isStyleBeingApplied)
            progressBar.setVisibility(View.VISIBLE);
        fab = findViewById(R.id.open_select_fab);
        opacitySeekbar = findViewById(R.id.opacitySeekbar);
        if (isStyleApplied)
            opacitySeekbar.setVisibility(View.VISIBLE);
        appBar = findViewById(R.id.appbar);


        toolbar = findViewById(R.id.toolbar);
        try {
            styles = Items.populateList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setSupportActionBar(toolbar);
        appBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomNavFragment bottomNavFragment = new BottomNavFragment();
                bottomNavFragment.show(getSupportFragmentManager(), bottomNavFragment.getTag());
            }
        });

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                  String key) {

                for (ArtStyle s : styles) {
                    if (getBitmapFromMemCache(s.getModelName()) != null) {
                        memoryCache.remove(s.getModelName());
                    }
                }

                for (int i = 0; i < styles.size(); i++) {
                    if (styles.get(i).getStatus() != ArtStyle.StyleStatusType.NOT_APPLIED)
                        styles.get(i).setStyleStatus(ArtStyle.StyleStatusType.NOT_APPLIED);
                }
                adapter.notifyDataSetChanged();

            }
        };
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (!isImageSelected) {
                    Toast.makeText(MainActivity.this, "Select an image first", Toast.LENGTH_LONG).show();
                    return true;
                }
                switch (item.getItemId()) {
                    case R.id.toolbar_menu_crop:
                        try {
                            File outputDir = MainActivity.this.getCacheDir(); // context being the Activity pointer
                            File outputFile = File.createTempFile("cropImage", ".jpg", outputDir);
                            FileOutputStream fos = new FileOutputStream(outputFile);
                            if (styleImage != null)
                                styleImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            else
                                originalImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();
                            CropImage.activity(Uri.fromFile(outputFile))
                                    .start(MainActivity.this);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case R.id.toolbar_menu_save:
                        if (styleImage != null) {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            boolean isWaterMarkEnabled = sharedPreferences.getBoolean("watermark", true);
                            Bitmap styleImageWithAlpha = getBitmapWithAlpha(currentOpacity);

                            if (isWaterMarkEnabled) {
                                styleImageWithAlpha = Utility.addWaterMark(styleImageWithAlpha, MainActivity.this);
                                Utility.saveToInternalStorage(styleImageWithAlpha, MainActivity.this);
                            } else {
                                Utility.saveToInternalStorage(styleImageWithAlpha, MainActivity.this);
                            }

                        } else
                            Utility.saveToInternalStorage(originalImage, MainActivity.this);
                        break;
                }
                return true;
            }
        });


        for (ArtStyle style : styles) {
            if (!styleOpacities.containsKey(style.getStyleName()))
                styleOpacities.put(style.getStyleName(), 100);
        }
        opacitySeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentOpacity = progress;
                styleOpacities.put(currentStyle, currentOpacity);
                selectedImageView.setImageBitmap(getBitmapWithAlpha(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        Collections.sort(styles, new Comparator<ArtStyle>() {
            @Override
            public int compare(ArtStyle o1, ArtStyle o2) {
                return o1.getStyleName().compareTo(o2.getStyleName());
            }
        });

        if (!isImageSelected) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;
            selectedImageView.setPadding(width / 4, width / 4, width / 4, width / 4);
            selectedImageView.setImageResource(R.drawable.ic_select_image);
        } else {
            selectedImageView.setPadding(0, 0, 0, 0);
            if (!isStyleApplied)
                selectedImageView.setImageBitmap(originalImage);
            else
                selectedImageView.setImageBitmap(styleImage);
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            stylesRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        } else {
            stylesRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        }
        adapter = new ArtStyleAdapter(this, styles, this);
        stylesRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        stylesRecyclerView.setVisibility(View.VISIBLE);
        if (memoryCache == null) {
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;

            memoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickImageDialog.build(new PickSetup().setMaxSize(1280)).show(MainActivity.this);
            }
        });

    }

    public Bitmap getBitmapWithAlpha(int opacity) {
        float scaledOpacity = Utility.scaleOpacity(opacity);
        Bitmap bmOverlay = Bitmap.createBitmap(originalImage.getWidth(), originalImage.getHeight(), originalImage.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(originalImage, new Matrix(), null);
        Paint paint = new Paint();
        paint.setAlpha((int) scaledOpacity);
        canvas.drawBitmap(styleImage, new Matrix(), paint);
        return bmOverlay;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        if (isStyleApplied) {
            toolbar.getMenu().removeItem(R.id.toolbar_menu_crop);
            toolbar.invalidate();
        }
        return true;
    }

    @Override
    public void onClick(final int position) {
        if (isImageSelected) {
            if (getBitmapFromMemCache(styles.get(position).getModelName()) != null) {
                styleImage = getBitmapFromMemCache(styles.get(position).getModelName());
                styleImage = Bitmap.createScaledBitmap(styleImage, originalImage.getWidth(), originalImage.getHeight(), true);
                currentOpacity = styleOpacities.get(styles.get(position).getStyleName());
                currentStyle = styles.get(position).getStyleName();
                opacitySeekbar.setProgress(currentOpacity);

                selectedImageView.setImageBitmap(getBitmapWithAlpha(currentOpacity));

                resetStyleStatus(position);
            } else {
                if (!isStyleBeingApplied) {
                    StylizeTask stylizeTask = new StylizeTask(this);
                    stylizeTask.execute(styles.get(position).getModelName(), styles.get(position).getStyleName(), Integer.toString(position));
                }
            }


        }


    }


    @Override
    public void onPickResult(PickResult pickResult) {
        if (pickResult.getError() == null) {
            selectedImageView.setPadding(0, 0, 0, 0);
            originalImage = pickResult.getBitmap();
            selectedImageView.setImageBitmap(originalImage);
            isImageSelected = true;
            currentOpacity = 100;
            for (ArtStyle s : styles) {
                if (getBitmapFromMemCache(s.getModelName()) != null) {
                    memoryCache.remove(s.getModelName());
                }
            }

            for (int i = 0; i < styles.size(); i++) {
                styles.get(i).setStyleStatus(ArtStyle.StyleStatusType.NOT_APPLIED);
            }
            adapter.notifyDataSetChanged();
            opacitySeekbar.setVisibility(View.GONE);
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.toolbar_menu);
            styleImage = null;
            isStyleApplied = false;

        } else {
            Toast.makeText(this, pickResult.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();

                originalImage = BitmapFactory.decodeFile(resultUri.getPath());
                selectedImageView.setImageBitmap(originalImage);
                for (int i = 0; i < styles.size(); i++) {
                    styles.get(i).setStyleStatus(ArtStyle.StyleStatusType.NOT_APPLIED);
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                error.printStackTrace();
            }


            adapter.notifyDataSetChanged();
        }
    }

    void resetStyleStatus(int position) {
        for (int i = 0; i < styles.size(); i++) {

            if (styles.get(i).getStatus() == ArtStyle.StyleStatusType.CURRENT)
                styles.get(i).setStyleStatus(ArtStyle.StyleStatusType.READY);
            if (i == position)
                styles.get(i).setStyleStatus(ArtStyle.StyleStatusType.CURRENT);
        }
        adapter.notifyDataSetChanged();
    }

    private static class StylizeTask extends AsyncTask<String, Void, Void> {

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
            activity.selectedImageView.setImageBitmap(activity.getBitmapWithAlpha(activity.currentOpacity));
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
            activity.currentOpacity = activity.styleOpacities.get(strings[1]);
            activity.currentStyle = strings[1];
            return null;
        }

    }


    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);

        fab = findViewById(R.id.open_select_fab);
        appBar = findViewById(R.id.appbar);
        selectedImageView = findViewById(R.id.selectImage);
        stylesRecyclerView = findViewById(R.id.stylesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        if (isStyleBeingApplied)
            progressBar.setVisibility(View.VISIBLE);
        toolbar = findViewById(R.id.toolbar);

        opacitySeekbar = findViewById(R.id.opacitySeekbar);
        if (isStyleApplied)
            opacitySeekbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);
        appBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomNavFragment bottomNavFragment = new BottomNavFragment();
                bottomNavFragment.show(getSupportFragmentManager(), bottomNavFragment.getTag());
            }
        });
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (!isImageSelected) {
                    Toast.makeText(MainActivity.this, "Select an image first", Toast.LENGTH_LONG).show();
                    return true;
                }
                switch (item.getItemId()) {
                    case R.id.toolbar_menu_crop:
                        try {
                            File outputDir = MainActivity.this.getCacheDir(); // context being the Activity pointer
                            File outputFile = File.createTempFile("cropImage", ".jpg", outputDir);
                            FileOutputStream fos = new FileOutputStream(outputFile);
                            if (styleImage != null)
                                styleImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            else
                                originalImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();
                            CropImage.activity(Uri.fromFile(outputFile))
                                    .start(MainActivity.this);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case R.id.toolbar_menu_save:
                        if (styleImage != null) {
                            Bitmap styleImageWithAlpha = getBitmapWithAlpha(currentOpacity);
                            Utility.saveToInternalStorage(styleImageWithAlpha, MainActivity.this);
                        } else
                            Utility.saveToInternalStorage(originalImage, MainActivity.this);
                        break;
                }
                return true;
            }
        });

        if (!isImageSelected) {
            selectedImageView.setImageResource(R.drawable.ic_select_image);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;
            int height = displayMetrics.heightPixels;
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // In landscape
                selectedImageView.setPadding(height / 8, height / 8, height / 8, height / 8);

            } else {
                // In portrait
                selectedImageView.setPadding(width / 4, width / 4, width / 4, width / 4);

            }
        } else {
            selectedImageView.setPadding(0, 0, 0, 0);

            if (!isStyleApplied)
                selectedImageView.setImageBitmap(originalImage);
            else
                selectedImageView.setImageBitmap(styleImage);
        }

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            stylesRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        } else {
            stylesRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        }
        adapter = new ArtStyleAdapter(this, styles, this);
        stylesRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        stylesRecyclerView.setVisibility(View.VISIBLE);
        if (memoryCache == null) {
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;

            memoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }

                @Override
                protected void entryRemoved(boolean evicted, @NonNull String key, @NonNull Bitmap oldBitmap, Bitmap newBitmap) {
                    for (int i = 0; i < styles.size(); i++) {
                        if (key.equals(styles.get(i).getModelName())) {
                            styles.get(i).setStyleStatus(ArtStyle.StyleStatusType.NOT_APPLIED);
                        }
                    }
                    if (oldBitmap != styleImage) {
                        oldBitmap.recycle();
                    }
                    adapter.notifyDataSetChanged();
                }
            };


        }


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickImageDialog.build(new PickSetup()).show(MainActivity.this);
            }
        });

        opacitySeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentOpacity = progress;
                styleOpacities.put(currentStyle, currentOpacity);
                selectedImageView.setImageBitmap(getBitmapWithAlpha(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }


}
