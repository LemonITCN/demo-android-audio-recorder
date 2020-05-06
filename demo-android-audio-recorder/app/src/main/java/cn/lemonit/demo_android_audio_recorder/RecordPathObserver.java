package cn.lemonit.demo_android_audio_recorder;

import android.os.FileObserver;

import androidx.annotation.Nullable;

import cn.lemonit.demo_android_audio_recorder.protocol.RecordPathChangedListener;
import cn.lemonit.demo_android_audio_recorder.service.RecordFileService;

public class RecordPathObserver extends FileObserver {

    private RecordPathChangedListener listener;

    public RecordPathObserver(RecordPathChangedListener listener) {
        super(RecordFileService.getRecordDirPath());
        this.listener = listener;
    }

    @Override
    public void onEvent(int event, @Nullable String path) {
        this.listener.onPathChanged();
    }
}
