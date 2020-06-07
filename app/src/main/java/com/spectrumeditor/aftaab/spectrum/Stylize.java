package com.spectrumeditor.aftaab.spectrum;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;


class Stylize {

    private String modelPath;
    private Integer styleIndex;
    private boolean useGPU;
    private int width;
    private int height;
    private Context context;

    Stylize(String model_name, Context ctx) {

        this.width = 288;
        this.height = 384;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (Utility.getDevicePerformance(ctx) != Utility.DevicePerf.DEVICE_PERF_HIGHRES_NOT_COMPATIBLE && preferences.getBoolean("highResolution", true)) {
            width = 384;
            height = 512;
        }
        this.modelPath = model_name.split("_")[0] + String.format("_%s.tflite", this.height);
        this.styleIndex = Integer.parseInt(model_name.split("_")[1]);
//        this.useGPU = Utility.getDevicePerformance(ctx) == Utility.DevicePerf.DEVICE_PERF_COMPATIBLE_GPU;
        this.useGPU = false;
        this.context = ctx;
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private float[][][] preprocessImage(Bitmap src) {
        float[][][] inputArray = new float[width][height][3];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = src.getPixel(x, y);
                inputArray[x][y][0] = Color.red(pixel);
                inputArray[x][y][1] = Color.green(pixel);
                inputArray[x][y][2] = Color.blue(pixel);
            }
        }

        return inputArray;
    }

    private Bitmap deprocessImage(float[][][][] outputArray) {

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //Set back the pixels of the output bitmap. Convert from GBR to RGB
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int red = Utility.clip(outputArray[0][i][j][0]);
                int green = Utility.clip(outputArray[0][i][j][1]);
                int blue = Utility.clip(outputArray[0][i][j][2]);
                output.setPixel(i, j, Color.rgb(red, green, blue));
            }
        }


        return output;
    }

    Bitmap stylizeImage(Activity activity, Bitmap source) {

        Bitmap sourceImage = Bitmap.createScaledBitmap(source, width, height, true);
        try {
            Interpreter interpreter;
            GpuDelegate delegate = null;
            if (useGPU) {
                delegate = new GpuDelegate();
                Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
                interpreter = new Interpreter(loadModelFile(activity), options);
            } else {
                Interpreter.Options options = new Interpreter.Options();
                interpreter = new Interpreter(loadModelFile(activity), options);
            }

            // Inputs
            float[][][] inputArray = preprocessImage(sourceImage);
            int[] styleIndexArray = new int[1];
            styleIndexArray[0] = this.styleIndex;

            //Outputs
            float[][][][] outputArray = new float[1][width][height][3];
            Map<Integer, Object> outputMap = new HashMap<>();
            outputMap.put(0, outputArray);

            long start = System.currentTimeMillis();
            interpreter.runForMultipleInputsOutputs(new Object[]{inputArray, styleIndexArray}, outputMap);
            System.out.println("Elapsed inference time:" + (System.currentTimeMillis() - start));

            if (useGPU && delegate!=null)
                delegate.close();

            interpreter.close();
            System.out.println("Done");
            Bitmap styleImage = deprocessImage(outputArray);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
            boolean keepColors = sharedPreferences.getBoolean("keepColors", false);

            if (keepColors) {
                PreserveColors preserveColors = new PreserveColors(styleImage.getWidth(), styleImage.getHeight());
                return preserveColors.transferLuminosity(sourceImage, styleImage);
            } else {
                return styleImage;
            }


        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Model not found", Toast.LENGTH_LONG).show();
        }

        return source;
    }
}