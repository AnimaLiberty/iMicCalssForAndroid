package cn.lemon.whiteboard.module.account;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import cn.alien95.util.Utils;
import cn.lemon.common.base.ToolbarActivity;
import cn.lemon.whiteboard.R;

public class VideoPlayActivity extends ToolbarActivity {

    private VideoView mVideoView;
    private String videoPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        useStatusPages(false);
        setContentView(R.layout.account_activity_video_play);

        videoPath = getIntent().getStringExtra("videoPath");

        mVideoView = (VideoView) findViewById(R.id.video_play);
        setupVideo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.video, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.upload:
                if (TextUtils.isEmpty(Utils.getLoginName())) {
                    Toast.makeText(this, "试用用户不支持视频上传", Toast.LENGTH_SHORT).show();
                    return true;
                }
                Intent intent = new Intent(this, UploadVideoActivity.class);
                intent.putExtra("videoPath", videoPath);
                startActivity(intent);
                finish();
                break;
        }
        return true;
    }

    private void setupVideo() {
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mVideoView.start();
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlaybackVideo();
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                stopPlaybackVideo();
                return true;
            }
        });
        try {
            mVideoView.setVideoPath(videoPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mVideoView.isPlaying()) {
            mVideoView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView.canPause()) {
            mVideoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlaybackVideo();
    }

    private void stopPlaybackVideo() {
        try {
            mVideoView.stopPlayback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
