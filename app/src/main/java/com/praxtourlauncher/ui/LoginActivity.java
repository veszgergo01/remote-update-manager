package com.praxtourlauncher.ui;

import static com.praxtourlauncher.constants.PraxConstants.Api.PRAXCLOUD_API_URL;
import static com.praxtourlauncher.constants.PraxConstants.Auth.AUTH_SUCCESS;
import static com.praxtourlauncher.constants.PraxConstants.Auth.DEVICE_UUID;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.praxtourlauncher.R;
import com.praxtourlauncher.api.PraxCloud;
import com.praxtourlauncher.api.entity.LoginUser;
import com.praxtourlauncher.api.entity.Product;
import com.praxtourlauncher.api.helpers.NavHelper;
import com.praxtourlauncher.api.helpers.WifiCallback;
import com.praxtourlauncher.api.helpers.WifiSpeedtest;
import com.praxtourlauncher.constants.HttpStatus;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
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

    private ProgressBar loadingWheel;
    private LinearLayout everythingLayout;

    private Button nextButton;
    private Button previousButton;
    private Button retryButton;
    private ImageView serverStatusImageView;
    private ProgressBar serverStatusLoadingWheel;
    private Button retryServerConnectionButton;

    private String apikey;

    private final Gson gson = new GsonBuilder()
            .setLenient()
            .create();
    private final PraxCloud praxCloud = new Retrofit.Builder()
            .baseUrl(PRAXCLOUD_API_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
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
        serverStatusLoadingWheel = findViewById(R.id.server_status_loading_wheel);
        retryServerConnectionButton = findViewById(R.id.retry_server_connection_button);
        loadingWheel = findViewById(R.id.loading_wheel);
        everythingLayout = findViewById(R.id.everything_layout);

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
                            runOnUiThread(() -> showFailedLoginPage(getString(R.string.login_failed_description)));
                            return;
                        }

                        if (!authenticateDevice()) {
                            runOnUiThread(() -> showFailedLoginPage(getString(R.string.invalid_device_uuid_description)));
                            return;
                        }

                        if (!userHasExistingProducts()) {
                            runOnUiThread(() -> showFailedLoginPage(getString(R.string.no_active_products_description)));
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
        } else {
            verifyIncomingApikey(incomingIntent);
        }

        previousButton.setOnClickListener(v -> showEnterUsernamePage());

        retryServerConnectionButton.setOnClickListener(v -> retryServerConnection());

        retryServerConnection(); // initial try
    }

    private void retryServerConnection() {
        new Thread(() -> {
            WifiSpeedtest.getPingTo(PRAXCLOUD_API_URL, new WifiCallback() {
                @Override
                public void onSuccess(long value) {
                    Log.d(TAG, "Greg ping success");

                    runOnUiThread(() -> {
                        loadingWheel.setVisibility(View.GONE);
                        everythingLayout.setVisibility(View.VISIBLE);
                        showOnlineUi();
                    });

                    final boolean savedApiKey = checkForSavedApikey();
                    final boolean correctDevice = savedApiKey && authenticateDevice();

                    runOnUiThread(() -> {
                        if (savedApiKey) {
                            if (correctDevice) {
                                openInstallerActivity();
                                return;
                            }
                            showFailedLoginPage(getString(R.string.invalid_device_uuid_description));
                        } // else login screen is initialized by default
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.w(TAG, "Greg ping error: " + e);
                    if (checkForSavedApikey()) {
                        NavHelper.launchPraxtourMainApp(LoginActivity.this, apikey);
                    }

                    runOnUiThread(() -> {
                        loadingWheel.setVisibility(View.GONE);
                        everythingLayout.setVisibility(View.VISIBLE);
                        showNoConnectionUi();
                    });
                }
            });

            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Unexpected interrupt: " + e);
            }
        }).start();
    }

    private void showOnlineUi() {
        serverStatusImageView.setImageResource(R.drawable.green_tick);
        serverStatusLoadingWheel.setVisibility(View.GONE);
        serverStatusImageView.setVisibility(View.VISIBLE);
        usernameInput.setEnabled(true);
        nextButton.setEnabled(true);
        retryServerConnectionButton.setVisibility(View.GONE);
    }

    private void showNoConnectionUi() {
        serverStatusImageView.setImageResource(R.drawable.red_x_cross);
        serverStatusLoadingWheel.setVisibility(View.GONE);
        serverStatusImageView.setVisibility(View.VISIBLE);
        usernameInput.setEnabled(false);
        nextButton.setEnabled(false);
        retryServerConnectionButton.setVisibility(View.VISIBLE);
        retryServerConnectionButton.requestFocus();
    }

    private String authenticate(String username, String password) {
        try {
            LoginUser userObj = new LoginUser(username, password);
            return praxCloud.authenticateUser(userObj).execute().body().getAccountToken();
        } catch (IOException e) {
            throw new RuntimeException("Error during authentication", e);
        } catch (NullPointerException e) {
            return null;
        }
    }

    private boolean authenticateDevice() {
        try {
            Response<String> deviceAuthResponse = praxCloud.authenticateDevice(getSavedDeviceUuid(), apikey).execute();
            int responseCode = deviceAuthResponse.code();
            String responseString = deviceAuthResponse.body();

            if (responseCode == HttpStatus.OK.getCode()) {
                if (!AUTH_SUCCESS.equals(responseString)) {
                    // AUTH_SUCCESS occurs when deviceUuid matches with stored.
                    // See further explanation in backend comments
                    saveNewDeviceUuid(responseString);
                }

                return true;
            }

            if (responseCode == HttpStatus.UNAUTHORIZED.getCode()) {
                return false;
            }

            throw new RuntimeException("Unexpected response code from backend during device authentication: " + responseCode);
        } catch (IOException e) {
            throw new RuntimeException("Error during device authentication", e);
        }
    }

    private boolean userHasExistingProducts() {
        Call<List<Product>> allAccountProducts = praxCloud.getActiveProducts(apikey);
        List<Product> activeProducts;
        try {
            activeProducts = allAccountProducts.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.toString());
            return false;
        }

        return activeProducts != null && !activeProducts.isEmpty();
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

    private void showFailedLoginPage(final String reason) {
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
        loginFailedTextView.setText(reason);
        loginFailedTextView.setVisibility(View.VISIBLE);

        retryButton.setOnClickListener(v -> {
            clearSavedCredentials();
            restartLoginActivity();
        });
    }

    @SuppressLint("ApplySharedPref")
    private void clearSavedCredentials() {
        SharedPreferences.Editor editor = getSharedPreferences("app", Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit(); // commit, because we immediately need the value saved
        apikey = null;
    }

    /**
     * When incoming from Praxtour#SplashActivity, we check for the passed apikey (to remain logged
     * in when upgrading from earlier APKs). If the apikey saved differs from that passed, we clear
     * both requiring a new login. If they match or only {@code incomingApikey} has a value, we remain
     * logged in and continue a smooth workflow.
     */
    private void verifyIncomingApikey(Intent incomingIntent) {
        checkForSavedApikey();
        final String incomingApikey = incomingIntent.getStringExtra(EXTRA_ACCOUNT_TOKEN);

        if (apikey == null) {
            if (incomingApikey != null) {
                apikey = incomingApikey;
            }
        } else {
            // null check needed for when Launcher is started straight away, since
            // then it will be null, but if apikey != null we can proceed safely
            if (incomingApikey != null && !apikey.equals(incomingApikey)) {
                clearSavedCredentials();
            }
        }
    }

    private boolean checkForSavedApikey() {
        SharedPreferences sp = getSharedPreferences("app", Context.MODE_PRIVATE);
        apikey = sp.getString(EXTRA_ACCOUNT_TOKEN, "");
        return !apikey.isBlank();
    }

    /**
     * If there is no saved UUID, it means that the device has not been linked to the account yet.
     * In this case this method will return a string with a space(!)*, which corresponds to this situation
     * in the REST API as well.
     * <p>
     * Important: the UUID will be deleted when the app is uninstalled/reinstalled.
     * <p>
     * *not an empty string, as that will cause issues with the URL.
     */
    private String getSavedDeviceUuid() {
        SharedPreferences sp = getSharedPreferences("app", Context.MODE_PRIVATE);
        return sp.getString(DEVICE_UUID, " ");
    }

    private void saveNewDeviceUuid(String deviceUuid) {
        SharedPreferences.Editor editor = getSharedPreferences("app", Context.MODE_PRIVATE).edit();
        editor.putString(DEVICE_UUID, deviceUuid);
        editor.apply();
    }

    private void openInstallerActivity() {
        Intent installerIntent = new Intent(LoginActivity.this, UpdateActivity.class);
        installerIntent.putExtra(EXTRA_ACCOUNT_TOKEN, apikey);
        startActivity(installerIntent);
        finish();
    }

    private void restartLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}