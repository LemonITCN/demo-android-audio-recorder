package cn.lemonit.demo_android_audio_recorder.activity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.lemonit.demo_android_audio_recorder.R;
import cn.lemonit.demo_android_audio_recorder.RecordPathObserver;
import cn.lemonit.demo_android_audio_recorder.adapter.RecordAdapter;
import cn.lemonit.demo_android_audio_recorder.model.RecordInfo;
import cn.lemonit.demo_android_audio_recorder.protocol.AudioMonitorListener;
import cn.lemonit.demo_android_audio_recorder.protocol.RecordPathChangedListener;
import cn.lemonit.demo_android_audio_recorder.service.AudioRecordService;
import cn.lemonit.demo_android_audio_recorder.service.RecordFileService;

public class MainActivity extends AppCompatActivity {

    private TextView monitorValueTextView;
    private Switch monitorSwitchView;
    private ListView recordListView;

    private RecordAdapter recordAdapter;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private List<RecordInfo> recordInfoList = new ArrayList<>();
    private RecordInfo currentPlayRecordInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.bindViews();
        this.bindViewListeners();

        this.refreshRecordList();
        new RecordPathObserver(new RecordPathChangedListener() {
            @Override
            public void onPathChanged() {
                System.out.println("hello");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshRecordList();
                    }
                });
            }
        }).startWatching();
    }

    private void bindViews() {
        this.monitorValueTextView = this.findViewById(R.id.monitor_value_text);
        this.monitorSwitchView = this.findViewById(R.id.monitor_switch);
        this.recordListView = this.findViewById(R.id.record_list);
        List<RecordInfo> recordInfos = new ArrayList<>();
        recordAdapter = new RecordAdapter(this, R.layout.record_info_line, recordInfos);
        this.recordListView.setAdapter(recordAdapter);
    }

    private void bindViewListeners() {
        this.monitorSwitchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startMonitor();
                } else {
                    endMonitor();
                }
            }
        });
        this.recordListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setPlayRecordInfo(recordInfoList.get(position));
            }
        });
    }

    private void refreshRecordList() {
        this.recordAdapter.clear();
        this.recordInfoList.clear();
        File rootPathFile = new File(RecordFileService.getRecordDirPath());
        if (rootPathFile.exists()) {
            for (File file : rootPathFile.listFiles()) {
                if (file.getName().endsWith(".wav")) {
                    RecordInfo recordInfo = new RecordInfo("3", file.getName());
                    this.recordAdapter.add(recordInfo);
                    this.recordInfoList.add(recordInfo);
                }
            }
        }
    }

    private void startMonitor() {
        Toast.makeText(MainActivity.this, "环境声音检测启动", Toast.LENGTH_SHORT).show();
        System.out.println(RecordFileService.getRecordDirPath());
        this.monitorValueTextView.setVisibility(View.VISIBLE);
        AudioRecordService.startMonitor(new AudioMonitorListener() {
            @Override
            public void onWave(final double value) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        monitorValueTextView.setText(String.format("声音 %.2f 分贝", value));
                    }
                });
            }

            @Override
            public void onRecordStart() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "检测到声音变大，开始录音...", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onRecordEnd() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "五秒钟录音结束", Toast.LENGTH_SHORT).show();
                        refreshRecordList();
                    }
                });
            }
        });
    }

    private void endMonitor() {
        Toast.makeText(MainActivity.this, "环境声音检测关闭", Toast.LENGTH_SHORT).show();
        AudioRecordService.endMonitor();
        this.monitorValueTextView.setVisibility(View.GONE);
    }

    private void setPlayRecordInfo(RecordInfo recordInfo) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.stop();
            mediaPlayer.setDataSource(RecordFileService.getRecordDirPath() + File.separator + recordInfo.getRecordTitle());
            mediaPlayer.prepare();
            mediaPlayer.start();
            this.currentPlayRecordInfo = recordInfo;
            Toast.makeText(this, "开始播放" + recordInfo.getRecordTitle(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
