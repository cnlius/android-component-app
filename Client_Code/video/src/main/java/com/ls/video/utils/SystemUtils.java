package com.ls.video.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import static android.content.Context.AUDIO_SERVICE;

/**
 * 系统相关的属性
 * 1> 亮度相关；
 * 2> 音量相关；
 * Created by liusong on 2018/1/23.
 */

public class SystemUtils {

    //--voice--start--------------------------------------

    /**
     * 调高音量
     */
    public static void upVolume(Context context) {
        adjustVolume(context, 1);
    }

    /**
     * 调低音量
     */
    public static void downVolume(Context context) {
        adjustVolume(context, -1);
    }

    /**
     * 调节音量，使用adjustStreamVolume(平滑，推荐)
     *
     * @param volume
     */
    public static void adjustVolume(Context context, int volume) {
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        //当前音量
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); //音乐媒体的音量
        Log.i("volume", "当前音量=" + curVolume);
        //最大音量
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.i("volume", "最大音量=" + maxVolume);
        //调节音量（adjustStreamVolume：平滑的增减；setStreamVolume：会有卡顿）
        if (volume > 0) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
        } else {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
        }
    }

    /**
     * 设置音量,通过setStreamVolume(会卡顿一下)
     *
     * @param isBigger 是否是增加音量
     */
    public static void changeVolume(Context context, boolean isBigger) {
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        //当前音量
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); //音乐媒体的音量
        Log.i("volume", "当前音量=" + curVolume);
        //最大音量
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.i("volume", "最大音量=" + maxVolume);
        //调节音量（adjustStreamVolume：平滑的增减；setStreamVolume：会有卡顿）
        if(isBigger){
            curVolume++;
        }else{
            curVolume--;
        }

        if(curVolume>maxVolume){
            curVolume=maxVolume;
        }else if(curVolume<0){
            curVolume=0;
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume, AudioManager.FLAG_PLAY_SOUND);
    }
    //--voice--end----------------------------------------


    //--brightness--start----------------------------------------

    /* require android.permission.WRITE_SETTINGS */

    public static final int BRIGHTNESS_STEP=25;

    /**
     * 设置亮度调节手动模式
     *
     * @param context
     * @overview 如果需要实现亮度调节，首先需要设置屏幕亮度调节模式为手动模式。
     * @brightness_mode: 亮度调节模式
     * Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC：值为1，自动调节亮度。
     * Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL：值为0，手动模式。
     */
    public static void setScreenBrightnessMode(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            int mode = Settings.System.getInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE
            );
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(
                        contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                );
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取系统的屏幕亮度的值
     *
     * @return
     * @attention:
     * 1> 屏幕最大亮度为255。
     * 2> 屏幕最低亮度为0。
     * 3> 屏幕亮度值范围必须位于：0～255。
     */
    public static int getScreenBrightness(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        int defVal = 125; //默认值
        return Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, defVal);
    }

    /**
     * 亮度应用到全局(屏幕最大亮度为255)
     *
     * @require android.permission.WRITE_SETTINGS
     * @param context
     */
    public static void applyBrightness2Global(Context context, int brightness) {
        setScreenBrightnessMode(context);
        ContentResolver contentResolver = context.getContentResolver();
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
    }

    /**
     * 设置窗口亮度
     *
     * 1> lp = 0 全暗 ，
     * 2> lp= -1,根据系统设置，
     * 3> lp = 1; 最亮
     * @param brightness
     */
    public static void setBrightness(Activity activity, float brightness) {
        Log.i("brightness","brightness="+brightness);
        setScreenBrightnessMode(activity);
        WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();

        //屏幕最大亮度为255。
        if (brightness > 255) {
            brightness = 255;
        } else if (brightness < 51) {
            brightness = 51; //阀值
        }

        layoutParams.screenBrightness = brightness / 255f;
        activity.getWindow().setAttributes(layoutParams);
    }

    //--brightness--end----------------------------------------
}
