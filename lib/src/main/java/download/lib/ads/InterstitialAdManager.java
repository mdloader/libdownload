package download.lib.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;


public class InterstitialAdManager {
    private static final String TAG = "InterstitialAdManager";
    private InterstitialAd interstitialAd;
    private final Context context;
    private boolean isLoading = false;
    private boolean isLoaded = false;
    private long lastShowTime = 0;

    // Use test ad unit ID for debug builds
    private static   String interstitialAdId  ; // Replace with your production ad unit ID

    public InterstitialAdManager(Context context,String interstitialAdId ) {
        this.context = context;
        this.interstitialAdId = interstitialAdId;
        loadAd();
    }

    public void loadAd() {
        // Check if ad is already loaded or loading
        if (isLoaded || isLoading) {
            Log.d(TAG, "Ad is already " + (isLoaded ? "loaded" : "loading"));
            return;
        }

        isLoading = true;
        InterstitialAd.load(context, interstitialAdId, AdManager.getAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        Log.d(TAG, "Interstitial ad loaded successfully");
                        interstitialAd = ad;
                        isLoading = false;
                        isLoaded = true;
                        setupFullScreenCallback();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Failed to load interstitial ad: " + loadAdError.getMessage());
                        interstitialAd = null;
                        isLoading = false;
                        isLoaded = false;
                        retryLoadAd();
                    }
                });
    }

    private void setupFullScreenCallback() {
        if (interstitialAd == null) return;

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                isLoaded = false;
                loadAd(); // Preload next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Failed to show interstitial ad: " + adError.getMessage());
                interstitialAd = null;
                isLoaded = false;
                loadAd(); // Retry loading
            }

            @Override
            public void onAdShowedFullScreenContent() {
                lastShowTime = System.currentTimeMillis();
            }
        });
    }

    private void retryLoadAd() {
        // Implement exponential backoff for retries
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (!isLoaded && !isLoading) {
                loadAd();
            }
        }, 60000); // Retry after 1 minute
    }

    public void showAd(Activity activity, AdManager.OnAdDismissedListener listener) {
        if (!isAdAvailable()) {
            Log.d(TAG, "Interstitial ad not available");
            if (listener != null) {
                listener.onAdDismissed();
            }
            loadAd(); // Try to load for next time
            return;
        }

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                isLoaded = false;
                if (listener != null) {
                    listener.onAdDismissed();
                }
                loadAd(); // Preload next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Failed to show interstitial ad: " + adError.getMessage());
                interstitialAd = null;
                isLoaded = false;
                if (listener != null) {
                    listener.onAdDismissed();
                }
                loadAd(); // Retry loading
            }

            @Override
            public void onAdShowedFullScreenContent() {
                lastShowTime = System.currentTimeMillis();
            }
        });

        interstitialAd.show(activity);
    }

    public boolean isAdAvailable() {
        return isLoaded && interstitialAd != null &&
                (System.currentTimeMillis() - lastShowTime) >= AdManager.MIN_INTERSTITIAL_INTERVAL;
    }
}
