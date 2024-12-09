# Add project specific ProGuard rules here.

# AdMob
-keep public class com.google.android.gms.ads.** {
    public *;
}

-keep public class com.google.ads.** {
    public *;
}

# Keep required classes for ads
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# Keep the classes used by reflection
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ExoPlayer rules
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Glide rules
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# Lottie rules
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** {*;}
