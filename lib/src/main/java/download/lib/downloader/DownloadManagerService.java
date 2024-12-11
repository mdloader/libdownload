package download.lib.downloader;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadManagerService {

    private final DownloadManager downloadManager;
    private long downloadId;
    private final Context context;

    public DownloadManagerService(Context context) {
        this.context = context;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    // Start download
    public long startDownload(String url, String fileName) {
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(fileName);
        request.setDescription("Downloading...");
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        downloadId = downloadManager.enqueue(request);
        return downloadId;
    }

    // Check download progress
    @SuppressLint("DefaultLocale")
    public void checkDownloadProgress(long downloadId, ProgressBar progressBar, TextView fileSizeText) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);

        if (cursor.moveToFirst()) {
             @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            @SuppressLint("Range") int totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            @SuppressLint("Range") int downloadedBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

            if (status == DownloadManager.STATUS_RUNNING) {
                progressBar.setProgress((int) ((downloadedBytes * 100L) / totalBytes));
                fileSizeText.setText(String.format("%d%%", (downloadedBytes * 100) / totalBytes));
            } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                Toast.makeText(context, "Download Complete!", Toast.LENGTH_SHORT).show();
            } else if (status == DownloadManager.STATUS_FAILED) {
                Toast.makeText(context, "Download Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Pause download (Optional - requires custom logic to stop the request)
    public void pauseDownload() {
        // DownloadManager does not have a pause method, but you can cancel it and restart later
        downloadManager.remove(downloadId);
    }

    // Cancel download
    public void cancelDownload() {
        downloadManager.remove(downloadId);
    }
}

