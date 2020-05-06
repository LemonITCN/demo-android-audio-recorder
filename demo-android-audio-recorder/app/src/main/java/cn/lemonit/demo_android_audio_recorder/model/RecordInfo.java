package cn.lemonit.demo_android_audio_recorder.model;

public class RecordInfo {
    private String recordState;
    private String recordTitle;

    public RecordInfo() {
    }

    public RecordInfo(String recordState, String recordTitle) {
        this.recordState = recordState;
        this.recordTitle = recordTitle;
    }

    public String getRecordState() {
        return recordState;
    }

    public void setRecordState(String recordState) {
        this.recordState = recordState;
    }

    public String getRecordTitle() {
        return recordTitle;
    }

    public void setRecordTitle(String recordTitle) {
        this.recordTitle = recordTitle;
    }
}
