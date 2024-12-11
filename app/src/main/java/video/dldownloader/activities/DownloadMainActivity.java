package video.dldownloader.activities;

 import static video.dldownloader.activities.FacebookDownloader.DownloadConfig.isVideoFormat;
 import static video.dldownloader.util.Constants.BANNERADUNITID;
import static video.dldownloader.util.Constants.INTERSTITIALADUNITID;
import static video.dldownloader.util.Constants.NATIVEADUNITID;
import static video.dldownloader.util.Constants.OPENADUNITID;
import static video.dldownloader.util.Constants.REWARDEDADUNITID;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.URLUtil;
 import android.widget.Button;
 import android.widget.LinearLayout;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
 import androidx.work.WorkManager;

 import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
 import java.util.Objects;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;

 import download.lib.ads.AdManager;
import download.lib.ads.NativeAdManager;
import download.lib.util.DialogUtils;
import download.lib.util.ToastUtil;
import video.dldownloader.DownloadedFile;
import video.dldownloader.R;
import video.dldownloader.databinding.ActivityMainDownloadBinding;

public class DownloadMainActivity extends AppCompatActivity  {
    private static final int PERMISSION_REQUEST_CODE = 1234;
    private ActivityMainDownloadBinding binding;
    private FacebookDownloader downloadManager;
    private boolean isPaused = false;
     // Track current format
    private String lastFailedUrl;
    private AdManager adManager;
    private NativeAdManager nativeAdManager;


    private ProgressBar progressBar;
    private TextView fileSizeText;
    private Button pauseResumeButton, cancelButton;
    private WorkManager workManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        binding = ActivityMainDownloadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressBar = findViewById(R.id.progressBar2);
        fileSizeText = findViewById(R.id.fileSizeTextProgress);
        pauseResumeButton = findViewById(R.id.pauseResumeButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Initialize AdManager
        adManager = AdManager.getInstance(this,BANNERADUNITID,INTERSTITIALADUNITID,REWARDEDADUNITID,OPENADUNITID);

        ViewGroup viewGroup= findViewById(R.id.native_ad_container);
        nativeAdManager = new NativeAdManager(this, NATIVEADUNITID, viewGroup);

        // Load a single native ad
        nativeAdManager.loadAd();


        // Initialize video download manager
        downloadManager = new FacebookDownloader(this);
        setupClickListeners();
        setupBottomNavigation();
        checkPermissions();


    }






    private void setupClickListeners() {
        binding.formatButton.setOnClickListener(v ->  showFormatDialog());
        binding.pauseResumeButton.setOnClickListener(v -> togglePauseResume());
        binding.cancelButton.setOnClickListener(v -> cancelDownload());
        binding.pasteButton.setOnClickListener(v -> pasteFromClipboard());
        binding.urlInputLayout.setEndIconOnClickListener(v -> binding.urlInput.setText(""));
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_downloads) {
                startDownloadsActivity();
                return false;
            } else if (item.getItemId() == R.id.navigation_settings) {
                startSettingsActivity();
                return false;
            }
            return true;
        });
    }

    private void showFormatDialog(){
        String url=binding.urlInput.getText().toString().trim();
        if (!isValidUrl(url)){
            ToastUtil.showCustomToast(this,"This not a valid Url",false);
            return;
        }
        if (!isInternetAvailable()) {
            showErrorDialog(getString(R.string.error_no_internet_title), getString(R.string.error_no_internet));
            return;
        }

        DialogUtils.showFormatDialog(DownloadMainActivity.this,url, (url1, isVideoFormat) -> {
            startDownload(url);
        });
    }

    private boolean isValidUrl(String url) {
        return url != null && !url.isEmpty() && URLUtil.isValidUrl(url);
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

    private void showErrorDialog(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void startDownload(String url) {
        if (!isValidUrl(url)) {
            showErrorDialog(getString(R.string.error_invalid_url_title), getString(R.string.error_invalid_url));
            return;
        }

        if (!isInternetAvailable()) {
            showErrorDialog(getString(R.string.error_no_internet_title), getString(R.string.error_no_internet));
            return;
        }

        lastFailedUrl = url;
        File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "VideoDownloader");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        // Initialize download
        binding.formatButton.setEnabled(false);
        setProgress(0);

        FacebookDownloader.DownloadConfig config = new FacebookDownloader.DownloadConfig(
                url,
                downloadDir,
                FacebookDownloader.DownloadConfig.VideoQuality.BEST,
                null,
                isVideoFormat ? FacebookDownloader.DownloadConfig.DownloadType.VIDEO
                        : FacebookDownloader.DownloadConfig.DownloadType.AUDIO_ONLY,
                null,
                3,
                300,
                false
        );

        downloadManager.downloadVideo(config, new FacebookDownloader.DownloadCallback() {
            @Override
            public void onProgressUpdate(float progress, String status) {
                // Update progress bar and status
             //   binding.progressBarContainer.setBackgroundColor(Color.GREEN);
                if (status.length() <= 40) {
                    binding.statusText.setText(status);
                } else binding.statusText.setText(getString(R.string.status_paste_download));
            }

            @Override
            public void onSuccess(String filePath, String thumbnailPath) {
                binding.statusText.setText(getString(R.string.status_download_completed));
            //    binding.progressBarContainer.setBackgroundColor(Integer.parseInt("?attr/colorPrimary"));
                resetUI();
                ToastUtil.showCustomToast(DownloadMainActivity.this,
                        isVideoFormat ? getString(R.string.status_video_downloaded) : getString(R.string.status_audio_downloaded), true);
                saveDownloadInfo(filePath, thumbnailPath);
            }

            @Override
            public void onError(String error) {
             //   binding.progressBarContainer.setBackgroundColor(Color.RED);
                FacebookDownloader.update(DownloadMainActivity.this);
                binding.statusText.setText("Error: " + error);
                resetUI();
                if (!isInternetAvailable()) {
                    showErrorDialog(getString(R.string.error_no_internet_title), getString(R.string.error_no_internet));
                    return;
                }
                new MaterialAlertDialogBuilder(DownloadMainActivity.this)
                        .setTitle(R.string.error_download_failed)
                        .setMessage(R.string.error_retry_message)
                        .setPositiveButton(R.string.retry, (dialog, which) -> {
                            if (lastFailedUrl != null && isValidUrl(lastFailedUrl)) {
                                startDownload(lastFailedUrl);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }

            @Override
            public void onCancelled() {
                binding.statusText.setText(getString(R.string.status_download_cancelled));
                resetUI();
            }

            @Override
            public void onPaused() {
                binding.statusText.setText(getString(R.string.status_download_paused));
                updatePauseResumeButton(true);
            }

            @Override
            public void onResumed() {
                binding.statusText.setText(getString(R.string.status_download_resumed));
                updatePauseResumeButton(false);
            }

            @Override
            public void onFileExists(String filePath, FileExistsCallback fileExistsCallback) {
                new MaterialAlertDialogBuilder(DownloadMainActivity.this)
                        .setTitle(R.string.file_exists)
                        .setMessage(getString(R.string.file_exists_message, filePath))
                        .setPositiveButton(R.string.overwrite, (dialog, which) -> {
                            fileExistsCallback.onOverwrite();
                        })
                        .setNegativeButton(R.string.skip, (dialog, which) -> {
                            fileExistsCallback.onSkip();
                        })
                        .setCancelable(false)
                        .show();
            }
        });
    }

    private void togglePauseResume() {
        if (isPaused) {
            downloadManager.resumeDownload();
        } else {
            downloadManager.pauseDownload();
        }
        isPaused = !isPaused;
        updatePauseResumeButton(isPaused);
    }

    private void updatePauseResumeButton(boolean paused) {
        binding.pauseResumeButton.setText(paused ? "Resume" : "Pause");
    }

    private void cancelDownload() {
        downloadManager.cancelDownload();
    }

    private void resetUI() {
        binding.progressCard.setVisibility(View.GONE);
        binding.formatButton.setEnabled(true);
        setProgress(0);
        isPaused = false;
        updatePauseResumeButton(false);
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData.Item item = Objects.requireNonNull(clipboard.getPrimaryClip()).getItemAt(0);
            String text = item.getText().toString();
            if (text != null && !text.isEmpty()) {
                binding.urlInput.setText(text);
                binding.urlInput.setSelection(text.length());
            }
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void saveDownloadInfo(String filePath, String thumbnailPath) {
        // Save the downloaded file info, e.g., in a database or shared preferences
        DownloadedFile downloadedFile = new DownloadedFile(filePath);
        downloadedFile.setThumbnailPath(thumbnailPath);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadManager.release();
        // Clean up the native ad when the activity is destroyed
        // Release resources held by the NativeAdManager
        if (nativeAdManager != null) {
            nativeAdManager.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void startDownloadsActivity() {
        adManager.showInterstitialAd(this, () -> {
            Intent intent = new Intent(this, DownloadsActivity.class);
            startActivity(intent);
        });
    }

    private void startSettingsActivity() {
        adManager.showInterstitialAd(this, () -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    // Method to update the progress
    private void setLayoutProgress(int progress) {
        int width = (int) (binding.progressBarContainer.getWidth() * (progress / 100.0f));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.progressBar.getLayoutParams();
        params.width = width;
        binding.progressBar.setLayoutParams(params);

        if (progress == 100) {
            binding.progressBar.setBackgroundColor(getResources().getColor(R.color.progress_color_green));
            Toast.makeText(DownloadMainActivity.this, "Progress complete!", Toast.LENGTH_SHORT).show();
        } else if (progress < 50) {
            binding.progressBar.setBackgroundColor(getResources().getColor(R.color.progress_color_red));
        } else {
            binding.progressBar.setBackgroundColor(Color.YELLOW);
        }
    }

    public  boolean matchesPattern(String url) {
        // Define the regular expression pattern for a valid URL
        String urlPattern = "^(https?|ftp)://[a-zA-Z0-9.-]+(?:/[a-zA-Z0-9.-]*)?$";

        // Create a Pattern object
        Pattern pattern = Pattern.compile(urlPattern);

        // Create a matcher object
        Matcher matcher = pattern.matcher(url);

        // Return true if the URL matches the pattern, otherwise false
        return matcher.matches();
    }
}
