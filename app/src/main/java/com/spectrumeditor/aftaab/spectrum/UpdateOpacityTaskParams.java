package com.spectrumeditor.aftaab.spectrum;

import android.graphics.Bitmap;

public class UpdateOpacityTaskParams {

    private Bitmap originalImage;
    private Bitmap styleImage;
    private int opacity;

    public UpdateOpacityTaskParams(Bitmap originalImage, Bitmap styleImage, int opacity) {
        this.originalImage = originalImage;
        this.styleImage = styleImage;
        this.opacity = opacity;
    }

    public Bitmap getOriginalImage() {
        return originalImage;
    }

    public Bitmap getStyleImage() {
        return styleImage;
    }

    public int getOpacity() {
        return opacity;
    }
}
