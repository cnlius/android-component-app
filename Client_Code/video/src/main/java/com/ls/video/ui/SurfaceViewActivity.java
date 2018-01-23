package com.ls.video.ui;

import android.databinding.DataBindingUtil;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.RelativeLayout;

import com.ls.video.R;
import com.ls.video.databinding.ActivitySurfaceViewBinding;
import com.ls.video.utils.MathUtils;
import com.ls.video.utils.SystemUtils;

import java.io.IOException;

/**
 * MediaPlayer+SurfaceView
 * Created by liusong on 2018/1/23.
 */

public class SurfaceViewActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = SurfaceViewActivity.class.getSimpleName();
    private static final String mediaUrl = "http://p2ybqeiuz.bkt.clouddn.com/douyin_dance_01.MP4";
    private ActivitySurfaceViewBinding mBinding;
    private MediaPlayer mediaPlayer;
    private SurfaceHolder surfaceHolder; //显示帧数据
    private int brightness; //窗口亮度，默认设置为系统的

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_surface_view);
        initData();
        testPlayer();
    }

    private void initData() {
        brightness = SystemUtils.getScreenBrightness(this);
    }

    private void testPlayer() {
        mBinding.progressBar.setVisibility(View.VISIBLE);
        //初始化MediaPlayer实例，处于Idle（空闲）状态
        mediaPlayer = new MediaPlayer();
        //重置设为IDLE空闲状态
        mediaPlayer.reset();
        //将player设置为循环或非循环。
        mediaPlayer.setLooping(false);
        setupListener();

        surfaceHolder = mBinding.sv.getHolder();
        //设置Holder类型,该类型表示surfaceView自己不管理缓存区,避免视频播放时，出现有声音没图像的问题;
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        /*SURFACE_TYPE_NORMAL：用RAM缓存原生数据的普通Surface
         *SURFACE_TYPE_HARDWARE：适用于DMA(Direct memory access )引擎和硬件加速的Surface
         *SURFACE_TYPE_GPU：适用于GPU加速的Surface
         *SURFACE_TYPE_PUSH_BUFFERS：表明该Surface不包含原生数据，Surface用到的数据由其他对象提供，
         *在Camera图像预览中就使用该类型的Surface，有Camera负责提供给预览Surface数据，这样图像预览会比较流畅。
         *如果设置这种类型则就不能调用lockCanvas来获取Canvas对象了。
         */
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.i(TAG, "surfaceCreated");
                //设置帧数据显示方式
                mediaPlayer.setDisplay(surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "surfaceChanged");
                //SurfaceView的大小改变
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.i(TAG, "surfaceDestroyed");
                // surfaceView销毁
                // 如果MediaPlayer没被销毁，则销毁mediaPlayer
                if (null != mediaPlayer) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        });

        //设置surfaceView保持在屏幕上，保持屏幕长亮。
        mediaPlayer.setScreenOnWhilePlaying(true);
        surfaceHolder.setKeepScreenOn(true);

        try {
            //设置播放源
            mediaPlayer.setDataSource(mediaUrl);
            //异步准备播放
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * mediaPlayer播放操作监听
     */
    private void setupListener() {
        //错误发生时回调
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.i(TAG, "onError");
                return true;
            }
        });

        //准备完成回调
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "onPrepared");
                mBinding.progressBar.setVisibility(View.GONE);
                //--------------------------------------
//                mediaPlayer.getDuration(); //视频的总时间
//                mediaPlayer.getCurrentPosition(); //当前的播放位置
//                mediaPlayer.seekTo(progress); //播放指定位置
                resetSvSizeByRealVideoSize();
                //--------------------------------------
                //开始播放
                mp.start();
            }
        });

        //视频的缓存回调（视频是分段缓存的，每缓存一段都会调用更新方法,此方法会一直回调）
        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.i(TAG, "onBufferingUpdate，percent=" + percent);
            }
        });

        //播放进度定位完成回调
        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                Log.i(TAG, "onSeekComplete");
            }
        });

        //播放完成回调
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.i(TAG, "onCompletion");
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play_pause:
                playOrPause();
                break;
            case R.id.btn_voice_up:
                SystemUtils.upVolume(this);
//                SystemUtils.changeVolume(this,true);
                break;
            case R.id.btn_voice_down:
                SystemUtils.downVolume(this);
//                SystemUtils.changeVolume(this,false);
                break;
            case R.id.btn_brightness_up:
                brightness += SystemUtils.BRIGHTNESS_STEP;
                SystemUtils.setBrightness(this, brightness);
                break;
            case R.id.btn_brightness_down:
                brightness -= SystemUtils.BRIGHTNESS_STEP;
                SystemUtils.setBrightness(this, brightness);
                break;
            default:
                break;
        }
    }

    /**
     * 播放或者暂停播放
     */
    private void playOrPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release(); //释放资源
            mediaPlayer = null;
        }
    }

    /**
     * 根据视频的真实大小重置surfaceView的大小
     */
    private void resetSvSizeByRealVideoSize() {
        //获取surfaceView的实际宽高
        int svHeight = mBinding.sv.getMeasuredHeight();
        int svWidth = mBinding.sv.getMeasuredWidth();
        //获取视频的原始宽度和高度
        int width = mediaPlayer.getVideoWidth();
        int height = mediaPlayer.getVideoHeight();
        Log.i(TAG, "onPrepared，video width=" + width + ",height=" + height);

        if (width > svWidth || height > svHeight) {
            //计算出宽高的倍数
            float wScale = MathUtils.division((float) width, (float) svWidth, 2);
            float hScale = MathUtils.division((float) height, (float) svHeight, 2);
            // 获取最大的倍数值，按大数值进行缩放
            float maxScale = Math.max(wScale, hScale);
            //获取接近的最大正数如：Math.ceil(11.1)=12
            width = (int) Math.ceil(width / maxScale);
            height = (int) Math.ceil(height / maxScale);
        }
        //设置SurfaceView的大小并居中显示
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mBinding.sv.setLayoutParams(layoutParams);
    }

}
