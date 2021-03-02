package com.systemdecoder.sdbulksms.services;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.systemdecoder.sdbulksms.R;
import com.systemdecoder.sdbulksms.SendSMS;
import com.systemdecoder.sdbulksms.activities.LoginActivity;
import com.systemdecoder.sdbulksms.activities.MainActivity;
import com.systemdecoder.sdbulksms.activities.StopSMSActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.systemdecoder.sdbulksms.utils.Url.MAIN_URL;

public class SendSMSService extends Service {

    private final static String TAG = "SendSMSService";
    CountDownTimer cdt = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, StopSMSActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, SendSMS.CHANNEL_ID)
                .setContentTitle("SD Bulk SMS")
                .setContentText("SD Bulk SMS is Running. Tap to stop.")
                .setSmallIcon(R.drawable.ic_baseline_check_circle_24)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_forward_to_inbox_24))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(Color.WHITE)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        cdt = new CountDownTimer(Long.MAX_VALUE, 10000) {
            
            @Override
            public void onTick(long millisUntilFinished) {
                viewSmsStatus();
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "Timer finished");
            }
        };

        cdt.start();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        cdt.cancel();
        stopSelf();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @SuppressLint("SetTextI18n")
    private void viewSmsStatus() {
        RequestQueue MyRequestQueue = Volley.newRequestQueue(getBaseContext());
        String url = MAIN_URL + "sms/view"; // <----enter your post url her
        //Create an error listener to handle errors appropriately.
        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, url, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.getBoolean("success")) {
                    JSONObject object = new JSONObject(jsonObject.getString("message"));
                    sendSMS(object.getString("mobile_number"), object.getString("message"), object.getString("id"));
                }
//                else {
//                    Toast.makeText(getBaseContext(), jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
//                }
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
            protected Map<String, String> getParams() {
                Map<String, String> MyData = new HashMap<>();
                MyData.put("device_unique_id", Build.ID);
                return MyData;
            }
        };

        MyRequestQueue.add(MyStringRequest);
    }

    @SuppressLint("SetTextI18n")
    private void sendSmsSendStatus(String rejection_reason, String status, String sendingId) {
        RequestQueue MyRequestQueue = Volley.newRequestQueue(getBaseContext());
        String url = MAIN_URL + "sms/update"; // <----enter your post url her
        //Create an error listener to handle errors appropriately.
        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, url, response -> {
            try {

                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.getBoolean("success")) {
                    Log.i(TAG, "SMS SEND");
                } else {
                    Log.d(TAG, "SMS Not SEND");
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
                MyData.put("rejection_reason", rejection_reason);
                MyData.put("status", status);
                MyData.put("id", sendingId);
                return MyData;
            }
        };

        MyRequestQueue.add(MyStringRequest);
    }

    
    private void sendSMS(String phone_number, String message, String sendingId) {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sent_message = PendingIntent.getBroadcast(getBaseContext(), 0, new Intent(SENT), 0);
        PendingIntent delivered_message = PendingIntent.getBroadcast(getBaseContext(), 0, new Intent(DELIVERED), 0);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        sendSmsSendStatus("ok", "1", sendingId);
                        break;

                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        sendSmsSendStatus("SMS not sent due to Generic failure to " + phone_number, "3", sendingId);
                        break;

                    case SmsManager.RESULT_INVALID_SMS_FORMAT:
                        sendSmsSendStatus("SMS not sent due to Invalid SNS Format to " + phone_number, "3", sendingId);
                        break;

                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        sendSmsSendStatus("SMS not sent due to No service to " + phone_number, "3", sendingId);
                        break;

                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        sendSmsSendStatus("SMS not sent due to Null PDU to " + phone_number, "3", sendingId);
                        break;

                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        sendSmsSendStatus("SMS not sent due to Radio off to " + phone_number, "3", sendingId);
                        break;
                }
            }
        }, new IntentFilter(SENT));

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        sendSmsSendStatus("ok", "1", sendingId);
                        break;

                    case Activity.RESULT_CANCELED:
                        sendSmsSendStatus("SMS not delivered to " + phone_number, "3", sendingId);
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

//        SubscriptionManager subscriptionManager  = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
//        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionManager.getActiveSubscriptionInfoCountMax());
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone_number, null, message, sent_message, delivered_message);
    }
}