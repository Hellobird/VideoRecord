package io.hellobird.videorecord.lib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/*******************************************************************
 * RecordControllerLayout.java  2020-03-18
 * <P>
 * 包含录制控制、信息状态显示的layout<br/>
 * <br/>
 * </p>
 *
 * @author:zhoupeng
 *
 ******************************************************************/
public class RecordControllerLayout extends FrameLayout implements View.OnClickListener {


    /**
     * 当前控制的RecordView
     */
    private RecordView mRecordView;

    /**
     * 录制按钮
     */
    private ImageButton mBtnStart;

    /**
     * 录制时间
     */
    private TextView mTvTime;

    /**
     * 最小录制时间，单位秒，0表示不限制
     */
    private int mMinDuration;

    /**
     * 最大录制时间，单位秒，0表示不限制
     */
    private int mMaxDuration;

    /**
     * 开始录制时间
     */
    private long mStartTime;

    /**
     * 主线程handler
     */
    private Handler mHandler;

    /**
     * 回调
     */
    private OnRecordListener mOnRecordListener;


    public RecordControllerLayout(@NonNull Context context) {
        this(context, null);
    }

    public RecordControllerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordControllerLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View layoutController = LayoutInflater.from(context).inflate(R.layout.layout_record_controller, this);
        mBtnStart = layoutController.findViewById(R.id.btn_start);
        mTvTime = layoutController.findViewById(R.id.tv_time);
        mBtnStart.setOnClickListener(this);
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 绑定 RecordView
     *
     * @param recordView
     */
    public void bindRecordView(@NonNull RecordView recordView) {
        if (mRecordView != null) {
            Log.w("RecordControllerLayout", "RecordView 已经绑定，不能再次绑定");
            return;
        }
        mRecordView = recordView;
    }

    /**
     * 获取最小录制时间，0表示不限制
     *
     * @return
     */
    public int getMinDuration() {
        return mMinDuration;
    }

    /**
     * 获取最大录制时间，<=0表示不显示
     *
     * @return
     */
    public int getMaxDuration() {
        return mMaxDuration;
    }

    /**
     * 设置录制时间，
     *
     * @param minDuration 最小录制时间 <=0表示不限制
     * @param maxDuration 最大录制时间 <=0表示不限制
     */
    public void setDuration(int minDuration, int maxDuration) {
        if (minDuration > maxDuration) {
            throw new IllegalArgumentException("minDuration 不能大于 maxDuration");
        }
        this.mMinDuration = minDuration;
        this.mMaxDuration = maxDuration;
    }


    /**
     * 获取录制监听接口
     *
     * @return
     */
    public OnRecordListener getOnRecordListener() {
        return mOnRecordListener;
    }

    /**
     * 设置录制监听接口
     *
     * @param recordListener
     */
    public void setOnRecordListener(OnRecordListener recordListener) {
        this.mOnRecordListener = recordListener;
    }

    @Override
    public void onClick(View v) {
        if (mRecordView == null) {
            Log.w("RecordControllerLayout", "请先绑定 RecordView");
            return;
        }
        if (mRecordView.isRecording()) {
            stopRecord(false);
        } else {
            startRecord();
        }
    }

    /**
     * 开始录制
     */
    private void startRecord() {
        // 如果正在录制，则不处理
        if (mRecordView.isRecording()) {
            return;
        }
        mStartTime = System.currentTimeMillis();
        mRecordView.startRecord();
        mBtnStart.setImageResource(R.drawable.button_stop_record);
        if (mOnRecordListener != null) {
            mOnRecordListener.onStartRecord();
        }
        // 开始计时
        mHandler.removeCallbacks(mDurationCounter);
        mHandler.post(mDurationCounter);
    }

    /**
     * 强制结束录制
     */
    public void cancelRecord() {
        stopRecord(true);
    }

    /**
     * 结束录制
     */
    private void stopRecord(boolean isCancel) {
        // 如果不是正在录制状态，则不处理
        if (!mRecordView.isRecording()) {
            return;
        }
        long duration = System.currentTimeMillis() - mStartTime;
        // 如果不是取消录制，且当前录制时间小于指定时间，则提示
        if (!isCancel && mMinDuration > 0 && (duration < mMinDuration * 1000)) {
            Toast.makeText(getContext(), getContext().getString(R.string.record_min_duration_hint, mMinDuration),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        mRecordView.stopRecord();
        mBtnStart.setImageResource(R.drawable.button_start_record);
        mTvTime.setText("");
        if (mOnRecordListener != null) {
            if (isCancel) {
                mOnRecordListener.onCancelRecord();
            } else {
                mOnRecordListener.onStopRecord(duration);
            }
        }
        // 停止计时
        mHandler.removeCallbacks(mDurationCounter);
    }

    /**
     * 录制时长计时器
     */
    private Runnable mDurationCounter = new Runnable() {
        @Override
        public void run() {
            long duration = System.currentTimeMillis() - mStartTime;
            mTvTime.setText(formatTime(duration));
            // 如果已经超过最大录制时长
            if (mMaxDuration > 0 && duration >= mMaxDuration * 1000) {
                stopRecord(false);
            } else {
                // 继续计时
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * 输出格式化的时间
     *
     * @param duration 当前时长
     * @return
     */
    private String formatTime(long duration) {
        // 获得总秒数
        long seconds = duration / 1000;
        if (seconds >= 60) {
            long mintues = seconds / 60;
            seconds = seconds - mintues * 60;
            return formatUnit(mintues) + ":" + formatUnit(seconds);
        } else {
            return "00:" + formatUnit(seconds);
        }
    }

    /**
     * 不足10的数字会补0
     *
     * @param number
     * @return
     */
    private String formatUnit(long number) {
        return number >= 10 ? String.valueOf(number) : "0" + number;
    }

    public interface OnRecordListener {
        /**
         * 开始录制
         */
        void onStartRecord();

        /**
         * 录制结束
         *
         * @param duration 录制时长
         */
        void onStopRecord(long duration);

        /**
         * 取消录制
         */
        void onCancelRecord();
    }
}
