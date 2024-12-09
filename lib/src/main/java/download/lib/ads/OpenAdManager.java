package download.lib.ads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;



public class OpenAdManager implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private static final String TAG = "OpenAdManager";
    private AppOpenAd appOpenAd;
    private final Context context;
    private boolean isLoading = false;
    private boolean isLoaded = false;
    private boolean isShowingAd = false;
    private long lastShowTime = 0;
    private Activity currentActivity;

    // Use test ad unit ID for debug builds
    private static   String openAdId  ; // Replace with your production ad unit ID

    public OpenAdManager(Context context,String openAdId) {
        this.context = context;
        this.openAdId = openAdId;
        loadAd();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isShowingAd) {
            showAdIfAvailable();
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {
        if (!isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }

    public void loadAd() {
        // Check if ad is already loaded or loading
        if (isLoaded || isLoading) {
            Log.d(TAG, "Ad is already " + (isLoaded ? "loaded" : "loading"));
            return;
        }

        isLoading = true;
        AppOpenAd.load(context, openAdId, AdManager.getAdRequest(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        Log.d(TAG, "App open ad loaded successfully");
                        appOpenAd = ad;
                        isLoading = false;
                        isLoaded = true;
                        setupFullScreenCallback();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Failed to load app open ad: " + loadAdError.getMessage());
                        appOpenAd = null;
                        isLoading = false;
                        isLoaded = false;
                        retryLoadAd();
                    }
                });
    }

    private void setupFullScreenCallback() {
        if (appOpenAd == null) return;

        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                appOpenAd = null;
                isLoaded = false;
                isShowingAd = false;
                loadAd(); // Preload next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Failed to show app open ad: " + adError.getMessage());
                appOpenAd = null;
                isLoaded = false;
                isShowingAd = false;
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

    public void showAdIfAvailable() {
        if (!isAdAvailable() || currentActivity == null) {
            Log.d(TAG, "App open ad not available or no activity");
            loadAd(); // Try to load for next time
            return;
        }

        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                appOpenAd = null;
                isLoaded = false;
                isShowingAd = false;
                loadAd(); // Preload next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Failed to show app open ad: " + adError.getMessage());
                appOpenAd = null;
                isLoaded = false;
                isShowingAd = false;
                loadAd(); // Retry loading
            }

            @Override
            public void onAdShowedFullScreenContent() {
                lastShowTime = System.currentTimeMillis();
                isShowingAd = true;
            }
        });

        isShowingAd = true;
        appOpenAd.show(currentActivity);
    }

    public boolean isAdAvailable() {
        return isLoaded && appOpenAd != null && !isShowingAd &&
                (System.currentTimeMillis() - lastShowTime) >= AdManager.MIN_APP_OPEN_INTERVAL;
    }
}