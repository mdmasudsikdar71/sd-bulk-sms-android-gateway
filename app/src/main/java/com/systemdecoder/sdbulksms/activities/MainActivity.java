package com.systemdecoder.sdbulksms.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.iid.FirebaseInstanceId;
import com.systemdecoder.sdbulksms.R;
import com.systemdecoder.sdbulksms.services.SendSMSService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.systemdecoder.sdbulksms.utils.Url.MAIN_URL;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private RelativeLayout console_switcher_show_hide;
    private TextView device_connection_status;
    private TextView console_text;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch status_change_switcher;
    private ImageView connection_check;
    private ImageView check_status_connection;
    private RelativeLayout cardView;
    private InterstitialAd mInterstitialAd;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences userPref = getSharedPreferences("user", Context.MODE_PRIVATE);
        boolean isLoggedIn = userPref.getBoolean("isLoggedIn", false);

        if (!isLoggedIn) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }

        MobileAds.initialize(this, initializationStatus -> {
        });

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        AdView cardAds = findViewById(R.id.cardAds);
        cardAds.loadAd(adRequest);

        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
            }
        });

        if (mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }

        console_switcher_show_hide = findViewById(R.id.console_switcher_show_hide);

        TextView device_name = findViewById(R.id.device_name);
        console_text = findViewById(R.id.console_text);
        cardView = findViewById(R.id.cardView);
        device_connection_status = findViewById(R.id.device_connection_status);
        ImageView console_switcher = findViewById(R.id.console_switcher);
        connection_check = findViewById(R.id.connection_check);
        check_status_connection = findViewById(R.id.check_status_connection);
        ImageView exit_button = findViewById(R.id.exit_button);
        status_change_switcher = findViewById(R.id.status_change_switcher);

        device_name.setText(Build.MANUFACTURER + " " + Build.MODEL);

        cardView.setVisibility(View.GONE);

        console_text.setText("-> " + "Turn On Service to Start this application!");

        status_change_switcher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                console_text.setText("-> " + "Connecting...");
                checkConnection();
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                } else {
                    Log.d("TAG", "The interstitial ad wasn't ready yet.");
                }
            } else {
                console_text.append("\n-> " + "Discounting...");
                status_change_switcher.setChecked(false);
                device_connection_status.setText(R.string.disconnected);
                connection_check.setImageResource(R.drawable.ic_baseline_cancel_24);
                check_status_connection.setImageResource(R.drawable.ic_baseline_sensors_off_24);
                console_text.append("\n-> " + "Disconnected!");
                stopService();
            }
        });

        exit_button.setOnClickListener(v -> exitMethod());

        console_switcher.setOnClickListener(view -> {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(MainActivity.this);
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.");
            }

            if (console_switcher_show_hide.getVisibility() == View.VISIBLE) {
                console_switcher_show_hide.setVisibility(View.INVISIBLE);
            } else {
                console_switcher_show_hide.setVisibility(View.VISIBLE);
            }

        });
    }

    private boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(MainActivity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @SuppressLint("SetTextI18n")
    private void checkConnection() {
        if (isOnline()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                status_change_switcher.setChecked(false);
            } else {
                deviceConnectStatus(status_change_switcher, device_connection_status, connection_check, check_status_connection);
            }
        } else {
            status_change_switcher.setChecked(false);
            console_text.append("\n-> " + getResources().getString(R.string.no_internet_check_your_connection));
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getResources().getString(R.string.app_name))
                    .setIcon(R.drawable.ic_baseline_error_24)
                    .setMessage(getResources().getString(R.string.no_internet_check_your_connection))
                    .setPositiveButton("Open Wifi Setting", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    })
                    .setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void startService() {
        Intent intent = new Intent(MainActivity.this, SendSMSService.class);
        startService(intent);
        console_text.append("\n-> " + "Service Started!");
    }

    private void stopService() {
        stopService(new Intent(MainActivity.this, SendSMSService.class));
        console_text.append("\n-> " + "Service Stopped!");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isOnline()) {
            status_change_switcher.setChecked(false);
            console_text.append("\n-> " + getResources().getString(R.string.no_internet_check_your_connection));
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getResources().getString(R.string.app_name))
                    .setIcon(R.drawable.ic_baseline_error_24)
                    .setMessage(getResources().getString(R.string.no_internet_check_your_connection))
                    .setPositiveButton("Open Wifi Setting", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    })
                    .setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        Toast.makeText(MainActivity.this, "Service Stopped!", Toast.LENGTH_LONG).show();
        stopService();
        super.onDestroy();
    }

    @SuppressLint("SetTextI18n")
    private void deviceConnectStatus(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch status_change_switcher, TextView device_connection_status, ImageView connection_check, ImageView check_status_connection) {
        RequestQueue MyRequestQueue = Volley.newRequestQueue(MainActivity.this);
        String url = MAIN_URL + "devices/register_device"; // <----enter your post url her
        //Create an error listener to handle errors appropriately.
        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, url, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.getBoolean("success")) {
                    Toast.makeText(MainActivity.this, jsonObject.getString("message"), Toast.LENGTH_LONG).show();
                    status_change_switcher.setChecked(true);
                    device_connection_status.setText(R.string.connected);
                    connection_check.setImageResource(R.drawable.ic_baseline_check_circle_24);
                    check_status_connection.setImageResource(R.drawable.ic_baseline_sensors_24);
                    console_text.append("\n-> " + jsonObject.getString("message"));
                    startService();
                } else {
                    Toast.makeText(MainActivity.this, "Something Went Wrong", Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }, Throwable::printStackTrace) {

            final SharedPreferences userPref = getSharedPreferences("user", Context.MODE_PRIVATE);
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = userPref.getString("token", "");
                HashMap<String, String> map = new HashMap<>();
                map.put("Authorization", "Bearer " + token);
                return map;
            }

            @SuppressLint("HardwareIds")
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> MyData = new HashMap<>();
                MyData.put("name", Build.MANUFACTURER + " " + Build.MODEL);
                MyData.put("version", Build.SERIAL);
                MyData.put("manufacturer", Build.MANUFACTURER);
                MyData.put("device_unique_id", Build.ID);
                MyData.put("firebase_token", FirebaseInstanceId.getInstance().getToken());
                MyData.put("user_id", userPref.getString("user_id", ""));
                MyData.put("user_api_key", userPref.getString("api_key", ""));
                return MyData;
            }
        };
        MyRequestQueue.add(MyStringRequest);
    }

    @Override
    public void onBackPressed() {
        exitMethod();
    }

    private void exitMethod() {
        if (cardView.getVisibility() == View.GONE) {
            cardView.setVisibility(View.VISIBLE);
        } else {
            cardView.setVisibility(View.GONE);
        }

        Button exit_yes = findViewById(R.id.exit_yes);
        exit_yes.setOnClickListener(view -> {
            stopService();
            finish();
        });

        Button exit_no = findViewById(R.id.exit_no);
        exit_no.setOnClickListener(view -> cardView.setVisibility(View.GONE));
    }
}