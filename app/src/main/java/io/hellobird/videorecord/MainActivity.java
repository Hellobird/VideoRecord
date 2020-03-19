package io.hellobird.videorecord;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import io.hellobird.videorecord.lib.RecordControllerLayout;
import io.hellobird.videorecord.lib.RecordView;

public class MainActivity extends Activity {

    RecordView mRecordView;
    RecordControllerLayout mController;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecordView = findViewById(R.id.recordView);
        mController = findViewById(R.id.layout_controller);
        mController.bindRecordView(mRecordView);
        mController.setDuration(0, 90);
        mController.setOnRecordListener(new RecordControllerLayout.OnRecordListener() {
            @Override
            public void onStart() {
                Toast.makeText(MainActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStop(long duration) {
                Toast.makeText(MainActivity.this, "结束录制，时长" + duration, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(MainActivity.this, "取消录制", Toast.LENGTH_SHORT).show();
            }
        });
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mController.cancelRecord();
        mRecordView.closeCamera();
    }
}