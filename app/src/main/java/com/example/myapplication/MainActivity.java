package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int INPUT_SIZE = 224;
    private HandlerThread classificationThread;
    private Handler classificationHandler;
    private TensorFlowImageClassifier classifier;
    private HashMap<String, String> CLASS_MAP = new HashMap<>();

    private TextView txtProb;
    private ProgressBar prgClassification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtProb = findViewById(R.id.txtProb);
        prgClassification = findViewById(R.id.prgClassification);

        CLASS_MAP.put("BINARY", "BINARY");
        CLASS_MAP.put("MULTI_CLASS", "MULTI_CLASS");

        enableUI(false);

        initClassifier();

        classificationThread = new HandlerThread("ClassificationThread");
        classificationThread.start();

        classificationHandler = new Handler(this.getMainLooper());

        txtProb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableUI(false);
                classificationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognition();
                    }
                });

            }
        });
    }

    private void enableUI(boolean val){
        if (val){
            txtProb.setVisibility(View.VISIBLE);
            prgClassification.setVisibility(View.GONE);
        }else{
            txtProb.setVisibility(View.GONE);
            prgClassification.setVisibility(View.VISIBLE);
        }
    }

    private void initClassifier() {
        try {
            classifier = new TensorFlowImageClassifier(this, getAssets(), INPUT_SIZE, CLASS_MAP.get("BINARY"));
//            Toast.makeText(this, "success", Toast.LENGTH_LONG).show();
            Log.e(TAG, "init(): SUCCESS to create Classifier");
            enableUI(true);
        } catch (Exception e) {
            Toast.makeText(this, "failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "init(): FAILED to create Classifier", e);
        }
    }

    private void recognition() {
        try {
            Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.normal);
            image = Bitmap.createScaledBitmap(image, INPUT_SIZE, INPUT_SIZE, false);

            List<Recognition> recognitions = classifier.recognizeImage(image);

            enableUI(true);

            for (Recognition r : recognitions) {
                String prediction = r.toString();
                txtProb.setText(prediction);
            }
        } catch (Exception exp) {
            Toast.makeText(this, "Error: " + exp.getMessage(), Toast.LENGTH_LONG).show();

            Log.e("MAIN ACTIVITY", "" + exp.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (classificationThread != null) classificationThread.quit();
        } catch (Throwable t) {
            // close quietly
        }
        classificationThread = null;
        classificationHandler = null;

        classifier.destroyClassifier();
    }

}