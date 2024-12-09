package download.lib;

public interface DownloadCallback {
    void onDownloadRequested(String url, boolean isVideoFormat);
}

