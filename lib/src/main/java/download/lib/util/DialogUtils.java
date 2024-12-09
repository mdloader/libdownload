package download.lib.util;

 import static download.lib.VideoDownloadManager.list;

import android.content.Context;
import android.text.TextUtils;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.URI;
import java.net.URISyntaxException;

import download.lib.DownloadCallback;
import download.lib.R;
import download.lib.VideoDownloadManager;

public class DialogUtils {

    public static void showFormatDialog(Context context, String url,  DownloadCallback callback) {
        url = url.trim();

        if (TextUtils.isEmpty(url)) {
            ToastUtil.showCustomToast(context, context.getString(R.string.error_invalid_url), false);
            return;
        }

        boolean isIn = list.stream().anyMatch(url.toLowerCase()::contains);
        if (!isIn) {
            ToastUtil.showCustomToast(context, extractDomain(url).toUpperCase() + " Is Not Supported", false);
            return;
        }

        String[] formats = {
                context.getString(R.string.format_video),
                context.getString(R.string.format_audio)
        };

        String finalUrl = url;
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.select_format)
                .setSingleChoiceItems(formats, VideoDownloadManager.DownloadConfig.isVideoFormat ? 0 : 1, null)
                .setPositiveButton(R.string.download, (dialogInterface, which) -> {
                    AlertDialog alertDialog = (AlertDialog) dialogInterface;
                    int selectedPosition = alertDialog.getListView().getCheckedItemPosition();
                    boolean isVideoFormat = (selectedPosition == 0);
                    VideoDownloadManager.DownloadConfig.isVideoFormat = isVideoFormat;

                    // Call the callback method to handle the download
                    if (callback != null) {
                        callback.onDownloadRequested(finalUrl, isVideoFormat);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
    }

    public static String extractDomain(String url) {
        try {
            // Parse the URL
            URI uri = new URI(url);
            String host = uri.getHost();

            // Remove the top-level domain (e.g., .com, .org)
            if (host != null) {
                // Split the host by dots and take the second-to-last part
                String[] parts = host.split("\\.");
                if (parts.length > 1) {
                    return parts[parts.length - 2];
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return null; // Return null if extraction fails
    }
}

