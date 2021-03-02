package com.systemdecoder.sdbulksms.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.systemdecoder.sdbulksms.R;

import java.util.Timer;
import java.util.TimerTask;

public class SplashScreenActivity extends AppCompatActivity {

    private Timer timer;
    private int progress = 20;
    private ImageView splashScreenImageId;
    private Animation topAnimation, bottomAnimation;
    private TextView splashScreenAppNameId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash_screen);

        splashScreenImageId = (ImageView) findViewById(R.id.splashScreenImageId);
        splashScreenAppNameId = (TextView) findViewById(R.id.splashScreenAppNameId);
        topAnimation = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);

        splashScreenImageId.setAnimation(topAnimation);
        splashScreenAppNameId.setAnimation(bottomAnimation);

        final long period = 100;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (progress < 100) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {}
                    });
                    progress++;
                }
                else {
                    timer.cancel();
                    startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
                    finish();
                }
            }
        }, 0, period);
    }
}