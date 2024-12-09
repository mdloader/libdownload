package download.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class VideoDownloadManager {
    private static final String TAG = "VideoDownloadManager";
    private final Context context;
    private final ExecutorService executorService;
    private Future<?> currentDownload;
    private boolean isPaused;
    private final Handler mainHandler;
    private static final String THUMBNAILS_DIR = "thumbnails";

    public interface DownloadCallback {
        void onProgressUpdate(float progress, String status);

        void onSuccess(String filePath, String thumbnailPath);

        void onError(String error);

        void onCancelled();

        void onPaused();

        void onResumed();

        void onFileExists(String filePath, FileExistsCallback fileExistsCallback);

        public interface FileExistsCallback {
            void onOverwrite();

            void onSkip();
        }
    }

    private DownloadCallback callback;
    private String currentProcessId;
    private static boolean updating = false;
    private static final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public VideoDownloadManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initializeYoutubeDL();
    }

    private void initializeYoutubeDL() {
        try {
            YoutubeDL.getInstance().init(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize YoutubeDL", e);
        }
    }

    public  void startDownload(String url, File outputDir, DownloadCallback callback) {
        this.callback = callback;
        isPaused = false;


        currentDownload = executorService.submit(() -> {
            try {
                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("-o", new File(outputDir, "%(title)s.%(ext)s").getAbsolutePath());
                request.addOption("-f", "best");

                // Add progress callback
                request.addOption("--progress-template", "%(progress.downloaded_bytes)s/%(progress.total_bytes)s");

                // Get video info first
                updateProgress(0, "Getting video information...");
                VideoInfo videoInfo = YoutubeDL.getInstance().getInfo(url);

                updateProgress(5, "Starting download...");

                String processId = String.valueOf(System.currentTimeMillis());
                currentProcessId = processId;

                YoutubeDL.getInstance().execute(request, new DownloadProgressCallback() {
                    @Override
                    public void onProgressUpdate(float v, long l, @Nullable String s) {
                        if (v >= 0) {
                            updateProgress(v, String.format("Downloading... %.1f%%", v));
                        }
                    }
                }.toString());

                if (!isPaused) {
                    String downloadedFilePath = new File(outputDir, videoInfo.getTitle() + "." + videoInfo.getExt()).getAbsolutePath();
                    String thumbnailPath = generateThumbnail(new File(downloadedFilePath));
                    notifySuccess(downloadedFilePath, thumbnailPath);
                }
            } catch (Exception e) {
                notifyError("Download failed: " + e.getMessage());
            }
        });
    }


    public void pauseDownload() {
        if (currentProcessId != null) {
            isPaused = true;
            YoutubeDL.getInstance().destroyProcessById(currentProcessId);
            notifyPaused();
        }
    }

    public void resumeDownload() {
        if (isPaused && callback != null) {
            isPaused = false;
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
            notifyCancelled();
        }
    }

    private void updateProgress(float progress, String status) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onProgressUpdate(progress, status);
            }
        });
    }

    private void notifySuccess(String filePath, String thumbnailPath) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onSuccess(filePath, thumbnailPath);
            }
        });
    }

    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(error);
            }
        });
    }

    private void notifyCancelled() {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onCancelled();
            }
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
        executorService.shutdown();
    }

    public void downloadVideo(DownloadConfig config, DownloadCallback callback) {
        this.callback = callback;
        isPaused = false;

        currentDownload = executorService.submit(() -> {
            try {
                // First get video info to check if file exists
                updateProgress(0, "Getting video information...");
                VideoInfo videoInfo = YoutubeDL.getInstance().getInfo(config.url);

                // Check if file exists
                String extension = config.downloadType == DownloadConfig.DownloadType.AUDIO_ONLY ? "mp3" : videoInfo.getExt();
                File outputFile = new File(config.outputPath, videoInfo.getTitle() + "." + extension);

                if (outputFile.exists()) {
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onFileExists(outputFile.getAbsolutePath(), new DownloadCallback.FileExistsCallback() {
                                @Override
                                public void onOverwrite() {
                                    outputFile.delete();
                                    startActualDownload(config, videoInfo);
                                }

                                @Override
                                public void onSkip() {
                                    notifySuccess(outputFile.getAbsolutePath(), generateThumbnail(outputFile));
                                }
                            });
                        }
                    });
                } else {
                    startActualDownload(config, videoInfo);
                }
            } catch (Exception e) {
                notifyError("Download failed: " + e.getMessage());
                //    update(context);
            }
        });
    }

    private void startActualDownload(DownloadConfig config, VideoInfo videoInfo) {
        try {
            YoutubeDLRequest request = new YoutubeDLRequest(config.url);
            request.addOption("-o", new File(config.outputPath, "%(title)s.%(ext)s").getAbsolutePath());

            // Set format based on download type
            if (config.downloadType == DownloadConfig.DownloadType.AUDIO_ONLY) {
                request.addOption("-x"); // Extract audio
                request.addOption("--audio-format", "mp3");
            } else {
                // Video quality selection
                switch (config.quality) {
                    case BEST:
                        request.addOption("-f", "bestvideo+bestaudio/best");
                        break;
                    case WORST:
                        request.addOption("-f", "worstvideo+worstaudio/worst");
                        break;
                    case SPECIFIC:
                        if (config.specificQuality != null) {
                            request.addOption("-f", config.specificQuality);
                        }
                        break;
                }
            }

            // Add subtitle options if specified
            if (config.subtitleLanguages != null && !config.subtitleLanguages.isEmpty()) {
                request.addOption("--write-sub");
                request.addOption("--sub-langs", String.join(",", config.subtitleLanguages));
            }

            // Add aria2 if specified
            if (config.useAria2Download) {
                request.addOption("--external-downloader", "aria2c");
            }

            // Add timeout
            request.addOption("--socket-timeout", String.valueOf(config.timeoutSeconds));

            updateProgress(5, "Starting download...");

            String processId = String.valueOf(System.currentTimeMillis());
            currentProcessId = processId;

            YoutubeDL.getInstance().execute(request, new DownloadProgressCallback() {
                @Override
                public void onProgressUpdate(float percent, long l, @Nullable String s) {
                    if (percent >= 0) {
                        updateProgress(percent, String.format("Downloading... %.1f%%", percent));
                    }
                }
            }.toString());

            if (!isPaused) {
                String extension = config.downloadType == DownloadConfig.DownloadType.AUDIO_ONLY ? "mp3" : videoInfo.getExt();
                String downloadedFilePath = new File(config.outputPath, videoInfo.getTitle() + "." + extension).getAbsolutePath();
                String thumbnailPath = generateThumbnail(new File(downloadedFilePath));
                notifySuccess(downloadedFilePath, thumbnailPath);
            }
        } catch (Exception e) {
            notifyError("Download failed: " + e.getMessage());
            update(context);
        }
    }

    private String generateThumbnail(File videoFile) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoFile.getAbsolutePath());

            // Get the first frame
            Bitmap frame = retriever.getFrameAtTime(0);
            if (frame != null) {
                // Create thumbnails directory
                File thumbnailsDir = new File(videoFile.getParentFile(), THUMBNAILS_DIR);
                if (!thumbnailsDir.exists()) {
                    thumbnailsDir.mkdirs();
                }

                // Create thumbnail file
                String thumbnailName = "thumb_" + videoFile.getName() + ".jpg";
                File thumbnailFile = new File(thumbnailsDir, thumbnailName);

                // Save thumbnail
                FileOutputStream out = new FileOutputStream(thumbnailFile);
                frame.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();

                frame.recycle();
                retriever.release();

                return thumbnailFile.getAbsolutePath();
            }
            retriever.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class DownloadConfig {

        private String url;
        private File outputPath;
        private VideoQuality quality;
        private String specificQuality;
        private DownloadType downloadType;
        private List<String> subtitleLanguages;
        private int maxDownloadRetries;
        private long timeoutSeconds;
        private boolean useAria2Download;
        public static boolean isVideoFormat = true;

        // Constructor
        public DownloadConfig(String url, File outputPath, VideoQuality quality,
                              String specificQuality, DownloadType downloadType,
                              List<String> subtitleLanguages, int maxDownloadRetries,
                              long timeoutSeconds, boolean useAria2Download) {
            this.url = url;
            this.outputPath = outputPath;
            this.quality = quality != null ? quality : VideoQuality.BEST; // Default to BEST
            this.specificQuality = specificQuality;
            this.downloadType = downloadType != null ? downloadType : DownloadType.VIDEO; // Default to VIDEO
            this.subtitleLanguages = subtitleLanguages != null ? subtitleLanguages : List.of();
            this.maxDownloadRetries = maxDownloadRetries > 0 ? maxDownloadRetries : 3; // Default to 3 retries
            this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 300; // Default timeout to 300 seconds
            this.useAria2Download = useAria2Download;
        }

        // Getters and setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public File getOutputPath() {
            return outputPath;
        }

        public void setOutputPath(File outputPath) {
            this.outputPath = outputPath;
        }

        public VideoQuality getQuality() {
            return quality;
        }

        public void setQuality(VideoQuality quality) {
            this.quality = quality;
        }

        public String getSpecificQuality() {
            return specificQuality;
        }

        public void setSpecificQuality(String specificQuality) {
            this.specificQuality = specificQuality;
        }

        public DownloadType getDownloadType() {
            return downloadType;
        }

        public void setDownloadType(DownloadType downloadType) {
            this.downloadType = downloadType;
        }

        public List<String> getSubtitleLanguages() {
            return subtitleLanguages;
        }

        public void setSubtitleLanguages(List<String> subtitleLanguages) {
            this.subtitleLanguages = subtitleLanguages;
        }

        public int getMaxDownloadRetries() {
            return maxDownloadRetries;
        }

        public void setMaxDownloadRetries(int maxDownloadRetries) {
            this.maxDownloadRetries = maxDownloadRetries;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public boolean isUseAria2Download() {
            return useAria2Download;
        }

        public void setUseAria2Download(boolean useAria2Download) {
            this.useAria2Download = useAria2Download;
        }

        // Enum for VideoQuality
        public enum VideoQuality {
            BEST,
            WORST,
            SPECIFIC
        }

        // Enum for DownloadType
        public enum DownloadType {
            VIDEO,
            AUDIO_ONLY,
            THUMBNAIL
        }
    }



    public static void update(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Update Channel")
                .setItems(new String[]{"Stable Releases", "Nightly Releases", "Master Releases"},
                        (dialogInterface, which) -> {
                            if (which == 0)
                                updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE);
                            else if (which == 1)
                                updateYoutubeDL(context, YoutubeDL.UpdateChannel._NIGHTLY);
                            else
                                updateYoutubeDL(context, YoutubeDL.UpdateChannel._MASTER);
                        })
                .create();
        dialog.show();
    }

    private static void updateYoutubeDL(Context context, YoutubeDL.UpdateChannel updateChannel) {
        if (updating) {
            return;
        }

        updating = true;
        // progressBar.setVisibility(View.VISIBLE);
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().updateYoutubeDL(context, updateChannel))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    // progressBar.setVisibility(View.GONE);
                    switch (status) {
                        case DONE:
                            //       Toast.makeText(MainActivity.this, "Update successful " + YoutubeDL.getInstance().versionName(this), Toast.LENGTH_LONG).show();
                            break;
                        case ALREADY_UP_TO_DATE:
                            //     Toast.makeText(MainActivity.this, "Already up to date " + YoutubeDL.getInstance().versionName(this), Toast.LENGTH_LONG).show();
                            break;
                        default:
                            //     Toast.makeText(MainActivity.this, status.toString(), Toast.LENGTH_LONG).show();
                            break;
                    }
                    updating = false;
                }, e -> {
                    Log.e(TAG, "failed to update", e);
                    //  progressBar.setVisibility(View.GONE);
                    //   Toast.makeText(MainActivity.this, "update failed", Toast.LENGTH_LONG).show();
                    updating = false;
                });
        compositeDisposable.add(disposable);
    }
    public static final List<String> list = Arrays.asList("facebook", "fb", "instagram", "tiktok");

    public static String getHostFromURL(String urlString) {
        try {
            // Create a URL object from the string
            URL url = new URL(urlString);

            // Return the host component of the URL (e.g., "www.instagram.com")
            return url.getHost();
        } catch (MalformedURLException e) {
            // Handle the case where the URL is not valid
            System.err.println("Malformed URL: " + e.getMessage());
            return null;
        }
    }
}