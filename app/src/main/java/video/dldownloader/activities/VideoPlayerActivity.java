package video.dldownloader.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import video.dldownloader.databinding.ActivityVideoPlayerBinding;
import java.io.File;

public class VideoPlayerActivity extends AppCompatActivity {
    private ActivityVideoPlayerBinding binding;
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String videoPath = getIntent().getStringExtra("video_path");
        if (videoPath != null) {
            initializePlayer(videoPath);
        }
    }

    private void initializePlayer(String videoPath) {
        player = new ExoPlayer.Builder(this).build();
        binding.playerView.setPlayer(player);

        File videoFile = new File(videoPath);
        Uri videoUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider", videoFile);

        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }
}
