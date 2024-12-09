package download.lib.ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;


public class BannerAdManager {
    private static final String TAG = "BannerAdManager";
    private AdView adView;
    private boolean isLoading = false;
    private boolean isLoaded = false;
   private Activity activity;
    // Use test ad unit ID for debug builds
    private  String bannerAdId; // Replace with your production ad unit ID

    public BannerAdManager( Activity activity,String bannerAdId) {
        this.bannerAdId = bannerAdId;
        this.activity = activity;
    }

    public void loadAd(Activity activity, int containerId) {
        // Check if ad is already loaded or loading
        if (isLoaded || isLoading) {
            Log.d(TAG, "Ad is already " + (isLoaded ? "loaded" : "loading"));
            return;
        }

        if (adView != null) {
            destroyAd();
        }

        ViewGroup adContainer = activity.findViewById(containerId);
        if (adContainer == null) {
            Log.e(TAG, "Ad container not found");
            return;
        }

        adView = new AdView(activity);
        adView.setAdUnitId(bannerAdId);

        // Set a fixed banner size first
        AdSize adSize = AdSize.BANNER; // Standard 320x50 banner

        // Try to get adaptive size if possible
        try {
            Display display = activity.getWindowManager().getDefaultDisplay();
            DisplayMetrics outMetrics = new DisplayMetrics();
            display.getMetrics(outMetrics);

            float density = outMetrics.density;
            int adWidthPixels = outMetrics.widthPixels;
            int adWidth = (int) (adWidthPixels / density);

            adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
        } catch (Exception e) {
            Log.e(TAG, "Error getting adaptive ad size: " + e.getMessage());
        }

        adView.setAdSize(adSize);

        // Set ad listener
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.e(TAG, "Failed to load banner ad: " + loadAdError.getMessage());
                isLoading = false;
                isLoaded = false;
                retryLoadAd(activity, containerId);
            }

            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully");
                isLoading = false;
                isLoaded = true;
            }

            @Override
            public void onAdClosed() {
                isLoaded = false;
                // Optionally reload the ad
                loadAd(activity, containerId);
            }
        });

        // Add adView to container
        adContainer.removeAllViews();
        adContainer.addView(adView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Set loading state and load ad
        isLoading = true;
        adView.loadAd(AdManager.getAdRequest());
    }

    private void retryLoadAd(Activity activity, int containerId) {
        // Implement exponential backoff for retries
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (!isLoaded && !isLoading) {
                loadAd(activity, containerId);
            }
        }, 60000); // Retry after 1 minute
    }

    public void destroyAd() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        isLoading = false;
        isLoaded = false;
    }

    public void pauseAd() {
        if (adView != null) {
            adView.pause();
        }
    }

    public void resumeAd() {
        if (adView != null) {
            adView.resume();
        }
    }

    public boolean isAdLoaded() {
        return isLoaded;
    }
}
