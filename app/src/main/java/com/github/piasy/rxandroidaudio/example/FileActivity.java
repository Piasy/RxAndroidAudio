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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.piasy.rxandroidaudio.AudioRecorder;
import com.github.piasy.rxandroidaudio.PlayConfig;
import com.github.piasy.rxandroidaudio.RxAmplitude;
import com.github.piasy.rxandroidaudio.RxAudioPlayer;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FileActivity extends RxAppCompatActivity implements AudioRecorder.OnErrorListener {

    public static final int MIN_AUDIO_LENGTH_SECONDS = 2;

    public static final int SHOW_INDICATOR_DELAY_MILLIS = 300;

    private static final String TAG = "FileActivity";

    private static final ButterKnife.Action<View> INVISIBLE = new ButterKnife.Action<View>() {
        @Override
        public void apply(View view, int index) {
            view.setVisibility(View.INVISIBLE);
        }
    };

    private static final ButterKnife.Action<View> VISIBLE = new ButterKnife.Action<View>() {
        @Override
        public void apply(View view, int index) {
            view.setVisibility(View.VISIBLE);
        }
    };

    @BindView(R.id.mFlIndicator)
    FrameLayout mFlIndicator;

    @BindView(R.id.mTvPressToSay)
    TextView mTvPressToSay;

    @BindView(R.id.mTvLog)
    TextView mTvLog;

    @BindView(R.id.mTvRecordingHint)
    TextView mTvRecordingHint;

    List<ImageView> mIvVoiceIndicators;

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
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator1));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator2));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator3));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator4));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator5));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator6));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator7));

        mAudioRecorder = AudioRecorder.getInstance();
        mRxAudioPlayer = RxAudioPlayer.getInstance();
        mAudioRecorder.setOnErrorListener(this);

        mTvPressToSay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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
            }
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

        boolean isPermissionsGranted = mPermissions
                .isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                mPermissions.isGranted(Manifest.permission.RECORD_AUDIO);
        if (!isPermissionsGranted) {
            mPermissions
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO)
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean granted) {
                            // not record first time to request permission
                            if (granted) {
                                Toast.makeText(getApplicationContext(), "Permission granted",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Permission not granted",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
        } else {
            recordAfterPermissionGranted();
        }
    }

    private void recordAfterPermissionGranted() {
        Single.just(true)
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<Boolean, SingleSource<?>>() {
                    @Override
                    public Single<Boolean> apply(Boolean aBoolean) {
                        Log.d(TAG, "to play audio_record_start: " + R.raw.audio_record_start);
                        return mRxAudioPlayer.play(
                                PlayConfig.res(getApplicationContext(), R.raw.audio_record_start)
                                        .build());
                    }
                })
                .doOnSuccess(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        Log.d(TAG, "audio_record_start play finished");

                    }
                })
                .map(new Function<Object, Object>() {
                    @Override
                    public Boolean apply(Object ignored) {
                        mAudioFile = new File(
                                Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        File.separator + System.nanoTime() + ".file.m4a");
                        Log.d(TAG, "to prepare record");
                        return mAudioRecorder.prepareRecord(MediaRecorder.AudioSource.MIC,
                                MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.AudioEncoder.AAC,
                                192000, 192000, mAudioFile);
                    }
                })
                .doOnSuccess(new Consumer<Object>() {
                    @Override
                    public void accept(Object ignored) {
                        Log.d(TAG, "prepareRecord success");
                    }
                })
                .flatMap(new Function<Object, SingleSource<Boolean>>() {
                    @Override
                    public SingleSource<Boolean> apply(Object aBoolean) {
                        Log.d(TAG, "to play audio_record_ready: " + R.raw.audio_record_ready);
                        return mRxAudioPlayer.play(
                                PlayConfig.res(getApplicationContext(), R.raw.audio_record_ready)
                                        .build());
                    }
                })
                .doOnSuccess(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        Log.d(TAG, "audio_record_ready play finished");
                    }
                })
                .map(new Function<Boolean, Object>() {
                    @Override
                    public Boolean apply(Boolean aBoolean) {
                        // TODO why need delay?
                        mFlIndicator.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mFlIndicator.setVisibility(View.VISIBLE);
                            }
                        }, SHOW_INDICATOR_DELAY_MILLIS);
                        return mAudioRecorder.startRecord();
                    }
                })
                .doOnSuccess(new Consumer<Object>() {
                    @Override
                    public void accept(Object aBoolean) {
                        Log.d(TAG, "startRecord success");
                    }
                })
                .toObservable()
                .flatMap(new Function<Object, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Object o) throws Exception {
                        return RxAmplitude.from(mAudioRecorder);

                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<Integer>bindToLifecycle())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        mRecordDisposable = disposable;
                    }
                })
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer level) {
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
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

    private void release2Send() {
        mTvPressToSay.setBackgroundResource(R.drawable.button_press_to_say_bg);
        mFlIndicator.setVisibility(View.GONE);

        if (mRecordDisposable != null && !mRecordDisposable.isDisposed()) {
            mRecordDisposable.dispose();
            mRecordDisposable = null;
        }

        Log.d(TAG, "to play audio_record_end: " + R.raw.audio_record_end);
        mRxAudioPlayer.play(PlayConfig.res(getApplicationContext(), R.raw.audio_record_end).build())
                .doOnSuccess(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        Log.d(TAG, "audio_record_end play finished");
                    }
                })
                .subscribeOn(Schedulers.io())
                .map(new Function<Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean aBoolean) {
                        int seconds = mAudioRecorder.stopRecord();
                        if (seconds >= MIN_AUDIO_LENGTH_SECONDS) {
                            mAudioFiles.offer(mAudioFile);
                            return true;
                        }
                        return false;
                    }
                })
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<Boolean>bindToLifecycle())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean added) {
                        if (added) {
                            mTvLog.setText(
                                    mTvLog.getText() + "\n" + "audio file " + mAudioFile.getName() +
                                            " added");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
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
                    PlayConfig.file(audioFile).streamType(AudioManager.STREAM_VOICE_CALL).build())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean aBoolean) {
                            startPlay();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
        }
    }

    @Override
    public void onError(int error) {
        Toast.makeText(this, "Error code: " + error, Toast.LENGTH_SHORT).show();
    }
}
