package com.ls.video;

import android.databinding.DataBindingUtil;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.ls.video.databinding.ActivityMainBinding;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding mBinding;
    private static final String path="http://fairee.vicp.net:83/2016rm/0116/baishi160116.mp4";
    private SurfaceHolder svHolder;
    private MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding= DataBindingUtil.setContentView(this,R.layout.activity_main);
        test();
    }

    private void test() {
        try {
            mp = new MediaPlayer();
            mp.setLooping(true);
            mp.setDataSource(path);
            mp.prepareAsync();
            //监听是否准备好
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });

            svHolder = mBinding.sv.getHolder();
            svHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mp.setDisplay(svHolder);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {

                }
            });

            mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {

                }
            });

//            mBinding.tsv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//                @Override
//                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//
//                }
//
//                @Override
//                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//
//                }
//
//                @Override
//                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//                    return false;
//                }
//
//                @Override
//                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//                }
//            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
