package com.ls.video;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * MTVView(MediaPlayer TextureView Video View)
 * Created by liusong on 2017/5/22.
 */

public class MTVView extends RelativeLayout implements View.OnClickListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener,
        TextureView.SurfaceTextureListener {

    /**
     * Constant(常量)
     */
    private static final String TAG = "MTVView";
    private static final int TIME_MSG = 0x01; //事件类型，在handleMessage中处理；
    private static final int TIME_INVAL = 1000; //时间间隔，每隔一秒发一个TIME_MSG类型消息，执行一次handleMessage；
    //播放器的生命周期状态
    private static final int STATE_ERROR = -1; //错误状态；
    private static final int STATE_IDLE = 0; //空闲状态；
    private static final int STATE_PLAYING = 1; //正在播放；
    private static final int STATE_PAUSING = 2; //暂停状态；
    //如果加载失败，加载重试次数
    private static final int LOAD_TOTAL_COUNT = 3;

    /**
     * UI(布局需要的ui控件)
     */
    private ViewGroup mParentContainer; //MTVView需要添加到的父控件；
    private RelativeLayout mPlayerView; //当前布局；
    private TextureView mVideoView; //显示帧数据的view
    //控制按钮
    private Button mMiniPlayBtn;
    private ImageView mFullBtn;
    private ImageView mLoadingBar;
    private ImageView mFrameView;

    //音频管理器，控制音量和铃声
    private AudioManager audioManager;
    //从TextureView中获取到的，最终真正显示帧数据的类
    private Surface videoSurface;

    /**
     * Data
     */
    private String mUrl; //视频资源地址
    private String mFrameURI;
    private boolean isMute; //是否静音
    private int mScreenWidth; //屏幕的宽度；
    private int mDestationHeight; //默认的高度,按照一定比例算出来的高度;

    /**
     * Status状态保护
     */
    private boolean canPlay = true;
    private boolean mIsRealPause; //是否真正暂停了；
    private boolean mIsComplete; //是否播放完成；
    private int mCurrentCount;
    private int playerState = STATE_IDLE; //播放状态，默认IDLE空闲状态;

    private MediaPlayer mediaPlayer; //播放核心类；
    private MTVPlayerListener listener; //事件监听回调，主要提供给外界；
    private ScreenEventReceiver mScreenReceiver; //监听锁屏事件的广播接收器;

    //主线程handler
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TIME_MSG:
                    //播中监测，每隔一段事件发送一个TIME_MSG事件，执行一次进度更新；
                    if (isPlaying()) {
                        //还可以在这里更新progressbar
                        //LogUtils.i(TAG, "TIME_MSG");
                        listener.onBufferUpdate(getCurrentPosition());
                        sendEmptyMessageDelayed(TIME_MSG, TIME_INVAL);
                    }
                    break;
            }
        }
    };

    /**
     * 获得当前播放位置
     * @return
     */
    public int getCurrentPosition() {
        if (this.mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    /**
     * 播放器是否正在播放
     * @return
     */
    public boolean isPlaying() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            return true;
        }
        return false;
    }

    public MTVView(Context context) {
        super(context);
    }

    /**
     * MediaPlayer.OnBufferingUpdateListener
     * 视频分段缓存，这里监听每次缓存的更新回调
     * @param mp
     * @param percent
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    /**
     * MediaPlayer.OnCompletionListener
     * 监听播放完成回调
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    /**
     * MediaPlayer.OnErrorListener
     * 出错回调
     * @param mp
     * @param what
     * @param extra
     * @return
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }


    /**
     * MediaPlayer.OnPreparedListener
     * 由初始化进入准备完成状态
     * @param mp
     */
    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    //SurfaceTextureListener--begin---------------
    /**
     * TextureView.SurfaceTextureListener
     * 播放帧数据画面监听
     */

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    //SurfaceTextureListener--end---------------

    @Override
    public void onClick(View v) {

    }

    /**
     * 监听锁屏事件的广播接收器
     */
    private class ScreenEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //主动锁屏时 pause, 主动解锁屏幕时，resume
            switch (intent.getAction()) {
                case Intent.ACTION_USER_PRESENT:
                    if (playerState == STATE_PAUSING) {
                        if (mIsRealPause) {
                            //手动点的暂停，回来后还暂停
//                            pause();
                        } else {
//                            decideCanPlay();
                        }
                    }
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    if (playerState == STATE_PLAYING) {
//                        pause();
                    }
                    break;
            }
        }
    }

    /**
     * 事件监听回调，主要提供给外界处理
     *
     * 供slot层来实现具体点击逻辑,具体逻辑还会变，
     * 如果对UI的点击没有具体监测的话可以不回调
     */
    public interface MTVPlayerListener {

        void onBufferUpdate(int time);

        void onClickFullScreenBtn();

        void onClickVideo();

        void onClickBackBtn();

        void onClickPlay();

        void onMTVLoadSuccess();

        void onMTVLoadFailed();

        void onMTVLoadComplete();
    }
}
