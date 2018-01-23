package com.ls.video.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ls.video.R;
import com.ls.video.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_sv:
                startActivity(new Intent(this,SurfaceViewActivity.class));
                break;
            case R.id.btn_tv:
                startActivity(new Intent(this,TextureViewActivity.class));
                break;
            default:
                break;
        }
    }
}
