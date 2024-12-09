package download.lib.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;


public class RewardedAdManager {
    private static final String TAG = "RewardedAdManager";
    private RewardedAd rewardedAd;
    private final Context context;
    private boolean isLoading = false;
    private boolean isLoaded = false;
    private long lastShowTime = 0;

    // Use test ad unit ID for debug builds
    private   String rewardedAdId; // Replace with your production ad unit ID

    public RewardedAdManager(Context context,String rewardedAdId) {
        this.context = context;
        this.rewardedAdId = rewardedAdId;
        loadAd();
    }

    public void loadAd() {
        // Check if ad is already loaded or loading
        if (isLoaded || isLoading) {
            Log.d(TAG, "Ad is already " + (isLoaded ? "loaded" : "loading"));
            return;
        }

        isLoading = true;
        RewardedAd.load(context, rewardedAdId, AdManager.getAdRequest(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        Log.d(TAG, "Rewarded ad loaded successfully");
                        rewardedAd = ad;
                        isLoading = false;
                        isLoaded = true;
                        setupFullScreenCallback();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Failed to load rewarded ad: " + loadAdError.getMessage());
                        rewardedAd = null;
                        isLoading = false;
                        isLoaded = false;
                        retryLoadAd();
                    }
                });
    }

    private void setupFullScreenCallback() {
        if (rewardedAd == null) return;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                isLoaded = false;
                loadAd(); // Preload next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Failed to show rewarded ad: " + adError.getMessage());
                rewardedAd = null;
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

    public void showAd(Activity activity, OnRewardedAdCallback callback) {
        if (!isAdAvailable()) {
            Log.d(TAG, "Rewarded ad not available");
            if (callback != null) {
                callback.onRewardedAdFailedToShow();
            }
            loadAd(); // Try to load for next time
            return;
        }

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                isLoaded = false;
                if (callback != null) {
                    callback.onRewardedAdClosed();
                }
                loadAd(); // Preload next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Failed to show rewarded ad: " + adError.getMessage());
                rewardedAd = null;
                isLoaded = false;
                if (callback != null) {
                    callback.onRewardedAdFailedToShow();
                }
                loadAd(); // Retry loading
            }

            @Override
            public void onAdShowedFullScreenContent() {
                lastShowTime = System.currentTimeMillis();
            }
        });

        rewardedAd.show(activity, reward -> {
            if (callback != null) {
                callback.onUserEarnedReward(reward);
            }
        });
    }

    public boolean isAdAvailable() {
        return isLoaded && rewardedAd != null &&
                (System.currentTimeMillis() - lastShowTime) >= AdManager.MIN_REWARDED_INTERVAL;
    }

    public interface OnRewardedAdCallback {
        void onUserEarnedReward(com.google.android.gms.ads.rewarded.RewardItem reward);

        void onRewardedAdClosed();

        void onRewardedAdFailedToShow();
    }
}
