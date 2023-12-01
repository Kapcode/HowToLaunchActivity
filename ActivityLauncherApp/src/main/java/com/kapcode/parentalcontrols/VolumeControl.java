package com.kapcode.parentalcontrols;

import android.content.Context;
import android.media.AudioManager;

public class VolumeControl {
    public static volatile float max_percent_volume = 0.75f;//default
    static float off = 0.100f;
    //parentalControlService could be started while this is changing
    //percent = 0.7f; //70%
    public static void keepVolumeAtOrBelowMaxPercent(Context context){

        if(max_percent_volume==off){

        }else{
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = (int) (audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*max_percent_volume);
            if(currentVolume>maxVolume)
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        }
    }
    public static void setMax_percent_volume(int seekBarValue){
        float seek = (float) seekBarValue;
        max_percent_volume = seek/100;
    }
}
