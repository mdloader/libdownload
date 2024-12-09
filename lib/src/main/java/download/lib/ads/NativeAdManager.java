package download.lib.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

import download.lib.R;


public class NativeAdManager {

    private static final String TAG = "NativeAdManager";
    private final Context context;
    private NativeAd currentNativeAd;
    private AdLoader adLoader;
    private final String adUnitId;

    public NativeAdManager(Context context, String adUnitId, ViewGroup parentView  ) {
        this.context = context;
        this.adUnitId = adUnitId;
        setupAdLoader(  parentView  );
    }

    private void setupAdLoader(ViewGroup parentView  ) {
        adLoader = new AdLoader.Builder(context, adUnitId)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        // Ensure any previous ad is destroyed before replacing it
                        if (currentNativeAd != null) {
                            currentNativeAd.destroy();
                        }

                        currentNativeAd = nativeAd;
                        Log.d(TAG, "Native ad loaded successfully.");
                        // Notify the UI or other components to display the ad.
                        displayAd(nativeAd, parentView  );
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e(TAG, "Ad failed to load: " + adError.getMessage());
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Configure native ad options if needed
                        .build())
                .build();
    }

    public void loadAd() {
        if (adLoader != null) {
            adLoader.loadAd(new AdRequest.Builder().build());
            Log.d(TAG, "Ad request sent.");
        } else {
            Log.e(TAG, "AdLoader is not initialized.");
        }
    }

    public void loadAds(int count) {
        if (adLoader != null) {
            adLoader.loadAds(new AdRequest.Builder().build(), count);
            Log.d(TAG, "Multiple ad requests sent.");
        } else {
            Log.e(TAG, "AdLoader is not initialized.");
        }
    }

    public void destroyAds() {
        if (currentNativeAd != null) {
            currentNativeAd.destroy();
            currentNativeAd = null;
            Log.d(TAG, "Native ad destroyed.");
        }
    }

    private void displayAd(NativeAd nativeAd,ViewGroup parentView  ) {
        // Inflate the NativeAdView layout
        NativeAdView adView = (NativeAdView) View.inflate(context, R.layout.native_ad_layout, null);


// Make sure views are correctly initialized
        TextView headlineView = adView.findViewById(R.id.ad_headline);
        TextView bodyView = adView.findViewById(R.id.ad_body);
        ImageView iconView = adView.findViewById(R.id.ad_app_icon);
        Button ctaButton = adView.findViewById(R.id.ad_call_to_action);
        TextView advertiserView = adView.findViewById(R.id.ad_advertiser);
        MediaView mediaView = adView.findViewById(R.id.ad_media);

// Assign views to ad content
        adView.setHeadlineView(headlineView);
        headlineView.setText(nativeAd.getHeadline());

        if (nativeAd.getBody() != null) {
            bodyView.setVisibility(View.VISIBLE);
            bodyView.setText(nativeAd.getBody());
            adView.setBodyView(bodyView);
        } else {
            bodyView.setVisibility(View.GONE);
        }

        if (nativeAd.getIcon() != null) {
            iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.setIconView(iconView);
        } else {
            iconView.setVisibility(View.GONE);
        }

        if (nativeAd.getCallToAction() != null) {
            ctaButton.setText(nativeAd.getCallToAction());
            adView.setCallToActionView(ctaButton);
        } else {
            ctaButton.setVisibility(View.GONE);
        }

        if (nativeAd.getAdvertiser() != null) {
            advertiserView.setText(nativeAd.getAdvertiser());
            adView.setAdvertiserView(advertiserView);
        } else {
            advertiserView.setVisibility(View.GONE);
        }

        if (nativeAd.getMediaContent() != null) {
            mediaView.setMediaContent(nativeAd.getMediaContent());
            adView.setMediaView(mediaView);
        }

        // Add the NativeAdView to your app's layout

        parentView.removeAllViews(); // Clear previous ads if any
        parentView.addView(adView);
    }


    // Call this method in the onDestroy() of the activity or fragment to release resources
    public void release() {
        destroyAds();
    }
}