package com.github.piasy.rxandroidaudio.example;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.github.piasy.rxandroidaudio.RxAmplitude;
import com.github.piasy.rxandroidaudio.RxAudioPlayer;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import rx.Observable;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements AudioRecorder.OnErrorListener {

    private static final String TAG = "MainActivity";
    public static final int MIN_AUDIO_LENGTH_SECONDS = 2;

    @Bind(R.id.mFlIndicator)
    FrameLayout mFlIndicator;
    @Bind(R.id.mTvPressToSay)
    TextView mTvPressToSay;
    @Bind(R.id.mTvLog)
    TextView mTvLog;
    @Bind(R.id.mTvRecordingHint)
    TextView mTvRecordingHint;

    private AudioRecorder mAudioRecorder;
    private RxAudioPlayer mRxAudioPlayer;

    private File mAudioFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

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

    private Subscription mRecordSubscription;

    private void press2Record() {
        mTvPressToSay.setBackgroundResource(R.drawable.button_press_to_say_pressed_bg);
        mTvRecordingHint.setText(R.string.voice_msg_input_hint_speaking);

        mRecordSubscription = Single.just(true)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<Boolean, Single<Boolean>>() {
                    @Override
                    public Single<Boolean> call(Boolean aBoolean) {
                        Log.d(TAG, "to play audio_record_start: " + R.raw.audio_record_start);
                        return mRxAudioPlayer.play(getApplicationContext(),
                                R.raw.audio_record_start);
                    }
                })
                .doOnSuccess(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        Log.d(TAG, "audio_record_start play finished");
                    }
                })
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        mAudioFile = new File(
                                Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        File.separator + System.nanoTime() + ".m4a");
                        Log.d(TAG, "to prepare record");
                        return mAudioRecorder.prepareRecord(MediaRecorder.AudioSource.MIC,
                                MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.AudioEncoder.AAC,
                                mAudioFile);
                    }
                })
                .doOnSuccess(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        Log.d(TAG, "prepareRecord success");
                    }
                })
                .flatMap(new Func1<Boolean, Single<Boolean>>() {
                    @Override
                    public Single<Boolean> call(Boolean aBoolean) {
                        Log.d(TAG, "to play audio_record_ready: " + R.raw.audio_record_ready);
                        return mRxAudioPlayer.play(getApplicationContext(),
                                R.raw.audio_record_ready);
                    }
                })
                .doOnSuccess(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        Log.d(TAG, "audio_record_ready play finished");
                    }
                })
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        mFlIndicator.post(new Runnable() {
                            @Override
                            public void run() {
                                mFlIndicator.setVisibility(View.VISIBLE);
                            }
                        });
                        return mAudioRecorder.startRecord();
                    }
                })
                .doOnSuccess(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        Log.d(TAG, "startRecord success");
                    }
                })
                .toObservable()
                .flatMap(new Func1<Boolean, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(Boolean aBoolean) {
                        return RxAmplitude.from(mAudioRecorder);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer level) {
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
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

    private void release2Send() {
        mTvPressToSay.setBackgroundResource(R.drawable.button_press_to_say_bg);
        mFlIndicator.setVisibility(View.GONE);

        if (mRecordSubscription != null && !mRecordSubscription.isUnsubscribed()) {
            mRecordSubscription.unsubscribe();
            mRecordSubscription = null;
        }

        Single.just(true).subscribeOn(Schedulers.io()).map(new Func1<Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean aBoolean) {
                int seconds = mAudioRecorder.stopRecord();
                if (seconds >= MIN_AUDIO_LENGTH_SECONDS) {
                    mAudioFiles.offer(mAudioFile);
                    return true;
                }
                return false;
            }
        }).doOnSuccess(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                Log.d(TAG, "to play audio_record_end: " + R.raw.audio_record_end);
                mRxAudioPlayer.play(getApplicationContext(), R.raw.audio_record_end)
                        .toBlocking()
                        .value();
                Log.d(TAG, "audio_record_end play finished");
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean added) {
                if (added) {
                    mTvLog.setText(mTvLog.getText() + "\n" + "audio file " + mAudioFile.getName() +
                            " added");
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
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
}
