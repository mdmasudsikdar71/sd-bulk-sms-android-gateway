package com.systemdecoder.sdbulksms;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseInstanceID extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String string) {
        super.onNewToken(string);
        Log.e("newToken", string);
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", string).apply();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
    }
}
