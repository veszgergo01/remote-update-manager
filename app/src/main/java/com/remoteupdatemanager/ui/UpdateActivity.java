package com.remoteupdatemanager.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.remoteupdatemanager.api.PraxCloud;
import com.remoteupdatemanager.api.entity.ApkPublicDescription;
import com.remoteupdatemanager.install.PraxPackageInstaller;
import com.remoteupdatemanager.R;
import com.remoteupdatemanager.download.ApkDownloader;
import com.remoteupdatemanager.download.CallbackByteChannel;
import com.remoteupdatemanager.download.ProgressCallback;

import static com.remoteupdatemanager.constants.PraxConstants.Api.PRAXCLOUD_API_URL;
import static com.remoteupdatemanager.constants.PraxConstants.ApkUpdate.EVENT_INSTALL_COMPLETE;
import static com.remoteupdatemanager.constants.PraxConstants.EXTRA_ACCOUNT_TOKEN;
import static com.remoteupdatemanager.constants.PraxConstants.EXTRA_FIRST_LOGIN;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Stack;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UpdateActivity extends AppCompatActivity {
    private final static String TAG = UpdateActivity.class.getSimpleName();
    // ~200MB
    private static final long APK_EXPECTED_FILE_SIZE = 200 * 1024 * 1024L; // TODO put on cloudserver as metadata
    private static final int MIN_WAIT_TIME_LOADING_MS = 1500;

    public static final String PRAXTOUR_APP_PACKAGE_NAME = "com.videostreamtest";
    private String accountToken;
    Stack<ApkPublicDescription> packagesInfo = new Stack<>();

    private TextView titleTextView;
    private ProgressBar checkingForUpdatesLoadingWheel;
    private TextView descriptionTextView;
    private TextView statusTextView;
    private Button startInstallationButton;
    private Button openPraxtourButton;
    private Button permissionButton;
    private Button restartProcessButton;
    private LinearLayout installationInProgressLayout;
    private SeekBar downloadStatusProgressBar;
    private TextView appAccountInfoTextView;
    private final BroadcastReceiver installResultLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            processNextPackage();
        }
    };
    /**
     * TLDR; A different text is displayed after the user rechecks for the {@code Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES}
     * permission, but they did not grant it.
     * <p>
     * At a point the user might be asked to grant {@code Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES}
     * permission. Since the permission is toggleable and there is no direct "result" that
     * we could register for, the user manually has to recheck the permission. When this happens,
     * and the user did NOT grant the permission (did not toggle), a different text is displayed
     * to emphasize it. For that, this boolean is used.
     *   */
    boolean manuallyRecheckedInstallPermission = false;

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    switch (result.getResultCode()) {
                        case Activity.RESULT_OK:
//                            runOnUiThread(UpdateActivity.this::showInstallationStep);
                            break;
                        case Activity.RESULT_CANCELED:
                            // TODO show permission required page
                            break;
                        default:
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
        checkingForUpdatesLoadingWheel = findViewById(R.id.checking_for_updates_loading_wheel);
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

        openPraxtourButton.setOnClickListener(view -> launchPraxtourMainApp());
        restartProcessButton.setOnClickListener(view -> restartUpdateProcess());
        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void onClick(View v) {
                requestInstallPackagesPermission();
                permissionButton.setText(getString(R.string.recheck_permission_button_text));
                permissionButton.setOnClickListener(view1 -> {
                    manuallyRecheckedInstallPermission = true;
                    runOnUiThread(UpdateActivity.this::checkInstallPackagePermissionRunnable);
                });
            }
        });

        accountToken = getIntent().getStringExtra(EXTRA_ACCOUNT_TOKEN);

        new Thread(() -> {
            try {
                Thread.sleep(MIN_WAIT_TIME_LOADING_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Unexpected interrupt", e);
            }
            packagesInfo.addAll(fetchAllPackagePublicInfo());
            runOnUiThread(() -> checkingForUpdatesLoadingWheel.setVisibility(View.GONE));
            processNextPackage();
        }).start();
    }

    private void processNextPackage() {
        new Thread(() -> {
            if (!packagesInfo.isEmpty()) {
                ApkPublicDescription packageInfo = packagesInfo.pop();
                String packageName = packageInfo.getPackageName();
                if (packageName.equals(getPackageName()) || !packageShouldBeProcessed(packageInfo)) {
                    processNextPackage();
                    return;
                }

                String appName = packageInfo.getAppName();
                runOnUiThread(() -> showDownloadStep(appName));
                boolean downloaded = downloadApk(packageName);
                if (!downloaded) return;

                runOnUiThread(() -> {
                    if (!checkInstallPackagePermissionRunnable()) {
                        showInstallationStep(appName);
                    }
                });
            } else {
                launchPraxtourMainApp();
            }
        }).start();
    }

    private boolean packageShouldBeProcessed(ApkPublicDescription packageInfo) {
        PackageInfo onDevicePackageInfo;

        try {
            onDevicePackageInfo = getPackageManager().getPackageInfo(packageInfo.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }

        String onDevicePackageVersion = onDevicePackageInfo.versionName;
        String remotePackageVersion = packageInfo.getVersion();

        return isUpdateAvailable(onDevicePackageVersion, remotePackageVersion);
    }

    private boolean isUpdateAvailable(String installedVersion, String remoteVersion) {
        String[] installedVersionSplit = installedVersion.split("\\.");
        String[] remoteVersionSplit = remoteVersion.split("\\.");

        if (installedVersionSplit.length != remoteVersionSplit.length) {
            throw new RuntimeException("Version number patterns don't match!" +
                    "\n\tLocal: " + installedVersion +
                    "\n\tRemote: " + remoteVersion);
        }

        for (int i = 0; i < installedVersionSplit.length; i++) {
            int local = Integer.parseInt(installedVersionSplit[i]);
            int remote = Integer.parseInt(remoteVersionSplit[i]);

            if (remote > local) return true;
        }

        return false;
    }

    private boolean downloadApk(String packageName) {
        URL updateFileUrl = fetchUpdateLink(packageName);
        File apkFile = ApkDownloader.download(this, updateFileUrl, APK_EXPECTED_FILE_SIZE, new ProgressCallback() {
            @Override
            public void callback(CallbackByteChannel rbc, double progress) {
                downloadStatusProgressBar.setProgress((int) progress);
            }
        });
        runOnUiThread(() -> downloadStatusProgressBar.setVisibility(View.GONE));

        if (apkFile == null) {
            runOnUiThread(() -> displayErrorEncountered(ErrorStep.DOWNLOAD));
            return false;
        }
        Log.d(TAG, "Greg file is ok: " + apkFile.getAbsolutePath());

        startInstallationButton.setOnClickListener(view -> {
            try {
                startInstallationButton.setVisibility(View.GONE);
                installationInProgressLayout.setVisibility(View.VISIBLE);
                boolean result = PraxPackageInstaller.installApk(this, apkFile);
                if (!result) throw new IOException("Unknown error:(");
            } catch (IOException e) {
                runOnUiThread(() -> displayErrorEncountered(ErrorStep.INSTALL));
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

    private boolean checkInstallPackagePermissionRunnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            if (manuallyRecheckedInstallPermission) {
                descriptionTextView.setText(getString(R.string.no_permission_after_interaction));
                return false;
            } else {
                showPermissionRequiredPage();
                return true;
            }
        }

        return false;
    }

    private void showDownloadStep(String appName) {
        titleTextView.setText(String.format("Downloading %s", appName));
        descriptionTextView.setText(getString(R.string.app_update_status_download_text));
        descriptionTextView.setVisibility(View.VISIBLE);
        permissionButton.setVisibility(View.GONE);
        downloadStatusProgressBar.setVisibility(View.VISIBLE);
    }

    private void showInstallationStep(String appName) {
        titleTextView.setText(String.format("%s downloaded", appName));
        descriptionTextView.setVisibility(View.GONE);
        permissionButton.setVisibility(View.GONE);
        statusTextView.setText(getString(R.string.app_update_status_install_text));
        statusTextView.setVisibility(View.VISIBLE);
        startInstallationButton.setVisibility(View.VISIBLE);
        startInstallationButton.requestFocus();
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

    private void launchPraxtourMainApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(PRAXTOUR_APP_PACKAGE_NAME);
        boolean isFirstLogin = getIntent().getBooleanExtra(EXTRA_FIRST_LOGIN, true);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            launch.putExtra(EXTRA_ACCOUNT_TOKEN, accountToken);
            launch.putExtra(EXTRA_FIRST_LOGIN, isFirstLogin);
            startActivity(launch);
            finishAffinity();
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

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(installResultLocalReceiver, new IntentFilter(EVENT_INSTALL_COMPLETE));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(installResultLocalReceiver);
    }
}