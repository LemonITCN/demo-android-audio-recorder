package cn.lemonit.demo_android_audio_recorder.protocol;

public interface AudioMonitorListener {

    void onWave(double value);

    void onRecordStart();

    void onRecordEnd();

}
