package com.frank.living.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import com.frank.living.R;
import com.frank.living.listener.IjkPlayerListener;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import com.frank.living.widget.IjkVideoView;

public class RtspLiveActivity extends AppCompatActivity implements IjkPlayerListener, View.OnClickListener{

    private final static String TAG = RtspLiveActivity.class.getSimpleName();

    private IjkMediaPlayer ijkMediaPlayer;
    private IjkVideoView mVideoView;
    private ImageButton btnPlay;
    private ImageButton btnSound;
    private boolean isPause;
    private boolean isSilence;

    private final static String url = "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        init();

    }

    private void init(){
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        TableLayout mHudView = (TableLayout) findViewById(R.id.hud_view);
        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        mVideoView.setHudView(mHudView);
        mVideoView.setIjkPlayerListener(this);
        mVideoView.setVideoPath(url);
        mVideoView.start();

        btnPlay = (ImageButton) findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(this);
        btnSound = (ImageButton) findViewById(R.id.btn_sound);
        btnSound.setOnClickListener(this);

    }

    private void initOptions(){
        if (ijkMediaPlayer == null)
            return;
        Log.e(TAG, "initOptions");
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fast", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 200);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        //0：代表关闭  1：代表开启
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 2);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 30);
        //input buffer:don't limit the input buffer size (useful with realtime streams)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "nobuffer");
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzedmaxduration", 100);
    }

    @Override
    public void onIjkPlayer(IjkMediaPlayer ijkMediaPlayer) {
        this.ijkMediaPlayer = ijkMediaPlayer;
        initOptions();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_play:
                isPause = !isPause;
                if (isPause){//直播暂停
                    mVideoView.pause();
                    btnPlay.setBackgroundResource(R.drawable.ic_play);
                }else {//直播继续
                    mVideoView.start();
                    btnPlay.setBackgroundResource(R.drawable.ic_pause);
                }
                break;
            case R.id.btn_sound:
                isSilence = !isSilence;
                if (ijkMediaPlayer == null)
                    return;
                if (isSilence){
                    ijkMediaPlayer.setVolume(0, 0);
                    btnSound.setBackgroundResource(R.drawable.ic_sound);
                }else {
                    ijkMediaPlayer.setVolume(50, 50);
                    btnSound.setBackgroundResource(R.drawable.ic_silence);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mVideoView.stopPlayback();
        mVideoView.release(true);
        IjkMediaPlayer.native_profileEnd();
    }

}
