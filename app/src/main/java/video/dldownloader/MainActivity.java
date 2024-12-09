package video.dldownloader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.io.File;

import download.lib.FacebookDownloader;
import video.dldownloader.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1234;
    private ActivityMainBinding binding;
    private FacebookDownloader downloadManager;
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        downloadManager = new FacebookDownloader(this);
        setupClickListeners();
        checkPermissions();
    }

    private void setupClickListeners() {
        binding.downloadButton.setOnClickListener(v -> startDownload());
        binding.pauseResumeButton.setOnClickListener(v -> togglePauseResume());
        binding.cancelButton.setOnClickListener(v -> cancelDownload());
    }

    private void startDownload() {
        String url = binding.urlInput.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "VideoDownloader");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        binding.controlsLayout.setVisibility(View.VISIBLE);
        binding.downloadButton.setEnabled(false);
        binding.progressBar.setProgress(0);

        // Create download configuration based on selected format
        FacebookDownloader.DownloadConfig config = new FacebookDownloader.DownloadConfig(
                url,
                downloadDir,
                 FacebookDownloader.DownloadConfig.VideoQuality.BEST,
                null,
                binding.audioRadio.isChecked() ?  FacebookDownloader.DownloadConfig.DownloadType.AUDIO_ONLY :  FacebookDownloader.DownloadConfig.DownloadType.VIDEO,
                null,
                3,
                300,
                false
        );

        downloadManager.downloadVideo(config, new FacebookDownloader.DownloadCallback() {
            @Override
            public void onProgressUpdate(float progress, String status) {
                binding.progressBar.setProgress((int) progress);
                binding.statusText.setText(status);
            }

            @Override
            public void onSuccess(String filePath, String thumbnailPath) {
                binding.statusText.setText("Download completed: " + filePath);
                resetUI();
                Toast.makeText(MainActivity.this,
                        binding.audioRadio.isChecked() ? "Audio downloaded!" : "Video downloaded!",
                        Toast.LENGTH_SHORT).show();

            }



            @Override
            public void onError(String error) {
                binding.statusText.setText("Error: " + error);
                resetUI();
                Toast.makeText(MainActivity.this, "Download failed: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled() {
                binding.statusText.setText("Download cancelled");
                resetUI();
            }

            @Override
            public void onPaused() {
                binding.statusText.setText("Download paused");
                updatePauseResumeButton(true);
            }

            @Override
            public void onResumed() {
                binding.statusText.setText("Download resumed");
                updatePauseResumeButton(false);
            }

            @Override
            public void onFileExists(String filePath, FileExistsCallback fileExistsCallback) {

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
        binding.controlsLayout.setVisibility(View.GONE);
        binding.downloadButton.setEnabled(true);
        binding.progressBar.setProgress(0);
        isPaused = false;
        updatePauseResumeButton(false);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadManager.release();
    }
}