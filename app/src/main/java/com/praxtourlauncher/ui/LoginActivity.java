package com.praxtourlauncher.ui;

import static com.praxtourlauncher.constants.PraxConstants.Api.PRAXCLOUD_API_URL;
import static com.praxtourlauncher.constants.PraxConstants.IntentExtra.EXTRA_ACCOUNT_TOKEN;
import static com.praxtourlauncher.constants.PraxConstants.IntentExtra.EXTRA_LOGOUT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.praxtourlauncher.R;
import com.praxtourlauncher.api.PraxCloud;
import com.praxtourlauncher.api.entity.ApiKey;
import com.praxtourlauncher.api.entity.LoginUser;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {
    private final static String TAG = LoginActivity.class.getSimpleName();

    private TextView pageTitleTextView;
    private TextView usernameTitleTextView;
    private EditText usernameInput;
    private TextView passwordTitleTextView;
    private EditText passwordInput;
    private TextView loginFailedTextView;

    private Button nextButton;
    private Button previousButton;
    private Button retryButton;
    private ImageView serverStatusImageView;

    private String apikey;

    private final PraxCloud praxCloud = new Retrofit.Builder()
            .baseUrl(PRAXCLOUD_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(PraxCloud.class);

    private enum Step {
        ENTER_USERNAME,
        ENTER_PASSWORD
    }
    private Step step = Step.ENTER_USERNAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        pageTitleTextView = findViewById(R.id.page_title_textview);
        usernameTitleTextView = findViewById(R.id.login_enter_username_textview);
        usernameInput = findViewById(R.id.login_enter_username_input);
        passwordTitleTextView = findViewById(R.id.login_enter_password_title);
        passwordInput = findViewById(R.id.login_enter_password_input);
        loginFailedTextView = findViewById(R.id.login_failed_textview);
        nextButton = findViewById(R.id.next_button);
        previousButton = findViewById(R.id.previous_button);
        retryButton = findViewById(R.id.retry_button);
        serverStatusImageView = findViewById(R.id.server_status_indicator);

        nextButton.setOnClickListener(v -> {
            switch (step) {
                case ENTER_USERNAME:
                    showEnterPasswordPage();
                    break;
                case ENTER_PASSWORD:
                    if (passwordInput.getText().toString().isBlank()) return;
                    new Thread(() -> {
                        apikey = authenticate(usernameInput.getText().toString(), passwordInput.getText().toString());
                        if (apikey == null) {
                            runOnUiThread(this::showFailedLoginPage);
                            return;
                        }

                        SharedPreferences.Editor editor = getSharedPreferences("app", Context.MODE_PRIVATE).edit();
                        editor.putString(EXTRA_ACCOUNT_TOKEN, apikey);
                        editor.apply();

                        openInstallerActivity();
                    }).start();
                    break;
            }
        });

        Intent incomingIntent = getIntent();
        boolean logout = incomingIntent.getBooleanExtra(EXTRA_LOGOUT, false);
        if (logout) {
            clearSavedCredentials();
        }

        if (checkForSavedApikey()) {
            openInstallerActivity();
        }

        previousButton.setOnClickListener(v -> showEnterUsernamePage());

        // FIXME show loading until this is confirmed
        new Thread(() -> {
            if (isServerOnline()) {
                runOnUiThread(() -> serverStatusImageView.setImageResource(R.drawable.green_tick));
            } else {
                runOnUiThread(() -> {
                    serverStatusImageView.setImageResource(R.drawable.red_x_cross);
                    usernameInput.setEnabled(false);
                    // TODO implement retry connection button
                });
            }
        }).start();
    }

    private boolean isServerOnline() {
        try {
            return praxCloud.getServerStatus().execute().body().getStreamserver();
        } catch (IOException | NullPointerException e) {
            Log.e(TAG, "Is server online catch: " + e);
            return false;
        }
    }

    private String authenticate(String username, String password) {
        try {
            ApiKey apiKey = praxCloud.authenticateUser(new LoginUser(username, password)).execute().body();
            return apiKey == null ? null : apiKey.getApiKey();
        } catch (IOException e) {
            throw new RuntimeException("Error during authentication", e);
        }
    }

    private void showEnterUsernamePage() {
        usernameInput.setEnabled(true);
        usernameInput.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        usernameInput.setTextColor(getColor(R.color.black));
        passwordTitleTextView.setVisibility(View.GONE);
        passwordInput.setVisibility(View.GONE);
        passwordInput.setText("");
        previousButton.setVisibility(View.GONE);
        step = Step.ENTER_USERNAME;
    }

    private void showEnterPasswordPage() {
        if (usernameInput.getText().toString().isBlank()) return;
        usernameInput.setEnabled(false);
        usernameInput.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        usernameInput.setTextColor(getColor(R.color.gray));
        passwordTitleTextView.setVisibility(View.VISIBLE);
        passwordInput.setVisibility(View.VISIBLE);
        previousButton.setVisibility(View.VISIBLE);
        passwordInput.requestFocus();
        step = Step.ENTER_PASSWORD;
    }

    private void showFailedLoginPage() {
        pageTitleTextView.setText(getString(R.string.login_failed_title));
        pageTitleTextView.setTextColor(getColor(R.color.red));
        usernameInput.setVisibility(View.GONE);
        usernameTitleTextView.setVisibility(View.GONE);
        passwordInput.setVisibility(View.GONE);
        passwordTitleTextView.setVisibility(View.GONE);
        previousButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);

        retryButton.setVisibility(View.VISIBLE);
        retryButton.requestFocus();
        loginFailedTextView.setVisibility(View.VISIBLE);

        retryButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, LoginActivity.class));
            finish();
        });
    }

    @SuppressLint("ApplySharedPref")
    private void clearSavedCredentials() {
        SharedPreferences.Editor editor = getSharedPreferences("app", Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit(); // commit, because we immediately need the value saved
    }

    private boolean checkForSavedApikey() {
        SharedPreferences sp = getSharedPreferences("app", Context.MODE_PRIVATE);
        apikey = sp.getString(EXTRA_ACCOUNT_TOKEN, "");
        return !apikey.isBlank();
    }

    private void openInstallerActivity() {
        Intent installerIntent = new Intent(LoginActivity.this, UpdateActivity.class);
        installerIntent.putExtra(EXTRA_ACCOUNT_TOKEN, apikey);
        startActivity(installerIntent);
        finish();
    }
}