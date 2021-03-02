package com.systemdecoder.sdbulksms.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.systemdecoder.sdbulksms.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.systemdecoder.sdbulksms.utils.Url.MAIN_URL;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText loginUsername, loginPassword;
    private TextInputLayout loginUsernameLayout, loginPasswordLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginUsername = findViewById(R.id.loginUsername);
        loginPassword = findViewById(R.id.loginPassword);
        Button loginButton = findViewById(R.id.loginButton);
        loginUsernameLayout = findViewById(R.id.loginUsernameLayout);
        loginPasswordLayout = findViewById(R.id.loginPasswordLayout);

        loginButton.setOnClickListener(v -> {
            if (validate()) {
                login();
            }
        });

        loginUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Objects.requireNonNull(loginUsername.getText()).toString().length() > 1) {
                    loginUsernameLayout.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        loginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Objects.requireNonNull(loginPassword.getText()).toString().length() > 5) {
                    loginPasswordLayout.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private boolean validate() {
        if (Objects.requireNonNull(loginUsername.getText()).toString().isEmpty()) {
            loginUsernameLayout.setErrorEnabled(true);
            loginUsernameLayout.setError("Usernamee is required");
            return false;
        }
        if (Objects.requireNonNull(loginPassword.getText()).toString().isEmpty()) {
            loginPasswordLayout.setErrorEnabled(true);
            loginPasswordLayout.setError("Password is required");
            return false;
        }
        if (loginPassword.getText().toString().length() < 6) {
            loginPasswordLayout.setErrorEnabled(true);
            loginPasswordLayout.setError("Password at least 6(SIX) charecters");
            return false;
        }

        return true;
    }

    @SuppressLint("SetTextI18n")
    private void login() {
        RequestQueue MyRequestQueue = Volley.newRequestQueue(LoginActivity.this);
        String url = MAIN_URL + "login"; // <----enter your post url her
        //Create an error listener to handle errors appropriately.
        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, url, response -> {
            try {

                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.getBoolean("success")) {
                    JSONObject object = new JSONObject(jsonObject.getString("user"));
                    SharedPreferences userPreferences = getApplicationContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                    @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = userPreferences.edit();
                    editor.putString("token", jsonObject.getString("token"));
                    editor.putString("user_id", object.getString("id"));
                    editor.putString("name", object.getString("name"));
                    editor.putString("phone", object.getString("phone"));
                    editor.putString("email", object.getString("email"));
                    editor.putString("username", object.getString("username"));
                    editor.putString("api_key", object.getString("api_key"));
                    editor.putString("profile_photo_path", object.getString("profile_photo_path"));
                    editor.putBoolean("isLoggedIn", true);
                    editor.apply();
                    Toast.makeText(LoginActivity.this, "Login Success as " + object.getString("name"), Toast.LENGTH_LONG).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Something Went Wrong! Try again!", Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }, Throwable::printStackTrace) {
            @SuppressLint("HardwareIds")
            protected Map<String, String> getParams() {
                Map<String, String> MyData = new HashMap<>();
                MyData.put("username", Objects.requireNonNull(loginUsername.getText()).toString().trim());
                MyData.put("password", Objects.requireNonNull(loginPassword.getText()).toString());
                MyData.put("device_name", "Android: " + Build.ID);
                return MyData;
            }
        };
        MyRequestQueue.add(MyStringRequest);
    }
}