package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int INPUT_SIZE = 224;
    private HandlerThread classificationThread;
    private Handler classificationHandler;
    private TensorFlowImageClassifier classifier;
    private HashMap<String, String> CLASS_MAP = new HashMap<>();

    private TextView btnPredict, lblPrediction;
    private ProgressBar prgClassification;

    private RelativeLayout lytPrediction;

    private Bitmap bitmapXray = null;

    private ImageView imgXray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPredict = findViewById(R.id.btnPredict);
        lblPrediction = findViewById(R.id.lblPrediction);
        prgClassification = findViewById(R.id.prgClassification);

        lytPrediction = findViewById(R.id.lytPrediction);

        imgXray = findViewById(R.id.imgXray);

        try {
//            Bitmap no_image = BitmapFactory.decodeResource(getResources(), R.drawable.no_image);
            Picasso.with(this).load(R.drawable.no_image).resize(324, 324).transform(new RoundedCornersTransformation(30, 0)).into(imgXray, new com.squareup.picasso.Callback() {
                @Override
                public void onSuccess() {
                    //do smth when picture is loaded successfully
                }

                @Override
                public void onError() {
                    //do smth when there is picture loading error
                    Toast.makeText(MainActivity.this, "Error: ", Toast.LENGTH_SHORT).show();
                }

            });
        } catch (Exception exp) {

        }

        CLASS_MAP.put("BINARY", "BINARY");
        CLASS_MAP.put("MULTI_CLASS", "MULTI_CLASS");

        enableUI(false);

        initClassifier();

        classificationThread = new HandlerThread("ClassificationThread");
        classificationThread.start();

        classificationHandler = new Handler(this.getMainLooper());

        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bitmapXray != null) {
                    enableUI(false);
                    classificationHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            recognition();
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Please upload a XRay image first.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void openGallery(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    try {

                        bitmapXray = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
//                        Uri selectedImage = data.getData();
//                        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), selectedImage);
//                        bitmapXray = ImageDecoder.decodeBitmap(source);
//                        imgXray.setImageBitmap(bitmapXray);

                        try {
                            Picasso.with(this).load(data.getData()).resize(324, 324).transform(new RoundedCornersTransformation(30, 0)).into(imgXray, new com.squareup.picasso.Callback() {
                                @Override
                                public void onSuccess() {
                                    //do smth when picture is loaded successfully
                                }

                                @Override
                                public void onError() {
                                    //do smth when there is picture loading error
                                    Toast.makeText(MainActivity.this, "Error: ", Toast.LENGTH_SHORT).show();
                                }

                            });
                        } catch (Exception exp) {

                        }
                        lblPrediction.setText("...prediction...");
                        final int sdk = android.os.Build.VERSION.SDK_INT;
                        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                            lytPrediction.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_prediction));
                        } else {
                            lytPrediction.setBackground(ContextCompat.getDrawable(this, R.drawable.background_prediction));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableUI(boolean val) {
        if (val) {
            btnPredict.setVisibility(View.VISIBLE);
            prgClassification.setVisibility(View.GONE);
        } else {
            btnPredict.setVisibility(View.GONE);
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
//            Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.normal);
            Bitmap image = Bitmap.createScaledBitmap(bitmapXray, INPUT_SIZE, INPUT_SIZE, false);

            List<Recognition> recognitions = classifier.recognizeImage(image);

            enableUI(true);

            for (Recognition r : recognitions) {
//                String prediction = r.toString();
//                lblPrediction.setText(prediction);
                String label = r.getTitle();
                lblPrediction.setText(label);

                if (label.equals("NORMAL")) {
                    final int sdk = android.os.Build.VERSION.SDK_INT;
                    if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        lytPrediction.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_prediction_normal));
                    } else {
                        lytPrediction.setBackground(ContextCompat.getDrawable(this, R.drawable.background_prediction_normal));
                    }
                } else if (label.equals("PNEUMONIA")) {
                    final int sdk = android.os.Build.VERSION.SDK_INT;
                    if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        lytPrediction.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_prediction_pneumonia));
                    } else {
                        lytPrediction.setBackground(ContextCompat.getDrawable(this, R.drawable.background_prediction_pneumonia));
                    }
                }
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    ringtone.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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