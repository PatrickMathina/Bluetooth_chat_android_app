package com.pat254.BluChat;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import java.util.Objects;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
         Objects.requireNonNull(getSupportActionBar()).hide(); //hide taskbar

        new Handler().postDelayed(() -> {
            Intent intent=new Intent(SplashScreen.this,MainActivity.class);
            startActivity(intent);
            finish();
        },1500);
    }
}