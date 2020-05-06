package cn.lemonit.demo_android_audio_recorder.service;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import cn.lemonit.demo_android_audio_recorder.protocol.AudioMonitorListener;

public class AudioRecordService {

    private static final int SAMPLE_SIZE = 44100; // 采样频率,频率越高采样越“积极”，音质越好,通常为44100，可以自己设定
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC; //声音来源
    private static final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO; //通道数，单通道
    private static final int AUDIO_ENCODING_BIT = AudioFormat.ENCODING_PCM_16BIT; //采样位深，16位
    private static final int MIN_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_SIZE, AUDIO_CHANNEL, AUDIO_ENCODING_BIT); //计算AudioRecord正常工作所需要的最小字节数组大小

    private static final int RECORD_SECONDS = 5;
    private static final double RECORD_INVOKE_VALUE = 50;

    private static AudioRecord audioRecord;
    private static AudioMonitorListener cachedAudioMonitorListener;
    /**
     * 是否开始录音到文件的标识
     * 0 - 未开始录制，1 - 录制中，2 - 录制停止，写入文件
     */
    private static Integer recordTag = 0;
    private static File currentRecordFile;
    private static FileOutputStream currentRecordFileOutputStream;
    private static BufferedOutputStream currentRecordBufferedOutputStream;

    public static void startMonitor(final AudioMonitorListener audioMonitorListener) {
        cachedAudioMonitorListener = audioMonitorListener;
        if (audioRecord != null) {
            audioRecord.release();
        }
        recordTag = 0;
        audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_SIZE, AUDIO_CHANNEL, AUDIO_ENCODING_BIT, MIN_BUFFER_SIZE);
        audioRecord.startRecording();
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] readData = new byte[MIN_BUFFER_SIZE];
                int readLength = 0;
                while (true) {
                    if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                        //说明被释放资源,此AudioRecord将无效，需要重新初始化,结束录制,录制过程中点击结束录制
                        break;
                    } else if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        //正在录制状态
                        readLength = audioRecord.read(readData, 0, readData.length);
                        if (readLength > 0) {
                            double dbValue = getDecibelForPcm(readData, readLength);
                            if (recordTag == 0) {
                                // 当前未录制
                                if (dbValue >= RECORD_INVOKE_VALUE) {
                                    startRecord();
                                }
                            } else if (recordTag == 1) {
                                // 需要开始录制了
                                try {
                                    if (currentRecordBufferedOutputStream != null) {
                                        currentRecordBufferedOutputStream.write(readData, 0, readLength);
                                        currentRecordBufferedOutputStream.flush();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else if (recordTag == 2) {
                                // 录制结束，准备写入文件
                                endRecord();
                            }
                            audioMonitorListener.onWave(dbValue);
                        }
                    }
                }
            }
        }).start();
    }

    private static void startRecord() {
        try {
            currentRecordFile = RecordFileService.createNewRecordFile("pcm");
            currentRecordFileOutputStream = new FileOutputStream(currentRecordFile, true);
            currentRecordBufferedOutputStream = new BufferedOutputStream(currentRecordFileOutputStream);
            cachedAudioMonitorListener.onRecordStart();
            recordTag = 1;
            new Timer().schedule(new TimerTask() {
                public void run() {
                    recordTag = 2;
                }
            }, RECORD_SECONDS * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void endRecord() {
        try {
            if (currentRecordBufferedOutputStream != null) {
                currentRecordBufferedOutputStream.close();
            }
            if (currentRecordFileOutputStream != null) {
                currentRecordFileOutputStream.close();
            }
            convertPcmToWav(currentRecordFile.getAbsolutePath(), currentRecordFile.getAbsolutePath().replace(".pcm", ".wav"), SAMPLE_SIZE, 1, AUDIO_ENCODING_BIT);
            cachedAudioMonitorListener.onRecordEnd();
            recordTag = 0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            recordTag = 0;
        }
    }

    public static void endMonitor() {
        audioRecord.stop();
        audioRecord.release();
//        endRecord();
    }

    private static double getDecibelForPcm(byte[] data, int dataLength) {
        long sum = 0;
        long temp = 0;
        for (int i = 0; i < data.length; i += 2) {
            temp = (data[i + 1] * 128 + data[i]); //累加求和
            temp *= temp;
            sum += temp;
        }

        //平方和除以数据长度，得到音量大小
        double square = sum / (double) dataLength; //音量大小
        double result = 10 * Math.log10(square * 2); //分贝值
        return result;
    }

    /**
     * PCM文件转WAV文件
     *
     * @param inPcmFilePath  输入PCM文件路径
     * @param outWavFilePath 输出WAV文件路径
     * @param sampleRate     采样率，例如15000
     * @param channels       声道数 单声道：1或双声道：2
     * @param bitNum         采样位数，8或16
     */
    public static void convertPcmToWav(String inPcmFilePath, String outWavFilePath,
                                       int sampleRate,
                                       int channels, int bitNum) {
        FileInputStream in = null;
        FileOutputStream out = null;
        byte[] data = new byte[1024];

        try {
            //采样字节byte率
            long byteRate = sampleRate * channels * bitNum / 8;

            in = new FileInputStream(inPcmFilePath);
            out = new FileOutputStream(outWavFilePath);

            //PCM文件大小
            long totalAudioLen = in.getChannel().size();

            //总大小，由于不包括RIFF和WAV，所以是44 - 8 = 36，在加上PCM文件大小
            long totalDataLen = totalAudioLen + 36;

            writeWaveFileHeader(out, totalAudioLen, totalDataLen, sampleRate, channels, byteRate);

            int length = 0;
            while ((length = in.read(data)) > 0) {
                out.write(data, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 输出WAV文件
     *
     * @param out           WAV输出文件流
     * @param totalAudioLen 整个音频PCM数据大小
     * @param totalDataLen  整个数据大小
     * @param sampleRate    采样率
     * @param channels      声道数
     * @param byteRate      采样字节byte率
     * @throws IOException
     */
    private static void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                            long totalDataLen, int sampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

}
