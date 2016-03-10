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
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.piasy.rxandroidaudio.AudioRecorder;
import com.github.piasy.rxandroidaudio.RxAudioPlayer;
import com.github.piasy.voiceinputmanager.VoiceInputManager;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class FileActivity extends RxAppCompatActivity
        implements AudioRecorder.OnErrorListener, VoiceInputManager.EventListener {

    public static final int SHOW_INDICATOR_DELAY_MILLIS = 300;

    @Bind(R.id.mFlIndicator)
    FrameLayout mFlIndicator;
    @Bind(R.id.mTvPressToSay)
    TextView mTvPressToSay;
    @Bind(R.id.mTvLog)
    TextView mTvLog;
    @Bind(R.id.mTvRecordingHint)
    TextView mTvRecordingHint;

    private RxAudioPlayer mRxAudioPlayer;
    private VoiceInputManager mVoiceInputManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        ButterKnife.bind(this);

        mIvVoiceIndicators = new ArrayList<>();
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator1));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator2));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator3));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator4));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator5));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator6));
        mIvVoiceIndicators.add(ButterKnife.<ImageView>findById(this, R.id.mIvVoiceIndicator7));

        AudioRecorder audioRecorder = AudioRecorder.getInstance();
        mRxAudioPlayer = RxAudioPlayer.getInstance();
        audioRecorder.setOnErrorListener(this);
        mVoiceInputManager =
                new VoiceInputManager(audioRecorder, Environment.getExternalStorageDirectory(),
                        this);

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
        mFlIndicator.removeCallbacks(mShowIndicatorRunnable);
    }

    private void press2Record() {
        mTvPressToSay.setBackgroundResource(R.drawable.button_press_to_say_pressed_bg);
        mTvRecordingHint.setText(R.string.voice_msg_input_hint_speaking);

        boolean isPermissionsGranted = RxPermissions.getInstance(getApplicationContext())
                .isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                RxPermissions.getInstance(getApplicationContext())
                        .isGranted(Manifest.permission.RECORD_AUDIO);
        if (!isPermissionsGranted) {
            RxPermissions.getInstance(getApplicationContext())
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO)
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean granted) {
                            // not record first time to request permission
                            if (granted) {
                                Toast.makeText(getApplicationContext(), "Permission granted",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Permission not granted",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
        } else {
            recordAfterPermissionGranted();
        }
    }

    private void recordAfterPermissionGranted() {
        mVoiceInputManager.toggleOn();
    }

    private void release2Send() {
        mTvPressToSay.setBackgroundResource(R.drawable.button_press_to_say_bg);
        mFlIndicator.setVisibility(View.GONE);
        mFlIndicator.removeCallbacks(mShowIndicatorRunnable);
        mVoiceInputManager.toggleOff();
    }

    List<ImageView> mIvVoiceIndicators;

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

    private void refreshAudioAmplitudeView(int level) {
        int end = level < mIvVoiceIndicators.size() ? level : mIvVoiceIndicators.size();
        ButterKnife.apply(mIvVoiceIndicators.subList(0, end), VISIBLE);
        ButterKnife.apply(mIvVoiceIndicators.subList(end, mIvVoiceIndicators.size()), INVISIBLE);
    }

    private Queue<File> mAudioFiles = new LinkedList<>();

    @OnClick(R.id.mBtnPlay)
    public void startPlay() {
        mTvLog.setText("");
        if (!mAudioFiles.isEmpty()) {
            File audioFile = mAudioFiles.poll();
            mRxAudioPlayer.play(audioFile)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            startPlay();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
        }
    }

    @Override
    public void onError(int error) {
        Toast.makeText(this, "Error code: " + error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPreparing() {
        mRxAudioPlayer.play(getApplicationContext(), R.raw.audio_record_start).toBlocking().value();
    }

    private Runnable mShowIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            mFlIndicator.setVisibility(View.VISIBLE);
        }
    };

    @Override
    public void onPrepared() {
        mFlIndicator.postDelayed(mShowIndicatorRunnable, SHOW_INDICATOR_DELAY_MILLIS);
        mRxAudioPlayer.play(getApplicationContext(), R.raw.audio_record_ready).toBlocking().value();
    }

    @Override
    public void onStopped() {
        mRxAudioPlayer.play(getApplicationContext(), R.raw.audio_record_end).toBlocking().value();
    }

    @Override
    public void onSend(final File audioFile, int duration) {
        mAudioFiles.offer(audioFile);
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mTvLog.post(new Runnable() {
            @Override
            public void run() {
                mTvLog.setText(mTvLog.getText() + "\n" + "audio file " + audioFile.getName() +
                        " added");
            }
        });
    }

    @Override
    public void onAmplitudeChanged(final int level) {
        mFlIndicator.post(new Runnable() {
            @Override
            public void run() {
                refreshAudioAmplitudeView(level);
            }
        });
    }

    @Override
    public void onExpireCountdown(final int second) {
        mTvRecordingHint.post(new Runnable() {
            @Override
            public void run() {
                mTvRecordingHint.setText(String.format(
                        getString(R.string.voice_msg_input_hint_time_limited_formatter), second));
            }
        });
    }
}
