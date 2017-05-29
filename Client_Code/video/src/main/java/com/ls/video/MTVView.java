package com.ls.video;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
        TextureView.SurfaceTextureListener,MediaPlayer.OnInfoListener {

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
                    //播中监测，每隔一段事件发送一个TIME_MSG事件，通知播放器播放到了那里，执行一次进度更新；
                    if (isPlaying()) {
                        //还可以在这里更新progressbar
                        listener.onBufferUpdate(getCurrentPosition());
                        sendEmptyMessageDelayed(TIME_MSG, TIME_INVAL);
                    }
                    break;
            }
        }
    };

    /**
     * @param context
     * @param parentContainer video的父容器
     */
    public MTVView(Context context,ViewGroup parentContainer) {
        super(context);
        mParentContainer=parentContainer;
        audioManager= (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        initData();
        initView();
        registerBroadcastReceiver();
    }

    private void initData() {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        //宽度=屏幕的宽度
        mScreenWidth = dm.widthPixels;
        //高度是宽度的9/16
        mDestationHeight = (int) (mScreenWidth * VideoConstant.VIDEO_HEIGHT_PERCENT);
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        mPlayerView = (RelativeLayout) inflater.inflate(R.layout.video_player, this);
        mVideoView = (TextureView) mPlayerView.findViewById(R.id.xadsdk_player_video_textureView);
        mVideoView.setOnClickListener(this);
        mVideoView.setKeepScreenOn(true); //设置屏幕常亮
        mVideoView.setSurfaceTextureListener(this);
        initSmallLayoutMode(); //init the small mode
    }

    // 小模式状态
    private void initSmallLayoutMode() {
        LayoutParams params = new LayoutParams(mScreenWidth, mDestationHeight);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        mPlayerView.setLayoutParams(params);

        mMiniPlayBtn = (Button) mPlayerView.findViewById(R.id.xadsdk_small_play_btn);
        mFullBtn = (ImageView) mPlayerView.findViewById(R.id.xadsdk_to_full_view);
        mLoadingBar = (ImageView) mPlayerView.findViewById(R.id.loading_bar);
        mFrameView = (ImageView) mPlayerView.findViewById(R.id.framing_view);
        mMiniPlayBtn.setOnClickListener(this);
        mFullBtn.setOnClickListener(this);
    }

    /**
     * view显隐状态改变监测
     * @param changedView
     * @param visibility
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true; //点击播放器时消耗事件，防止于外界产生事件冲突
    }

    @Override
    public void onClick(View v) {

    }

    //MediaPlayer生命周期相关回调方法--begin----------------------------

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


    /**
     * 标明TextureView进入就绪状态
     * @param surface
     * @param width
     * @param height
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

    //MediaPlayer.OnInfoListener
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    //MediaPlayer生命周期相关回调方法--end----------------------------


    //播放器的功能方法--begin----------------------------

    //加载视频的url
    public void load() {

    }

    //播放暂停
    public void pause() {

    }

    /**
     * 恢复继续播放
     * > 就绪状态->播放状态；
     * > pause->播放状态；
     */
    public void resume() {

    }

    //播放完成后回到初始状态
    public void playBack() {

    }

    //播放停止
    public void stop() {

    }

    //销毁我们当前自定义的videoView
    public void destroy() {

    }

    //跳到指定点播放视频
    public void seekAndResume(int position) {

    }

    //跳到指定点暂停视频
    public void seekAndPause(int position) {

    }

    //通知外界播放器产生的一些事件
    public void setListener(MTVPlayerListener listener) {
        this.listener = listener;
    }

    //播放器的功能方法--end----------------------------

    //播放器的辅助方法--begin----------------------------

    private synchronized void checkMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = createMediaPlayer(); //每次都重新创建一个新的播放器
        }
    }

    //创建MediaPlayer
    private MediaPlayer createMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.reset();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if (videoSurface != null && videoSurface.isValid()) {
            mediaPlayer.setSurface(videoSurface);
        } else {
            stop();
        }
        return mediaPlayer;
    }

    //屏幕事件广播
    private void registerBroadcastReceiver() {
        if (mScreenReceiver == null) {
            mScreenReceiver = new ScreenEventReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            getContext().registerReceiver(mScreenReceiver, filter);
        }
    }

    private void unRegisterBroadcastReceiver() {
        if (mScreenReceiver != null) {
            getContext().unregisterReceiver(mScreenReceiver);
        }
    }

    private void decideCanPlay() {
    }

    public void isShowFullBtn(boolean isShow) {
        mFullBtn.setImageResource(isShow ? R.mipmap.video_ad_mini : R.mipmap.video_ad_mini_null);
        mFullBtn.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    public boolean isRealPause() {
        return mIsRealPause;
    }

    public boolean isComplete() {
        return mIsComplete;
    }

    private void showPauseView(boolean show) {
    }

    private void showLoadingView() {
    }

    private void showPlayView() {
        mLoadingBar.clearAnimation();
        mLoadingBar.setVisibility(View.GONE);
        mMiniPlayBtn.setVisibility(View.GONE);
        mFrameView.setVisibility(View.GONE);
    }

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

    //播放器的辅助方法--end----------------------------

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
