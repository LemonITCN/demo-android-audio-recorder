package cn.lemonit.demo_android_audio_recorder.service;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordFileService {

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");

    public static File createNewRecordFile(String fileFormat) {
        File newFile = new File(getRecordDirPath() + File.separator + simpleDateFormat.format(new Date()) + "." + fileFormat);
        return newFile;
    }

    public static String getRecordDirPath() {
        String externalStorageDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String recordPath = externalStorageDirectoryPath + "/lem-audio-record";
        File recordPathFile = new File(recordPath);
        if (!recordPathFile.exists()) {
            recordPathFile.mkdirs();
        }
        return recordPath;
    }

}
