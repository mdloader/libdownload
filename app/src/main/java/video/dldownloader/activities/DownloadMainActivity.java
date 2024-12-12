package video.dldownloader.activities;



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
import video.dldownloader.R;
import video.dldownloader.databinding.ActivityMainBinding;

public class DownloadMainActivity extends AppCompatActivity  {
    private static final int PERMISSION_REQUEST_CODE = 1234;
    private ActivityMainBinding binding;

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

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());




        // Load a single native ad
        nativeAdManager.loadAd();


        // Initialize video download manager



        checkPermissions();


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






    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
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
