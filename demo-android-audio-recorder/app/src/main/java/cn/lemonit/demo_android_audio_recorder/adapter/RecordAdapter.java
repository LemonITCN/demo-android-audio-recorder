package cn.lemonit.demo_android_audio_recorder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import cn.lemonit.demo_android_audio_recorder.R;
import cn.lemonit.demo_android_audio_recorder.model.RecordInfo;

public class RecordAdapter extends ArrayAdapter<RecordInfo> {

    private final int resourceId;

    public RecordAdapter(@NonNull Context context, int resource, List<RecordInfo> recordInfoList) {
        super(context, resource, recordInfoList);
        resourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        RecordInfo recordInfo = getItem(position);
        View view = LayoutInflater.from(getContext()).inflate(resourceId, null);
        ImageView fruitImage = view.findViewById(R.id.record_state_icon);
        TextView fruitName = view.findViewById(R.id.record_title);
        switch (recordInfo.getRecordState()) {
            case "1":
                fruitImage.setImageResource(R.mipmap.level1);
                break;
            case "2":
                fruitImage.setImageResource(R.mipmap.level2);
                break;
            default:
                fruitImage.setImageResource(R.mipmap.level3);
                break;
        }
        fruitName.setText(recordInfo.getRecordTitle());//为文本视图设置文本内容
        return view;
    }
}
