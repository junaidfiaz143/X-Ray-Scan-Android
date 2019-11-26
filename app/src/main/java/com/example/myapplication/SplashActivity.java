package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        TextView txtTitle = findViewById(R.id.txtTitle);

        final RelativeLayout boxView = findViewById(R.id.boxView);
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(4000);

        AnimationSet animation = new AnimationSet(false); //change to false
        animation.addAnimation(fadeIn);
        txtTitle.setAnimation(animation);

        RotateAnimation rotate = new RotateAnimation(0, 45,
                Animation.RELATIVE_TO_PARENT, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);

        rotate.setDuration(2000);
        rotate.setRepeatCount(0);
        rotate.setFillAfter(true);
        rotate.setFillEnabled(true);
        boxView.setAnimation(rotate);
        boxView.startAnimation(rotate);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
//                Toast.makeText(SplashActivity.this, "timer", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 3000);
    }
}
