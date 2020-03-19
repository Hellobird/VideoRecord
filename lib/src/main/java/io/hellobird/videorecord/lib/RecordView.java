package io.hellobird.videorecord.lib;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

import io.hellobird.videorecord.lib.camera.CameraManager;
import io.hellobird.videorecord.lib.camera.open.CameraFacing;

/*******************************************************************
 * RecordView.java  2020-03-16
 * <P>
 * 录制控件<br/>
 * <br/>
 * </p>
 *
 * @author:zhoupeng
 *
 ******************************************************************/
public class RecordView extends SurfaceView {

    /**
     * 1KB大小
     */
    private static final int KB = 1024 * 8;

    /**
     * 后置相机
     */
    public static final int FACING_BACK = 0;

    /**
     * 前置相机
     */
    public static final int FACING_FRONT = 1;

    /**
     * 默认录制宽度
     */
    public static final int DEFAULT_WIDTH = 1280;

    /**
     * 默认录制高度
     */
    public static final int DEFAULT_HEIGHT = 720;

    /**
     * 默认帧数
     */
    public static final int DEFAULT_FRAME_RATE = 20;

    /**
     * 默认码率，512 KB
     */
    public static final int DEFAULT_BIT_RATE = 512;
    /**
     * 相机管理类
     */
    private CameraManager mCameraManager;

    /**
     * 媒体录制类
     */
    private MediaRecorder mVideoRecorder;

    /**
     * Surface是否已打开
     */
    private boolean mSurfaceEnable;

    /**
     * 是否正在录制
     */
    private boolean mRecording;

    /**
     * 输出路径
     */
    private String mOutFilePath;

    /**
     * 是否自动打开相机，如果是true，需要确保有 CAMERA 与 RECORD_AUDIO 权限，否则会报错
     */
    private boolean mAutoOpen;

    /**
     * 视频目标分辨率宽度
     */
    private int mVideoWidth;

    /**
     * 视频目标分辨率高度
     */
    private int mVideoHeight;

    /**
     * 帧数
     */
    private int mFrameRate;

    /**
     * 码率，单位是 KB，此单位控制视频1秒钟的体积大小，在分辨率固定的情况下，码率越大画质越好，码率越小画质越差
     */
    private int mBitRate;
    /**
     * 指定相机位置
     */
    private CameraFacing mCameraFacing = CameraFacing.FRONT;

    public RecordView(Context context) {
        this(context, null);
    }

    public RecordView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
        getHolder().addCallback(mCallBack);
        mCameraManager = new CameraManager(getContext().getApplicationContext());
        //指定默认路径
        mOutFilePath = context.getExternalCacheDir().getAbsolutePath() + File.separator + "temp.mp4";
    }

    /**
     * 初始化View属性
     *
     * @param attrs 属性
     */
    private void initAttrs(@Nullable AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.RecordView);
        mCameraFacing = typedArray.getInt(R.styleable.RecordView_facing, FACING_BACK) == FACING_FRONT ? CameraFacing.FRONT : CameraFacing.BACK;
        mVideoWidth = typedArray.getInteger(R.styleable.RecordView_videoWidth, DEFAULT_WIDTH);
        mVideoHeight = typedArray.getInteger(R.styleable.RecordView_videoHeight, DEFAULT_HEIGHT);
        mFrameRate = typedArray.getInteger(R.styleable.RecordView_frameRate, DEFAULT_FRAME_RATE);
        mAutoOpen = typedArray.getBoolean(R.styleable.RecordView_autoOpen, false);
        mBitRate = typedArray.getInteger(R.styleable.RecordView_bitRate, DEFAULT_BIT_RATE);
        typedArray.recycle();
    }

    /**
     * 根据相机尺重设控件宽高
     *
     * @param camera
     */
    private void resizeWithCamera(Camera camera) {
        if (camera != null && camera.getParameters() != null) {
            Camera.Size size = camera.getParameters().getPreviewSize();
            int sizeWith = size.width > size.height ? size.height : size.width;
            int sizeHeight = size.width + size.height - sizeWith;
            // 算出宽高比
            double ratio = sizeHeight / (double) sizeWith;
            int targetHeight = (int) (getWidth() * ratio);
            // 如果目标高度与当前高度差值大于100，就重设高度
            if (Math.abs(targetHeight - getHeight()) > 100) {
                ViewGroup.LayoutParams params = getLayoutParams();
                params.height = targetHeight;
                setLayoutParams(params);
            }
        }
    }

    /**
     * 打开相机
     */
    public void openCamera() {
        // 打开相机前确认是否有权限
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(getContext(), R.string.open_camera_error, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mSurfaceEnable) {
            // 先关闭之前的相机
            closeCamera();
            try {
                mCameraManager.openDriver(getHolder(), mCameraFacing);
//                resizeWithCamera(mCameraManager.getCamera());
                // 开始预览
                mCameraManager.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), R.string.open_camera_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 关闭相机
     */
    public void closeCamera() {
        if (mCameraManager != null) {
            mCameraManager.stopPreview();
            mCameraManager.closeDriver();
        }
    }

    /**
     * 开始录制
     *
     * @return 是否已开始录制
     */
    public boolean startRecord() {
        // 录制前确认是否有权限
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(getContext(), R.string.start_record_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        // 先停止之前的
        stopRecord();
        Camera camera = mCameraManager.getCamera();
        if (camera != null) {
            mRecording = true;
            // 先获取相机当前参数，如果unlock后获取会报错
            Camera.Parameters parameters = camera.getParameters();
            // 解锁相机
            camera.unlock();
            mVideoRecorder = MediaRecorderFactory.newCustomConfigInstance(camera, parameters, mVideoWidth, mVideoHeight,
                    mFrameRate, mBitRate * KB);
            //设置输出文件
            File file = new File(mOutFilePath);
            if (file.exists()) {
                file.delete();
            }
            mVideoRecorder.setOutputFile(mOutFilePath);
            //设置旋转
            int cameraOrientation = mCameraManager.getOpenCamera().getOrientation();
            mVideoRecorder.setOrientationHint(cameraOrientation);
            mVideoRecorder.setPreviewDisplay(getHolder().getSurface());
            try {
                mVideoRecorder.prepare();
                mVideoRecorder.start();
                return true;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.w("RecordView", "========== open camera first =========");
        }
        mRecording = false;
        return false;
    }

    /**
     * 结束录制
     */
    public void stopRecord() {
        if (mRecording) {
            mVideoRecorder.stop();
            mVideoRecorder.reset();
            mVideoRecorder.release();
            Camera camera = mCameraManager.getCamera();
            if (camera != null) {
                camera.lock();
            }
            mRecording = false;
        }
    }

    /**
     * 是否正在录制
     *
     * @return
     */
    public boolean isRecording() {
        return mRecording;
    }

    public SurfaceHolder.Callback mCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceEnable = true;
            if (mAutoOpen) {
                openCamera();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mSurfaceEnable = false;
            closeCamera();
        }
    };

    /**
     * 获取视频输出路径
     *
     * @return
     */
    public String getOutFilePath() {
        return mOutFilePath;
    }

    /**
     * 设置视频输出路径
     *
     * @param outFilePath
     */
    public void setOutFilePath(String outFilePath) {
        this.mOutFilePath = outFilePath;
    }

    /**
     * View创建后是否自动打开预览
     *
     * @return
     */
    public boolean isAutoOpen() {
        return mAutoOpen;
    }

    /**
     * 设置View创建后是否自动打开预览
     *
     * @param autoOpen
     */
    public void setAutoOpen(boolean autoOpen) {
        this.mAutoOpen = autoOpen;
    }

    /**
     * 获取输出视频参考宽度，仅做参考，以最终相机支持尺寸为准
     *
     * @return
     */
    public int getVideoWidth() {
        return mVideoWidth;
    }

    /**
     * 设置输出视频参考宽度
     *
     * @param videoWidth
     */
    public void setVideoWidth(int videoWidth) {
        this.mVideoWidth = videoWidth;
    }

    /**
     * 获取输出视频参考高度，仅做参考，以最终相机支持尺寸为准
     *
     * @return
     */
    public int getVideoHeight() {
        return mVideoHeight;
    }

    /**
     * 设置输出视频参考高度
     *
     * @param videoHeight
     */
    public void setVideoHeight(int videoHeight) {
        this.mVideoHeight = videoHeight;
    }

    /**
     * 获取帧率
     *
     * @return
     */
    public int getFrameRate() {
        return mFrameRate;
    }

    /**
     * 设置帧率
     *
     * @param frameRate
     */
    public void setFrameRate(int frameRate) {
        this.mFrameRate = frameRate;
    }

    /**
     * 获取码率，单位为kb/s，表示一秒钟视频的大小
     *
     * @return
     */
    public int getBitRate() {
        return mBitRate;
    }

    /**
     * 设置码率，单位为kb/s，表示一秒钟视频的大小
     *
     * @param bitRate
     */
    public void setBitRate(int bitRate) {
        this.mBitRate = bitRate;
    }

    /**
     * 设置闪光灯
     *
     * @param isOpen true 打开，false 关闭
     */
    public void setTorch(boolean isOpen) {
        if (mCameraManager != null) {
            mCameraManager.setTorch(isOpen);
        }
    }

    /**
     * 获取闪光灯状态
     *
     * @return true 打开，false 关闭
     */
    public boolean getTorchState() {
        return mCameraManager != null && mCameraManager.getTorchState();
    }
}
