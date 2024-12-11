package download.lib.downloader;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;

public class FileDownloadWorker extends Worker {

    private final DownloadManagerService downloadManagerService;

    public FileDownloadWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
        downloadManagerService = new DownloadManagerService(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        String url = getInputData().getString("url");
        String fileName = getInputData().getString("file_name");

        if (url != null && fileName != null) {
            long downloadId = downloadManagerService.startDownload(url, fileName);

            // Monitor download progress and handle retries if the download fails
            boolean downloadComplete = false;
            while (!downloadComplete) {
                downloadManagerService.checkDownloadProgress(downloadId, null, null);
                // Sleep for a bit to prevent rapid checks, and retry if necessary
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return Result.failure();
                }
            }

            return Result.success();
        }

        return Result.failure();
    }

    public static WorkRequest createDownloadWorkRequest(String url, String fileName) {
        return new OneTimeWorkRequest.Builder(FileDownloadWorker.class)
                .setInputData(new Data.Builder()
                        .putString("url", url)
                        .putString("file_name", fileName)
                        .build())
                .build();
    }
}

