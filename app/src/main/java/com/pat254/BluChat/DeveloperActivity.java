package com.pat254.BluChat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class DeveloperActivity extends AppCompatActivity {

    Intent intent = null, chooser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);

//        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).hide();
    }

    public void gmail(View V) {
        intent = new Intent(Intent.ACTION_VIEW);
        Uri data = Uri.parse("mailto:?subject=" + "BluChat feedback" + "&body=" +
                "Hey,..." + "&to=" + "patsofts.help@gmail.com");
        intent.setData(data);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            Toast.makeText(getApplicationContext(), "Type an email", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "No application can handle the operation", Toast.LENGTH_SHORT).show();
        }
    }

    public void whatsApp(View V) {
        boolean installed = isAppInstalled();
        if (installed) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://api.whatsapp.com/send?phone=" + "+254799858285" + "&text=" + "(BluChat feedback) Hey, my name is ... "));
            startActivity(intent);

        } else {
            Toast.makeText(getApplicationContext(), "WhatsApp is not installed on your device", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAppInstalled() {
        PackageManager packageManager = getPackageManager();
        boolean app_installed;
        try {
            packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }
}