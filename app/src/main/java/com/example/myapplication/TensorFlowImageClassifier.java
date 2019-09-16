package com.example.myapplication;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import static android.content.ContentValues.TAG;

public class TensorFlowImageClassifier {

    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final float THRESHOLD = 0.5f;

//    private static final int IMAGE_MEAN = 128;
//    private static final float IMAGE_STD = 128.0f;

    private static final String MODEL_PATH = "xray_model.tflite";
    private static final String LABEL_PATH = "labels.txt";
    private static int MAX_RESULTS = 1;
    private static String CLASSIFICATION_TYPE; // BINARY OR MULTI_CLASS

    private Interpreter interpreter;
    private int INPUT_SIZE;
    private List<String> labelList;
    private Context ctx;

    public TensorFlowImageClassifier(Context ctx, AssetManager assetManager, int inputSize, String classificationType) throws IOException {

        this.ctx = ctx;
        interpreter = new Interpreter((MappedByteBuffer) loadModelFile(assetManager));
        labelList = loadLabelList(assetManager);
        INPUT_SIZE = inputSize;

        CLASSIFICATION_TYPE = classificationType;

        switch (CLASSIFICATION_TYPE) {
            case "BINARY":
                MAX_RESULTS = 1;
                break;
            case "MULTI_CLASS":
                MAX_RESULTS = labelList.size();
                break;
        }
    }


    public List<Recognition> recognizeImage(Bitmap bitmap) {
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);

        float[][] result = new float[1][MAX_RESULTS];

        long startTime = SystemClock.uptimeMillis();

        interpreter.run(byteBuffer, result);

        long endTime = SystemClock.uptimeMillis();
        String runTime = String.valueOf(endTime - startTime);

        Log.d(TAG, "recognizeImage: " + runTime + "ms");

        Log.d("RESULT", "" + result);

        Toast.makeText(ctx, "[]: " + result[0][0], Toast.LENGTH_SHORT).show();

        return getSortedResult(result);
    }

    public void destroyClassifier() {
        interpreter.close();
        interpreter = null;
    }

    private ByteBuffer loadModelFile(AssetManager assetManager) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(AssetManager assetManager) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];

        byteBuffer.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < bitmap.getWidth(); ++i) {
            for (int j = 0; j < bitmap.getHeight(); ++j) {
                final int val = intValues[pixel++];
//                byteBuffer.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                byteBuffer.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                byteBuffer.putFloat((((val) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                byteBuffer.putFloat(((val >> 16) & 0xFF));
                byteBuffer.putFloat(((val >> 8) & 0xFF));
                byteBuffer.putFloat((val & 0xFF));
            }
        }
        return byteBuffer;
    }

    @SuppressLint("DefaultLocale")
    private List<Recognition> getSortedResult(float[][] labelProbArray) {

        PriorityQueue<Recognition> pq = new PriorityQueue<>(MAX_RESULTS, new Comparator<Recognition>() {
            @Override
            public int compare(Recognition lhs, Recognition rhs) {
                Toast.makeText(ctx, "[COMPARE]: " + rhs.getConfidence() + " " + lhs.getConfidence(), Toast.LENGTH_LONG).show();
                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
            }
        });

        float confidence;

        if (CLASSIFICATION_TYPE.equals("MULTI_CLASS")) {
            for (int i = 0; i < labelList.size(); ++i) {

                confidence = (labelProbArray[0][i] * 0xff) / 255.0f; //  & 0xff

                // 0.5(50%)
                if (confidence > THRESHOLD) {
                    pq.add(new Recognition("" + i, labelList.size() > i ? labelList.get(i) : "unknown", confidence));
                }
            }
        } else if (CLASSIFICATION_TYPE.equals("BINARY")) {

            confidence = (labelProbArray[0][0] * 0xff) / 255.0f;

            Recognition r;

            // 0.5(50%)
            if (confidence >= THRESHOLD) {
                r = new Recognition("1", labelList.size() > 0 ? labelList.get(1) : "unknown", confidence);
                pq.add(r);
            } else if (confidence < THRESHOLD) {
                r = new Recognition("0", labelList.size() > 0 ? labelList.get(0) : "unknown", confidence);
                pq.add(r);
            }

        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

}