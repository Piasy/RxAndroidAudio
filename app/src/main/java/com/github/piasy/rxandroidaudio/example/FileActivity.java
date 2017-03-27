/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.rxandroidaudio.example;

import android.Manifest;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.piasy.rxandroidaudio.AudioRecorder;
import com.github.piasy.rxandroidaudio.PlayConfig;
import com.github.piasy.rxandroidaudio.RxAmplitude;
import com.github.piasy.rxandroidaudio.RxAudioPlayer;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FileActivity extends RxAppCompatActivity implements AudioRecorder.OnErrorListener {

    public static final int MIN_AUDIO_LENGTH_SECONDS = 2;

    private static final String TAG = "FileActivity";

    private static final ButterKnife.Action<View> INVISIBLE
            = (view, index) -> view.setVisibility(View.INVISIBLE);
    private static final ButterKnife.Action<View> VISIBLE
            = (view, index) -> view.setVisibility(View.VISIBLE);

    @BindView(R.id.mFlIndicator)
    FrameLayout mFlIndicator;
    @BindView(R.id.mTvPressToSay)
    TextView mTvPressToSay;
    @BindView(R.id.mTvLog)
    TextView mTvLog;
    @BindView(R.id.mTvRecordingHint)
    TextView mTvRecordingHint;

    private List<ImageView> mIvVoiceIndicators;

    private AudioRecorder mAudioRecorder;
    private RxAudioPlayer mRxAudioPlayer;
    private File mAudioFile;
    private Disposable mRecordDisposable;
    private RxPermissions mPermissions;
    private Queue<File> mAudioFiles = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        ButterKnife.bind(this);
        mPermissions = new RxPermissions(this);

        mIvVoiceIndicators = new ArrayList<>();
        mIvVoiceIndicators.add(ButterKnife.findById(this, R.id.mIvVoiceIndicator1));
        mIvVoiceIndicators.add(ButterKnife.findById(this, R.id.mIvVoiceIndicator2));
        mIvVoiceIndicators.add(ButterKnife.findById(this, R.id.mIvVoiceIndicator3));
        mIvVoiceIndicators.add(ButterKnife.findById(this, R.id.mIvVoiceIndicator4));
        mIvVoiceIndicators.add(ButterKnife.findById(this, R.id.mIvVoiceIndicator5));
        mIvVoiceIndicators.add(ButterKnife.findById(this, R.id.mIvVoiceIndicator6));
        mIvVoiceIndicators.add(ButterKnife.findById(this, R.id.mIvVoiceIndicator7));

        mAudioRecorder = AudioRecorder.getInstance();
        mRxAudioPlayer = RxAudioPlayer.getInstance();
        mAudioRecorder.setOnErrorListener(this);

        mTvPressToSay.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    press2Record();
                    break;
                case MotionEvent.ACTION_UP:
                    release2Send();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    release2Send();
                    break;
                default:
                    break;
            }

            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRxAudioPlayer != null) {
            mRxAudioPlayer.stopPlay();
        }
    }

    private void press2Record() {
        mTvPressToSay.setBackgroundResource(R.drawable.button_press_to_say_pressed_bg);
        mTvRecordingHint.setText(R.string.voice_msg_input_hint_speaking);

        boolean isPermissionsGranted
                = mPermissions.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                  && mPermissions.isGranted(Manifest.permission.RECORD_AUDIO);
        if (!isPermissionsGranted) {
            mPermissions
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO)
                    .subscribe(granted -> {
                        // not record first time to request permission
                        if (granted) {
                            Toast.makeText(getApplicationContext(), "Permission granted",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Permission not granted",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, Throwable::printStackTrace);
        } else {
            recordAfterPermissionGranted();
        }
    }

    private void recordAfterPermissionGranted() {
        mRecordDisposable = Observable
                .fromCallable(() -> {
                    mAudioFile = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                            + File.separator + System.nanoTime() + ".file.m4a");
                    Log.d(TAG, "to prepare record");
                    return mAudioRecorder.prepareRecord(MediaRecorder.AudioSource.MIC,
                            MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.AudioEncoder.AAC,
                            192000, 192000, mAudioFile);
                })
                .flatMap(b -> {
                    Log.d(TAG, "prepareRecord success");
                    Log.d(TAG, "to play audio_record_ready: " + R.raw.audio_record_ready);
                    return mRxAudioPlayer.play(
                            PlayConfig.res(getApplicationContext(), R.raw.audio_record_ready)
                                    .build());
                })
                .doOnComplete(() -> {
                    Log.d(TAG, "audio_record_ready play finished");
                    mFlIndicator.post(() -> mFlIndicator.setVisibility(View.VISIBLE));
                    mAudioRecorder.startRecord();
                })
                .doOnNext(b -> Log.d(TAG, "startRecord success"))
                .flatMap(o -> RxAmplitude.from(mAudioRecorder))
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(level -> {
                    int progress = mAudioRecorder.progress();
                    Log.d(TAG, "amplitude: " + level + ", progress: " + progress);

                    refreshAudioAmplitudeView(level);

                    if (progress >= 12) {
                        mTvRecordingHint.setText(String.format(
                                getString(R.string.voice_msg_input_hint_time_limited_formatter),
                                15 - progress));
                        if (progress == 15) {
                            release2Send();
                        }
                    }
                }, Throwable::printStackTrace);
    }

    private void release2Send() {
        mTvPressToSay.setBackgroundResource(R.drawable.button_press_to_say_bg);
        mFlIndicator.setVisibility(View.GONE);

        if (mRecordDisposable != null && !mRecordDisposable.isDisposed()) {
            mRecordDisposable.dispose();
            mRecordDisposable = null;
        }

        Observable
                .fromCallable(() -> {
                    int seconds = mAudioRecorder.stopRecord();
                    Log.d(TAG, "stopRecord: " + seconds);
                    if (seconds >= MIN_AUDIO_LENGTH_SECONDS) {
                        mAudioFiles.offer(mAudioFile);
                        return true;
                    }
                    return false;
                })
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(added -> {
                    if (added) {
                        mTvLog.setText(mTvLog.getText() + "\n"
                                       + "audio file " + mAudioFile.getName() + " added");
                    }
                }, Throwable::printStackTrace);
    }

    private void refreshAudioAmplitudeView(int level) {
        int end = level < mIvVoiceIndicators.size() ? level : mIvVoiceIndicators.size();
        ButterKnife.apply(mIvVoiceIndicators.subList(0, end), VISIBLE);
        ButterKnife.apply(mIvVoiceIndicators.subList(end, mIvVoiceIndicators.size()), INVISIBLE);
    }

    @OnClick(R.id.mBtnPlay)
    public void startPlay() {
        mTvLog.setText("");
        if (!mAudioFiles.isEmpty()) {
            File audioFile = mAudioFiles.poll();
            mRxAudioPlayer.play(
                    PlayConfig.file(audioFile)
                            .streamType(AudioManager.STREAM_VOICE_CALL)
                            .build())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Functions.emptyConsumer(), Throwable::printStackTrace,
                            this::startPlay);
        }
    }

    @WorkerThread
    @Override
    public void onError(int error) {
        runOnUiThread(
                () -> Toast.makeText(FileActivity.this, "Error code: " + error, Toast.LENGTH_SHORT)
                        .show());
    }
}
