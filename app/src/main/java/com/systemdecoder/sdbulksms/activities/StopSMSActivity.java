package com.systemdecoder.sdbulksms.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.systemdecoder.sdbulksms.services.SendSMSService;

public class StopSMSActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        stopService(new Intent(getApplicationContext(), SendSMSService.class));
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }
}