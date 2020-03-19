package io.hellobird.videorecord.lib;

import android.graphics.Point;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

/*******************************************************************
 * MediaRecorderFactory.java  2020-03-17
 * <P>
 * 录像工程类<br/>
 * <br/>
 * </p>
 *
 * @author:zhoupeng
 *
 ******************************************************************/
public final class MediaRecorderFactory {

    /**
     * 使用系统指定质量参数生成 MediaRecorder
     *
     * @param camera           相机
     * @param camcorderProfile 录制质量，可以参考{@link CamcorderProfile}类常量
     * @return
     */
    public static MediaRecorder newSystemConfigInstance(Camera camera, int camcorderProfile) {
        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);
        //设置视频录制过程中所录制的音频来自手机的麦克风
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        //设置视频源为摄像头
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        CamcorderProfile profile = null;
        if (CamcorderProfile.hasProfile(camcorderProfile)) {
            profile = CamcorderProfile.get(camcorderProfile);
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        }
        if (profile != null) {
            mediaRecorder.setProfile(profile);
        }
        return mediaRecorder;
    }

    /**
     * 使用自定义参数生成 MediaRecorder
     *
     * @param camera      相机
     * @param parameters  相机参数
     * @param resolutionX 视频宽度
     * @param resolutionY 视频高度
     * @param frameRate   帧数
     * @param bitRate     码率，单位为 kb/s，表示1秒视频的体积。码率越大，视频画质越好
     * @return
     */
    public static MediaRecorder newCustomConfigInstance(@NonNull Camera camera, @NonNull Camera.Parameters parameters, int resolutionX, int resolutionY,
                                                        int frameRate, int bitRate) {
        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);
        //设置视频录制过程中所录制的音频来自手机的麦克风
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        //设置视频源为摄像头
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        //设置视频录制的输出文件为MP4
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        //计算视频尺寸
        Point point = findCloseSizeValue(parameters, new Point(resolutionX, resolutionY));
        mediaRecorder.setVideoSize(point.x, point.y);
        //设置帧数
        mediaRecorder.setVideoFrameRate(findCloseFrameRate(parameters, frameRate));
        //设置音频编码方式为AAC
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //设置录制的视频编码为MPEG_4_SP
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        //设置编码帧率，此项会严重影响适配质量，基准数字越高，质量越好
        mediaRecorder.setVideoEncodingBitRate(bitRate);
        // 设置帧率
        mediaRecorder.setVideoFrameRate(frameRate);
        return mediaRecorder;
    }

    /**
     * 找到近似支持的帧数
     *
     * @param parameters 相机参数
     * @param frameRate  目标帧数
     * @return
     */
    public static int findCloseFrameRate(Camera.Parameters parameters, int frameRate) {
        // 帧数参数在相机中会*1000,所以判断的时候 *1000
        frameRate *= 1000;
        int resultRate = Integer.MIN_VALUE;
        List<int[]> rangeList = parameters.getSupportedPreviewFpsRange();
        if (rangeList != null) {
            for (int[] range : rangeList) {
                // 如果在范围内，则直接返回
                if (frameRate >= range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] && frameRate <= range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) {
                    resultRate = frameRate;
                    break;
                }
                // 不再范围内，则比较一下更接近的值
                int minOffset = Math.abs(frameRate - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]);
                int maxOffset = Math.abs(frameRate - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
                int currentOffset = Math.abs(frameRate - resultRate);
                // 找出最小差距
                int offset = Math.min(Math.min(minOffset, maxOffset), currentOffset);
                if (offset == minOffset) {
                    resultRate = range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
                } else if (offset == maxOffset) {
                    resultRate = range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
                }
            }
        }
        if (resultRate == Integer.MIN_VALUE) {
            resultRate = frameRate;
        }
        return resultRate / 1000;
    }

    /**
     * 寻找最接近的分辨率
     *
     * @param parameters       相机参数信息
     * @param targetResolution 目标分辨率
     * @return
     */
    public static Point findCloseSizeValue(Camera.Parameters parameters, Point targetResolution) {
        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Log.w("MediaRecorderFactory", "Device returned no supported preview sizes; using default");
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                throw new IllegalStateException("Parameters contained no preview size!");
            }
            return new Point(defaultSize.width, defaultSize.height);
        }

        long targetPixels = targetResolution.x * targetResolution.y;

        Camera.Size resultSize = null;
        long minOffset = Long.MAX_VALUE;
        for (Camera.Size size : rawSupportedSizes) {
            long pixels = size.width * size.height;
            long offset = Math.abs(targetPixels - pixels);
            if (Math.abs(offset) < minOffset) {
                minOffset = offset;
                resultSize = size;
                // 如果是0，直接跳出
                if (minOffset == 0){
                    break;
                }
            }
        }
        if (resultSize == null) {
            resultSize = parameters.getPreviewSize();
        }
        return new Point(resultSize.width, resultSize.height);
    }
}
