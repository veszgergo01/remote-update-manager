package com.remoteupdatemanager.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.remoteupdatemanager.install.PraxPackageInstaller;
import com.remoteupdatemanager.R;
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
    // ~200MB
    private static final long APK_EXPECTED_FILE_SIZE = 200 * 1024 * 1024L; // TODO put on cloudserver as metadata

    public static final String PRAXTOUR_APP_PACKAGE_NAME = "com.videostreamtest";

    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView statusTextView;
    private Button startInstallationButton;
    private Button openPraxtourButton;
    private Button permissionButton;
    private Button restartProcessButton;
    private LinearLayout installationInProgressLayout;
    private SeekBar downloadStatusProgressBar;
    private TextView appAccountInfoTextView;

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    switch (result.getResultCode()) {
                        case Activity.RESULT_OK:
                            Log.d(TAG, "Greg1");
                            runOnUiThread(() -> showInstallationStep());
                            break;
                        case Activity.RESULT_CANCELED:
                            Log.d(TAG, "Greg2");
                            // TODO show permission required page
                            break;
                        default:
                            Log.d(TAG, "Greg3");
                            // TODO error occurred during permission giving, contact service@praxtour.com
                            break;
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        titleTextView = findViewById(R.id.title_textview);
        descriptionTextView = findViewById(R.id.description_textview);
        statusTextView = findViewById(R.id.status_textview);
        startInstallationButton = findViewById(R.id.start_installation_button);
        openPraxtourButton = findViewById(R.id.open_praxtour_button);
        permissionButton = findViewById(R.id.permission_button);
        restartProcessButton = findViewById(R.id.restart_process_button);
        installationInProgressLayout = findViewById(R.id.installation_in_progress_layout);
        downloadStatusProgressBar = findViewById(R.id.download_status_progressbar);
        downloadStatusProgressBar.setMax(100);
        appAccountInfoTextView = findViewById(R.id.app_account_info_textview);

        appAccountInfoTextView.setText(String.format("Version %s", getAppVersion()));

        openPraxtourButton.setOnClickListener(view -> launchPraxtourMainApp(this));
        restartProcessButton.setOnClickListener(view -> restartUpdateProcess());

        Intent updateIntent = getIntent();
        if (!ACTION_PRAX_REMOTE_UPDATE.equals(updateIntent.getAction())) {
            downloadStatusProgressBar.setVisibility(View.GONE);
            titleTextView.setText(getString(R.string.alternative_title));
            descriptionTextView.setText(getString(R.string.alternative_description));
            statusTextView.setVisibility(View.GONE);
            openPraxtourButton.setVisibility(View.VISIBLE);
            openPraxtourButton.requestFocus();
            return;
        }

        String updateFilename = updateIntent.getStringExtra(EXTRA_APK_FILENAME);
        String currentApkVersion = updateIntent.getStringExtra(EXTRA_CURRENT_VERSION);
        String newApkVersion = updateIntent.getStringExtra(EXTRA_UPDATE_VERSION);

        descriptionTextView.setText(String.format(getString(R.string.app_update_description), currentApkVersion, newApkVersion));
        statusTextView.setText(getString(R.string.app_update_status_download_text));

        new Thread(() -> {
            File apkFile = ApkDownloader.download(this, updateFilename, APK_EXPECTED_FILE_SIZE, new ProgressCallback() {
                @Override
                public void callback(CallbackByteChannel rbc, double progress) {
                    downloadStatusProgressBar.setProgress((int) progress);
                }
            });
            runOnUiThread(() -> downloadStatusProgressBar.setVisibility(View.GONE));

            if (apkFile == null) {
                displayErrorEncountered(ErrorStep.DOWNLOAD);
                return;
            }
            Log.d(TAG, "Greg file is ok: " + apkFile.getAbsolutePath());

            startInstallationButton.setOnClickListener(view -> {
                try {
                    startInstallationButton.setVisibility(View.GONE);
                    installationInProgressLayout.setVisibility(View.VISIBLE);
                    boolean result = PraxPackageInstaller.installApk(this, apkFile);
                    if (!result) throw new IOException("Unknown error:(");
                } catch (IOException e) {
                    displayErrorEncountered(ErrorStep.INSTALL);
                    Log.e(TAG, "Greg uh-oh " + e);
                }
            });

        return true;
    }

    private List<ApkPublicDescription> fetchAllPackagePublicInfo() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);

        try {
            return praxCloud.getAllPackagesPublicInfo().execute().body();
        } catch (IOException e) {
            throw new RuntimeException("Problem while getting package names");
        }
    }

    private URL fetchUpdateLink(String packageName) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);

        try {
            return praxCloud.getNewestUpdateUrl(packageName, accountToken).execute().body();
        } catch (IOException e) {
            throw new RuntimeException("Problem while getting package URL for " + packageName);
        }
    }

    private final Runnable checkInstallPackagePermissionRunnable = new Runnable() {
        boolean manuallyRechecked = false;
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
                if (manuallyRechecked) {
                    descriptionTextView.setText(getString(R.string.no_permission_after_interaction));
                } else {
                    showPermissionRequiredPage();
                }

                permissionButton.setOnClickListener(view -> {
                    requestInstallPackagesPermission();
                    permissionButton.setText(getString(R.string.recheck_permission_button_text));
                    permissionButton.setOnClickListener(view1 -> {
                        manuallyRechecked = true;
                        runOnUiThread(this);
                    });
                });

                return;
            }

            showInstallationStep();
        }
    };

    private void showInstallationStep() {
        descriptionTextView.setVisibility(View.GONE);
        permissionButton.setVisibility(View.GONE);
        statusTextView.setText(getString(R.string.app_update_status_install_text));
        startInstallationButton.setVisibility(View.VISIBLE);
        startInstallationButton.requestFocus();
        statusTextView.setVisibility(View.VISIBLE);
    }

    private void showPermissionRequiredPage() {
        titleTextView.setText(getString(R.string.permission_required_title));
        descriptionTextView.setText(getString(R.string.permission_required_description));
        statusTextView.setVisibility(View.GONE);
        permissionButton.setText(getString(R.string.grant_permission_button_text));
        permissionButton.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void requestInstallPackagesPermission() {
        Intent settingsIntent = new Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getPackageName())
        );
        activityResultLauncher.launch(settingsIntent);
    }

    public static void launchPraxtourMainApp(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(PRAXTOUR_APP_PACKAGE_NAME);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launch);
        }
    }

    private void restartUpdateProcess() {
        startActivity(new Intent(UpdateActivity.this, UpdateActivity.class));
        finish();
    }

    enum ErrorStep {
        DOWNLOAD,
        INSTALL
    }

    private void displayErrorEncountered(ErrorStep step) {
        String titleText = "";
        String titleDescription = "";

        switch (step) {
            case DOWNLOAD:
                titleText = "Download error";
                titleDescription = "We encountered an error while downloading the update. Please try again. If the problem persists, contact us at service@praxtour.com";
                break;
            case INSTALL:
                titleText = "Error while installing APK";
                titleDescription = "We encountered an error while installing the update. Please try again. If the problem persists, contact us at service@praxtour.com";
        }

        titleTextView.setText(titleText);
        descriptionTextView.setText(titleDescription);
        statusTextView.setVisibility(View.GONE);
        restartProcessButton.setVisibility(View.VISIBLE);
    }

    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "ERROR";
        }
    }
}