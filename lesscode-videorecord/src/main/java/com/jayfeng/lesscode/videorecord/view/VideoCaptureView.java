/*
 *  Copyright 2016 Jeroen Mols
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.jayfeng.lesscode.videorecord.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.jayfeng.lesscode.videorecord.R;
import com.jayfeng.lesscode.videorecord.preview.CapturePreview;

public class VideoCaptureView extends FrameLayout implements OnClickListener {
    private ImageView mDeclineBtnIv;
    private ImageView mAcceptBtnIv;
    private ImageView mRecordBtnIv;
    private ImageView mChangeCameraIv;
    private ImageView mRecordPlayIv;
    private ImageView mBackIv;
    private RoundProgressBar mRecordingIv;

    private SurfaceView mSurfaceView;
    private ImageView mThumbnailIv;
    private TextView mTimerTv;
    private Handler customHandler = new Handler();
    private long startTime = 0L;

    private RecordingButtonInterface mRecordingInterface;
    private boolean mShowTimer;
    private boolean isFrontCameraEnabled;
    private boolean isCameraSwitchingEnabled;


    public int mVideoTime;

    public VideoCaptureView(Context context) {
        super(context);
        initialize(context);
    }

    public VideoCaptureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public VideoCaptureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        final View videoCapture = View.inflate(context, R.layout.view_videocapture, this);

        mRecordBtnIv = (ImageView) videoCapture.findViewById(R.id.videocapture_recordbtn_iv);
        mAcceptBtnIv = (ImageView) videoCapture.findViewById(R.id.videocapture_acceptbtn_iv);
        mDeclineBtnIv = (ImageView) videoCapture.findViewById(R.id.videocapture_declinebtn_iv);
        mChangeCameraIv = (ImageView) videoCapture.findViewById(R.id.right_image);
        mRecordPlayIv = (ImageView) videoCapture.findViewById(R.id.videorecoder_play);
        mBackIv = (ImageView) videoCapture.findViewById(R.id.back);
        mRecordingIv = (RoundProgressBar) videoCapture.findViewById(R.id.videocapture_recording);

        mRecordBtnIv.setOnClickListener(this);
        mAcceptBtnIv.setOnClickListener(this);
        mDeclineBtnIv.setOnClickListener(this);
        mChangeCameraIv.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
        mRecordPlayIv.setOnClickListener(this);
        mRecordingIv.setOnClickListener(this);

        mThumbnailIv = (ImageView) videoCapture.findViewById(R.id.videocapture_preview_iv);
        mSurfaceView = (SurfaceView) videoCapture.findViewById(R.id.videocapture_preview_sv);
        mTimerTv = (TextView) videoCapture.findViewById(R.id.videocapture_timer_tv);
        mVideoTime = 0;
    }

    public void setRecordingButtonInterface(RecordingButtonInterface mBtnInterface) {
        this.mRecordingInterface = mBtnInterface;
    }

    public void setCameraSwitchingEnabled(boolean isCameraSwitchingEnabled) {
        this.isCameraSwitchingEnabled = isCameraSwitchingEnabled;
        mChangeCameraIv.setVisibility(isCameraSwitchingEnabled ? View.VISIBLE : View.INVISIBLE);
    }

    public void setCameraFacing(boolean isFrontFacing) {
        if (!isCameraSwitchingEnabled) return;
        isFrontCameraEnabled = isFrontFacing;
        mChangeCameraIv.setImageResource(isFrontCameraEnabled ?
                R.drawable.ic_camera_change :
                R.drawable.ic_camera_change);
    }

    public SurfaceHolder getPreviewSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    public void updateUINotRecording() {
        mRecordBtnIv.setSelected(false);
        mChangeCameraIv.setVisibility(allowCameraSwitching() ? VISIBLE : INVISIBLE);
        mRecordBtnIv.setVisibility(View.VISIBLE);
        mAcceptBtnIv.setVisibility(View.GONE);
        mRecordPlayIv.setVisibility(GONE);
        mDeclineBtnIv.setVisibility(View.GONE);
        mThumbnailIv.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            long timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            int seconds = (int) (timeInMilliseconds / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            mVideoTime = seconds;
            updateRecordingTime(seconds, minutes);
            customHandler.postDelayed(this, 1000);
            if (mVideoTime > 3) {
                mAcceptBtnIv.setVisibility(View.VISIBLE);
                mDeclineBtnIv.setVisibility(View.VISIBLE);
            } else {
                mAcceptBtnIv.setVisibility(View.GONE);
                mDeclineBtnIv.setVisibility(View.GONE);
            }
        }
    };

    public void updateUIRecordingOngoing() {
        mRecordBtnIv.setSelected(true);
        mRecordBtnIv.setVisibility(View.GONE);
        mRecordingIv.setVisibility(VISIBLE);
        mChangeCameraIv.setVisibility(View.INVISIBLE);
        mAcceptBtnIv.setVisibility(View.GONE);
        mDeclineBtnIv.setVisibility(View.GONE);
        mThumbnailIv.setVisibility(View.GONE);
        mRecordPlayIv.setVisibility(GONE);

        mSurfaceView.setVisibility(View.VISIBLE);
        if (mShowTimer) {
            mTimerTv.setVisibility(View.VISIBLE);
            startTime = SystemClock.uptimeMillis();
            updateRecordingTime(0, 0);
            customHandler.postDelayed(updateTimerThread, 1000);
        }
    }

    public void updateUIRecordingFinished(Bitmap videoThumbnail) {
        mRecordBtnIv.setVisibility(View.INVISIBLE);
        mAcceptBtnIv.setVisibility(View.VISIBLE);
        mChangeCameraIv.setVisibility(View.INVISIBLE);
        mRecordingIv.setVisibility(GONE);
        mRecordPlayIv.setVisibility(VISIBLE);
        mDeclineBtnIv.setVisibility(View.VISIBLE);
        mThumbnailIv.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.GONE);
        mVideoTime = 0;
        if (videoThumbnail != null) {
            mThumbnailIv.setScaleType(ScaleType.CENTER_CROP);
            mThumbnailIv.setImageBitmap(videoThumbnail);
        }
        customHandler.removeCallbacks(updateTimerThread);

    }

    @Override
    public void onClick(View v) {
        if (mRecordingInterface == null) return;

        if (v.getId() == mBackIv.getId()) {
            mRecordingInterface.onBackClicked();
        } else if (v.getId() == mRecordPlayIv.getId()) {
            mRecordingInterface.onVideoPlayClicked();
        } else if (v.getId() == mRecordingIv.getId()) {
            mRecordingInterface.onRecordingonClicked();
        } else if (v.getId() == mRecordBtnIv.getId()) {
            mRecordingInterface.onRecordButtonClicked();
        } else if (v.getId() == mAcceptBtnIv.getId()) {
            mRecordingInterface.onAcceptButtonClicked();
        } else if (v.getId() == mDeclineBtnIv.getId()) {
            mRecordingInterface.onDeclineButtonClicked();
        } else if (v.getId() == mChangeCameraIv.getId()) {
            isFrontCameraEnabled = !isFrontCameraEnabled;
            mChangeCameraIv.setImageResource(isFrontCameraEnabled ? R.drawable.ic_camera_change : R.drawable.ic_camera_change);
            mRecordingInterface.onSwitchCamera(isFrontCameraEnabled);
        }

    }

    public void showTimer(boolean showTimer) {
        this.mShowTimer = showTimer;
    }

    private void updateRecordingTime(int seconds, int minutes) {
        mTimerTv.setText(String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
        mRecordingIv.setProgress(seconds * 10);
    }

    private boolean allowCameraSwitching() {
        return CapturePreview.isFrontCameraAvailable() && isCameraSwitchingEnabled;
    }
}
