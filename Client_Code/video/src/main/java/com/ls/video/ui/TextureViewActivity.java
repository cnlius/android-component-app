package com.ls.video.ui;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.ls.video.R;
import com.ls.video.databinding.ActivityTextureViewBinding;

/**
 * Created by liusong on 2018/1/23.
 */

public class TextureViewActivity extends AppCompatActivity {

    private ActivityTextureViewBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_texture_view);
    }

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
}
