package com.spectrumeditor.aftaab.spectrum;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class PreserveColors {

    int width, height;

    static {
        System.loadLibrary("opencv_java3");
    }

    public PreserveColors(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Bitmap transferLuminosity(Bitmap orgImageBitmap, Bitmap styImageBitmap) {

        Mat originalMat = new Mat(height, width, CvType.CV_8UC3);
        Mat styleMat = new Mat(height, width, CvType.CV_8UC3);

        Utils.bitmapToMat(orgImageBitmap, originalMat);
        Utils.bitmapToMat(styImageBitmap, styleMat);

        Mat styleImageGray = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.cvtColor(styleMat, styleImageGray, Imgproc.COLOR_RGB2GRAY);

        Mat originalYIQ = new Mat(height, width, CvType.CV_8UC3);
        Imgproc.cvtColor(originalMat, originalYIQ, Imgproc.COLOR_RGB2YCrCb);

        Mat styleImageWithLuminosity = new Mat(height, width, CvType.CV_8UC3);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double[] pixelYIQ = originalYIQ.get(i, j);
                double[] pixelGray = styleImageGray.get(i, j);
                double[] data = {pixelGray[0], pixelYIQ[1], pixelYIQ[2]};
                styleImageWithLuminosity.put(i, j, data);
            }
        }
        Mat result = new Mat(height, width, CvType.CV_8UC3);
        Imgproc.cvtColor(styleImageWithLuminosity, result, Imgproc.COLOR_YCrCb2RGB);
        Mat result1 = new Mat(height, width, CvType.CV_8UC4);
        Imgproc.cvtColor(result, result1, Imgproc.COLOR_RGB2RGBA);
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result1, output);
        return output;

    }
}
