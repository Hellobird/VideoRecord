# VideoRecord

对 `Android` 自带的 `MediaRecorder`
类封装视频录制组件，可以很轻松地设置分辨率、帧数、码率等参数。

## 使用方式

首先确保你已经获取到 `CAMERA` 与 `VIDEO_RECORD` 权限。

然后在你的布局中加入 `RecordView` 与 `RecordControllerLayout`

```xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">


    <io.hellobird.videorecord.lib.RecordView xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/recordView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:autoOpen="true"
        app:bitRate="512"
        app:facing="BACK"
        app:frameRate="15"
        app:videoHeight="480"
        app:videoWidth="720" />

    <io.hellobird.videorecord.lib.RecordControllerLayout
        android:id="@+id/layout_controller"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"/>

</FrameLayout>
```

在代码中将他们绑定

```java
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
```

记得在关闭页面时取消录制同时关闭相机

```java
@Override
protected void onPause() {
    super.onPause();
    mController.cancelRecord();
    mRecordView.closeCamera();
}
```
