package com.example.speaktoothv4;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class StartScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Disable dark mode of the application if the user has turned it on
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        NextActivity();

    }
    //Wait 3 seconds and after that goes to another activity
    public void NextActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(StartScreen.this, MainActivity.class);
                StartScreen.this.startActivity(mainIntent);
                overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);

            }
        }, 3000);
    }
}