package download.lib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import kotlin.jvm.functions.Function3;


import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

import io.reactivex.rxjava3.schedulers.Schedulers;

import kotlin.Unit;

public class VideoDownloadManager {
    private static final String TAG = "VideoDownloadManager";
    private static final int DEFAULT_THUMBNAIL_QUALITY = 90;
    private static final String THUMBNAILS_DIR = "thumbnails";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final CompositeDisposable compositeDisposable;

    private Future<?> currentDownload;
    private String currentProcessId;
    private DownloadCallback callback;
    private final AtomicBoolean isPaused;
    private final AtomicBoolean isDownloading;
    private static final AtomicBoolean updating = new AtomicBoolean(false);

    public static final List<String> SUPPORTED_PLATFORMS = Arrays.asList(
            "facebook", "fb", "instagram", "tiktok"
    );

    public interface DownloadCallback {
        void onProgressUpdate(float progress, long downloadedBytes, long totalBytes, String status);
        void onSuccess(String filePath, String thumbnailPath);
        void onError(String error, boolean isRetryable);
        void onCancelled();
        void onPaused();
        void onResumed();
        void onFileExists(String filePath, FileExistsCallback fileExistsCallback);

        interface FileExistsCallback {
            void onOverwrite();
            void onSkip();
        }
    }

    public VideoDownloadManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.compositeDisposable = new CompositeDisposable();
        this.isPaused = new AtomicBoolean(false);
        this.isDownloading = new AtomicBoolean(false);
        initializeYoutubeDL();
    }

    private void initializeYoutubeDL() {
        try {
            YoutubeDL.getInstance().init(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize YoutubeDL", e);
        }
    }

    public void downloadVideo(@NonNull DownloadConfig config, @NonNull DownloadCallback callback) {
        if (!validateUrl(config.getUrl())) {
            notifyError("Invalid URL format", false);
            return;
        }

        if (isDownloading.get()) {
            notifyError("A download is already in progress", false);
            return;
        }

        this.callback = callback;
        isPaused.set(false);
        isDownloading.set(true);

        AtomicInteger retryCount = new AtomicInteger(0);
        startDownloadWithRetry(config, retryCount);
    }

    private void startDownloadWithRetry(@NonNull DownloadConfig config, @NonNull AtomicInteger retryCount) {
        currentDownload = executorService.submit(() -> {
            try {
                updateProgress(0, 0, 0, "Getting video information...");
                VideoInfo videoInfo = YoutubeDL.getInstance().getInfo(config.getUrl());

                File outputFile = prepareOutputFile(config, videoInfo);
                if (outputFile.exists()) {
                    handleExistingFile(outputFile, config, videoInfo);
                    return;
                }

                executeDownload(config, videoInfo, outputFile);

            } catch (Exception e) {
                handleDownloadError(e, config, retryCount);
            }
        });
    }

    private final Function3<Float, Long, String, Unit> progressCallback = new Function3<Float, Long, String, Unit>() {
        @Override
        public Unit invoke(Float aFloat, Long aLong, String string) {
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onProgressUpdate(
                            aFloat.floatValue(),
                            -1,
                            -1,
                            string
                    );
                }
            });
            return Unit.INSTANCE;
        }

    };

    @SuppressLint("CheckResult")
    private void executeDownload(@NonNull DownloadConfig config, @NonNull VideoInfo videoInfo, @NonNull File outputFile) throws Exception {
        YoutubeDLRequest request = buildDownloadRequest(config, outputFile);
        String processId = String.valueOf(System.currentTimeMillis());
        currentProcessId = processId;

        Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request,"ff",   progressCallback))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    if (!isPaused.get()) {
                    }
                }, e -> {
                    if (!isPaused.get()) {
                        notifyError("Download failed: " + e.getMessage(), true);
                    }
                });
    }

    private YoutubeDLRequest buildDownloadRequest(@NonNull DownloadConfig config, @NonNull File outputFile) {
        YoutubeDLRequest request = new YoutubeDLRequest(config.getUrl());
        request.addOption("-o", outputFile.getAbsolutePath());

        if (config.getDownloadType() == DownloadConfig.DownloadType.AUDIO_ONLY) {
            request.addOption("-x")
                    .addOption("--audio-format", "mp3")
                    .addOption("--audio-quality", "0");
        } else {
            configureVideoQuality(request, config.getQuality(), config.getSpecificQuality());
        }

        if (config.isUseAria2Download()) {
            request.addOption("--external-downloader", "aria2c")
                    .addOption("--external-downloader-args", "aria2c:'-x 16 -s 16 -k 1M'");
        }

        request.addOption("--socket-timeout", String.valueOf(config.getTimeoutSeconds()));

        if (config.getSubtitleLanguages() != null && !config.getSubtitleLanguages().isEmpty()) {
            request.addOption("--write-sub")
                    .addOption("--sub-langs", String.join(",", config.getSubtitleLanguages()));
        }

        return request;
    }

    private void configureVideoQuality(YoutubeDLRequest request, DownloadConfig.VideoQuality quality, String specificQuality) {
        switch (quality) {
            case BEST:
                request.addOption("-f", "best");
                break;
            case WORST:
                request.addOption("-f", "worstvideo+worstaudio/worst");
                break;
            case SPECIFIC:
                if (specificQuality != null && !specificQuality.isEmpty()) {
                    request.addOption("-f", specificQuality);
                }
                break;
        }
    }

    private File prepareOutputFile(@NonNull DownloadConfig config, @NonNull VideoInfo videoInfo) {
        String extension = config.getDownloadType() == DownloadConfig.DownloadType.AUDIO_ONLY ? "mp3" : videoInfo.getExt();
        String fileName = videoInfo.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_"); // Remove invalid characters
        return new File(config.getOutputPath(), fileName + "." + extension);
    }

    private void handleExistingFile(@NonNull File outputFile, @NonNull DownloadConfig config, @NonNull VideoInfo videoInfo) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onFileExists(outputFile.getAbsolutePath(), new DownloadCallback.FileExistsCallback() {
                    @Override
                    public void onOverwrite() {
                        if (outputFile.delete()) {
                            startDownloadWithRetry(config, new AtomicInteger(0));
                        } else {
                            notifyError("Failed to delete existing file", false);
                        }
                    }

                    @Override
                    public void onSkip() {
                    }
                });
            }
        });
    }

    public void pauseDownload() {
        if (currentProcessId != null && isDownloading.get()) {
            isPaused.set(true);
            YoutubeDL.getInstance().destroyProcessById(currentProcessId);
            notifyPaused();
        }
    }

    public void resumeDownload() {
        if (isPaused.get() && callback != null) {
            isPaused.set(false);
            notifyResumed();
            // Implement resume logic here
        }
    }

    public void cancelDownload() {
        if (currentProcessId != null) {
            YoutubeDL.getInstance().destroyProcessById(currentProcessId);
            if (currentDownload != null) {
                currentDownload.cancel(true);
            }
            isDownloading.set(false);
            notifyCancelled();
        }
    }

    private void handleDownloadError(Exception e, DownloadConfig config, AtomicInteger retryCount) {
        if (retryCount.incrementAndGet() <= config.getMaxDownloadRetries()) {
            mainHandler.postDelayed(() ->
                    startDownloadWithRetry(config, retryCount), RETRY_DELAY_MS);
        } else {
            notifyError("Download failed after " + config.getMaxDownloadRetries() +
                    " attempts: " + e.getMessage(), false);
        }
    }

    private boolean validateUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost().toLowerCase();
            return SUPPORTED_PLATFORMS.stream()
                    .anyMatch(platform -> host.contains(platform));
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private void updateProgress(float progress, long downloadedBytes, long totalBytes, String status) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onProgressUpdate(progress, downloadedBytes, totalBytes, status);
            }
        });
    }

    private void notifySuccess(String filePath, String thumbnailPath) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onSuccess(filePath, thumbnailPath);
            }
            isDownloading.set(false);
        });
    }

    private void notifyError(String error, boolean isRetryable) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(error, isRetryable);
            }
            isDownloading.set(false);
        });
    }

    private void notifyCancelled() {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onCancelled();
            }
            isDownloading.set(false);
        });
    }

    private void notifyPaused() {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onPaused();
            }
        });
    }

    private void notifyResumed() {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onResumed();
            }
        });
    }

    public void release() {
        compositeDisposable.dispose();
        executorService.shutdown();
    }

    public static void updateYoutubeDL(@NonNull Context context) {
        if (updating.get()) return;

        updating.set(true);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Update Channel")
                .setItems(new String[]{"Stable", "Nightly", "Master"},
                        (dialogInterface, which) -> {
                            YoutubeDL.UpdateChannel channel = which == 0 ?
                                    YoutubeDL.UpdateChannel._STABLE :
                                    (which == 1 ? YoutubeDL.UpdateChannel._NIGHTLY :
                                            YoutubeDL.UpdateChannel._MASTER);

                            performUpdate(context, channel);
                        })
                .setOnCancelListener(dialog1 -> updating.set(false))
                .create();
        dialog.show();
    }

    private static void performUpdate(@NonNull Context context, @NonNull YoutubeDL.UpdateChannel channel) {
        Disposable disposable = Observable.fromCallable(() ->
                        YoutubeDL.getInstance().updateYoutubeDL(context, channel))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        status -> {
                            Log.i(TAG, "Update status: " + status);
                            updating.set(false);
                        },
                        error -> {
                            Log.e(TAG, "Update failed", error);
                            updating.set(false);
                        }
                );
    }
}
