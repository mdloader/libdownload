package download.lib.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;

public class AdManager {
    private static final String TAG = "AdManager";

    // Minimum intervals between ads (in milliseconds)
    public static final long MIN_INTERSTITIAL_INTERVAL = 60000; // 1 minute
    public static final long MIN_REWARDED_INTERVAL = 60000; // 1 minute
    public static final long MIN_APP_OPEN_INTERVAL = 300000; // 5 minutes

    private final BannerAdManager bannerAdManager;
    private final InterstitialAdManager interstitialAdManager;
    private final RewardedAdManager rewardedAdManager;
    //private final NativeAdManager nativeAdManager;
    private final OpenAdManager openAdManager;

    private static volatile AdManager instance;
    private boolean isInitialized = false;

    private AdManager(Context context,String bannerAdId,String interstitialAdId,String rewardedAdId,String openAdId) {
        bannerAdManager = new BannerAdManager((Activity)context,bannerAdId);
        interstitialAdManager = new InterstitialAdManager(context,interstitialAdId);
        rewardedAdManager = new RewardedAdManager(context,rewardedAdId);
        //   nativeAdManager = new NativeAdManager( );
        openAdManager = new OpenAdManager(context,openAdId);
    }

    public static AdManager getInstance(Context context,String bannerAdId,String interstitialAdId,String rewardedAdId,String openAdId) {
        if (instance == null) {
            synchronized (AdManager.class) {
                if (instance == null) {
                    instance = new AdManager(context,bannerAdId,interstitialAdId,rewardedAdId,openAdId);
                }
            }
        }
        return instance;
    }

    public void initialize(Context context) {
        if (isInitialized) {
            Log.d(TAG, "AdManager already initialized");
            return;
        }

        // Initialize the Mobile Ads SDK on a background thread
        new Thread(() -> {
            try {
                MobileAds.initialize(context, initializationStatus -> {
                    Log.d(TAG, "Mobile Ads SDK initialized successfully");
                    isInitialized = true;
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Mobile Ads SDK: " + e.getMessage());
            }
        }).start();
    }

    public static AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    // Banner Ad Methods
    public void loadBannerAd(Activity activity, int containerId) {
        bannerAdManager.loadAd(activity, containerId);
    }

    public boolean isBannerAdLoaded() {
        return bannerAdManager.isAdLoaded();
    }

    // Interstitial Ad Methods
    public void showInterstitialAd(Activity activity, OnAdDismissedListener listener) {
        if (interstitialAdManager.isAdAvailable()) {
            interstitialAdManager.showAd(activity, listener);
        } else {
            if (listener != null) {
                listener.onAdDismissed();
            }
        }
    }

    public boolean isInterstitialAdAvailable() {
        return interstitialAdManager.isAdAvailable();
    }

    // Rewarded Ad Methods
    public void showRewardedAd(Activity activity, RewardedAdManager.OnRewardedAdCallback callback) {
        if (rewardedAdManager.isAdAvailable()) {
            rewardedAdManager.showAd(activity, callback);
        } else {
            if (callback != null) {
                callback.onRewardedAdFailedToShow();
            }
        }
    }

    public boolean isRewardedAdAvailable() {
        return rewardedAdManager.isAdAvailable();
    }



    // App Open Ad Methods
    public void showAppOpenAd() {
        if (openAdManager.isAdAvailable()) {
            openAdManager.showAdIfAvailable();
        }
    }

    public boolean isAppOpenAdAvailable() {
        return openAdManager.isAdAvailable();
    }

    // Cleanup Methods
    public void destroyBannerAd() {
        bannerAdManager.destroyAd();
    }

    //  public void destroyNativeAd() {  nativeAdManager.destroy();   }

    public void pauseBannerAd() {
        bannerAdManager.pauseAd();
    }

    public void resumeBannerAd() {
        bannerAdManager.resumeAd();
    }

    // Callback Interfaces
    public interface OnAdDismissedListener {
        void onAdDismissed();
    }
}
