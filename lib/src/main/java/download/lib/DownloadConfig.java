package download.lib;


import java.io.File;
import java.util.List;



import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

public class DownloadConfig {
    private final String url;
    private final File outputPath;
    private final VideoQuality quality;
    private final String specificQuality;
    private final DownloadType downloadType;
    private final List<String> subtitleLanguages;
    private final int maxDownloadRetries;
    private final long timeoutSeconds;
    private final boolean useAria2Download;
    public static boolean isVideoFormat = true;

    private DownloadConfig(Builder builder) {
        this.url = builder.url;
        this.outputPath = builder.outputPath;
        this.quality = builder.quality;
        this.specificQuality = builder.specificQuality;
        this.downloadType = builder.downloadType;
        this.subtitleLanguages = builder.subtitleLanguages;
        this.maxDownloadRetries = builder.maxDownloadRetries;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.useAria2Download = builder.useAria2Download;
    }

    public enum VideoQuality {
        BEST,
        WORST,
        SPECIFIC
    }

    public enum DownloadType {
        VIDEO,
        AUDIO_ONLY,
        THUMBNAIL
    }

    public static class Builder {
        private String url;
        private File outputPath;
        private VideoQuality quality = VideoQuality.BEST;
        private String specificQuality;
        private DownloadType downloadType = DownloadType.VIDEO;
        private List<String> subtitleLanguages;
        private int maxDownloadRetries = 3;
        private long timeoutSeconds = 300;
        private boolean useAria2Download = false;

        public Builder(@NonNull String url, @NonNull File outputPath) {
            this.url = url;
            this.outputPath = outputPath;
        }

        public Builder quality(VideoQuality quality) {
            this.quality = quality;
            return this;
        }

        public Builder specificQuality(String quality) {
            this.specificQuality = quality;
            this.quality = VideoQuality.SPECIFIC;
            return this;
        }

        public Builder downloadType(DownloadType type) {
            this.downloadType = type;
            return this;
        }

        public Builder subtitleLanguages(List<String> languages) {
            this.subtitleLanguages = languages;
            return this;
        }

        public Builder maxRetries(int retries) {
            this.maxDownloadRetries = retries;
            return this;
        }

        public Builder timeout(long seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        public Builder useAria2(boolean use) {
            this.useAria2Download = use;
            return this;
        }

        public DownloadConfig build() {
            return new DownloadConfig(this);
        }
    }

    // Getters
    @NonNull
    public String getUrl() {
        return url;
    }

    @NonNull
    public File getOutputPath() {
        return outputPath;
    }

    @NonNull
    public VideoQuality getQuality() {
        return quality;
    }

    @Nullable
    public String getSpecificQuality() {
        return specificQuality;
    }

    @NonNull
    public DownloadType getDownloadType() {
        return downloadType;
    }

    @Nullable
    public List<String> getSubtitleLanguages() {
        return subtitleLanguages;
    }

    public int getMaxDownloadRetries() {
        return maxDownloadRetries;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isUseAria2Download() {
        return useAria2Download;
    }
}
