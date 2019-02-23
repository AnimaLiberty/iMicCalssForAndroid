package cn.lemon.whiteboard.screen;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;

import cn.alien95.util.Utils;
import cn.lemon.whiteboard.R;
import cn.lemon.whiteboard.data.CurveModel;
import cn.lemon.whiteboard.screen.util.CommonUtil;
import cn.lemon.whiteboard.screen.util.FileUtil;


/**
 * Created by admin on 2018/3/28.
 */

public class ScreenRecordService extends Service implements Handler.Callback {

    private boolean isLimitDuration = false; // 是否限制时长
    private int maxDuration = 3 * 60;// 默认最大时长 未限制时长时不生效
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;

    private boolean mIsRunning;
    private int mRecordWidth = CommonUtil.getScreenWidth();
    private int mRecordHeight = CommonUtil.getScreenHeight();
    private int mScreenDpi = CommonUtil.getScreenDpi();

    private int mResultCode;
    private Intent mResultData;

    //录屏文件的保存地址
    private String mRecordFilePath;

    private Handler mHandler;
    //已经录制多少秒了
    private int mRecordSeconds = 0;
    private static final int MSG_TYPE_COUNT_DOWN = 110;

    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mIsRunning = false;
        mMediaRecorder = new MediaRecorder();
        mHandler = new Handler(Looper.getMainLooper(), this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public boolean isReady() {
        return mMediaProjection != null && mResultData != null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void clearRecordElement() {
        clearAll();
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        mResultData = null;
        mIsRunning = false;
    }

    public boolean ismIsRunning() {
        return mIsRunning;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setResultData(int resultCode, Intent resultData) {
        mResultCode = resultCode;
        mResultData = resultData;

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mProjectionManager != null) {
            mMediaProjection = mProjectionManager.getMediaProjection(mResultCode, mResultData);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean startRecord() {
        if (mIsRunning) {
            return false;
        }
        if (mMediaProjection == null) {
            mMediaProjection = mProjectionManager.getMediaProjection(mResultCode, mResultData);

        }

        setUpMediaRecorder();
        createVirtualDisplay();
        mMediaRecorder.start();

        ScreenUtil.startRecord();
        mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN, 1000);
        mIsRunning = true;
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean stopRecord(String tip) {
//        Log.w("lala","stopRecord: first ");
        if (!mIsRunning) {
            return false;
        }
        mIsRunning = false;
//        Log.w("lala","stopRecord  middle");
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder = null;
            mVirtualDisplay.release();
            mMediaProjection.stop();
//            Log.w("lala","stopRecord ");
        } catch (Exception e) {
            e.printStackTrace();
            mMediaRecorder.release();
            mMediaRecorder = null;
//            Log.w("lala","stopRecord  exception");
        }

        mMediaProjection = null;
        mHandler.removeMessages(MSG_TYPE_COUNT_DOWN);
        ScreenUtil.stopRecord(tip);
        if (mRecordSeconds <= 2) {
            FileUtil.deleteSDFile(mRecordFilePath);
        } else {
            //通知系统图库更新
            FileUtil.fileScanVideo(this, mRecordFilePath, mRecordWidth, mRecordHeight, mRecordSeconds);
        }
//        mRecordFilePath = null;
        mRecordSeconds = 0;

        return true;
    }

    /**
     * 暂停录制
     */
    public void pauseRecord() {
        if (mMediaRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mHandler.removeMessages(MSG_TYPE_COUNT_DOWN);
                mMediaRecorder.pause();
            }
        }
    }

    /**
     * 继续录制
     */
    public void resumeRecord() {
        if (mMediaRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mHandler.sendEmptyMessage(MSG_TYPE_COUNT_DOWN);
                mMediaRecorder.resume();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createVirtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("MainScreen", mRecordWidth, mRecordHeight, mScreenDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setUpMediaRecorder() {
        mRecordFilePath = getSaveDirectory() + File.separator + Utils.getVideoTitle() + ".mp4";
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mRecordFilePath);
        mMediaRecorder.setVideoSize(mRecordWidth, mRecordHeight);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate((int) (mRecordWidth * mRecordHeight * 3.6));
        mMediaRecorder.setVideoFrameRate(20);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void clearAll() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    public String getRecordFilePath() {
        return mRecordFilePath;
    }

    /**
     * 课程保存目录
     *
     * @return
     */
    public String getSaveDirectory() {
//        return CommonUtil.getVideoCachePath(this);
        return CurveModel.getInstance().getAppVideoDir().getAbsolutePath();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_TYPE_COUNT_DOWN: {
                String str = null;
                // 计算内存
                boolean enough = FileUtil.getSDFreeMemory() / (1024 * 1024) < 4;
                if (enough) {
                    //空间不足，停止录屏
                    str = getString(R.string.record_space_tip);
                    stopRecord(str);
                    mRecordSeconds = 0;
                    break;
                }
                mRecordSeconds++;
                // 计算分秒
                int minute = 0, second = 0;
                if (mRecordSeconds >= 60) {
                    minute = mRecordSeconds / 60;
                    second = mRecordSeconds % 60;
                } else {
                    second = mRecordSeconds;
                }
                // 屏幕录制时长回调
                ScreenUtil.onRecording("0" + minute + ":" + (second < 10 ? "0" + second : second + ""));
                // 是否限制时长
                if (isLimitDuration) {
                    if (mRecordSeconds < maxDuration) {
                        mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN, 1000);
                    } else if (mRecordSeconds == maxDuration) {
                        str = getString(R.string.record_time_end_tip);
                        stopRecord(str);
                        mRecordSeconds = 0;
                    }
                } else {
                    mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN, 1000);
                }
                break;
            }
        }
        return true;
    }

    public class RecordBinder extends Binder {
        public ScreenRecordService getRecordService() {
            return ScreenRecordService.this;
        }
    }


}
