package com.remoteupdatemanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.remoteupdatemanager.download.ApkDownloader;
import com.remoteupdatemanager.download.CallbackByteChannel;
import com.remoteupdatemanager.download.ProgressCallback;

import java.io.File;
import java.io.IOException;

public class UpdateActivity extends AppCompatActivity {
    private final static String TAG = UpdateActivity.class.getSimpleName();

    private static final String EXTRA_APK_FILENAME = "com.praxupdatemanager.APK_FILENAME";
    private static final String EXTRA_UPDATE_VERSION = "com.praxupdatemanager.APK_UPDATE_VERSION";
    private static final String EXTRA_CURRENT_VERSION = "com.praxupdatemanager.APK_CURRENT_VERSION";
    private static final String ACTION_PRAX_REMOTE_UPDATE = "com.praxupdatemanager.ACTION_PRAX_REMOTE_UPDATE";

    public static final String PRAXTOUR_APP_PACKAGE_NAME = "com.videostreamtest";

    private TextView updateTitleTextView;
    private TextView updateDescriptionTextView;
    private TextView updateStatusTextView;
    private Button startInstallationButton;
    private Button openPraxtourButton;
    private Button restartProcessButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        updateTitleTextView = findViewById(R.id.update_title_textview);
        updateDescriptionTextView = findViewById(R.id.update_description_textview);
        updateStatusTextView = findViewById(R.id.update_status_textview);
        startInstallationButton = findViewById(R.id.start_installation_button);
        openPraxtourButton = findViewById(R.id.open_praxtour_button);
        restartProcessButton = findViewById(R.id.restart_process_button);

//        openPraxtourButton.setOnClickListener(view -> launchPraxtourMainApp());
        restartProcessButton.setOnClickListener(view -> restartUpdateProcess());

        Intent updateIntent = getIntent();
        if (!ACTION_PRAX_REMOTE_UPDATE.equals(updateIntent.getAction())) {
            updateTitleTextView.setText(getString(R.string.alternative_title));
            updateDescriptionTextView.setText(getString(R.string.alternative_description));
            updateStatusTextView.setVisibility(View.GONE);
            openPraxtourButton.setVisibility(View.VISIBLE);
            return;
        }

        String updateFilename = updateIntent.getStringExtra(EXTRA_APK_FILENAME);
        String currentApkVersion = updateIntent.getStringExtra(EXTRA_CURRENT_VERSION);
        String newApkVersion = updateIntent.getStringExtra(EXTRA_UPDATE_VERSION);

        updateDescriptionTextView.setText(String.format(getString(R.string.app_update_description), currentApkVersion, newApkVersion));
        updateStatusTextView.setText(getString(R.string.app_update_status_download_text));

        new Thread(() -> {
            try {
                File apkFile = ApkDownloader.download(this, updateFilename, new ProgressCallback() {
                    @Override
                    public void callback(CallbackByteChannel rbc, double progress) {

                    }
                });

                if (apkFile == null) {
                    displayErrorEncountered(ErrorStep.DOWNLOAD);
                    return;
                }
                Log.d(TAG, "Greg file is ok: " + apkFile.getAbsolutePath());

                startInstallationButton.setOnClickListener(view -> {
                    try {
                        boolean result = PraxPackageInstaller.installApk(this, apkFile);
                    } catch (IOException e) {
                        Log.d(TAG, "Greg uh-oh");
                    }
                });

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!getPackageManager().canRequestPackageInstalls()) {
                        Intent settingsIntent = new Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + getPackageName())
                        );
                        startActivityForResult(settingsIntent, 1001);
                    }
                } else {
                    Log.d(TAG, "Greg showing installation step");
                    runOnUiThread(this::showInstallationStep);
                }
            } catch (Exception e) {
                // TODO error: display error message, contact support
            }
        }).start();
    }

    private void showInstallationStep() {
        startInstallationButton.setVisibility(View.VISIBLE);
        updateStatusTextView.setText(getString(R.string.app_update_status_install_text));
        startInstallationButton.setEnabled(true);
    }

    private void displayErrorEncountered(ErrorStep step) {
        String titleText = "";
        String titleDescription = "";

        switch (step) {
            case DOWNLOAD:
                titleText = "Download error";
                titleDescription = "We encountered an error while downloading the update. Please try again. If the problem persists, contact us at service@praxtour.com";
                restartProcessButton.setVisibility(View.VISIBLE);
                break;
        }

        updateTitleTextView.setText(titleText);
        updateDescriptionTextView.setText(titleDescription);
        updateStatusTextView.setVisibility(View.GONE);
    }

    private void restartUpdateProcess() {
        startActivity(new Intent(UpdateActivity.this, UpdateActivity.class));
        finish();
    }

    enum ErrorStep {
        DOWNLOAD;
    }
}